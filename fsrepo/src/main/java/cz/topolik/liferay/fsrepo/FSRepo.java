/**
 * Copyright (c) 2012-2013 Tomáš Polešovský
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package cz.topolik.liferay.fsrepo;

import com.liferay.portal.NoSuchRepositoryEntryException;
import com.liferay.portal.kernel.cache.Lifecycle;
import com.liferay.portal.kernel.cache.ThreadLocalCache;
import com.liferay.portal.kernel.cache.ThreadLocalCacheManager;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.BaseRepositoryImpl;
import com.liferay.portal.kernel.repository.RepositoryException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Query;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Lock;
import com.liferay.portal.model.RepositoryEntry;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.persistence.RepositoryEntryUtil;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cz.topolik.liferay.fsrepo.Constants.*;

/**
 * @author Tomas Polesovsky
 */
public class FSRepo extends BaseRepositoryImpl {

    private static Log log = LogFactoryUtil.getLog(FSRepo.class);

    private PortalMapper mapper;

    static enum FILE_TYPES {
        FOLDER, FILE, EVERYTHING
    }

    @Override
    public String[] getSupportedConfigurations() {
        return new String[]{"FILESYSTEM"};
    }

    @Override
    public String[][] getSupportedParameters() {
        return new String[][]{{ROOT_FOLDER, ADD_GUEST_PERMISSIONS, ADD_GROUP_PERMISSIONS}};
    }

