package org.tdar.struts.action.resource;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.URLConstants;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.service.billing.AccountService;
import org.tdar.struts.action.AuthenticationAware;

/**
 * $Id$
 * 
 * Can probably remove this controller class.
 * 
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Rev$
 */
@ParentPackage("secured")
@Component
@Scope("prototype")
@Namespace("/resource")
public class ResourceController extends AuthenticationAware.Base {

    private static final long serialVersionUID = 7080916909862991142L;

    public static final String BILLING = "billing";

    @Autowired
    private transient AccountService accountService;

    // incoming data from /resource/add
    private ResourceType resourceType;
    private Long projectId;

    private Long resourceId;

    /**
     * Passthrough action, just loads add.ftl via conventions plugin.
     */
    @Action(value = "add",
            results = {
                    @Result(name = BILLING, type = TYPE_REDIRECT, location = URLConstants.CART_ADD),
                    @Result(name = CONTRIBUTOR, type = TYPE_REDIRECT, location = URLConstants.MY_PROFILE),
                    @Result(name = SUCCESS, location = "add.ftl")
            })
    @Override
    public String execute() {
        if (!isContributor()) {
            addActionMessage(getText("resourceController.must_be_contributor"));
            return CONTRIBUTOR;
        }
        if (!getTdarConfiguration().isPayPerIngestEnabled() || isAllowedToCreateResource()) {
            return SUCCESS;
        }
        addActionMessage(getText("resourceController.requires_funds"));
        return BILLING;
    }

    /**
     * Used to edit an existing resource's resource type / document type / etc.
     * 
     * @return
     */
    @Action(value = "edit",
            results = {
                    @Result(name = INPUT, location = "add.ftl"),
                    @Result(name = "DATASET", type = TYPE_REDIRECT, location = "/dataset/edit?resourceId=${resource.id}"),
                    @Result(name = "DOCUMENT", type = TYPE_REDIRECT, location = "/document/edit?resourceId=${resource.id}"),
                    @Result(name = "ONTOLOGY", type = TYPE_REDIRECT, location = "/ontology/edit?resourceId=${resource.id}"),
                    @Result(name = "IMAGE", type = TYPE_REDIRECT, location = "/image/edit?resourceId=${resource.id}"),
                    @Result(name = "SENSORY_DATA", type = TYPE_REDIRECT, location = "/sensory-data/edit?resourceId=${resource.id}"),
                    @Result(name = "CODING_SHEET", type = TYPE_REDIRECT, location = "/coding-sheet/edit?resourceId=${resource.id}")
            })
    public String edit() {
        InformationResource informationResource = getGenericService().find(InformationResource.class, resourceId);
        if (informationResource == null) {
            getLogger().error("trying to edit information resource but it was null.");
            addActionError(getText("resourceController.invalid"));
            return INPUT;
        }
        if (resourceType == null) {
            addFieldError("resourceType", "Please enter a resource type.");
            return INPUT;
        }

        return resourceType.name();
    }

    public boolean isAllowedToCreateResource() {
        // getLogger().info("ppi: {}", getTdarConfiguration().isPayPerIngestEnabled());
        if ((getTdarConfiguration().isPayPerIngestEnabled() == false) || accountService.hasSpaceInAnAccount(getAuthenticatedUser(), null, true)) {
            return true;
        }
        return false;
    }

    /**
     * Used to edit an existing resource's resource type / document type / etc.
     * 
     * @return
     */
    @Action(value = "view",
            results = {
                    @Result(name = "DATASET", type = TYPE_REDIRECT, location = "/dataset/view?id=${resourceId}"),
                    @Result(name = "DOCUMENT", type = TYPE_REDIRECT, location = "/document/view?id=${resourceId}"),
                    @Result(name = "ONTOLOGY", type = TYPE_REDIRECT, location = "/ontology/view?id=${resourceId}"),
                    @Result(name = "IMAGE", type = TYPE_REDIRECT, location = "/image/view?id=${resourceId}"),
                    @Result(name = "SENSORY_DATA", type = TYPE_REDIRECT, location = "/sensory-data/view?id=${resourceId}"),
                    @Result(name = "CODING_SHEET", type = TYPE_REDIRECT, location = "/coding-sheet/view?id=${resourceId}")
            })
    public String view() {
        InformationResource informationResource = getGenericService().find(InformationResource.class, resourceId);
        if (informationResource == null) {
            getLogger().error("trying to edit information resource but it was null.");
            addActionError(getText("resourceController.not_found"));
            return NOT_FOUND;
        }
        return informationResource.getResourceType().name();
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    // /**
    // * Returns a sorted map of the allowable subset of resource types that can be
    // * entered.
    // */
    // public SortedMap<ResourceType, String> getResourceTypes() {
    // synchronized (resourceTypes) {
    // if (resourceTypes.isEmpty()) {
    // addResourceType(ResourceType.CODING_SHEET);
    // addResourceType(ResourceType.DATASET);
    // addResourceType(ResourceType.DOCUMENT);
    // addResourceType(ResourceType.SENSORY_DATA);
    // addResourceType(ResourceType.IMAGE);
    // addResourceType(ResourceType.ONTOLOGY);
    // }
    // }
    // return resourceTypes;
    // }
    //
    // private void addResourceType(ResourceType resourceType) {
    // resourceTypes.put(resourceType, resourceType.getLabel());
    // }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getResourceId() {
        return resourceId;
    }
}
