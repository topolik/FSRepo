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

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.SimpleAction;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.BaseRepository;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.model.Repository;
import com.liferay.portal.service.RepositoryLocalServiceUtil;
import java.util.List;

/**
 *
 * @author Tomas Polesovsky
 */
public class RepositoryStartupAction extends SimpleAction {

    private static Log _log = LogFactoryUtil.getLog(RepositoryStartupAction.class);

    @Override
    public void run(String[] strings) throws ActionException {
        long companyId = GetterUtil.getLong(strings[0]);
        try {
            initAll(companyId);
        } catch (Exception ex) {
            _log.error("Cannot reindex FileSystemRepository: " + ex.getMessage(), ex);
        }
    }

    protected void initAll(long companyId) throws PortalException, SystemException {
        List<Repository> repositories = RepositoryLocalServiceUtil.getRepositories(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
        for (Repository repo : repositories) {
            if (repo.getCompanyId() == companyId) {
                init(repo);
            }
        }
    }

    protected void init(Repository repo) throws PortalException, SystemException {
        if (LocalFileSystemRepository.class.getName().equals(repo.getClassName())) {
            BaseRepository fsRepo = (BaseRepository) RepositoryLocalServiceUtil.getRepositoryImpl(repo.getRepositoryId());
            fsRepo.initRepository();
        }
    }
}
