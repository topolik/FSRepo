/**
 * Copyright (c) 2012 Tomáš Polešovský
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
package cz.topolik.fsrepo;

import com.liferay.portal.NoSuchRepositoryEntryException;
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
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Lock;
import com.liferay.portal.model.RepositoryEntry;
import com.liferay.portal.security.auth.PrincipalThreadLocal;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.persistence.RepositoryEntryUtil;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;
import com.liferay.portlet.documentlibrary.NoSuchFileVersionException;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.service.persistence.DLFolderUtil;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Tomas Polesovsky
 */
public class LocalFileSystemRepository extends BaseRepositoryImpl {
    private static Log _log = LogFactoryUtil.getLog(LocalFileSystemRepository.class);
    private PortalFileIndexer fileIndexer;

    
    @Override
    public void initRepository() throws PortalException, SystemException {
        fileIndexer = new PortalFileIndexer(this);
    }

    
    @Override
    public List<Object> getFoldersAndFileEntries(long folderId, int start, int end, OrderByComparator obc) throws SystemException {
        List<Object> result = new ArrayList<Object>();
        try {
            File systemFolder = folderIdToFile(folderId);
            if (systemFolder.canRead()) {
                for (File file : systemFolder.listFiles()) {
                    if (file.isDirectory()) {
                        Folder f = fileToFolder(file);
                        result.add(f);
                    } else {
                        FileEntry f = fileToFileEntry(file);
                        result.add(f);
                    }
                }
            }

        } catch (PortalException ex) {
            _log.error(ex);
        }

        result = result.subList(start < 0 ? 0 : start, end > result.size() ? result.size() : end);
        if(obc != null){
            Collections.sort(result, obc);
        }
        return result;
    }

    
    @Override
    public List<Object> getFoldersAndFileEntries(long folderId, String[] mimeTypes, int start, int end, OrderByComparator obc) throws PortalException, SystemException {
        return getFoldersAndFileEntries(folderId, start, end, obc);
    }

    
    @Override
    public int getFoldersAndFileEntriesCount(long folderId) throws SystemException {
        return getFoldersAndFileEntries(folderId, 0, Integer.MAX_VALUE, null).size();
    }

    
    @Override
    public int getFoldersAndFileEntriesCount(long folderId, String[] mimeTypes) throws PortalException, SystemException {
        return getFoldersAndFileEntries(folderId, mimeTypes, 0, Integer.MAX_VALUE, null).size();
    }

    
    public String[] getSupportedConfigurations() {
        return new String[]{"FILESYSTEM"};
    }

    
    public String[][] getSupportedParameters() {
        return new String[][]{{"ROOT_FOLDER"}};
    }
    
