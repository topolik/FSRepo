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
package cz.topolik.fsrepo.model;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.lar.StagedModelType;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;

import cz.topolik.fsrepo.LocalFileSystemRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Tomas Polesovsky
 */
public class FileSystemFolder extends FileSystemModel implements Folder {

    private File folder;
    private long folderId;

    public FileSystemFolder(LocalFileSystemRepository repository, String uuid, long folderId, File folder) {
        super(repository, uuid, folder);
        this.folder = folder;
        this.folderId = folderId;
    }
    
    public Object clone() {
    	FileSystemFolder lFSFolder = new FileSystemFolder(repository, uuid, folderId, folder);
    	return lFSFolder;
    }

    public List<Folder> getAncestors() throws PortalException, SystemException {
        List<Folder> result = new ArrayList<Folder>();

        Folder f = this;
        while (!f.isRoot()) {
            f = f.getParentFolder();
            result.add(f);
        }

        return result;
    }
    
	public List<Long> getAncestorFolderIds() throws PortalException, SystemException {
        List<Long> result = new ArrayList<Long>();

        Folder f = this;
        while (!f.isRoot()) {
            f = f.getParentFolder();
            result.add(f.getFolderId());
        }
        return result;
	}

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    public long getFolderId() {
        return folderId;
    }

    public Date getLastPostDate() {
        return new Date(folder.lastModified());
    }

    public String getName() {
        return folder.getName();
    }

    public long getParentFolderId() {
        try {
            Folder f = getParentFolder();
            return f == null ? DLFolderConstants.DEFAULT_PARENT_FOLDER_ID : f.getFolderId();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return DLFolderConstants.DEFAULT_PARENT_FOLDER_ID;
    }

    public boolean isMountPoint() {
        return false;
    }

    public boolean isRoot() {
        if (getParentFolderId() == DLFolderConstants.DEFAULT_PARENT_FOLDER_ID) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isSupportsMultipleUpload() {
        return true;
    }

    public boolean isSupportsShortcuts() {
        return false;
    }

	public boolean isSupportsSubscribing() {
		return false;
	}
    
    public Class<?> getModelClass() {
        return DLFolder.class;
    }

    public String getModelClassName() {
        return DLFolder.class.getName();
    }

    public Folder toEscapedModel() {
        return this;
    }

	public Folder toUnescapedModel() {
		return this;
	}

    @Override
    public long getPrimaryKey() {
        return folderId;
    }

    @Override
    public void setPrimaryKey(long primaryKey) {
        setFolderId(primaryKey);
    }

	public StagedModelType getStagedModelType() {
		return new StagedModelType(Folder.class);
	}

	public void setUuid(String pUuid) {
		super.uuid = pUuid;		
	}
}
