package org.tdar.core.dao.resource;

import java.io.IOException;

import org.hibernate.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.bean.resource.VersionType;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.Dao.HibernateBase;
import org.tdar.core.dao.TdarNamedQueries;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.filestore.Filestore;
import org.tdar.filestore.Filestore.ObjectType;

@Component
public class InformationResourceFileVersionDao extends HibernateBase<InformationResourceFileVersion> {

    private static final Filestore filestore = TdarConfiguration.getInstance().getFilestore();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public InformationResourceFileVersionDao() {
        super(InformationResourceFileVersion.class);
    }

    public int deleteDerivatives(InformationResourceFileVersion version) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.QUERY_DELETE_INFORMATION_RESOURCE_FILE_DERIVATIVES);
        query.setParameter("informationResourceFileId", version.getInformationResourceFileId());
        query.setParameterList("derivativeFileVersionTypes", VersionType.getDerivativeVersionTypes());
        return query.executeUpdate();
    }

    public void delete(InformationResourceFileVersion file) {
        delete(file, false);
    }

    public void delete(InformationResourceFileVersion file, boolean purge) {
        if (file.isUploadedOrArchival()) {
            throw new TdarRecoverableRuntimeException("error.cannot_delete_archival");
        }
        if (purge) {
            purgeFromFilestore(file);
        }
        if (file.getInformationResourceFile() != null) {
            file.getInformationResourceFile().getInformationResourceFileVersions().remove(file);
        }
        logger.debug("I'm about to delete file:{}", file);
        super.delete(file);

    }

    public void purgeFromFilestore(InformationResourceFileVersion file) {
        try {
            filestore.purge(ObjectType.RESOURCE, file);
        } catch (IOException e) {
            getLogger().warn("Problems purging file with filestoreID of {} from the filestore.", file.getFilename(), e);
        }

    }

}
