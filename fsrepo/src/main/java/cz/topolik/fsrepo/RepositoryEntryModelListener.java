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

import com.liferay.portal.ModelListenerException;
import com.liferay.portal.model.ModelListener;
import com.liferay.portal.model.RepositoryEntry;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.expando.service.ExpandoRowLocalServiceUtil;

/**
 *
 * @author Tomas Polesovsky
 */
public class RepositoryEntryModelListener implements ModelListener<RepositoryEntry> {

    public void onAfterAddAssociation(Object o, String string, Object o1) throws ModelListenerException {
        
    }

    public void onAfterCreate(RepositoryEntry t) throws ModelListenerException {
        
    }

    public void onAfterRemove(RepositoryEntry repositoryEntry) throws ModelListenerException {
        try {
            long companyId = GroupLocalServiceUtil.getGroup(repositoryEntry.getGroupId()).getCompanyId();
            ExpandoRowLocalServiceUtil.deleteRow(companyId, RepositoryEntry.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME, repositoryEntry.getRepositoryEntryId());
        } catch (Exception ex) {
            throw new ModelListenerException("Cannot remove expando attributes for RepositoryEntry", ex);
        }
    }

    public void onAfterRemoveAssociation(Object o, String string, Object o1) throws ModelListenerException {
        
    }

    public void onAfterUpdate(RepositoryEntry t) throws ModelListenerException {
        
    }

    public void onBeforeAddAssociation(Object o, String string, Object o1) throws ModelListenerException {
        
    }

    public void onBeforeCreate(RepositoryEntry t) throws ModelListenerException {
        
    }

    public void onBeforeRemove(RepositoryEntry t) throws ModelListenerException {
        
    }

    public void onBeforeRemoveAssociation(Object o, String string, Object o1) throws ModelListenerException {
        
    }

    public void onBeforeUpdate(RepositoryEntry t) throws ModelListenerException {
        
    }

}
