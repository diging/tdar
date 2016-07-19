package org.tdar.struts.action.api.collection;

import java.io.ByteArrayInputStream;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.collection.HierarchicalCollection;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.service.ResourceCollectionService;
import org.tdar.core.service.SerializationService;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.struts.action.TdarActionSupport;
import org.tdar.struts.action.api.AbstractJsonApiAction;
import org.tdar.struts.interceptor.annotation.HttpForbiddenErrorResponseOnly;
import org.tdar.struts.interceptor.annotation.HttpsOnly;
import org.tdar.struts.interceptor.annotation.PostOnly;
import org.tdar.utils.PersistableUtils;

import com.opensymphony.xwork2.Preparable;

@Namespace("/api/collection")
@Component
@Scope("prototype")
@ParentPackage("secured")
@HttpForbiddenErrorResponseOnly
@HttpsOnly
public class MoveResourceAction extends AbstractJsonApiAction implements Preparable {

    private Long resourceId;
    private Long fromCollectionId;
    private Long toCollectionId;
    private Resource resource;
    private HierarchicalCollection fromCollection;
    private HierarchicalCollection toCollection;

    @Autowired
    protected transient SerializationService serializationService;
    
    @Autowired
    protected transient ResourceCollectionService resourceCollectionService;

    @Autowired
    private AuthorizationService authorizationService;
    
    @Override
    public void validate() {
        super.validate();
        if (PersistableUtils.isNullOrTransient(resource) || !authorizationService.canEdit(getAuthenticatedUser(), resource)) {
            addActionError("cannot edit resource");
        }
        if (PersistableUtils.isNullOrTransient(fromCollection) || !authorizationService.canEdit(getAuthenticatedUser(), fromCollection)) {
            addActionError("cannot edit from colection");
        }
        if (PersistableUtils.isNullOrTransient(toCollection) || !authorizationService.canEdit(getAuthenticatedUser(), toCollection)) {
            addActionError("cannot edit to colection");
        }
    }
    
    @Override
    @PostOnly
    @Action(value="moveResource")
    public String execute() throws Exception {
        resourceCollectionService.moveResource(resource, fromCollection, toCollection);
        setJsonInputStream(new ByteArrayInputStream("SUCCESS".getBytes()));
        return super.execute();
    }

    /**
     * 
     */
    private static final long serialVersionUID = 2137331107886327060L;

    @Override
    public void prepare() throws Exception {
        this.resource = getGenericService().find(Resource.class, resourceId);
        this.fromCollection = getGenericService().find(HierarchicalCollection.class, fromCollectionId);
        this.toCollection = getGenericService().find(HierarchicalCollection.class, toCollectionId);
        
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getFromCollectionId() {
        return fromCollectionId;
    }

    public void setFromCollectionId(Long fromCollectionId) {
        this.fromCollectionId = fromCollectionId;
    }

    public Long getToCollectionId() {
        return toCollectionId;
    }

    public void setToCollectionId(Long toCollectionId) {
        this.toCollectionId = toCollectionId;
    }
    
}
