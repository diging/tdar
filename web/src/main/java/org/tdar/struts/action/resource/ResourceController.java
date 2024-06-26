package org.tdar.struts.action.resource;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Actions;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.UrlConstants;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.service.billing.BillingAccountService;
import org.tdar.struts.action.AbstractAuthenticatableAction;
import org.tdar.struts_base.action.TdarActionSupport;

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
public class ResourceController extends AbstractAuthenticatableAction {

    private static final long serialVersionUID = 7080916909862991142L;

    public static final String BILLING = "billing";

    @Autowired
    private transient BillingAccountService accountService;

    // incoming data from /resource/add
    private ResourceType resourceType;
    private Long projectId;

    /**
     * Passthrough action, just loads add.ftl via conventions plugin.
     */
    @Actions(value = {
            @Action(value = "add",
                    results = {
                            @Result(name = BILLING, type = TdarActionSupport.TDAR_REDIRECT, location = UrlConstants.CART_ADD),
                            @Result(name = CONTRIBUTOR, type = TdarActionSupport.TDAR_REDIRECT, location = UrlConstants.MY_PROFILE),
                            @Result(name = SUCCESS, location = "add.ftl")
                    }),
            @Action(value = "add/{projectId}",
                    results = {
                            @Result(name = BILLING, type = TdarActionSupport.TDAR_REDIRECT, location = UrlConstants.CART_ADD),
                            @Result(name = CONTRIBUTOR, type = TdarActionSupport.TDAR_REDIRECT, location = UrlConstants.MY_PROFILE),
                            @Result(name = SUCCESS, location = "add.ftl")
                    })

    })
    @Override
    public String execute() {
        if (!isContributor()) {
            addActionMessage(getText("resourceController.must_be_contributor"));
            return CONTRIBUTOR;
        }
        accountService.assignOrphanInvoicesIfNecessary(getAuthenticatedUser());
        if (!getTdarConfiguration().isPayPerIngestEnabled() || isAllowedToCreateResource()) {
            return SUCCESS;
        }
        addActionMessage(getText("resourceController.requires_funds"));
        return BILLING;
    }

    public boolean isAllowedToCreateResource() {
        getLogger().trace("ppi: {}", getTdarConfiguration().isPayPerIngestEnabled());
        return (!getTdarConfiguration().isPayPerIngestEnabled() || accountService.hasSpaceInAnAccount(getAuthenticatedUser(), null));
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

}
