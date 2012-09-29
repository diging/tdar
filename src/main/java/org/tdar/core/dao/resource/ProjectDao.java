package org.tdar.core.dao.resource;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.resource.Project;
import org.tdar.core.bean.resource.Resource;

/**
 * $Id$
 * 
 * Hibernate dao implementation for TDAR Projects.
 * 
 * @author <a href='Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
@Component("projectDao")
public class ProjectDao extends ResourceDao<Project> {

    public ProjectDao() {
        super(Project.class);
    }

    @SuppressWarnings("unchecked")
    public List<Project> findAllEditableProjects(final Person person) {
        Query query = getCurrentSession().getNamedQuery(QUERY_READ_ONLY_EDITABLE_PROJECTS);// QUERY_PROJECT_EDITABLE
        query.setLong("submitterId", person.getId());
        return (List<Project>) query.list();
    }

    @SuppressWarnings("unchecked")
    public List<Project> findAllOtherProjects(final Person person) {
        Query query = getCurrentSession().getNamedQuery(QUERY_PROJECT_ALL_OTHER);
        query.setLong("personId", person.getId());
        return (List<Project>) query.list();
    }

    @SuppressWarnings("unchecked")
    public List<Project> findViewableProjects(Person person) {
        Query query = getCurrentSession().getNamedQuery(QUERY_PROJECT_VIEWABLE);
        query.setLong("personId", person.getId());
        return (List<Project>) query.list();
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
    @SuppressWarnings("unchecked")
    public List<Resource> findSparseRecentlyEditedResources(Person updater, int maxResults) {
        Query query = getCurrentSession().getNamedQuery(QUERY_SPARSE_RECENT_EDITS);
        query.setLong("personId", updater.getId());
        query.setMaxResults(maxResults);
        return (List<Resource>) query.list();
    }

    /**
     * @param updater
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Project> findEmptyProjects(Person updater) {
        Query query = getCurrentSession().getNamedQuery(QUERY_SPARSE_EMPTY_PROJECTS);
        query.setLong("submitter", updater.getId());
        return (List<Project>) query.list();
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Project> findAllSparse() {
        Query query = getCurrentSession().getNamedQuery(QUERY_SPARSE_PROJECTS);
        return (List<Project>) query.list();
    }

}
