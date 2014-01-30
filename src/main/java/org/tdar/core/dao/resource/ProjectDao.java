package org.tdar.core.dao.resource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.hibernate.Query;
import org.hibernate.type.StandardBasicTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.Project;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.Status;

/**
 * $Id$
 * 
 * Hibernate dao implementation for TDAR Projects.
 * 
 * @author <a href='Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
@Component("projectDao")
@SuppressWarnings("unchecked")
public class ProjectDao extends ResourceDao<Project> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ProjectDao() {
        super(Project.class);
    }

    public List<Project> findAllEditableProjects(final Person person) {
        Query query = getCurrentSession().getNamedQuery(QUERY_READ_ONLY_EDITABLE_PROJECTS);// QUERY_PROJECT_EDITABLE
        query.setLong("submitterId", person.getId());
        return query.list();
    }


    public Boolean containsIntegratableDatasets(Project project) {
        Query query = getCurrentSession().getNamedQuery(QUERY_PROJECT_COUNT_INTEGRATABLE_DATASETS);
        query.setLong("projectId", project.getId());
        return (Long) query.uniqueResult() > 0;
    }

    public Boolean containsIntegratableDatasets(List<Long> projectIds) {
        if (projectIds.isEmpty())
            return Boolean.FALSE;
        Query query = getCurrentSession().getNamedQuery(QUERY_PROJECTS_COUNT_INTEGRATABLE_DATASETS);
        logger.debug("setting parameter list");
        query.setParameterList("projectIdList", projectIds, StandardBasicTypes.LONG);
        return (Long) query.uniqueResult() > 0;
    }

    // TODO:maxResults ignored for now. You can have as many results as you'd like, so long as it's 5
    public List<Resource> findSparseRecentlyEditedResources(Person updater, int maxResults) {
        Query query = getCurrentSession().getNamedQuery(QUERY_SPARSE_RECENT_EDITS);
        query.setLong("personId", updater.getId());
        query.setMaxResults(maxResults);
        return query.list();
    }

    /**
     * @param updater
     * @return
     */
    public List<Project> findEmptyProjects(Person updater) {
        Query query = getCurrentSession().getNamedQuery(QUERY_SPARSE_EMPTY_PROJECTS);
        query.setLong("submitter", updater.getId());
        return query.list();
    }

    /**
     * @return
     */
    public List<Project> findAllSparse() {
        Query query = getCurrentSession().getNamedQuery(QUERY_SPARSE_PROJECTS);
        return query.list();
    }

    public Set<InformationResource> findAllResourcesInProject(Project project, Status... statuses) {
        Query query = getCurrentSession().getNamedQuery(QUERY_RESOURCES_IN_PROJECT);
        if (ArrayUtils.isNotEmpty(statuses)) {
            query = getCurrentSession().getNamedQuery(QUERY_RESOURCES_IN_PROJECT_WITH_STATUS);
            query.setParameterList("statuses", statuses);
        }
        query.setParameter("projectId", project.getId());
        project.setCachedInformationResources(new HashSet<InformationResource>(query.list()));
        return project.getCachedInformationResources();
    }

}
