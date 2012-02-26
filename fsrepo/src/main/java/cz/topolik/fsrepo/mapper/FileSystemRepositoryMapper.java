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
package cz.topolik.fsrepo.mapper;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.service.PortalPreferencesLocalServiceUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.portlet.PortletPreferences;
import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;

/**
 * @author Tomas Polesovsky
 */
public class FileSystemRepositoryMapper {

    public static final int PREFERENCES_OWNER_TYPE_REPOSITORY = 1000;
    private static Log _log = LogFactoryUtil.getLog(FileSystemRepositoryMapper.class);
    private FileSystemRepositoryEnvironment environment;
    private MessageDigest md;
    private static final String HASH_ALG = "SHA-256";

    public FileSystemRepositoryMapper(FileSystemRepositoryEnvironment environment) {
        this.environment = environment;
        try {
            md = MessageDigest.getInstance(HASH_ALG);
        } catch (NoSuchAlgorithmException ex) {
            _log.error(ex);
        }
    }

    public File mappedIdToFile(String mappedId) throws FileNotFoundException, SystemException {
        synchronized (this) {
            if (null == getPrefs().getValue(mappedId, null)) {
                _log.error("Cannot find checksum: " + mappedId + ". Now we need to index whole mounted filesystem :/");
                if (!environment.getIndexer().reIndex(false)) {
                    // try to search at least the indexing files, maybe it's already there
                    List<File> indexingFiles = environment.getIndexer().getActuallyIndexedFiles();
                    for(File file : indexingFiles){
                        if(mappedId.equals(fileToMappedId(file, false))){
                            return file;
                        }
                    }
                    throw new SystemException("File system is being indexed. Try again later, please.");
                } else {
                    if (null == getPrefs().getValue(mappedId, null)) {
                        throw new FileNotFoundException("File is no longer accessible on the file system!");
                    }
                }
            }
        }
        return new File(getPrefs().getValue(mappedId, null));
    }

    public String fileToMappedId(File file) {
        return fileToMappedId(file, true);
    }

    public String fileToMappedId(File file, boolean save) {
        md.reset();
        md.update(file.getAbsolutePath().getBytes());
        byte[] mdbytes = md.digest();

        //convert the byte to hex format
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        String mappedId = sb.toString();

        if (save) {
            update(mappedId, file);
        }

        return mappedId;
    }

    public void add(File file) {
        fileToMappedId(file);
    }

    public void addAll(List<File> files) {
        PortletPreferences prefs = getPrefs();
        try {
            for (File file : files) {
                String checksum = fileToMappedId(file, false);
                prefs.setValue(checksum, file.getAbsolutePath());
            }
            prefs.store();
        } catch (IOException ex) {
            _log.error(ex);
        } catch (ValidatorException ex) {
            _log.error(ex);
        } catch (ReadOnlyException ex) {
            _log.error(ex);
        }
    }

    public void remove(File file) {
        try {
            PortletPreferences prefs = getPrefs();
            String checksum = fileToMappedId(file);
            prefs.reset(checksum);
            prefs.store();
        } catch (IOException ex) {
            _log.error(ex);
        } catch (ValidatorException ex) {
            _log.error(ex);
        } catch (ReadOnlyException ex) {
            _log.error(ex);
        }
    }

    protected void update(String mappedId, File file) {
        PortletPreferences prefs = getPrefs();
        if (prefs.getValue(mappedId, null) != null) {
            // let's hope there won't be any collision in SHA-256
            return;
        }
        try {
            prefs.setValue(mappedId, file.getAbsolutePath());
            prefs.store();
        } catch (IOException ex) {
            _log.error(ex);
        } catch (ValidatorException ex) {
            _log.error(ex);
        } catch (ReadOnlyException ex) {
            _log.error(ex);
        }
    }

    protected PortletPreferences getPrefs() {
        long companyId = environment.getRepository().getCompanyId();
        long ownerId = environment.getRepository().getRepositoryId();
        try {
            return PortalPreferencesLocalServiceUtil.getPreferences(companyId, ownerId, PREFERENCES_OWNER_TYPE_REPOSITORY);
        } catch (SystemException ex) {
            _log.error("Problem while fetching preferences for PortalFileIndexer", ex);
        }
        return null;
    }
}
