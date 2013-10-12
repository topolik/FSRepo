package cz.topolik.liferay.fsrepo;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.repository.BaseRepositoryImpl;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tomas Polesovsky
 */
public class FSRepoRepositoryImpl extends BaseRepositoryImpl {
    private FSRepo fsRepo = new FSRepo();

    @Override
    public void initRepository() throws PortalException, SystemException {
        fsRepo.initRepository();
    }


    @Override
    public List<Object> getFoldersAndFileEntries(long folderId, int start, int end, OrderByComparator obc) throws SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException e) {
            throw new SystemException(e);
        }

        return fsRepo.getFoldersAndFileEntries(folderId, start, end, obc);
    }

    @Override
    public List<Object> getFoldersAndFileEntries(long folderId, String[] mimeTypes, int start, int end, OrderByComparator obc) throws PortalException, SystemException {
        return getFoldersAndFileEntries(folderId, start, end, obc);
    }

    @Override
    public int getFoldersAndFileEntriesCount(long folderId) throws SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException e) {
            throw new SystemException(e);
        }

        return fsRepo.getFoldersAndFileEntriesCount(folderId);
    }

    @Override
    public int getFoldersAndFileEntriesCount(long folderId, String[] mimeTypes) throws PortalException, SystemException {
        return getFoldersAndFileEntriesCount(folderId);
    }

    public String[] getSupportedConfigurations() {
        return fsRepo.getSupportedConfigurations();
    }

    public String[][] getSupportedParameters() {
        return fsRepo.getSupportedParameters();
    }

    public FileEntry addFileEntry(long folderId, String sourceFileName, String mimeType, String title, String description, String changeLog, InputStream is, long size, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.ADD_DOCUMENT);

        return fsRepo.addFileEntry(folderId, sourceFileName, mimeType, title, description, changeLog, is, size, serviceContext);
    }

    public Folder addFolder(long parentFolderId, String title, String description, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), parentFolderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), parentFolderId, ActionKeys.ADD_SUBFOLDER);

        return fsRepo.addFolder(parentFolderId, title, description, serviceContext);
    }

    public void cancelCheckOut(long fileEntryId) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        fsRepo.cancelCheckOut(fileEntryId);
    }

    public void checkInFileEntry(long fileEntryId, boolean major, String changeLog, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        fsRepo.checkInFileEntry(fileEntryId, major, changeLog, serviceContext);
    }

    public void checkInFileEntry(long fileEntryId, String lockUuid) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        fsRepo.checkInFileEntry(fileEntryId, lockUuid);
    }

    public FileEntry checkOutFileEntry(long fileEntryId) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        return fsRepo.checkOutFileEntry(fileEntryId);
    }

    public FileEntry checkOutFileEntry(long fileEntryId, String owner, long expirationTime) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        return fsRepo.checkOutFileEntry(fileEntryId, owner, expirationTime);
    }

    public FileEntry copyFileEntry(long groupId, long fileEntryId, long destFolderId, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), destFolderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), destFolderId, ActionKeys.ADD_DOCUMENT);

        return fsRepo.copyFileEntry(groupId, fileEntryId, destFolderId, serviceContext);
    }

    public void deleteFileEntry(long fileEntryId) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.VIEW);
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.DELETE);

        fsRepo.deleteFileEntry(fileEntryId);
    }

    public void deleteFolder(long folderId) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.DELETE);

        fsRepo.deleteFolder(folderId);
    }

    public List<FileEntry> getFileEntries(long folderId, int start, int end, OrderByComparator obc) throws SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return fsRepo.getFileEntries(folderId, start, end, obc);
    }

    public List<FileEntry> getFileEntries(long folderId, long fileEntryTypeId, int start, int end, OrderByComparator obc) throws SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return fsRepo.getFileEntries(folderId, fileEntryTypeId, start, end, obc);
    }

    public List<FileEntry> getFileEntries(long folderId, String[] mimeTypes, int start, int end, OrderByComparator obc) throws PortalException, SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return fsRepo.getFileEntries(folderId, mimeTypes, start, end, obc);
    }

    public int getFileEntriesCount(long folderId) throws SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return fsRepo.getFileEntriesCount(folderId);
    }

    public int getFileEntriesCount(long folderId, long fileEntryTypeId) throws SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return fsRepo.getFileEntriesCount(folderId, fileEntryTypeId);
    }

    public int getFileEntriesCount(long folderId, String[] mimeTypes) throws PortalException, SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return fsRepo.getFileEntriesCount(folderId, mimeTypes);
    }

    public FileEntry getFileEntry(long fileEntryId) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.VIEW);

        return fsRepo.getFileEntry(fileEntryId);
    }

    public FileEntry getFileEntry(long folderId, String title) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);

        FileEntry entry = fsRepo.getFileEntry(folderId, title);
        if (entry == null) {
            throw new PrincipalException();
        }

        PermissionsUtil.checkFileEntry(getGroupId(), entry.getFileEntryId(), ActionKeys.VIEW);

        return entry;
    }

    public FileEntry getFileEntryByUuid(String uuid) throws PortalException, SystemException {
        FileEntry entry = fsRepo.getFileEntryByUuid(uuid);
        if (entry == null) {
            throw new PrincipalException();
        }

        PermissionsUtil.checkFileEntry(getGroupId(), entry.getFileEntryId(), ActionKeys.VIEW);

        return entry;
    }

    public FileVersion getFileVersion(long fileVersionId) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileVersionId, ActionKeys.VIEW);

        return fsRepo.getFileVersion(fileVersionId);
    }

    public Folder getFolder(long folderId) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);

        return fsRepo.getFolder(folderId);
    }

    public Folder getFolder(long parentFolderId, String title) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), parentFolderId, ActionKeys.VIEW);

        Folder folder = fsRepo.getFolder(parentFolderId, title);

        PermissionsUtil.checkFolder(getGroupId(), folder.getFolderId(), ActionKeys.VIEW);

        return folder;
    }

    public List<Folder> getFolders(long parentFolderId, boolean includeMountFolders, int start, int end, OrderByComparator obc) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), parentFolderId, ActionKeys.VIEW);

        return fsRepo.getFolders(parentFolderId, includeMountFolders, start, end, obc);
    }

    public int getFoldersCount(long parentFolderId, boolean includeMountfolders) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), parentFolderId, ActionKeys.VIEW);

        return fsRepo.getFoldersCount(parentFolderId, includeMountfolders);
    }

    public int getFoldersFileEntriesCount(List<Long> folderIds, int status) throws SystemException {
        List<Long> allowedFolderIds = new ArrayList<Long>(folderIds.size());
        for(Long folderId : folderIds) {
            if (PermissionsUtil.containsFolder(getGroupId(), folderId, ActionKeys.VIEW)) {
                allowedFolderIds.add(folderId);
            }
        }

        return fsRepo.getFoldersFileEntriesCount(allowedFolderIds, status);
    }

    public List<Folder> getMountFolders(long parentFolderId, int start, int end, OrderByComparator obc) throws SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), parentFolderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return fsRepo.getMountFolders(parentFolderId, start, end, obc);
    }

    public int getMountFoldersCount(long parentFolderId) throws SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), parentFolderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return fsRepo.getMountFoldersCount(parentFolderId);
    }

    public void getSubfolderIds(List<Long> folderIds, long folderId) throws SystemException {
        //TODO: where is it used from?
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        List<Long> allowedFolderIds = new ArrayList<Long>(folderIds.size());
        for(Long folderId2 : folderIds) {
            if (PermissionsUtil.containsFolder(getGroupId(), folderId2, ActionKeys.VIEW)) {
                allowedFolderIds.add(folderId2);
            }
        }

        fsRepo.getSubfolderIds(allowedFolderIds, folderId);
    }

    public List<Long> getSubfolderIds(long folderId, boolean recurse) throws SystemException {
        try {
            PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        } catch (PrincipalException ex) {
            throw new SystemException(ex);
        }

        return fsRepo.getSubfolderIds(folderId, recurse);
    }

    public Lock lockFolder(long folderId) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.UPDATE);

        return fsRepo.lockFolder(folderId);
    }

    public Lock lockFolder(long folderId, String owner, boolean inheritable, long expirationTime) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.UPDATE);

        return fsRepo.lockFolder(folderId, owner, inheritable, expirationTime);
    }

    public FileEntry moveFileEntry(long fileEntryId, long newFolderId, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.VIEW);
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);
        PermissionsUtil.checkFolder(getGroupId(), newFolderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), newFolderId, ActionKeys.ADD_DOCUMENT);

        return fsRepo.moveFileEntry(fileEntryId, newFolderId, serviceContext);
    }

    public Folder moveFolder(long folderId, long newParentFolderId, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.UPDATE);
        PermissionsUtil.checkFolder(getGroupId(), newParentFolderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), newParentFolderId, ActionKeys.ADD_SUBFOLDER);

        return fsRepo.moveFolder(folderId, newParentFolderId, serviceContext);
    }

    public Lock refreshFileEntryLock(String lockUuid, long expirationTime) throws PortalException, SystemException {
        return fsRepo.refreshFileEntryLock(lockUuid, expirationTime);
    }

    public Lock refreshFolderLock(String lockUuid, long expirationTime) throws PortalException, SystemException {
        return fsRepo.refreshFolderLock(lockUuid, expirationTime);
    }

    public void revertFileEntry(long fileEntryId, String version, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        fsRepo.revertFileEntry(fileEntryId, version, serviceContext);
    }

    public Hits search(SearchContext searchContext, Query query) throws SearchException {
        return fsRepo.search(searchContext, query);
    }

    public void unlockFolder(long folderId, String lockUuid) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.UPDATE);

        fsRepo.unlockFolder(folderId, lockUuid);
    }

    public FileEntry updateFileEntry(long fileEntryId, String sourceFileName, String mimeType, String title, String description, String changeLog, boolean majorVersion, InputStream is, long size, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.VIEW);
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.UPDATE);

        return fsRepo.updateFileEntry(fileEntryId, sourceFileName, mimeType, title, description, changeLog, majorVersion, is, size, serviceContext);
    }

    public Folder updateFolder(long folderId, String title, String description, ServiceContext serviceContext) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.UPDATE);

        return fsRepo.updateFolder(folderId, title, description, serviceContext);
    }

    public boolean verifyFileEntryCheckOut(long fileEntryId, String lockUuid) throws PortalException, SystemException {
        PermissionsUtil.checkFileEntry(getGroupId(), fileEntryId, ActionKeys.VIEW);

        return fsRepo.verifyFileEntryCheckOut(fileEntryId, lockUuid);
    }

    public boolean verifyInheritableLock(long folderId, String lockUuid) throws PortalException, SystemException {
        PermissionsUtil.checkFolder(getGroupId(), folderId, ActionKeys.VIEW);

        return fsRepo.verifyInheritableLock(folderId, lockUuid);
    }
}