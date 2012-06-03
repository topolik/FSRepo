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
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.CompanyThreadLocal;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.expando.model.ExpandoColumn;
import cz.topolik.fsrepo.model.FileSystemModel;

/**
 *
 * @author Tomas Polesovsky
 */
public class LocalFileSystemPermissionsUtil {

    public static PermissionChecker getPermissionChecker() {
        PermissionChecker permissionChecker = PermissionThreadLocal.getPermissionChecker();
        if(permissionChecker == null){
            try {
                // initialize to guest
                Long companyId = CompanyThreadLocal.getCompanyId();
                if(companyId == null){
                    companyId = PortalUtil.getDefaultCompanyId();
                }
                User defaultUser = UserLocalServiceUtil.getDefaultUser(companyId);
                permissionChecker = PermissionCheckerFactoryUtil.create(defaultUser, true);
                PermissionThreadLocal.setPermissionChecker(permissionChecker);
            } catch (Exception e){
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return permissionChecker;
    }

    public static void checkFolder(long groupId, long folderId, String actionId) throws PrincipalException {
        if (!containsFolder(groupId, folderId, actionId)) {
            throw new PrincipalException();
        }
    }

    public static void checkFileEntry(long groupId, long fileEntryId, String actionId) throws PrincipalException {
        if (!containsFileEntry(groupId, fileEntryId, actionId)) {
            throw new PrincipalException();
        }
    }

    public static boolean containsFolder(long groupId, long folderId, String actionId) {
        if(LocalFileSystemLocalRepository.isLocalCall()){
            return true;
        }
        return getPermissionChecker().hasPermission(groupId, DLFolder.class.getName(), folderId, actionId);
    }

    public static boolean containsFileEntry(long groupId, long fileEntryId, String actionId) {
        if(LocalFileSystemLocalRepository.isLocalCall()){
            return true;
        }
        return getPermissionChecker().hasPermission(groupId, DLFileEntry.class.getName(), fileEntryId, actionId);
    }

    public static boolean contains(FileSystemModel model, String actionId){
        if(LocalFileSystemLocalRepository.isLocalCall()){
            return true;
        }
        return getPermissionChecker().hasPermission(model.getGroupId(), model.getModelClassName(), model.getPrimaryKey(), actionId);
    }
}
