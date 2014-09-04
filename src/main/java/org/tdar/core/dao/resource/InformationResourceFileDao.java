package org.tdar.core.dao.resource;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.CriteriaSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.resource.Dataset;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.InformationResourceFile;
import org.tdar.core.bean.resource.InformationResourceFile.FileStatus;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.bean.resource.ResourceProxy;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.bean.resource.VersionType;
import org.tdar.core.dao.Dao.HibernateBase;
import org.tdar.core.dao.TdarNamedQueries;
import org.tdar.core.exception.TdarRecoverableRuntimeException;

@Component
public class InformationResourceFileDao extends HibernateBase<InformationResourceFile> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public InformationResourceFileDao() {
        super(InformationResourceFile.class);
    }

    @Autowired
    private InformationResourceFileVersionDao informationResourceFileVersionDao;

    public InformationResourceFile findByFilestoreId(String filestoreId) {
        return findByProperty("filestoreId", filestoreId);
    }

    public Map<String, Float> getAdminFileExtensionStats() {
        Query query = getCurrentSession().getNamedQuery(QUERY_KEYWORD_COUNT_FILE_EXTENSION);
        query.setParameterList("internalTypes", Arrays.asList(VersionType.ARCHIVAL,
                VersionType.UPLOADED, VersionType.UPLOADED_ARCHIVAL));
        Map<String, Float> toReturn = new HashMap<String, Float>();
        Long total = 0l;
        for (Object o : query.list()) {
            try {
                Object[] objs = (Object[]) o;
                if ((objs == null) || (objs[0] == null)) {
                    continue;
                }
                toReturn.put(String.format("%s (%s)", objs[0], objs[1]), ((Long) objs[1]).floatValue());
                total += (Long) objs[1];
            } catch (Exception e) {
                logger.debug("exception get admin file extension stats", e);
            }
        }

        for (String key : toReturn.keySet()) {
            toReturn.put(key, (toReturn.get(key) * 100) / total.floatValue());
        }

        return toReturn;
    }

    public Number getDownloadCount(InformationResourceFile irFile) {
        String sql = String.format(TdarNamedQueries.DOWNLOAD_COUNT_SQL, irFile.getId(), new Date());
        return (Number) getCurrentSession().createSQLQuery(sql).uniqueResult();
    }

    public void deleteTranslatedFiles(Dataset dataset) {
        for (InformationResourceFile irFile : dataset.getInformationResourceFiles()) {
            logger.debug("deleting {}", irFile);
            deleteTranslatedFiles(irFile);
        }
    }

    public void deleteTranslatedFiles(InformationResourceFile irFile) {
        for (InformationResourceFileVersion version : irFile.getLatestVersions()) {
            logger.debug("deleting version:{}  isTranslated:{}", version, version.isTranslated());
            if (version.isTranslated()) {
                // HQL here avoids issue where hibernate delays the delete
                deleteVersionImmediately(version);
                // we don't need safeguards on a translated file, so tell the dao to delete no matter what.
                // informationResourceFileVersionDao.forceDelete(version);
            }
        }
    }

    public void deleteVersionImmediately(InformationResourceFileVersion version) {
        if (Persistable.Base.isNullOrTransient(version)) {
            throw new TdarRecoverableRuntimeException("error.cannot_delete_transient");
        }

        if (version.isUploadedOrArchival()) {
            throw new TdarRecoverableRuntimeException("error.cannot_delete_archival");
        }
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.DELETE_INFORMATION_RESOURCE_FILE_VERSION_IMMEDIATELY);
        query.setParameter("id", version.getId()).executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<InformationResourceFile> findFilesWithStatus(FileStatus[] statuses) {
        Query query = getCurrentSession().getNamedQuery(QUERY_FILE_STATUS);
        query.setParameterList("statuses", Arrays.asList(statuses));
        return query.list();
    }

    public List<InformationResource> findInformationResourcesWithFileStatus(
            Person authenticatedUser, List<Status> resourceStatus,
            List<FileStatus> fileStatus) {
        Query query = getCurrentSession().getNamedQuery(QUERY_RESOURCE_FILE_STATUS);
        query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        query.setParameterList("statuses", resourceStatus);
        query.setParameterList("fileStatuses", fileStatus);
        query.setParameter("submitterId", authenticatedUser.getId());
        List<InformationResource> list = new ArrayList<>();
        for (ResourceProxy proxy : (List<ResourceProxy>) query.list()) {
            try {
                list.add((InformationResource) proxy.generateResource());
            } catch (IllegalAccessException | InvocationTargetException
                    | InstantiationException e) {
                logger.error("error happened manifesting: {} ", e);
            }
        }
        return list;
    }

    public ScrollableResults findScrollableVersionsForVerification() {
        Query query = getCurrentSession().getNamedQuery(QUERY_INFORMATION_RESOURCE_FILE_VERSION_VERIFICATION);
        return query.setReadOnly(true).setCacheable(false).scroll(ScrollMode.FORWARD_ONLY);
    }
}
