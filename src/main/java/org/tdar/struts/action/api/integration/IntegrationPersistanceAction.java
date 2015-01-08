package org.tdar.struts.action.api.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Actions;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.integration.DataIntegrationWorkflow;
import org.tdar.core.dao.external.auth.InternalTdarRights;
import org.tdar.core.service.SerializationService;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.core.service.integration.IntegrationSaveResult;
import org.tdar.core.service.integration.IntegrationWorkflowService;
import org.tdar.core.service.integration.dto.IntegrationDeserializationException;
import org.tdar.core.service.integration.dto.v1.IntegrationWorkflowData;
import org.tdar.struts.action.AbstractPersistableController.RequestType;
import org.tdar.struts.action.PersistableLoadingAction;
import org.tdar.struts.action.TdarActionException;
import org.tdar.struts.interceptor.annotation.PostOnly;
import org.tdar.struts.interceptor.annotation.WriteableSession;
import org.tdar.utils.json.JsonIntegrationFilter;

import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;

@ParentPackage("secured")
@Namespace("/api/integration")
@Component
@Scope("prototype")
public class IntegrationPersistanceAction extends AbstractIntegrationAction implements Preparable, PersistableLoadingAction<DataIntegrationWorkflow>,
        Validateable {

    private static final long serialVersionUID = 9053098961621133695L;
    private Long id;
    private DataIntegrationWorkflow workflow;
    private String integration;

    @Autowired
    IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    SerializationService serializationService;
    private IntegrationWorkflowData jsonData;
    private IntegrationSaveResult result;
    private List<String> errors = new ArrayList<>();

    @Actions(value = {
            @Action("save/{id}"),
            @Action("save")
    })
    @PostOnly
    @WriteableSession
    public String save() throws TdarActionException, IOException, IntegrationDeserializationException {
        setResult(integrationWorkflowService.saveForController(getPersistable(), jsonData, integration, getAuthenticatedUser()));
        setJsonObject(getResult(), JsonIntegrationFilter.class);
        if (result.getStatus() != IntegrationSaveResult.SUCCESS) {
            result.getErrors().addAll(errors);
            return INPUT;
        }
        return SUCCESS;
    }

    @Override
    public Class<DataIntegrationWorkflow> getPersistableClass() {
        return DataIntegrationWorkflow.class;
    }


    @Override
    public void prepare() throws Exception {
        prepareAndLoad(this, RequestType.SAVE);
        getLogger().trace(integration);
        try {
            jsonData = serializationService.readObjectFromJson(integration, IntegrationWorkflowData.class);
            if (workflow == null) {
                workflow = new DataIntegrationWorkflow();
            }
        } catch (Exception e) {
            getLogger().error("cannot prepare json", e);
            errors.add(e.getMessage());
            setupErrorResult();
        }
    }

    @Override
    public void validate() {
        try {
            integrationWorkflowService.validateWorkflow(jsonData);
        } catch (IntegrationDeserializationException e) {
            getLogger().error("cannot validate", e);
            getLogger().error("error validating json", e);
            errors.add(e.getMessage());
            setupErrorResult();
        }
    }

    private void setupErrorResult() {
        try {
            setResult(new IntegrationSaveResult(errors));
            setJsonObject(getResult(), JsonIntegrationFilter.class);
            getActionErrors().addAll(errors);
        } catch (IOException e1) {
            getLogger().error("erro setting up error json", e1);
        }
    }

    @Override
    public boolean authorize() {
        return authorizationService.canEditWorkflow(workflow, getAuthenticatedUser());
    }

    @Override
    public DataIntegrationWorkflow getPersistable() {
        return workflow;
    }

    @Override
    public void setPersistable(DataIntegrationWorkflow persistable) {
        workflow = persistable;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public InternalTdarRights getAdminRights() {
        return InternalTdarRights.EDIT_ANYTHING;
    }

    public String getIntegration() {
        return integration;
    }

    public void setIntegration(String integration) {
        this.integration = integration;
    }

    public DataIntegrationWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(DataIntegrationWorkflow workflow) {
        this.workflow = workflow;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IntegrationSaveResult getResult() {
        return result;
    }

    public void setResult(IntegrationSaveResult result) {
        this.result = result;
    }
}
