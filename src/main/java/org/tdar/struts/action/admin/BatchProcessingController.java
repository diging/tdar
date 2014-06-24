package org.tdar.struts.action.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.resource.Project;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.dao.external.auth.InternalTdarRights;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.core.service.resource.ProjectService;
import org.tdar.search.query.SortOption;
import org.tdar.struts.action.AuthenticationAware;
import org.tdar.struts.data.BatchAction;
import org.tdar.struts.interceptor.annotation.PostOnly;

/**
 * $Id$
 * 
 * Manages requests to create/delete/edit a Project and its associated metadata (including Datasets, etc).
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
@ParentPackage("secured")
@Namespace("/admin/batch")
@Component
@Scope("prototype")
public class BatchProcessingController extends AuthenticationAware.Base {

    private static final long serialVersionUID = 3323084509125780562L;
    private List<Resource> filteredFullUserProjects;
    private List<Resource> fullUserProjects;
    private BatchAction batchAction;

    @Autowired
    private transient AuthorizationService authorizationService;

    @Autowired
    private transient ProjectService projectService;
    
    @Override
    @Action("batch")
    public String execute() {
        // removing duplicates
        return SUCCESS;
    }

    @Action("select-action")
    @PostOnly
    public String selectAction() {
        return SUCCESS;
    }

    @Action("confirm-action")
    @PostOnly
    public String confirmAction() {
        return SUCCESS;
    }

    @Action("complete-action")
    @PostOnly
    public String completeAction() {
        return SUCCESS;
    }

    public List<Project> getAllSubmittedProjects() {
        List<Project> allSubmittedProjects = projectService.findBySubmitter(getAuthenticatedUser());
        Collections.sort(allSubmittedProjects);
        return allSubmittedProjects;
    }

    public List<Resource> getFullUserProjects() {
        if (fullUserProjects == null) {
            boolean canEditAnything = authorizationService.can(InternalTdarRights.EDIT_ANYTHING, getAuthenticatedUser());
            fullUserProjects = new ArrayList<Resource>(projectService.findSparseTitleIdProjectListByPerson(getAuthenticatedUser(), canEditAnything));
            Collections.sort(fullUserProjects);
            fullUserProjects.removeAll(getAllSubmittedProjects());
        }
        return fullUserProjects;
    }

    public List<Resource> getFilteredFullUserProjects() {
        if (filteredFullUserProjects == null) {
            filteredFullUserProjects = new ArrayList<Resource>(getFullUserProjects());
            filteredFullUserProjects.removeAll(getAllSubmittedProjects());
        }
        return filteredFullUserProjects;
    }

    public Set<Resource> getEditableProjects() {
        boolean canEditAnything = authorizationService.can(InternalTdarRights.EDIT_ANYTHING, getAuthenticatedUser());
        SortedSet<Resource> findSparseTitleIdProjectListByPerson = new TreeSet<Resource>(projectService.findSparseTitleIdProjectListByPerson(
                getAuthenticatedUser(), canEditAnything));
        return findSparseTitleIdProjectListByPerson;
    }

    public void prepare() {
    }

    public List<Status> getStatuses() {
        return new ArrayList<Status>(authorizationService.getAllowedSearchStatuses(getAuthenticatedUser()));
    }

    public List<ResourceType> getResourceTypes() {
        List<ResourceType> toReturn = new ArrayList<ResourceType>();
        toReturn.addAll(Arrays.asList(ResourceType.values()));
        return toReturn;
    }

    public List<SortOption> getResourceDatatableSortOptions() {
        return SortOption.getOptionsForContext(Resource.class);
    }

    public BatchAction getBatchAction() {
        return batchAction;
    }

    public void setBatchAction(BatchAction batchAction) {
        this.batchAction = batchAction;
    }

}