    @Override
    public void initRepository() throws PortalException, SystemException {
        try {
            if (log.isInfoEnabled()) {
                log.info("Initializing FileSystemRepository for: " + getRootFolder());
            }

            mapper = new PortalMapper();

            mapper.setAddGuestPermissions(GetterUtil.getBoolean(getTypeSettingsProperties().getProperty(ADD_GUEST_PERMISSIONS), true));
            mapper.setAddGroupPermissions(GetterUtil.getBoolean(getTypeSettingsProperties().getProperty(ADD_GROUP_PERMISSIONS), true));
            mapper.setCompanyId(getCompanyId());
            mapper.setRepositoryId(getRepositoryId());
            mapper.setGroupId(getGroupId());
            mapper.setRootFolder(getRootFolder());

            mapper.init();

        } catch (FileNotFoundException e) {
            log.error(e);
            throw new SystemException(e);
        } catch (PortalException e) {
            log.error(e);
            throw e;
        } catch (SystemException e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    public List<Object> getFoldersAndFileEntries(long folderId, int start, int end, OrderByComparator obc) throws SystemException {
        start = start == QueryUtil.ALL_POS ? 0 : start;
        end = end == QueryUtil.ALL_POS ? Integer.MAX_VALUE : end;

        List<Object> result = new ArrayList<Object>();
        try {
            File systemFolder = mapper.folderIdToFile(folderId);
            if (systemFolder.canRead()) {
                for (File file : loadFilesFromDisk(systemFolder, FILE_TYPES.EVERYTHING)) {
                    if (file.canRead()) {
                        if (file.isDirectory()) {
                            Folder f = mapper.fileToFolder(file);
                            if (f != null) {
                                result.add(f);
                            }
                        } else {
                            FileEntry f = mapper.fileToFileEntry(file);
                            if (f != null) {
                                result.add(f);
                            }
                        }
                    }
                    if (obc == null && result.size() > end) {
                        return result.subList(start < 0 ? 0 : start, end > result.size() ? result.size() : end);
                    }
                }
            }

        } catch (PortalException ex) {
            log.error(ex);
            throw new SystemException(ex);
        }

        if (obc != null) {
            Collections.sort(result, obc);
        }
        result = result.subList(start < 0 ? 0 : start, end > result.size() ? result.size() : end);
        return result;
    }

    @Override
    public List<Object> getFoldersAndFileEntries(long folderId, String[] mimeTypes, int start, int end, OrderByComparator obc) throws SystemException {
        return getFoldersAndFileEntries(folderId, start, end, obc);
    }

    @Override
    public FileEntry addFileEntry(long folderId, String sourceFileName, String mimeType, String title, String description, String changeLog, InputStream is, long size, ServiceContext serviceContext) throws SystemException, PortalException {
        File directory = mapper.folderIdToFile(folderId);

        if (directory.exists() && directory.canWrite()) {
            File file = new File(directory, sourceFileName);
            try {
                StreamUtil.transfer(is, new FileOutputStream(file), true);

                return mapper.fileToFileEntry(file);
            } catch (Exception ex) {
                log.error(ex);
                throw new SystemException(ex);
            }
        }

        throw new SystemException("Directory " + directory + " does not exist or cannot add files inside!");
    }

    @Override
    public Folder addFolder(long parentFolderId, String title, String description, ServiceContext serviceContext) throws SystemException, PortalException {
        File subDir = mapper.folderIdToFile(parentFolderId);

        if (subDir.exists() && subDir.canWrite()) {
            File folder = new File(subDir, title);
            folder.mkdir();

            return mapper.fileToFolder(folder);
        }

        throw new SystemException("Parent directory " + subDir + " does not exist or cannot add folders inside!!");
    }

    @Override
    public FileVersion cancelCheckOut(long fileEntryId) {
        return null;
    }

    @Override
    public void checkInFileEntry(long fileEntryId, boolean major, String changeLog, ServiceContext serviceContext) {

    }

    @Override
    public void checkInFileEntry(long fileEntryId, String lockUuid, ServiceContext serviceContext) {

    }

    @Override
    public FileEntry checkOutFileEntry(long fileEntryId, ServiceContext serviceContext) {
        return null;
    }

    @Override
    public FileEntry copyFileEntry(long groupId, long fileEntryId, long destFolderId, ServiceContext serviceContext) throws SystemException, PortalException {
        File srcFile = mapper.fileEntryIdToFile(fileEntryId);
        File destDir = mapper.folderIdToFile(destFolderId);

        if (!srcFile.exists()) {
            throw new SystemException("Source file " + srcFile + " cannot be read!");
        }
        if (!destDir.exists() || !destDir.canWrite()) {
            throw new SystemException("Cannot write into destination directory " + destDir);
        }

        File dstFile = new File(destDir, srcFile.getName());
        try {
            StreamUtil.transfer(new FileInputStream(srcFile), new FileOutputStream(dstFile), true);

            return mapper.fileToFileEntry(dstFile);
        } catch (Exception ex) {
            log.error(ex);
            throw new SystemException(ex);
        }
    }

    @Override
    public void deleteFileEntry(long fileEntryId) throws SystemException, PortalException {
        File file = mapper.fileEntryIdToFile(fileEntryId);

        if (!file.exists() || !file.canWrite()) {
            throw new SystemException("File doesn't exist or cannot be modified: " + file);
        }

        file.delete();
        RepositoryEntryUtil.remove(fileEntryId);
    }

    @Override
    public void deleteFolder(long folderId) throws SystemException, PortalException {
        File folder = mapper.folderIdToFile(folderId);

        if (!folder.exists() || !folder.canWrite()) {
            throw new SystemException("Folder doesn't exist or cannot be modified " + folder);
        }

        folder.delete();
        RepositoryEntryUtil.remove(folderId);
    }

    @Override
    public List<FileEntry> getFileEntries(long folderId, int start, int end, OrderByComparator obc) throws SystemException {
        start = start == QueryUtil.ALL_POS ? 0 : start;
        end = end == QueryUtil.ALL_POS ? Integer.MAX_VALUE : end;

        List<FileEntry> result = new ArrayList<FileEntry>();
        try {
            File systemFolder = mapper.folderIdToFile(folderId);
            if (systemFolder.canRead()) {
                for (File file : loadFilesFromDisk(systemFolder, FILE_TYPES.FILE)) {
                    if (!file.isDirectory()) {
                        FileEntry f = mapper.fileToFileEntry(file);
                        if (f != null) {
                            result.add(f);
                        }
                    }
                    if (obc == null && result.size() > end) {
                        return result.subList(start < 0 ? 0 : start, end > result.size() ? result.size() : end);
                    }
                }
            }

        } catch (PortalException ex) {
            log.error(ex);
            throw new SystemException(ex);
        }

        if (obc != null) {
            Collections.sort(result, obc);
        }
        result = result.subList(start < 0 ? 0 : start, end > result.size() ? result.size() : end);
        return result;
    }

    @Override
    public List<FileEntry> getFileEntries(long folderId, long fileEntryTypeId, int start, int end, OrderByComparator obc) throws SystemException {
        // TODO we don't support file entry types
        return getFileEntries(folderId, start, end, obc);
    }

    @Override
    public List<FileEntry> getFileEntries(long folderId, String[] mimeTypes, int start, int end, OrderByComparator obc) throws SystemException {
        // TODO: we don't support file entry mime types
        return getFileEntries(folderId, start, end, obc);
    }

    @Override
    public FileEntry getFileEntry(long fileEntryId) throws SystemException, PortalException {
        return mapper.fileToFileEntry(mapper.fileEntryIdToFile(fileEntryId));
    }

    @Override
    public FileEntry getFileEntry(long folderId, String title) throws SystemException, PortalException {
        File file = new File(mapper.folderIdToFile(folderId), title);

        if (!file.exists()) {
            throw new NoSuchFileEntryException("File " + file + " doesn't exist!");
        }

        return mapper.fileToFileEntry(file);
    }

    @Override

    public FileEntry getFileEntryByUuid(String uuid) throws SystemException, PortalException {
        try {
            RepositoryEntry repositoryEntry = RepositoryEntryUtil.findByUUID_G(
                    uuid, getGroupId());

            return getFileEntry(repositoryEntry.getRepositoryEntryId());
        } catch (NoSuchRepositoryEntryException nsree) {
            throw new NoSuchFileEntryException(nsree);
        } catch (SystemException se) {
            throw se;
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public FileVersion getFileVersion(long fileVersionId) throws SystemException, PortalException {
        return mapper.fileToFileVersion(mapper.fileVersionIdToFile(fileVersionId));
    }

    @Override
    public Folder getFolder(long folderId) throws SystemException, PortalException {
        return mapper.fileToFolder(mapper.folderIdToFile(folderId));
    }

    @Override
    public Folder getFolder(long parentFolderId, String title) throws SystemException, PortalException {
        File folder = new File(mapper.folderIdToFile(parentFolderId), title);

        if (!folder.exists()) {
            throw new NoSuchFolderException("Folder " + folder + " doesn't exist!");
        }

        return mapper.fileToFolder(folder);
    }

    @Override
    public List<Folder> getFolders(long parentFolderId, boolean includeMountFolders, int start, int end, OrderByComparator obc) throws SystemException, PortalException {
        start = start == QueryUtil.ALL_POS ? 0 : start;
        end = end == QueryUtil.ALL_POS ? Integer.MAX_VALUE : end;

        File dir = mapper.folderIdToFile(parentFolderId);
        if (dir.canRead()) {
            List<Folder> result = new ArrayList<Folder>();
            for (File subDir : loadFilesFromDisk(dir, FILE_TYPES.FOLDER)) {
                Folder f = mapper.fileToFolder(subDir);
                if (f != null) {
                    result.add(f);
                }
            }

            if (obc != null) {
                Collections.sort(result, obc);
            }

            result = result.subList(start < 0 ? 0 : start, end > result.size() ? result.size() : end);

            return result;

        }
        return new ArrayList<Folder>();
    }

    public List<FileEntry> getFoldersFileEntries(List<Long> allowedFolderIds, int status) throws SystemException {
        List<FileEntry> result = new ArrayList<FileEntry>();

        for (Long folderId : allowedFolderIds) {
            result.addAll(getFileEntries(folderId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null));
        }

        return result;
    }

    @Override
    public List<Folder> getMountFolders(long parentFolderId, int start, int end, OrderByComparator obc) {
        return new ArrayList<Folder>();
    }

    @Override
    public List<Long> getSubfolderIds(long folderId, boolean recurse) throws SystemException {
        try {
            List<Long> result = new ArrayList();

            List<Folder> folders = getFolders(folderId, false, 0, Integer.MAX_VALUE, null);
            for (Folder folder : folders) {
                result.add(folder.getFolderId());

                if (recurse) {
                    result.addAll(getSubfolderIds(folder.getFolderId(), recurse));
                }
            }

            return result;
        } catch (PortalException ex) {
            throw new SystemException(ex);
        }
    }

    @Override
    public Lock lockFolder(long folderId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock lockFolder(long folderId, String owner, boolean inheritable, long expirationTime) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileEntry moveFileEntry(long fileEntryId, long newFolderId, ServiceContext serviceContext) throws SystemException, PortalException {
        File fileToMove = mapper.fileEntryIdToFile(fileEntryId);
        File parentFolder = mapper.folderIdToFile(newFolderId);
        File dstFile = new File(parentFolder, fileToMove.getName());

        if (!fileToMove.exists()) {
            throw new SystemException("Source file doesn't exist: " + fileToMove);
        }
        if (!parentFolder.exists()) {
            throw new SystemException("Destination parent folder doesn't exist: " + parentFolder);
        }
        if (!parentFolder.exists()) {
            throw new SystemException("Destination file does exist: " + dstFile);
        }
        if (fileToMove.canWrite() && parentFolder.canWrite()) {
            if (!fileToMove.renameTo(dstFile)) {
                throw new SystemException("Moving was not successful (don't know why) [from, to]: [" + fileToMove + ", " + dstFile + "]");
            }

            RepositoryEntry repositoryEntry = RepositoryEntryUtil.fetchByPrimaryKey(fileEntryId);
            RepositoryEntryUtil.update(repositoryEntry);
            try {
                mapper.saveFileToExpando(repositoryEntry, dstFile);
            } catch (FileNotFoundException ex) {
                throw new SystemException(ex.getMessage(), ex);
            }

            return mapper.fileToFileEntry(dstFile);
        }

        throw new SystemException("Doesn't have rights to move the file [src, toParentDir]: [" + fileToMove + ", " + parentFolder + "]");
    }

    @Override
    public Folder moveFolder(long folderId, long newParentFolderId, ServiceContext serviceContext) throws SystemException, PortalException {
        File folderToMove = mapper.folderIdToFile(folderId);
        File parentFolder = mapper.folderIdToFile(newParentFolderId);
        File dstFolder = new File(parentFolder, folderToMove.getName());

        if (!folderToMove.exists()) {
            throw new SystemException("Source folder doesn't exist: " + folderToMove);
        }
        if (!parentFolder.exists()) {
            throw new SystemException("Destination parent folder doesn't exist: " + parentFolder);
        }
        if (!parentFolder.exists()) {
            throw new SystemException("Destination folder does exist: " + dstFolder);
        }
        if (folderToMove.canWrite() && parentFolder.canWrite()) {
            if (!folderToMove.renameTo(dstFolder)) {
                throw new SystemException("Moving was not successful (don't know why) [from, to]: [" + folderToMove + ", " + dstFolder + "]");
            }

            RepositoryEntry repositoryEntry = RepositoryEntryUtil.fetchByPrimaryKey(folderId);
            RepositoryEntryUtil.update(repositoryEntry);
            try {
                mapper.saveFileToExpando(repositoryEntry, dstFolder);
            } catch (FileNotFoundException ex) {
                throw new SystemException(ex.getMessage(), ex);
            }

            return mapper.fileToFolder(dstFolder);
        }

        throw new SystemException("Doesn't have rights to move the directory [srcDir, toParentDir]: [" + folderToMove + ", " + parentFolder + "]");
    }

    @Override
    public void revertFileEntry(long fileEntryId, String version, ServiceContext serviceContext) {
        // java file system doesn't support versions
    }

    @Override
    public void unlockFolder(long folderId, String lockUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileEntry updateFileEntry(long fileEntryId, String sourceFileName, String mimeType, String title, String description, String changeLog, boolean majorVersion, InputStream is, long size, ServiceContext serviceContext) throws SystemException, PortalException {
        File file = mapper.fileEntryIdToFile(fileEntryId);
        File dstFile = new File(file.getParentFile(), title);

        boolean toRename = false;
        if (!file.canWrite()) {
            throw new SystemException("Cannot modify file: " + file);
        }

        if (Validator.isNotNull(title) && !title.equals(file.getName())) {
            if (dstFile.exists()) {
                throw new SystemException("Destination file already exists: " + dstFile);
            }
            toRename = true;
        }

        if (size > 0) {
            try {
                StreamUtil.transfer(is, new FileOutputStream(file));
            } catch (IOException ex) {
                log.error(ex);
            }
        }

        if (toRename) {
            file.renameTo(dstFile);

            RepositoryEntry repositoryEntry = RepositoryEntryUtil.fetchByPrimaryKey(fileEntryId);
            RepositoryEntryUtil.update(repositoryEntry);
            try {
                mapper.saveFileToExpando(repositoryEntry, dstFile);
            } catch (FileNotFoundException ex) {
                throw new SystemException(ex.getMessage(), ex);
            }
        }

        return mapper.fileToFileEntry(dstFile);
    }

    @Override
    public Folder updateFolder(long folderId, String title, String description, ServiceContext serviceContext) throws PortalException, SystemException {
        if (title.contains(File.separator)) {
            throw new SystemException("Invalid character " + File.separator + " in the title! [title]: [" + title + "]");
        }

        File folder = mapper.folderIdToFile(folderId);
        if (!folder.exists() || !folder.canWrite()) {
            throw new SystemException("Folder doesn't exist or cannot be changed: " + folder);
        }

        File newFolder = new File(folder.getParentFile(), title);
        folder.renameTo(newFolder);

        return mapper.fileToFolder(newFolder);
    }

    @Override
    public boolean verifyFileEntryCheckOut(long fileEntryId, String lockUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean verifyInheritableLock(long folderId, String lockUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getFoldersAndFileEntriesCount(long folderId) throws SystemException {
        try {
            File dir = mapper.folderIdToFile(folderId);
            if (dir == null) {
                return 0;
            }
            return loadFilesFromDisk(dir, FILE_TYPES.EVERYTHING).size();
        } catch (PortalException e) {
            throw new SystemException(e);
        }
    }

    @Override
    public int getFoldersAndFileEntriesCount(long folderId, String[] mimeTypes) throws PortalException, SystemException {
        return getFoldersAndFileEntriesCount(folderId);
    }

    @Override
    public int getFileEntriesCount(long folderId) throws PortalException, SystemException {
        File dir = mapper.folderIdToFile(folderId);
        if (dir == null) {
            return 0;
        }
        return loadFilesFromDisk(dir, FILE_TYPES.FILE).size();
    }

    @Override
    public int getFileEntriesCount(long folderId, long fileEntryTypeId) throws PortalException, SystemException {
        return getFileEntries(folderId, fileEntryTypeId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null).size();
    }

    @Override
    public int getFileEntriesCount(long folderId, String[] mimeTypes) throws PortalException, SystemException {
        return getFileEntries(folderId, mimeTypes, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null).size();
    }

    @Override
    public int getFoldersCount(long parentFolderId, boolean includeMountfolders) throws PortalException, SystemException {
        File dir = mapper.folderIdToFile(parentFolderId);
        return loadFilesFromDisk(dir, FILE_TYPES.FOLDER).size();
    }

    @Override
    public int getFoldersFileEntriesCount(List<Long> folderIds, int status) throws PortalException, SystemException {
        int result = 0;
        for (Long folderId : folderIds) {
            result += getFileEntriesCount(folderId);
        }
        return result;
    }

    @Override
    public int getMountFoldersCount(long parentFolderId) throws PortalException, SystemException {
        return getMountFolders(parentFolderId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null).size();
    }

    @Override
    public void getSubfolderIds(List<Long> folderIds, long folderId) throws PortalException, SystemException {
        //TODO: where is it used from?
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock refreshFileEntryLock(String lockUuid, long companyId, long expirationTime) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock refreshFolderLock(String lockUuid, long companyId, long expirationTime) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Hits search(long creatorUserId, int status, int start, int end) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Hits search(long creatorUserId, long folderId, String[] mimeTypes, int status, int start, int end) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Hits search(SearchContext searchContext, Query query) throws SearchException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileEntry checkOutFileEntry(long fileEntryId, String owner, long expirationTime, ServiceContext serviceContext) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }


    protected File getRootFolder() throws FileNotFoundException {
        String file = getTypeSettingsProperties().getProperty(Constants.ROOT_FOLDER);
        if (file == null) {
            throw new RuntimeException("There is no ROOT_FOLDER configured for the repository [repositoryId]: [" + getRepositoryId() + "]");
        }

        File f = new File(file);

        if (!f.exists()) {
            throw new FileNotFoundException("Root folder no longer exists on the file system [folderPath, repositoryId] [" + f.getAbsolutePath() + ", " + getRepositoryId() + "]");
        }
        if (!f.canRead() || !f.canExecute()) {
            throw new FileNotFoundException("Root folder cannot be read [folderPath, repositoryId] [" + f.getAbsolutePath() + ", " + getRepositoryId() + "]");
        }
        if (!f.canWrite() && log.isWarnEnabled()) {
            log.warn("Repository mounted as read-only [folderPath, repositoryId] [" + f.getAbsolutePath() + ", " + getRepositoryId() + "]");
        }

        return f;
    }

    protected List<File> loadFilesFromDisk(File dir, final FILE_TYPES fileType) {
        List<File> result = new ArrayList<File>();
        if (!dir.canRead()) {
            return result;
        }

        String cacheKey = dir.getAbsolutePath();
        File[] cached = getFromCache(cacheKey);
        if (cached == null) {
            cached = dir.listFiles();
            putToCache(cacheKey, cached);
        }

        for (File f : cached) {
            switch (fileType) {
                case FILE: {
                    if (!f.isDirectory()) {
                        result.add(f);
                    }
                    break;
                }
                case FOLDER: {
                    if (f.isDirectory()) {
                        result.add(f);
                    }
                    break;
                }
                case EVERYTHING:
                default: {
                    result.add(f);
                    break;
                }
            }
        }
        return result;
    }

    protected File[] getFromCache(String cacheKey) {
        String cacheName = new Exception().getStackTrace()[1].getMethodName();
        ThreadLocalCache<File[]> threadLocalCache =
                ThreadLocalCacheManager.getThreadLocalCache(
                        Lifecycle.REQUEST, cacheName);
        return threadLocalCache != null ? threadLocalCache.get(cacheKey) : null;
    }

    protected void putToCache(String cacheKey, File[] value) {
        String cacheName = new Exception().getStackTrace()[1].getMethodName();
        ThreadLocalCache<File[]> threadLocalCache =
                ThreadLocalCacheManager.getThreadLocalCache(
                        Lifecycle.REQUEST, cacheName);
        threadLocalCache.put(cacheKey, value);
    }
}
