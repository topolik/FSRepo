package cz.topolik.fsrepo;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.repository.LocalRepository;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.util.AutoResetThreadLocal;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.service.ServiceContext;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * @author Tomas Polesovsky
 */
public class LocalFileSystemLocalRepository implements LocalRepository {
    private LocalFileSystemRepository repository;
    private static ThreadLocal<Boolean> localCall = new AutoResetThreadLocal<Boolean>(LocalFileSystemLocalRepository.class.getName()+".localCall");

    public LocalFileSystemLocalRepository(LocalFileSystemRepository repository) {
        this.repository = repository;
    }

    protected void setLocalCall(){
        localCall.set(Boolean.TRUE);
    }

    public static boolean isLocalCall(){
        return localCall.get() != null ? localCall.get() : false;
    }

    public void deleteFileEntry(long l) throws PortalException, SystemException {
        setLocalCall();
        repository.deleteFileEntry(l);
    }

    public void deleteFolder(long l) throws PortalException, SystemException {
        setLocalCall();
        repository.deleteFolder(l);
    }

    public List<FileEntry> getFileEntries(long l, int i, int i1, OrderByComparator orderByComparator) throws SystemException {
        setLocalCall();
        return repository.getFileEntries(l, i, i1, orderByComparator);
    }

    public List<Object> getFileEntriesAndFileShortcuts(long l, int i, int i1, int i2) 
    		throws SystemException, PortalException {
        setLocalCall();
        return repository.getFileEntriesAndFileShortcuts(l, i, i1, i2);
    }

    public int getFileEntriesAndFileShortcutsCount(long l, int i) 
    		throws SystemException, PortalException {
        setLocalCall();
        return repository.getFileEntriesAndFileShortcutsCount(l, i);
    }

    public int getFileEntriesCount(long l) throws SystemException {
        setLocalCall();
        return repository.getFileEntriesCount(l);
    }

    public FileEntry getFileEntry(long l) throws PortalException, SystemException {
        setLocalCall();
        return repository.getFileEntry(l);
    }

    public FileEntry getFileEntry(long l, String s) throws PortalException, SystemException {
        setLocalCall();
        return repository.getFileEntry(l, s);
    }

    public FileEntry getFileEntryByUuid(String s) throws PortalException, SystemException {
        setLocalCall();
        return repository.getFileEntryByUuid(s);
    }

    public FileVersion getFileVersion(long l) throws PortalException, SystemException {
        setLocalCall();
        return repository.getFileVersion(l);
    }

    public Folder getFolder(long l) throws PortalException, SystemException {
        setLocalCall();
        return repository.getFolder(l);
    }

    public Folder getFolder(long l, String s) throws PortalException, SystemException {
        setLocalCall();
        return repository.getFolder(l, s);
    }

    public List<Folder> getFolders(long l, boolean b, int i, int i1, OrderByComparator orderByComparator) throws PortalException, SystemException {
        setLocalCall();
        return repository.getFolders(l, b, i ,i1, orderByComparator);
    }

    public List<Object> getFoldersAndFileEntriesAndFileShortcuts(long l, int i, boolean b, int i1, int i2, OrderByComparator orderByComparator) throws SystemException {
        setLocalCall();
        return repository.getFoldersAndFileEntriesAndFileShortcuts(l, i, b, i1, i2, orderByComparator);
    }

    public List<Object> getFoldersAndFileEntriesAndFileShortcuts(long l, int i, String[] strings, boolean b, int i1, int i2, OrderByComparator orderByComparator) throws PortalException, SystemException {
        setLocalCall();
        return repository.getFoldersAndFileEntriesAndFileShortcuts(l, i, strings, b, i1, i2, orderByComparator);
    }

    public int getFoldersAndFileEntriesAndFileShortcutsCount(long l, int i, boolean b) throws SystemException {
        setLocalCall();
        return repository.getFoldersAndFileEntriesAndFileShortcutsCount(l, i, b);
    }

    public int getFoldersAndFileEntriesAndFileShortcutsCount(long l, int i, String[] strings, boolean b) throws PortalException, SystemException {
        setLocalCall();
        return repository.getFoldersAndFileEntriesAndFileShortcutsCount(l, i, strings, b);
    }

    public int getFoldersCount(long l, boolean b) throws PortalException, SystemException {
        setLocalCall();
        return repository.getFoldersCount(l, b);
    }

    public int getFoldersFileEntriesCount(List<Long> longs, int i) throws SystemException {
        setLocalCall();
        return repository.getFoldersFileEntriesCount(longs, i);
    }

    public List<Folder> getMountFolders(long l, int i, int i1, OrderByComparator orderByComparator) throws SystemException {
        setLocalCall();
        return repository.getMountFolders(l, i, i1, orderByComparator);
    }

    public int getMountFoldersCount(long l) throws SystemException {
        setLocalCall();
        return repository.getMountFoldersCount(l);
    }

    public long getRepositoryId() {
        setLocalCall();
        return repository.getRepositoryId();
    }

    public FileEntry addFileEntry(long l, long l1, String s, String s1, String s2, String s3, String s4, File file, ServiceContext serviceContext) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
	}

	public FileEntry addFileEntry(long l, long l1, String s, String s1, String s2, String s3, String s4, InputStream inputStream, long l2, ServiceContext serviceContext) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

	public Folder addFolder(long l, long l1, String s, String s1, ServiceContext serviceContext) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

	public void deleteAll() throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

	public FileEntry moveFileEntry(long l, long l1, long l2, ServiceContext serviceContext) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

	public Folder moveFolder(long pUserId, long pFolderId,
			long pParentFolderId, ServiceContext pServiceContext)
			throws PortalException, SystemException {
		throw new UnsupportedOperationException();
	}

	public void updateAsset(long l, FileEntry fileEntry, FileVersion fileVersion, long[] longs, String[] strings, long[] longs1) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

	public FileEntry updateFileEntry(long l, long l1, String s, String s1, String s2, String s3, String s4, boolean b, File file, ServiceContext serviceContext) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

	public FileEntry updateFileEntry(long l, long l1, String s, String s1, String s2, String s3, String s4, boolean b, InputStream inputStream, long l2, ServiceContext serviceContext) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

	public Folder updateFolder(long l, long l1, String s, String s1, ServiceContext serviceContext) throws PortalException, SystemException {
        throw new UnsupportedOperationException();
    }

}
