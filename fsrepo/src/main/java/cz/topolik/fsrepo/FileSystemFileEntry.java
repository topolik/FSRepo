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
 */package cz.topolik.fsrepo;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.Lock;
import com.liferay.portal.security.auth.PrincipalThreadLocal;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.DLAppHelperLocalServiceUtil;
import com.liferay.portlet.documentlibrary.util.DLUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Tomas Polesovsky
 */
public class FileSystemFileEntry extends FileSystemModel implements FileEntry {

    private static Log _log = LogFactoryUtil.getLog(FileSystemFileEntry.class);
    private FileVersion fileVersion;
    private long fileEntryId;
    private Folder parentFolder;

    public FileSystemFileEntry(LocalFileSystemRepository repository, String uuid, long fileEntryId, Folder parentFolder, File localFile, FileVersion fileVersion) {
        super(repository, uuid, localFile);

        this.fileEntryId = fileEntryId;
        this.parentFolder = parentFolder;
        this.fileVersion = fileVersion;
    }

    public InputStream getContentStream() throws PortalException, SystemException {
        try {
            DLAppHelperLocalServiceUtil.getFileAsStream(
                    PrincipalThreadLocal.getUserId(), this,
                    true);
        } catch (Exception e) {
            _log.error(e);
        }

        try {
            return new FileInputStream(localFile);
        } catch (FileNotFoundException ex) {
            _log.error(ex);
            return null;
        }
    }

    public InputStream getContentStream(String version) throws PortalException, SystemException {
        return getContentStream();
    }

    public String getExtension() {
        return FileUtil.getExtension(getTitle());
    }

    public long getFileEntryId() {
        return fileEntryId;
    }

    public FileVersion getFileVersion() throws PortalException, SystemException {
        if(fileVersion == null){
            fileVersion = repository.fileToFileVersion(localFile, this);
        }
        return fileVersion;
    }

    public FileVersion getFileVersion(String version) throws PortalException, SystemException {
        return getFileVersion();
    }

    public List<FileVersion> getFileVersions(int status) throws SystemException {
        try {
            return Arrays.asList(new FileVersion[]{getFileVersion()});
        } catch (PortalException ex) {
            _log.error(ex);
        }
        return new ArrayList<FileVersion>();
    }

    public Folder getFolder() {
        try {
            if (parentFolder != null) {
                return parentFolder;
            }

            parentFolder = repository.fileToFolder(localFile.getParentFile());
            return parentFolder;
        } catch (SystemException ex) {
            _log.error(ex);
        }
        return null;
    }

    public long getFolderId() {
        return getFolder().getFolderId();
    }

    public String getIcon() {
        return DLUtil.getFileIcon(getExtension());
    }

    public FileVersion getLatestFileVersion() throws PortalException, SystemException {
        return getFileVersion();
    }

    public String getMimeType() {
        return MimeTypesUtil.getContentType(localFile.getName());
    }

    public String getMimeType(String version) {
        return getMimeType();
    }

    public int getReadCount() {
        return 0;
    }

    public long getSize() {
        return localFile.length();
    }

    public String getTitle() {
        return localFile.getName();
    }

    public String getVersion() {
        return "1.0";
    }

    public long getVersionUserId() {
        return getUserId();
    }

    public String getVersionUserName() {
        return StringPool.BLANK;
    }

    public String getVersionUserUuid() throws SystemException {
        return StringPool.BLANK;
    }

    public boolean isCheckedOut() {
        return false;
    }

    public long getPrimaryKey() {
        return fileEntryId;
    }

    public FileEntry toEscapedModel() {
        return this;
    }

    public Class<?> getModelClass() {
        return DLFileEntry.class;
    }

    @Override
    public String getModelClassName() {
        return DLFileEntry.class.getName();
    }

    public Lock getLock() {
        return null;
    }

    @Override
    public void setPrimaryKey(long primaryKey) {
        fileEntryId = primaryKey;
    }
}