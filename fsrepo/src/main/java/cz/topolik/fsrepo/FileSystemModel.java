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

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoBridge;
import com.liferay.portlet.expando.util.ExpandoBridgeFactoryUtil;
import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Tomas Polesovsky
 */
public abstract class FileSystemModel {

    private static Log _log = LogFactoryUtil.getLog(FileSystemModel.class);
    private static Set<String> _mappedActionKeys = new HashSet<String>();
    private static Set<String> _unsupportedActionKeys = new HashSet<String>();

    static {
        _mappedActionKeys.add(ActionKeys.ACCESS);
        _mappedActionKeys.add(ActionKeys.ADD_DOCUMENT);
        _mappedActionKeys.add(ActionKeys.ADD_FOLDER);
        _mappedActionKeys.add(ActionKeys.ADD_SUBFOLDER);
        _mappedActionKeys.add(ActionKeys.DELETE);
        _mappedActionKeys.add(ActionKeys.UPDATE);
        _mappedActionKeys.add(ActionKeys.VIEW);

        _unsupportedActionKeys.add(ActionKeys.ADD_DISCUSSION);
        _unsupportedActionKeys.add(ActionKeys.ADD_SHORTCUT);
        _unsupportedActionKeys.add(ActionKeys.DELETE_DISCUSSION);
        _unsupportedActionKeys.add(ActionKeys.PERMISSIONS);
        _unsupportedActionKeys.add(ActionKeys.UPDATE_DISCUSSION);
    }
    protected LocalFileSystemRepository repository;
    protected String uuid;
    protected File localFile;

    public FileSystemModel(LocalFileSystemRepository repository, String uuid, File localFile) {
        this.repository = repository;
        this.uuid = uuid;
        this.localFile = localFile;
    }

    public boolean containsPermission(PermissionChecker permissionChecker, String actionId) throws PortalException, SystemException {
        if (_unsupportedActionKeys.contains(actionId)) {
            return false;
        }

        if (_mappedActionKeys.contains(actionId)) {
            if (actionId.equals(ActionKeys.ACCESS) || actionId.equals(ActionKeys.VIEW)) {
                return localFile.canRead();
            } else {
                return localFile.canWrite();
            }
        }

        return false;
    }

    public long getCompanyId() {
        return repository.getCompanyId();
    }

    public Date getCreateDate() {
        return getModifiedDate();
    }

    public String getDescription() {
        return StringPool.BLANK;
    }

    public long getGroupId() {
        return repository.getGroupId();
    }

    public Date getModifiedDate() {
        return new Date(localFile.lastModified());
    }

    public long getRepositoryId() {
        return repository.getRepositoryId();
    }

    public long getUserId() {
        try {
            return UserLocalServiceUtil.getDefaultUserId(getCompanyId());
        } catch (Exception ex) {
            _log.error(ex);
            throw new RuntimeException(ex.getMessage(), ex);

        }
    }

    public String getUserName() {
        return StringPool.BLANK;
    }

    public String getUserUuid() throws SystemException {
        return StringPool.BLANK;
    }

    public String getUuid() {
        return uuid;
    }

    public boolean hasInheritableLock() {
        return false;
    }

    public boolean hasLock() {
        return false;
    }

    public boolean isDefaultRepository() {
        return false;
    }

    public boolean isLocked() {
        return false;
    }

    public boolean isSupportsLocking() {
        return false;
    }

    public boolean isSupportsMetadata() {
        return false;
    }

    public boolean isSupportsSocial() {
        return false;
    }

    public Map<String, Serializable> getAttributes() {
        return new HashMap<String, Serializable>();
    }

    public boolean isEscapedModel() {
        return false;
    }

    public void setCompanyId(long companyId) {
        repository.setCompanyId(companyId);
    }

    public void setCreateDate(Date date) {
    }

    public void setGroupId(long groupId) {
        repository.setGroupId(groupId);
    }

    public void setModifiedDate(Date date) {
    }

    public void setPrimaryKeyObj(Serializable primaryKeyObj) {
        setPrimaryKey(((Long) primaryKeyObj).longValue());
    }

    public void setUserId(long userId) {
    }

    public void setUserName(String userName) {
    }

    public void setUserUuid(String userUuid) {
    }

    public ExpandoBridge getExpandoBridge() {
        return ExpandoBridgeFactoryUtil.getExpandoBridge(
                getCompanyId(), getModelClassName(), getPrimaryKey());
    }

    public Serializable getPrimaryKeyObj() {
        return getPrimaryKey();
    }

    public Object getModel() {
        return localFile;
    }

    public abstract long getPrimaryKey();

    public abstract String getModelClassName();

    public abstract void setPrimaryKey(long primaryKey);
}