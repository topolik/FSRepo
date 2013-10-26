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
package cz.topolik.liferay.fsrepo.model;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.security.auth.PrincipalThreadLocal;
import com.liferay.portlet.documentlibrary.model.DLFileEntryConstants;
import com.liferay.portlet.documentlibrary.model.DLFileVersion;
import com.liferay.portlet.documentlibrary.service.DLAppHelperLocalServiceUtil;
import com.liferay.portlet.documentlibrary.util.DLUtil;
import cz.topolik.fsrepo.LocalFileSystemRepository;
import cz.topolik.liferay.fsrepo.FSRepo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;

/**
 * @author Tomas Polesovsky
 */
public class FileSystemFileVersion extends FileSystemModel implements FileVersion {

    private static Log _log = LogFactoryUtil.getLog(FileSystemFileVersion.class);
    private long fileVersionId;
    private FileEntry fileEntry;

    public FileSystemFileVersion(FSRepo repository, long fileVersionId, FileEntry fileEntry, File f) {
        super(repository, null, f);
        this.fileVersionId = fileVersionId;
        this.fileEntry = fileEntry;
    }

    public String getChangeLog() {
        return StringPool.BLANK;
    }

    public InputStream getContentStream(boolean incrementCounter) throws PortalException, SystemException {
        try {
            DLAppHelperLocalServiceUtil.getFileAsStream(
                    PrincipalThreadLocal.getUserId(), getFileEntry(),
                    incrementCounter);
        } catch (Exception e) {
            _log.error(e);
        }

        try {
            return new FileInputStream(localFile);
        } catch (FileNotFoundException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    public String getExtension() {
        return FileUtil.getExtension(getTitle());
    }

    public String getExtraSettings() {
        return null;
    }

    public FileEntry getFileEntry() throws PortalException, SystemException {
        if(fileEntry == null){
            fileEntry = repository.fileToFileEntry(localFile, this);
        }
        return fileEntry;
    }

    public long getFileEntryId() {
        try {
            return fileEntry.getFileEntryId();
        } catch (Exception ex) {
            _log.error(ex);
        }
        return 0;
    }

    public long getFileVersionId() {
        return fileVersionId;
    }

    public String getIcon() {
        return DLUtil.getFileIcon(getExtension());
    }

    public String getMimeType() {
        return MimeTypesUtil.getContentType(localFile.getName());
    }

    public long getSize() {
        return localFile.length();
    }

    public int getStatus() {
        return 0;
    }

    public long getStatusByUserId() {
        return 0;
    }

    public String getStatusByUserName() {
        return null;
    }

    public String getStatusByUserUuid() throws SystemException {
        return null;
    }

    public Date getStatusDate() {
        return getModifiedDate();
    }

    public String getTitle() {
        return localFile.getName();
    }

    public String getVersion() {
        return DLFileEntryConstants.VERSION_DEFAULT;
    }

    public boolean isApproved() {
        return true;
    }

    public boolean isDraft() {
        return false;
    }

    public boolean isExpired() {
        return false;
    }

    public boolean isPending() {
        return false;
    }

    public long getPrimaryKey() {
        return fileVersionId;
    }

    public FileVersion toEscapedModel() {
        return this;
    }

    public Class<?> getModelClass() {
        return DLFileVersion.class;
    }

    @Override
    public String getModelClassName() {
        return DLFileVersion.class.getName();
    }

    @Override
    public void setPrimaryKey(long primaryKey) {
        fileVersionId = primaryKey;
    }
    
    @Override
    public String getName() {
        return getTitle();
    }
}