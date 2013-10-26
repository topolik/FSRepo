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
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.BaseRepositoryImpl;
import com.liferay.portal.kernel.repository.RepositoryException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.search.*;
import com.liferay.portal.kernel.util.*;
import com.liferay.portal.model.Lock;
import com.liferay.portal.model.RepositoryEntry;
import com.liferay.portal.security.auth.PrincipalThreadLocal;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.service.ResourceLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.service.persistence.RepositoryEntryUtil;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;
import com.liferay.portlet.documentlibrary.NoSuchFileVersionException;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.service.DLAppLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.persistence.DLFolderUtil;
import com.liferay.portlet.expando.model.ExpandoColumn;
import com.liferay.portlet.expando.model.ExpandoColumnConstants;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoColumnLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import cz.topolik.liferay.fsrepo.model.FileSystemFileEntry;
import cz.topolik.liferay.fsrepo.model.FileSystemFileVersion;
import cz.topolik.liferay.fsrepo.model.FileSystemFolder;

import java.io.File;
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
    private FSRepoLocalRepositoryImpl localRepositoryImpl;
    private ExpandoColumn expandoColumn;

    public FSRepo() {
        localRepositoryImpl = new FSRepoLocalRepositoryImpl(this);
    }

    @Override
    public void initRepository() throws PortalException, SystemException {
        try {
            if (log.isInfoEnabled()) {
                log.info("Initializing FileSystemRepository for: " + getRootFolder());
            }

            expandoColumn = ExpandoColumnLocalServiceUtil.getDefaultTableColumn(getCompanyId(), RepositoryEntry.class.getName(), Constants.ABSOLUTE_PATH);
            if (expandoColumn == null) {
                ExpandoTable table = ExpandoTableLocalServiceUtil.fetchDefaultTable(getCompanyId(), RepositoryEntry.class.getName());
                if (table == null) {
                    table = ExpandoTableLocalServiceUtil.addDefaultTable(getCompanyId(), RepositoryEntry.class.getName());
                }
                expandoColumn = ExpandoColumnLocalServiceUtil.addColumn(table.getTableId(), Constants.ABSOLUTE_PATH, ExpandoColumnConstants.STRING);
            }

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

    public File getRootFolder() throws FileNotFoundException {
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

    @Override
    public List<Object> getFoldersAndFileEntries(long folderId, int start, int end, OrderByComparator obc) throws SystemException {
        start = start == QueryUtil.ALL_POS ? 0 : start;
        end = end == QueryUtil.ALL_POS ? Integer.MAX_VALUE : end;

        List<Object> result = new ArrayList<Object>();
        try {
            File systemFolder = folderIdToFile(folderId);
            if (systemFolder.canRead()) {
                for (File file : loadFilesFromDisk(systemFolder, 0)) {
                    if (file.canRead()) {
                        if (file.isDirectory()) {
                            Folder f = fileToFolder(file);
                            if (f != null) {
                                result.add(f);
                            }
                        } else {
                            FileEntry f = fileToFileEntry(file);
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
    public List<Object> getFoldersAndFileEntries(long folderId, String[] mimeTypes, int start, int end, OrderByComparator obc) throws PortalException, SystemException {
        return getFoldersAndFileEntries(folderId, start, end, obc);
    }

    @Override
    public int getFoldersAndFileEntriesCount(long folderId) throws SystemException {
        try {
            File dir = folderIdToFile(folderId);
            if(dir == null){
                return 0;
            }
            return loadFilesFromDisk(dir, 0).size();
        } catch (PortalException e) {
            throw new SystemException(e);
        }
    }

    @Override
    public int getFoldersAndFileEntriesCount(long folderId, String[] mimeTypes) throws PortalException, SystemException {
        return getFoldersAndFileEntriesCount(folderId);
    }

    public String[] getSupportedConfigurations() {
        return new String[]{"FILESYSTEM"};
    }

    public String[][] getSupportedParameters() {
        return new String[][]{{ROOT_FOLDER, ADD_GUEST_PERMISSIONS, ADD_GROUP_PERMISSIONS}};
    }

    public FileEntry addFileEntry(long folderId, String sourceFileName, String mimeType, String title, String description, String changeLog, InputStream is, long size, ServiceContext serviceContext) throws PortalException, SystemException {
        File directory = folderIdToFile(folderId);
        if (directory.exists() && directory.canWrite()) {
            File file = new File(directory, sourceFileName);
            try {
                StreamUtil.transfer(is, new FileOutputStream(file), true);
                return fileToFileEntry(file);
            } catch (Exception ex) {
                log.error(ex);
                throw new SystemException(ex);
            }
        } else {
            throw new SystemException("Directory " + directory + " cannot be read!");
        }
    }

    public Folder addFolder(long parentFolderId, String title, String description, ServiceContext serviceContext) throws PortalException, SystemException {
        File subDir = folderIdToFile(parentFolderId);
        if (subDir.exists() && subDir.canWrite()) {
            File folder = new File(subDir, title);
            folder.mkdir();
            return fileToFolder(folder);
        } else {
            throw new SystemException("Parent directory " + subDir + " cannot be read!");
        }
    }

    public void cancelCheckOut(long fileEntryId) throws PortalException, SystemException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void checkInFileEntry(long fileEntryId, boolean major, String changeLog, ServiceContext serviceContext) throws PortalException, SystemException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void checkInFileEntry(long fileEntryId, String lockUuid) throws PortalException, SystemException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // 6.1 CE
    public FileEntry checkOutFileEntry(long fileEntryId) throws PortalException, SystemException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // 6.1 EE
    public FileEntry checkOutFileEntry(long fileEntryId, ServiceContext serviceContext) throws PortalException, SystemException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // 6.1 CE
    public FileEntry checkOutFileEntry(long fileEntryId, String owner, long expirationTime) throws PortalException, SystemException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // 6.1 EE
    public FileEntry checkOutFileEntry(long fileEntryId, String owner, long expirationTime, ServiceContext serviceContext) throws PortalException, SystemException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public FileEntry copyFileEntry(long groupId, long fileEntryId, long destFolderId, ServiceContext serviceContext) throws PortalException, SystemException {
        File srcFile = fileEntryIdToFile(fileEntryId);
        File destDir = folderIdToFile(destFolderId);
        if (!srcFile.exists()) {
            throw new SystemException("Source file " + srcFile + " cannot be read!");
        }
        if (!destDir.exists() || !destDir.canWrite()) {
            throw new SystemException("Cannot write into destination directory " + destDir);
        }

        File dstFile = new File(destDir, srcFile.getName());
        try {
            StreamUtil.transfer(new FileInputStream(srcFile), new FileOutputStream(dstFile), true);
            return fileToFileEntry(dstFile);
        } catch (Exception ex) {
            log.error(ex);
            throw new SystemException(ex);
        }
    }

    public void deleteFileEntry(long fileEntryId) throws PortalException, SystemException {
        File file = fileEntryIdToFile(fileEntryId);
        if (!file.exists() || !file.canWrite()) {
            throw new SystemException("File doesn't exist or cannot be modified " + file);
        }

        file.delete();
        RepositoryEntryUtil.remove(fileEntryId);
    }

    public void deleteFolder(long folderId) throws PortalException, SystemException {
        File folder = folderIdToFile(folderId);
        if (!folder.exists() || !folder.canWrite()) {
            throw new SystemException("Folder doesn't exist or cannot be modified " + folder);
        }

        folder.delete();
        RepositoryEntryUtil.remove(folderId);
    }

    public List<FileEntry> getFileEntries(long folderId, int start, int end, OrderByComparator obc) throws SystemException {
        start = start == QueryUtil.ALL_POS ? 0 : start;
        end = end == QueryUtil.ALL_POS ? Integer.MAX_VALUE : end;

        List<FileEntry> result = new ArrayList<FileEntry>();
        try {
            File systemFolder = folderIdToFile(folderId);
            if (systemFolder.canRead()) {
                for (File file : loadFilesFromDisk(systemFolder, 2)) {
                    if (!file.isDirectory()) {
                        FileEntry f = fileToFileEntry(file);
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

    public List<FileEntry> getFileEntries(long folderId, long fileEntryTypeId, int start, int end, OrderByComparator obc) throws SystemException {
        return new ArrayList<FileEntry>();
    }

    public List<FileEntry> getFileEntries(long folderId, String[] mimeTypes, int start, int end, OrderByComparator obc) throws PortalException, SystemException {
        return getFileEntries(folderId, start, end, obc);
    }

    public int getFileEntriesCount(long folderId) throws SystemException {
        try {
            File dir = folderIdToFile(folderId);
            if(dir == null){
                return 0;
            }
            return loadFilesFromDisk(dir, 2).size();
        } catch (PortalException e) {
            throw new SystemException(e);
        }
    }

    public int getFileEntriesCount(long folderId, long fileEntryTypeId) throws SystemException {
        return getFileEntriesCount(folderId);
    }

    public int getFileEntriesCount(long folderId, String[] mimeTypes) throws PortalException, SystemException {
        return getFileEntriesCount(folderId);
    }

    public FileEntry getFileEntry(long fileEntryId) throws PortalException, SystemException {
        return fileToFileEntry(fileEntryIdToFile(fileEntryId));
    }

    public FileEntry getFileEntry(long folderId, String title) throws PortalException, SystemException {
        return fileToFileEntry(new File(folderIdToFile(folderId), title));
    }

    public FileEntry getFileEntryByUuid(String uuid) throws PortalException, SystemException {
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

    public FileVersion getFileVersion(long fileVersionId) throws PortalException, SystemException {
        return fileToFileVersion(fileVersionIdToFile(fileVersionId));
    }

    public Folder getFolder(long folderId) throws PortalException, SystemException {
        return fileToFolder(folderIdToFile(folderId));
    }

    public Folder getFolder(long parentFolderId, String title) throws PortalException, SystemException {
        Folder f = fileToFolder(new File(folderIdToFile(parentFolderId), title));
        return f;
    }

    public List<Folder> getFolders(long parentFolderId, boolean includeMountFolders, int start, int end, OrderByComparator obc) throws PortalException, SystemException {
        start = start == QueryUtil.ALL_POS ? 0 : start;
        end = end == QueryUtil.ALL_POS ? Integer.MAX_VALUE : end;
        String fileSystemDirectory = folderIdToFile(parentFolderId).getAbsolutePath();
        File dir = new File(fileSystemDirectory);
        if (dir.canRead()) {
            List<Folder> result = new ArrayList<Folder>();
            for (File subDir : loadFilesFromDisk(dir, 1)) {
                Folder f = fileToFolder(subDir);
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

    public int getFoldersCount(long parentFolderId, boolean includeMountfolders) throws PortalException, SystemException {
        try {
            File dir = folderIdToFile(parentFolderId);
            return loadFilesFromDisk(dir, 1).size();
        } catch (PortalException e) {
            throw new SystemException(e);
        }
    }

    public int getFoldersFileEntriesCount(List<Long> folderIds, int status) throws SystemException {
        int result = 0;
        for (Long folderId : folderIds) {
            result += getFileEntriesCount(folderId);
        }
        return result;
    }

    public List<Folder> getMountFolders(long parentFolderId, int start, int end, OrderByComparator obc) throws SystemException {
        return new ArrayList<Folder>();
    }

    public int getMountFoldersCount(long parentFolderId) throws SystemException {
        return 0;
    }

    public void getSubfolderIds(List<Long> folderIds, long folderId) throws SystemException {
        //TODO: where is it used?
        throw new UnsupportedOperationException("Not supported yet.");
    }

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

    public Lock lockFolder(long folderId) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    public Lock lockFolder(long folderId, String owner, boolean inheritable, long expirationTime) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    public FileEntry moveFileEntry(long fileEntryId, long newFolderId, ServiceContext serviceContext) throws PortalException, SystemException {
        File fileToMove = fileEntryIdToFile(fileEntryId);
        File parentFolder = folderIdToFile(newFolderId);
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
            RepositoryEntryUtil.update(repositoryEntry, true);
            try {
                saveFileToExpando(repositoryEntry, dstFile);
            } catch (FileNotFoundException ex) {
                throw new SystemException(ex.getMessage(), ex);
            }


            return fileToFileEntry(dstFile);
        } else {
            throw new SystemException("Doesn't have rights to move the file [src, toParentDir]: [" + fileToMove + ", " + parentFolder + "]");
        }
    }

    public Folder moveFolder(long folderId, long newParentFolderId, ServiceContext serviceContext) throws PortalException, SystemException {
        File folderToMove = folderIdToFile(folderId);
        File parentFolder = folderIdToFile(newParentFolderId);
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
            RepositoryEntryUtil.update(repositoryEntry, true);
            try {
                saveFileToExpando(repositoryEntry, dstFolder);
            } catch (FileNotFoundException ex) {
                throw new SystemException(ex.getMessage(), ex);
            }

            return fileToFolder(dstFolder);
        } else {
            throw new SystemException("Doesn't have rights to move the directory [srcDir, toParentDir]: [" + folderToMove + ", " + parentFolder + "]");
        }
    }

    public Lock refreshFileEntryLock(String lockUuid, long expirationTime) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    public Lock refreshFolderLock(String lockUuid, long expirationTime) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    public void revertFileEntry(long fileEntryId, String version, ServiceContext serviceContext) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    public Hits search(SearchContext searchContext, Query query) throws SearchException {
        // TODO: implement indexing and add specific FILE_SYSTEM key into the query
        System.out.println("Searched: " + query);
        return SearchEngineUtil.search(searchContext, query);
    }

    public void unlockFolder(long folderId, String lockUuid) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    public FileEntry updateFileEntry(long fileEntryId, String sourceFileName, String mimeType, String title, String description, String changeLog, boolean majorVersion, InputStream is, long size, ServiceContext serviceContext) throws PortalException, SystemException {
        File file = fileEntryIdToFile(fileEntryId);
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
            RepositoryEntryUtil.update(repositoryEntry, true);
            try {
                saveFileToExpando(repositoryEntry, dstFile);
            } catch (FileNotFoundException ex) {
                throw new SystemException(ex.getMessage(), ex);
            }
        }
        return fileToFileEntry(dstFile);
    }

    public Folder updateFolder(long folderId, String title, String description, ServiceContext serviceContext) throws PortalException, SystemException {
        if (title.contains(File.separator)) {
            throw new SystemException("Invalid character " + File.separator + " in the title! [title]: [" + title + "]");
        }
        File folder = folderIdToFile(folderId);
        if (!folder.exists() || !folder.canWrite()) {
            throw new SystemException("Folder doesn't exist or cannot be changed: " + folder);
        }
        File newFolder = new File(folder.getParentFile(), title);
        folder.renameTo(newFolder);
        return fileToFolder(newFolder);
    }

    public boolean verifyFileEntryCheckOut(long fileEntryId, String lockUuid) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    public boolean verifyInheritableLock(long folderId, String lockUuid) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    /* *****************
     *
     */


    protected RepositoryEntry findEntryFromExpando(File file) throws SystemException {
        String className = RepositoryEntry.class.getName();
        long companyId = getCompanyId();
        ExpandoColumn col = ExpandoColumnLocalServiceUtil.getDefaultTableColumn(companyId, className, Constants.ABSOLUTE_PATH);

        DynamicQuery query = DynamicQueryFactoryUtil.forClass(ExpandoValue.class, PortalClassLoaderUtil.getClassLoader());
        query.add(RestrictionsFactoryUtil.eq("columnId", col.getColumnId()));

        try {
            query.add(RestrictionsFactoryUtil.eq("data", getCombinedExpandoValue(file)));
        } catch (FileNotFoundException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }

        List<ExpandoValue> result = (List<ExpandoValue>) ExpandoValueLocalServiceUtil.dynamicQuery(query);
        if (result.size() == 0) {
            return null;
        }
        long entryId = result.get(0).getClassPK();
        try {
            return RepositoryEntryUtil.findByPrimaryKey(entryId);
        } catch (NoSuchRepositoryEntryException ex) {
            log.error(ex);
            throw new SystemException(ex);
        }
    }

    protected RepositoryEntry retrieveRepositoryEntry(File file, Class modelClass) throws SystemException {
        RepositoryEntry repositoryEntry = findEntryFromExpando(file);

        if (repositoryEntry != null) {
            return repositoryEntry;
        }

        long repositoryEntryId = counterLocalService.increment();
        repositoryEntry = RepositoryEntryUtil.create(repositoryEntryId);
        repositoryEntry.setGroupId(getGroupId());
        repositoryEntry.setRepositoryId(getRepositoryId());
        repositoryEntry.setMappedId(FSRepo.class.getName() + String.valueOf(repositoryEntryId));
        RepositoryEntryUtil.update(repositoryEntry, false);
        try {
            saveFileToExpando(repositoryEntry, file);
        } catch (Exception ex) {
            throw new SystemException(ex.getMessage(), ex);
        }

        try {
            long userId = UserLocalServiceUtil.getDefaultUserId(getCompanyId());
            if (PermissionThreadLocal.getPermissionChecker() != null) {
                userId = PermissionThreadLocal.getPermissionChecker().getUserId();
            }
            ResourceLocalServiceUtil.addResources(getCompanyId(), getGroupId(), userId, modelClass.getName(), repositoryEntryId, false, addGroupPermissions(), addGuestPermissions());
        } catch (PortalException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }

        return repositoryEntry;
    }

    public Folder fileToFolder(File folder) throws SystemException, PortalException {
        try {
            if (folder.getAbsolutePath().length() <= getRootFolder().getAbsolutePath().length()) {
                return DLAppLocalServiceUtil.getMountFolder(getRepositoryId());
            }
        } catch (FileNotFoundException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }

        RepositoryEntry entry = retrieveRepositoryEntry(folder, DLFolder.class);

        return new FileSystemFolder(this, entry.getUuid(), entry.getRepositoryEntryId(), folder);
    }

    public FileVersion fileToFileVersion(File file) throws SystemException {
        return fileToFileVersion(file, null);
    }

    public FileVersion fileToFileVersion(File file, FileEntry fileEntry) throws SystemException {
        RepositoryEntry entry = retrieveRepositoryEntry(file, DLFileEntry.class);

        FileSystemFileVersion fileVersion = new FileSystemFileVersion(this, entry.getRepositoryEntryId(), fileEntry, file);

        return fileVersion;
    }

    public FileEntry fileToFileEntry(File file) throws SystemException {
        return fileToFileEntry(file, null);
    }

    public FileEntry fileToFileEntry(File file, FileVersion fileVersion) throws SystemException {
        RepositoryEntry entry = retrieveRepositoryEntry(file, DLFileEntry.class);

        FileSystemFileEntry fileEntry = new FileSystemFileEntry(this, entry.getUuid(), entry.getRepositoryEntryId(), null, file, fileVersion);

        try {
            long userId = PrincipalThreadLocal.getUserId();
            if (userId == 0) {
                userId = UserLocalServiceUtil.getDefaultUserId(getCompanyId());
            }
            dlAppHelperLocalService.checkAssetEntry(
                    userId, fileEntry,
                    fileEntry.getFileVersion());
        } catch (Exception e) {
            log.error("Unable to update asset", e);
        }

        return fileEntry;
    }

    protected File fileEntryIdToFile(long fileEntryId)
            throws PortalException, SystemException {

        RepositoryEntry repositoryEntry = RepositoryEntryUtil.fetchByPrimaryKey(
                fileEntryId);

        if (repositoryEntry == null) {
            throw new NoSuchFileEntryException(
                    "No LocalFileSystem file entry with {fileEntryId=" + fileEntryId + "}");
        }
        try {
            return getFileFromRepositoryEntry(repositoryEntry);
        } catch (FileNotFoundException ex) {
            RepositoryEntryUtil.remove(repositoryEntry.getRepositoryEntryId());
            throw new NoSuchFolderException("File is no longer present on the file system!", ex);
        }
    }

    protected File fileVersionIdToFile(long fileVersionId)
            throws PortalException, SystemException {

        RepositoryEntry repositoryEntry = RepositoryEntryUtil.fetchByPrimaryKey(
                fileVersionId);

        if (repositoryEntry == null) {
            throw new NoSuchFileVersionException(
                    "No LocalFileSystem file version with {fileVersionId=" + fileVersionId + "}");
        }

        try {
            return getFileFromRepositoryEntry(repositoryEntry);
        } catch (FileNotFoundException ex) {
            RepositoryEntryUtil.remove(repositoryEntry.getRepositoryEntryId());
            throw new NoSuchFolderException("File is no longer present on the file system!", ex);
        }
    }

    protected File folderIdToFile(long folderId)
            throws PortalException, SystemException {

        RepositoryEntry repositoryEntry =
                RepositoryEntryUtil.fetchByPrimaryKey(folderId);

        if (repositoryEntry != null) {

            try {
                return getFileFromRepositoryEntry(repositoryEntry);
            } catch (FileNotFoundException ex) {
                RepositoryEntryUtil.remove(repositoryEntry.getRepositoryEntryId());
                throw new NoSuchFolderException("Folder is no longer present on the file system!", ex);
            }
        }

        DLFolder dlFolder = DLFolderUtil.fetchByPrimaryKey(folderId);

        if (dlFolder == null) {
            throw new NoSuchFolderException(
                    "No LocalFileSystem folder with {folderId=" + folderId + "}");
        } else if (!dlFolder.isMountPoint()) {
            throw new RepositoryException(
                    "LocalFileSystem repository should not be used with {folderId="
                            + folderId + "}");
        }
        try {
            repositoryEntry = retrieveRepositoryEntry(getRootFolder(), DLFolder.class);
            return getRootFolder();
        } catch (FileNotFoundException ex) {
            throw new RepositoryException("Mapped root folder doesn't exist on the file system!", ex);
        }
    }

    protected File getFileFromRepositoryEntry(RepositoryEntry entry) throws FileNotFoundException, SystemException, PortalException {
        return getFileFromExpando(entry);
    }

    protected String getCombinedExpandoValue(File file) throws FileNotFoundException {
        String relativePath = file.getAbsolutePath().substring(getRootFolder().getAbsolutePath().length());
        return String.valueOf(getRepositoryId() + "-" + relativePath);
    }

    protected void saveFileToExpando(RepositoryEntry entry, File file) throws FileNotFoundException, SystemException, PortalException {
        ExpandoValueLocalServiceUtil.addValue(getCompanyId(), expandoColumn.getTableId(), expandoColumn.getColumnId(), entry.getPrimaryKey(), getCombinedExpandoValue(file));
    }

    protected File getFileFromExpando(RepositoryEntry entry) throws FileNotFoundException, SystemException, PortalException {
        ExpandoValue expandoValue = ExpandoValueLocalServiceUtil.getValue(expandoColumn.getTableId(), expandoColumn.getColumnId(), entry.getPrimaryKey());
        if(expandoValue == null){
            throw new IllegalStateException("Database is corrupted! Please recreate this repository!");
        }
        String value = expandoValue.getString();
        String file = value.substring(value.indexOf("-") + 1);
        if (file == null) {
            throw new RuntimeException("There is no absolute path in Expando for Repository Entry [id]: [" + entry.getRepositoryEntryId() + "]");
        }
        File f = new File(getRootFolder(), file);
        if (!f.exists()) {
            throw new FileNotFoundException("File no longer exists on the file system: " + f.getAbsolutePath());
        }
        return f;
    }

    public boolean addGuestPermissions() {
        return GetterUtil.getBoolean(getTypeSettingsProperties().getProperty(ADD_GUEST_PERMISSIONS), true);
    }

    public boolean addGroupPermissions() {
        return GetterUtil.getBoolean(getTypeSettingsProperties().getProperty(ADD_GROUP_PERMISSIONS), true);
    }

    protected List<File> loadFilesFromDisk(File dir, final int type){
        List<File> result = new ArrayList<File>();
        if(!dir.canRead()){
            return result;
        }
        String cacheKey = dir.getAbsolutePath();
        File[] cached = getFromCache(cacheKey);
        if(cached == null){
            cached = dir.listFiles();
            putToCache(cacheKey, cached);
        }
        for(File f : cached){
            switch (type){
                case 2 : {
                    if(!f.isDirectory()){
                        result.add(f);
                    }
                    break;
                }
                case 1 : {
                    if(f.isDirectory()){
                        result.add(f);
                    }
                    break;
                }
                case 0 :
                default : {
                    result.add(f);
                    break;
                }
            }
        }
        return result;
    }

    protected File[] getFromCache(String cacheKey){
        String cacheName = new Exception().getStackTrace()[1].getMethodName();
        ThreadLocalCache<File[]> threadLocalCache =
                ThreadLocalCacheManager.getThreadLocalCache(
                        Lifecycle.REQUEST, cacheName);
        return  threadLocalCache != null ? threadLocalCache.get(cacheKey) : null;
    }

    protected void putToCache(String cacheKey, File[] value){
        String cacheName = new Exception().getStackTrace()[1].getMethodName();
        ThreadLocalCache<File[]> threadLocalCache =
                ThreadLocalCacheManager.getThreadLocalCache(
                        Lifecycle.REQUEST, cacheName);
        threadLocalCache.put(cacheKey, value);
    }

}