    public FileEntry addFileEntry(long folderId, String sourceFileName, String mimeType, String title, String description, String changeLog, InputStream is, long size, ServiceContext serviceContext) throws PortalException, SystemException {
        File directory = folderIdToFile(folderId);
        if (directory.exists() && directory.canWrite()) {
            File file = new File(directory, sourceFileName);
            try {
                StreamUtil.transfer(is, new FileOutputStream(file), true);
                return fileToFileEntry(file);
            } catch (Exception ex) {
                _log.error(ex);
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

        File file = new File(destDir, srcFile.getName());
        try {
            StreamUtil.transfer(new FileInputStream(srcFile), new FileOutputStream(file), true);
            return fileToFileEntry(file);
        } catch (Exception ex) {
            _log.error(ex);
            throw new SystemException(ex);
        }
    }

    
    public void deleteFileEntry(long fileEntryId) throws PortalException, SystemException {
        File file = fileEntryIdToFile(fileEntryId);
        if (!file.exists() || !file.canWrite()) {
            throw new SystemException("File doesn't exist or cannot be modified " + file);
        }

        file.delete();
    }

    
    public void deleteFolder(long folderId) throws PortalException, SystemException {
        File folder = folderIdToFile(folderId);
        if (!folder.exists() || !folder.canWrite()) {
            throw new SystemException("Folder doesn't exist or cannot be modified " + folder);
        }

        folder.delete();
    }

    
    public List<FileEntry> getFileEntries(long folderId, int start, int end, OrderByComparator obc) throws SystemException {
        List<FileEntry> result = new ArrayList<FileEntry>();
        try {
            File systemFolder = folderIdToFile(folderId);
            if (systemFolder.canRead()) {
                for (File file : systemFolder.listFiles()) {
                    if (!file.isDirectory()) {
                        FileEntry f = fileToFileEntry(file);
                        result.add(f);
                    }
                }
            }

        } catch (PortalException ex) {
            _log.error(ex);
        }

        result = result.subList(start < 0 ? 0 : start, end > result.size() ? result.size() : end);
        if(obc != null){
            Collections.sort(result, obc);
        }
        return result;

    }

    
    public List<FileEntry> getFileEntries(long folderId, long fileEntryTypeId, int start, int end, OrderByComparator obc) throws SystemException {
        return new ArrayList<FileEntry>();
    }

    
    public List<FileEntry> getFileEntries(long folderId, String[] mimeTypes, int start, int end, OrderByComparator obc) throws PortalException, SystemException {
        return getFileEntries(folderId, start, end, obc);
    }

    
    public int getFileEntriesCount(long folderId) throws SystemException {
        return getFileEntries(folderId, 0, Integer.MAX_VALUE, null).size();
    }

    
    public int getFileEntriesCount(long folderId, long fileEntryTypeId) throws SystemException {
        return getFileEntries(folderId, fileEntryTypeId, 0, Integer.MAX_VALUE, null).size();
    }

    
    public int getFileEntriesCount(long folderId, String[] mimeTypes) throws PortalException, SystemException {
        return getFileEntries(folderId, mimeTypes, 0, Integer.MAX_VALUE, null).size();
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

            String mappedId = repositoryEntry.getMappedId();

            return fileToFileEntry(fileIndexer.mappedIdToFile(mappedId));
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
        return fileToFolder(new File(folderIdToFile(parentFolderId), title));
    }

    
    public List<Folder> getFolders(long parentFolderId, boolean includeMountFolders, int start, int end, OrderByComparator obc) throws PortalException, SystemException {
        String fileSystemDirectory = folderIdToFile(parentFolderId).getAbsolutePath();
        File dir = new File(fileSystemDirectory);
        if (dir.canRead()) {
            File[] subDirectories = dir.listFiles(new FileFilter() {

                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
            List<Folder> result = new ArrayList<Folder>(subDirectories.length);
            for (File subDir : subDirectories) {
                result.add(fileToFolder(subDir));
            }
            result = result.subList(start < 0 ? 0 : start, end > result.size() ? result.size() : end);
            if(obc != null){
                Collections.sort(result, obc);
            }
            return result;

        }
        return new ArrayList<Folder>();
    }

    
    public int getFoldersCount(long parentFolderId, boolean includeMountfolders) throws PortalException, SystemException {
        int result = getFolders(parentFolderId, includeMountfolders, 0, Integer.MAX_VALUE, null).size();
        return result;
    }

    
    public int getFoldersFileEntriesCount(List<Long> folderIds, int status) throws SystemException {
        int result = 0;
        for(Long folderId : folderIds){
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
        File fileToMove = folderIdToFile(fileEntryId);
        File parentFolder = folderIdToFile(newFolderId);
        File dstFile = new File(parentFolder, fileToMove.getName());

        if(!fileToMove.exists()){
            throw new SystemException("Source file doesn't exist: " + fileToMove);
        }
        if(!parentFolder.exists()){
            throw new SystemException("Destination parent folder doesn't exist: " + parentFolder);
        }
        if(!parentFolder.exists()){
            throw new SystemException("Destination file does exist: " + dstFile);
        }
        if(fileToMove.canWrite() && parentFolder.canWrite()){
            if(!fileToMove.renameTo(dstFile)){
                throw new SystemException("Moving was not successful (don't know why) [from, to]: ["+fileToMove+", "+dstFile+"]");
            }
            fileIndexer.remove(fileToMove);
            fileIndexer.add(dstFile);

            RepositoryEntry repositoryEntry = RepositoryEntryUtil.fetchByPrimaryKey(fileEntryId);
            repositoryEntry.setMappedId(fileIndexer.fileToMappedId(dstFile));
            RepositoryEntryUtil.update(repositoryEntry, true);

            return fileToFileEntry(dstFile);
        } else {
            throw new SystemException("Doesn't have rights to move the file [src, toParentDir]: ["+fileToMove+", "+parentFolder+"]");
        }
    }

    
    public Folder moveFolder(long folderId, long newParentFolderId, ServiceContext serviceContext) throws PortalException, SystemException {
        File folderToMove = folderIdToFile(folderId);
        File parentFolder = folderIdToFile(newParentFolderId);
        File dstFolder = new File(parentFolder, folderToMove.getName());
        
        if(!folderToMove.exists()){
            throw new SystemException("Source folder doesn't exist: " + folderToMove);
        }
        if(!parentFolder.exists()){
            throw new SystemException("Destination parent folder doesn't exist: " + parentFolder);
        }
        if(!parentFolder.exists()){
            throw new SystemException("Destination folder does exist: " + dstFolder);
        }
        if(folderToMove.canWrite() && parentFolder.canWrite()){
            if(!folderToMove.renameTo(dstFolder)){
                throw new SystemException("Moving was not successful (don't know why) [from, to]: ["+folderToMove+", "+dstFolder+"]");
            }
            fileIndexer.remove(folderToMove);
            fileIndexer.add(dstFolder);
            
            RepositoryEntry repositoryEntry = RepositoryEntryUtil.fetchByPrimaryKey(folderId);
            repositoryEntry.setMappedId(fileIndexer.fileToMappedId(dstFolder));
            RepositoryEntryUtil.update(repositoryEntry, true);

            return fileToFolder(dstFolder);
        } else {
            throw new SystemException("Doesn't have rights to move the directory [srcDir, toParentDir]: ["+folderToMove+", "+parentFolder+"]");
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
        return SearchEngineUtil.search(searchContext, query);
    }

    
    public void unlockFolder(long folderId, String lockUuid) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

    
    public FileEntry updateFileEntry(long fileEntryId, String sourceFileName, String mimeType, String title, String description, String changeLog, boolean majorVersion, InputStream is, long size, ServiceContext serviceContext) throws PortalException, SystemException {
        File file = fileEntryIdToFile(fileEntryId);
        File dstFile = new File(file.getParentFile(), title);
        boolean toRename = false;
        if(!file.canWrite()){
            throw new SystemException("Cannot modify file: " + file);
        }
        if(Validator.isNotNull(title) && !title.equals(file.getName())){
            if(dstFile.exists()){
                throw new SystemException("Destination file already exists: " + dstFile);
            }
            toRename = true;
        }
        if(size > 0){
            try {
                StreamUtil.transfer(is, new FileOutputStream(file));
            } catch (IOException ex) {
                _log.error(ex);
            }
        }
        if(toRename){
            file.renameTo(dstFile);
            fileIndexer.remove(file);
            fileIndexer.add(dstFile);

            RepositoryEntry repositoryEntry = RepositoryEntryUtil.fetchByPrimaryKey(fileEntryId);
            repositoryEntry.setMappedId(fileIndexer.fileToMappedId(dstFile));
            RepositoryEntryUtil.update(repositoryEntry, true);
        }
        return fileToFileEntry(dstFile);
    }

    
    public Folder updateFolder(long folderId, String title, String description, ServiceContext serviceContext) throws PortalException, SystemException {
        File folder = folderIdToFile(folderId);
        if(!folder.exists() || !folder.canWrite()){
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

    /******************
     *
     */


    protected Folder fileToFolder(File folder) throws SystemException {
        Object[] ids = getRepositoryEntryIds(fileIndexer.fileToMappedId(folder));
        long folderId = (Long) ids[0];
        String uuid = (String) ids[1];

        return new FileSystemFolder(this, uuid, folderId, folder);
    }

    public FileVersion fileToFileVersion(File file) throws SystemException {
        return fileToFileVersion(file, null);
    }
    public FileVersion fileToFileVersion(File file, FileEntry fileEntry) throws SystemException {
        Object[] ids = getRepositoryEntryIds(fileIndexer.fileToMappedId(file));

        long fileVersionId = (Long) ids[0];
        FileSystemFileVersion fileVersion = new FileSystemFileVersion(this, fileVersionId, fileEntry, file);

        return fileVersion;
    }

    public FileEntry fileToFileEntry(File file) throws SystemException {
        return fileToFileEntry(file, null);
    }
    public FileEntry fileToFileEntry(File file, FileVersion fileVersion) throws SystemException {
        Object[] ids = getRepositoryEntryIds(fileIndexer.fileToMappedId(file));

        long fileEntryId = (Long) ids[0];
        String uuid = (String) ids[1];

        FileSystemFileEntry fileEntry = new FileSystemFileEntry(this, uuid, fileEntryId, null, file, fileVersion);

        try {
            dlAppHelperLocalService.checkAssetEntry(
                    PrincipalThreadLocal.getUserId(), fileEntry,
                    fileEntry.getFileVersion());
        } catch (Exception e) {
            _log.error("Unable to update asset", e);
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
            return fileIndexer.mappedIdToFile(repositoryEntry.getMappedId());
        } catch (FileNotFoundException ex) {
            throw new NoSuchFileEntryException(
                    "No LocalFileSystem file entry with {fileEntryId=" + fileEntryId + "}");
        }
    }

    protected File fileVersionIdToFile(long fileVersionId)
            throws PortalException, SystemException {

        RepositoryEntry repositoryEntry = RepositoryEntryUtil.fetchByPrimaryKey(
                fileVersionId);

        if (repositoryEntry == null) {
            throw new NoSuchFileVersionException(
                    "No LocalFileSystem file version with {fileVersionId=" + fileVersionId+ "}");
        }
        try {
            return fileIndexer.mappedIdToFile(repositoryEntry.getMappedId());
        } catch (FileNotFoundException ex) {
            throw new NoSuchFileVersionException(
                    "No LocalFileSystem file version with {fileVersionId=" + fileVersionId+ "}");
        }
    }

    protected File folderIdToFile(long folderId)
            throws PortalException, SystemException {

        RepositoryEntry repositoryEntry =
                RepositoryEntryUtil.fetchByPrimaryKey(folderId);

        if (repositoryEntry != null) {
            try {
                return fileIndexer.mappedIdToFile(repositoryEntry.getMappedId());
            } catch (FileNotFoundException ex) {
                throw new NoSuchFolderException(
                        "No LocalFileSystem folder with {folderId=" + folderId + "}");
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


        File rootFolderPath = new File(getRootFolder());

        repositoryEntry = RepositoryEntryUtil.fetchByR_M(
                getRepositoryId(), fileIndexer.fileToMappedId(rootFolderPath));

        if (repositoryEntry == null) {
            long repositoryEntryId = counterLocalService.increment();

            repositoryEntry = RepositoryEntryUtil.create(repositoryEntryId);

            repositoryEntry.setGroupId(getGroupId());
            repositoryEntry.setRepositoryId(getRepositoryId());
            repositoryEntry.setMappedId(fileIndexer.fileToMappedId(rootFolderPath));

            RepositoryEntryUtil.update(repositoryEntry, false);
        }
        try {
            return fileIndexer.mappedIdToFile(repositoryEntry.getMappedId());
        } catch (FileNotFoundException ex) {
                throw new NoSuchFolderException(
                        "No LocalFileSystem folder with {path=" + rootFolderPath + "}");
        }
    }

    public String getRootFolder() {
        return getTypeSettingsProperties().getProperty("ROOT_FOLDER");
    }

}