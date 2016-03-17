package org.tdar.struts.action.resource;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.Indexable;
import org.tdar.core.bean.entity.permissions.GeneralPermissions;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.dao.external.auth.InternalTdarRights;
import org.tdar.core.service.DeleteIssue;
import org.tdar.core.service.SerializationService;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.core.service.resource.ResourceService;
import org.tdar.search.service.index.SearchIndexService;
import org.tdar.struts.action.AbstractDeleteAction;
import org.tdar.utils.jaxb.XMLFilestoreLogger;

import com.opensymphony.xwork2.Preparable;

@Component
@Scope("prototype")
@ParentPackage("secured")
@Namespace("/resource")
@ManuallyProcessEvents
public class ResourceDeleteAction extends AbstractDeleteAction<Resource> implements Preparable {

    private static final long serialVersionUID = -7593744067457475418L;
    @Autowired
    private transient ResourceService resourceService;
    @Autowired
    private transient AuthorizationService authorizationService;
    @Autowired
    private transient SearchIndexService searchIndexService;
    @Autowired
    private transient XMLFilestoreLogger xmlFilestoreLogger;

    @Override
    protected Resource loadPersistable() {
        return resourceService.find(getId());
    }

    @Override
    protected void delete(Resource persistable) {
        resourceService.deleteForController(getPersistable(), getDeletionReason(), getAuthenticatedUser());
        try {
            searchIndexService.index((Indexable) getPersistable());
            xmlFilestoreLogger.logRecordXmlToFilestore(getPersistable());
        } catch (SolrServerException | IOException e) {
            getLogger().error("error reindexing", e);
        }
    }

    @Override
    protected DeleteIssue getDeletionIssues() {
        return resourceService.getDeletionIssues(this, getPersistable());
    }

    @Override
    protected boolean canDelete() {
        if (authorizationService.can(InternalTdarRights.DELETE_RESOURCES, getAuthenticatedUser())) {
            return true;
        }

        return authorizationService.canEditResource(getAuthenticatedUser(), getPersistable(), GeneralPermissions.MODIFY_RECORD);

    }

}
