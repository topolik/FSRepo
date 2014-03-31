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

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.repository.BaseRepositoryImpl;
import com.liferay.portal.kernel.repository.DefaultLocalRepositoryImpl;
import com.liferay.portal.kernel.repository.LocalRepository;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Query;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.model.Lock;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tomas Polesovsky
 */
public class FSRepoRepositoryImpl extends BaseRepositoryImpl {

    FSRepo fsRepo;
    DefaultLocalRepositoryImpl defaultLocalRepository;

    public FSRepoRepositoryImpl() {
        fsRepo = new FSRepo();
        defaultLocalRepository = new DefaultLocalRepositoryImpl(fsRepo);
    }

    @Override
    public LocalRepository getLocalRepository() {
        return defaultLocalRepository;
    }

    @Override
    public void initRepository() throws PortalException, SystemException {
        fsRepo.initRepository();
    }

    @Override
    public String[] getSupportedConfigurations() {
        return fsRepo.getSupportedConfigurations();
    }

    @Override
    public String[][] getSupportedParameters() {
        return fsRepo.getSupportedParameters();
    }

    @Override
    public List<Object> getFoldersAndFileEntries(long folderId, int start, int end, OrderByComparator obc) throws SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException e) {
            throw new SystemException(e);
        }

        return filterFoldersAndFileEntries(fsRepo.getFoldersAndFileEntries(folderId, start, end, obc));
    }

    @Override
    public List<Object> getFoldersAndFileEntries(long folderId, String[] mimeTypes, int start, int end, OrderByComparator obc) throws PortalException, SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException e) {
            throw new SystemException(e);
        }

        return filterFoldersAndFileEntries(fsRepo.getFoldersAndFileEntries(folderId, mimeTypes, start, end, obc));
    }

    @Override
    public int getFoldersAndFileEntriesCount(long folderId) throws SystemException {
        return getFoldersAndFileEntries(folderId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null).size();
    }

    @Override
    public int getFoldersAndFileEntriesCount(long folderId, String[] mimeTypes) throws PortalException, SystemException {
        return getFoldersAndFileEntries(folderId, mimeTypes, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null).size();
    }

    @Override
    public FileEntry addFileEntry(long folderId, String sourceFileName, String mimeType, String title, String description, String changeLog, InputStream is, long size, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.ADD_DOCUMENT);

        return fsRepo.addFileEntry(folderId, sourceFileName, mimeType, title, description, changeLog, is, size, serviceContext);
    }

    @Override
    public Folder addFolder(long parentFolderId, String title, String description, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), parentFolderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), parentFolderId, ActionKeys.ADD_SUBFOLDER);

        return fsRepo.addFolder(parentFolderId, title, description, serviceContext);
    }

    @Override
    public FileVersion cancelCheckOut(long fileEntryId) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        return fsRepo.cancelCheckOut(fileEntryId);
    }

    @Override
    public void checkInFileEntry(long fileEntryId, boolean major, String changeLog, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        fsRepo.checkInFileEntry(fileEntryId, major, changeLog, serviceContext);
    }

    @Override
    public void checkInFileEntry(long fileEntryId, String lockUuid, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        fsRepo.checkInFileEntry(fileEntryId, lockUuid, serviceContext);
    }

    @Override
    public FileEntry checkOutFileEntry(long fileEntryId, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        return fsRepo.checkOutFileEntry(fileEntryId, serviceContext);
    }

    @Override
    public FileEntry checkOutFileEntry(long fileEntryId, String owner, long expirationTime, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        return checkOutFileEntry(fileEntryId, owner, expirationTime, serviceContext);
    }

    @Override
    public FileEntry copyFileEntry(long groupId, long fileEntryId, long destFolderId, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), destFolderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), destFolderId, ActionKeys.ADD_DOCUMENT);

        return fsRepo.copyFileEntry(groupId, fileEntryId, destFolderId, serviceContext);
    }

    @Override
    public void deleteFileEntry(long fileEntryId) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.VIEW);
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.DELETE);

        fsRepo.deleteFileEntry(fileEntryId);
    }

    @Override
    public void deleteFolder(long folderId) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.DELETE);

        fsRepo.deleteFolder(folderId);
    }

    @Override
    public List<FileEntry> getFileEntries(long folderId, int start, int end, OrderByComparator obc) throws SystemException, PortalException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return filterFileEntries(fsRepo.getFileEntries(folderId, start, end, obc));
    }

    @Override
    public List<FileEntry> getFileEntries(long folderId, long fileEntryTypeId, int start, int end, OrderByComparator obc) throws SystemException, PortalException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return filterFileEntries(fsRepo.getFileEntries(folderId, fileEntryTypeId, start, end, obc));
    }

    @Override
    public List<FileEntry> getFileEntries(long folderId, String[] mimeTypes, int start, int end, OrderByComparator obc) throws PortalException, SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return filterFileEntries(fsRepo.getFileEntries(folderId, mimeTypes, start, end, obc));
    }

    @Override
    public int getFileEntriesCount(long folderId) throws SystemException, PortalException {
        return filterFileEntries(getFileEntries(folderId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null)).size();
    }

    @Override
    public int getFileEntriesCount(long folderId, long fileEntryTypeId) throws SystemException, PortalException {
        return filterFileEntries(getFileEntries(folderId, fileEntryTypeId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null)).size();
    }

    @Override
    public int getFileEntriesCount(long folderId, String[] mimeTypes) throws PortalException, SystemException {
        return filterFileEntries(getFileEntries(folderId, mimeTypes, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null)).size();
    }

    @Override
    public FileEntry getFileEntry(long fileEntryId) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.VIEW);

        return fsRepo.getFileEntry(fileEntryId);
    }

    @Override
    public FileEntry getFileEntry(long folderId, String title) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);

        FileEntry entry = fsRepo.getFileEntry(folderId, title);
        if (entry == null) {
            throw new NoSuchFileEntryException();
        }

        PermissionsUtil.checkFileEntry(getGroupId(), entry.getFileEntryId(), ActionKeys.VIEW);

        return entry;
    }

    @Override
    public FileEntry getFileEntryByUuid(String uuid) throws PortalException, SystemException {
        FileEntry entry = fsRepo.getFileEntryByUuid(uuid);
        if (entry == null) {
            throw new NoSuchFileEntryException();
        }

        PermissionsUtil.checkFileEntry(getGroupId(), entry.getFileEntryId(), ActionKeys.VIEW);

        return entry;
    }

    @Override
    public FileVersion getFileVersion(long fileVersionId) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileVersionId, ActionKeys.VIEW);

        return fsRepo.getFileVersion(fileVersionId);
    }

    @Override
    public Folder getFolder(long folderId) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);

        return fsRepo.getFolder(folderId);
    }

    @Override
    public Folder getFolder(long parentFolderId, String title) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), parentFolderId, ActionKeys.VIEW);

        Folder folder = fsRepo.getFolder(parentFolderId, title);

        PermissionsUtil.checkFolder(getGroupId(), folder.getFolderId(), ActionKeys.VIEW);

        return folder;
    }

    @Override
    public List<Folder> getFolders(long parentFolderId, boolean includeMountFolders, int start, int end, OrderByComparator obc) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), parentFolderId, ActionKeys.VIEW);

        return filterFolders(fsRepo.getFolders(parentFolderId, includeMountFolders, start, end, obc));
    }

    @Override
    public int getFoldersCount(long parentFolderId, boolean includeMountfolders) throws PortalException, SystemException {
        return getFolders(parentFolderId, includeMountfolders, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null).size();
    }

    @Override
    public int getFoldersFileEntriesCount(List<Long> folderIds, int status) throws SystemException {
        List<Long> allowedFolderIds = new ArrayList<Long>(folderIds.size());
        for (Long folderId : folderIds) {
            if (PermissionsUtil.containsFolder(getGroupId(), folderId, ActionKeys.VIEW)) {
                allowedFolderIds.add(folderId);
            }
        }

        return filterFileEntries(fsRepo.getFoldersFileEntries(allowedFolderIds, status)).size();
    }

    @Override
    public List<Folder> getMountFolders(long parentFolderId, int start, int end, OrderByComparator obc) throws SystemException, PortalException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), parentFolderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return filterFolders(fsRepo.getMountFolders(parentFolderId, start, end, obc));
    }

    @Override
    public int getMountFoldersCount(long parentFolderId) throws SystemException, PortalException {
        return getMountFolders(parentFolderId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null).size();
    }

    @Override
    public void getSubfolderIds(List<Long> folderIds, long folderId) throws SystemException {
        //TODO: where is it used from?

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> getSubfolderIds(long folderId, boolean recurse) throws SystemException, PortalException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return filterFolderIds(fsRepo.getSubfolderIds(folderId, recurse));
    }

    @Override
    public Lock lockFolder(long folderId) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.UPDATE);

        return fsRepo.lockFolder(folderId);
    }

    @Override
    public Lock lockFolder(long folderId, String owner, boolean inheritable, long expirationTime) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.UPDATE);

        return fsRepo.lockFolder(folderId, owner, inheritable, expirationTime);
    }

    @Override
    public FileEntry moveFileEntry(long fileEntryId, long newFolderId, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.VIEW);
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);
        PermissionsUtil.checkFolder(getGroupId(), newFolderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), newFolderId, ActionKeys.ADD_DOCUMENT);

        return fsRepo.moveFileEntry(fileEntryId, newFolderId, serviceContext);
    }

    @Override
    public Folder moveFolder(long folderId, long newParentFolderId, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.UPDATE);
        PermissionsUtil.checkFolder(getGroupId(), newParentFolderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), newParentFolderId, ActionKeys.ADD_SUBFOLDER);

        return fsRepo.moveFolder(folderId, newParentFolderId, serviceContext);
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
    public void revertFileEntry(long fileEntryId, String version, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        fsRepo.revertFileEntry(fileEntryId, version, serviceContext);
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

//        return fsRepo.search(searchContext, query);
    }

    @Override
    public void unlockFolder(long folderId, String lockUuid) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.UPDATE);

        fsRepo.unlockFolder(folderId, lockUuid);
    }

    @Override
    public FileEntry updateFileEntry(long fileEntryId, String sourceFileName, String mimeType, String title, String description, String changeLog, boolean majorVersion, InputStream is, long size, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.VIEW);
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        return fsRepo.updateFileEntry(fileEntryId, sourceFileName, mimeType, title, description, changeLog, majorVersion, is, size, serviceContext);
    }

    @Override
    public Folder updateFolder(long folderId, String title, String description, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.UPDATE);

        return fsRepo.updateFolder(folderId, title, description, serviceContext);
    }

    @Override
    public boolean verifyFileEntryCheckOut(long fileEntryId, String lockUuid) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.VIEW);

        return fsRepo.verifyFileEntryCheckOut(fileEntryId, lockUuid);
    }

    @Override
    public boolean verifyInheritableLock(long folderId, String lockUuid) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);

        return fsRepo.verifyInheritableLock(folderId, lockUuid);
    }

    protected List<FileEntry> filterFileEntries(List<FileEntry> fileEntries) {
        List<FileEntry> result = new ArrayList<FileEntry>(fileEntries.size());
        for (FileEntry fileEntry : fileEntries) {
            if (PermissionsUtil.containsFileEntry(fileEntry.getGroupId(), fileEntry.getFileEntryId(), ActionKeys.VIEW)) {
                result.add(fileEntry);
            }
        }
        return result;
    }

    protected List<Folder> filterFolders(List<Folder> folders) {
        List<Folder> result = new ArrayList<Folder>(folders.size());
        for (Folder folder : folders) {
            if (PermissionsUtil.containsFolder(folder.getGroupId(), folder.getFolderId(), ActionKeys.VIEW)) {
                result.add(folder);
            }
        }
        return result;
    }

    protected List<Long> filterFolderIds(List<Long> folderIds) {
        List<Long> result = new ArrayList<Long>(folderIds.size());
        for (Long folderId : folderIds) {
            if (PermissionsUtil.containsFolder(getGroupId(), folderId, ActionKeys.VIEW)) {
                result.add(folderId);
            }
        }

        return result;
    }

    protected List<Object> filterFoldersAndFileEntries(List<Object> records) {
        List<Object> result = new ArrayList<Object>(records.size());

        for (Object record : records) {
            if (record instanceof FileEntry &&
                    PermissionsUtil.containsFileEntry(((FileEntry) record).getGroupId(), ((FileEntry) record).getFileEntryId(), ActionKeys.VIEW)) {
                result.add(record);
            }
            if (record instanceof Folder &&
                    PermissionsUtil.containsFileEntry(((Folder) record).getGroupId(), ((Folder) record).getFolderId(), ActionKeys.VIEW)) {
                result.add(record);
            }
        }

        return result;
    }
}