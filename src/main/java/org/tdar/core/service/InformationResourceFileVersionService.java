package org.tdar.core.service;

import java.io.IOException;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.resource.InformationResourceFileVersionDao;
import org.tdar.filestore.Filestore;

@Service
public class InformationResourceFileVersionService
        extends ServiceInterface.TypedDaoBase<InformationResourceFileVersion, InformationResourceFileVersionDao> {

    @Autowired
    public void setDao(InformationResourceFileVersionDao dao) {
        super.setDao(dao);
    }

    private static final Filestore filestore = TdarConfiguration.getInstance().getFilestore();

    /**
     * Deletes this information resource file from the filestore, database. Also removes the
     * translated file if it exists.
     * 
     * @param file
     */
    @Transactional(readOnly = false)
    public void delete(InformationResourceFileVersion file) {
        delete(file, true);
    }

    @Transactional(readOnly = false)
    public void delete(InformationResourceFileVersion file, boolean purge) {
        if (purge) {
            purgeFromFilestore(file);
        }
        if (file.getInformationResourceFile() != null) {
            file.getInformationResourceFile().getInformationResourceFileVersions().remove(file);
        }
        super.delete(file);
    }

    @Override
    public void delete(Collection<?> files) {
        for (Object object : files) {
            if (object instanceof InformationResourceFileVersion) {
                delete((InformationResourceFileVersion) object);
            } else {
                super.delete(object);
            }
        }
    }

    public void purgeFromFilestore(InformationResourceFileVersion file) {
        try {
            filestore.purge(file);
        } catch (IOException e) {
            getLogger().warn("Problems purging file with filestoreID of" +
                    file.getFilename() + " from the filestore.", e);
        }
    }

}
