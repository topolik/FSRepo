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

import cz.topolik.fsrepo.mapper.FileSystemRepositoryMapper;
import com.liferay.portal.ModelListenerException;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.model.ModelListener;
import com.liferay.portal.model.PortalPreferences;
import com.liferay.portal.model.Repository;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.PortalPreferencesLocalServiceUtil;
import java.util.List;

/**
 *
 * @author Tomas Polesovsky
 */
public class RepositoryModelListener implements ModelListener<Repository> {

    public void onAfterAddAssociation(Object o, String string, Object o1) throws ModelListenerException {
        
    }

    public void onAfterCreate(Repository t) throws ModelListenerException {
        
    }

    public void onAfterRemove(Repository fileRepository) throws ModelListenerException {
        if(fileRepository.getClassNameId() != ClassNameLocalServiceUtil.getClassNameId(LocalFileSystemRepository.class.getName())){
            return;
        }
        
        long ownerId = fileRepository.getRepositoryId();
        int ownerType = Constants.PREFERENCES_OWNER_TYPE_REPOSITORY;
        try {
            DynamicQuery query = DynamicQueryFactoryUtil.forClass(PortalPreferences.class, PortalClassLoaderUtil.getClassLoader());
            query.add(RestrictionsFactoryUtil.eq("ownerId", ownerId));
            query.add(RestrictionsFactoryUtil.eq("ownerType", ownerType));
            List<PortalPreferences> prefs = PortalPreferencesLocalServiceUtil.dynamicQuery(query);
            if(prefs.size() > 1){
                throw new ModelListenerException("Database is in incompatible state! " +
                        "Cannot delete associated preferences [ownerId, ownerType]: ["
                    + ownerId + ", " + ownerType + "]");
            }
            if(prefs.size() > 0){
                PortalPreferencesLocalServiceUtil.deletePortalPreferences(prefs.get(0));
            }
        } catch (Exception ex) {
            throw new ModelListenerException("Cannot delete associated preferences [ownerId, ownerType]: ["
                    + ownerId + ", " + ownerType + "]", ex);
        }
    }

    public void onAfterRemoveAssociation(Object o, String string, Object o1) throws ModelListenerException {
        
    }

    public void onAfterUpdate(Repository t) throws ModelListenerException {
        
    }

    public void onBeforeAddAssociation(Object o, String string, Object o1) throws ModelListenerException {
        
    }

    public void onBeforeCreate(Repository t) throws ModelListenerException {
        
    }

    public void onBeforeRemove(Repository t) throws ModelListenerException {
        
    }

    public void onBeforeRemoveAssociation(Object o, String string, Object o1) throws ModelListenerException {
        
    }

    public void onBeforeUpdate(Repository t) throws ModelListenerException {
        
    }

}
