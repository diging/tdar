package org.tdar.struts.action.resource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.TdarGroup;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.entity.permissions.GeneralPermissions;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.ResourceRevisionLog;
import org.tdar.core.service.ResourceCollectionService;
import org.tdar.core.service.XmlService;
import org.tdar.core.service.resource.ResourceService;
import org.tdar.struts.action.AuthenticationAware;
import org.tdar.struts.action.TdarActionException;
import org.tdar.struts.interceptor.annotation.RequiresTdarUserGroup;

import com.opensymphony.xwork2.Preparable;

@Component
@Scope("prototype")
@ParentPackage("secured")
@Namespace("/resource")
@RequiresTdarUserGroup(TdarGroup.TDAR_EDITOR)
public class ResourceAdminController extends AuthenticationAware.Base implements Preparable {

    private static final long serialVersionUID = -2071449250711089300L;
    public static final String ADMIN = "admin";
    private List<ResourceRevisionLog> resourceLogEntries;

    private List<ResourceRevisionLog> logEntries;
    private Set<ResourceCollection> effectiveResourceCollections = new HashSet<>();

    private Resource resource;
    private Long id;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ResourceCollectionService resourceCollectionService;

    @Autowired
    private XmlService xmlService;

    @Action(value = ADMIN, results = {
            @Result(name = SUCCESS, location = "../resource/admin.ftl")
    })
    public String viewAdmin() throws TdarActionException {
        setResourceLogEntries(resourceService.getLogsForResource(getResource()));
        getEffectiveResourceCollections().addAll(resourceCollectionService.getEffectiveResourceCollectionsForResource(getResource()));
        return SUCCESS;
    }

    @Override
    public void prepare() throws Exception {
        if (Persistable.Base.isNotNullOrTransient(getId())) {
            setResource(resourceService.find(getId()));
        } else {
            addActionError(getText("resourceAdminController.valid_resource_required"));
        }
    }

    public List<ResourceRevisionLog> getLogEntries() {
        return logEntries;
    }

    public void setLogEntries(List<ResourceRevisionLog> logEntries) {
        this.logEntries = logEntries;
    }

    public List<ResourceRevisionLog> getResourceLogEntries() {
        return resourceLogEntries;
    }

    public void setResourceLogEntries(List<ResourceRevisionLog> resourceLogEntries) {
        this.resourceLogEntries = resourceLogEntries;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Set<ResourceCollection> getEffectiveResourceCollections() {
        return effectiveResourceCollections;
    }

    public void setEffectiveResourceCollections(Set<ResourceCollection> effectiveResourceCollections) {
        this.effectiveResourceCollections = effectiveResourceCollections;
    }

    public List<GeneralPermissions> getAvailablePermissions() {
        List<GeneralPermissions> permissions = new ArrayList<GeneralPermissions>();
        for (GeneralPermissions permission : GeneralPermissions.values()) {
            if ((permission.getContext() == null) || Resource.class.isAssignableFrom(permission.getContext())) {
                permissions.add(permission);
            }
        }
        return permissions;
    }

}
