package cz.topolik.liferay.fsrepo;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.NoSuchRepositoryEntryException;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.RepositoryException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.model.RepositoryEntry;
import com.liferay.portal.security.auth.PrincipalThreadLocal;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.service.ResourceLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.service.persistence.RepositoryEntryUtil;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;
import com.liferay.portlet.documentlibrary.NoSuchFileVersionException;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.service.DLAppHelperLocalServiceUtil;
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
import java.io.FileNotFoundException;
import java.util.List;

/**
 * @author Tomas Polesovsky
 */
public class PortalMapper {
    private static Log log = LogFactoryUtil.getLog(PortalMapper.class);

    private long companyId;
    private long groupId;
    private long repositoryId;
    private File rootFolder;
    private ExpandoColumn expandoColumn;
    private boolean addGroupPermissions;
    private boolean addGuestPermissions;

    public void init() throws SystemException, PortalException {
        if (companyId == 0) {
            throw new IllegalStateException("CompanyID wasn't initialized yet!");
        }

        expandoColumn = ExpandoColumnLocalServiceUtil.getDefaultTableColumn(getCompanyId(), RepositoryEntry.class.getName(), Constants.ABSOLUTE_PATH);
        if (expandoColumn == null) {
            ExpandoTable table = ExpandoTableLocalServiceUtil.fetchDefaultTable(getCompanyId(), RepositoryEntry.class.getName());
            if (table == null) {
                table = ExpandoTableLocalServiceUtil.addDefaultTable(getCompanyId(), RepositoryEntry.class.getName());
            }
            expandoColumn = ExpandoColumnLocalServiceUtil.addColumn(table.getTableId(), Constants.ABSOLUTE_PATH, ExpandoColumnConstants.STRING);
        }

    }

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

        long repositoryEntryId = CounterLocalServiceUtil.increment();
        repositoryEntry = RepositoryEntryUtil.create(repositoryEntryId);
        repositoryEntry.setGroupId(getGroupId());
        repositoryEntry.setRepositoryId(getRepositoryId());
        repositoryEntry.setMappedId(FSRepo.class.getName() + String.valueOf(repositoryEntryId));
        RepositoryEntryUtil.update(repositoryEntry);

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
            ResourceLocalServiceUtil.addResources(getCompanyId(), getGroupId(), userId, modelClass.getName(), repositoryEntryId, false, isAddGroupPermissions(), isAddGuestPermissions());
        } catch (PortalException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }

        return repositoryEntry;
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

    public Folder fileToFolder(File folder) throws SystemException, PortalException {
        if (folder.getAbsolutePath().length() <= getRootFolder().getAbsolutePath().length()) {
            return DLAppLocalServiceUtil.getMountFolder(getRepositoryId());
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
            DLAppHelperLocalServiceUtil.checkAssetEntry(userId, fileEntry, fileEntry.getFileVersion());
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

        // create if necessary
        retrieveRepositoryEntry(getRootFolder(), DLFolder.class);

        return getRootFolder();
    }

    public long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(long companyId) {
        this.companyId = companyId;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public File getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(File rootFolder) {
        this.rootFolder = rootFolder;
    }

    public boolean isAddGroupPermissions() {
        return addGroupPermissions;
    }

    public void setAddGroupPermissions(boolean addGroupPermissions) {
        this.addGroupPermissions = addGroupPermissions;
    }

    public boolean isAddGuestPermissions() {
        return addGuestPermissions;
    }

    public void setAddGuestPermissions(boolean addGuestPermissions) {
        this.addGuestPermissions = addGuestPermissions;
    }
}
