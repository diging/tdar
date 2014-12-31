package org.tdar.core.service.integration;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.integration.DataIntegrationWorkflow;
import org.tdar.core.dao.GenericDao;
import org.tdar.core.dao.integration.IntegrationWorkflowDao;
import org.tdar.core.service.SerializationService;
import org.tdar.core.service.ServiceInterface;
import org.tdar.core.service.integration.dto.IntegrationDeserializationException;
import org.tdar.core.service.integration.dto.IntegrationWorkflowWrapper;
import org.tdar.core.service.integration.dto.v1.IntegrationWorkflowData;

import com.opensymphony.xwork2.TextProvider;

/**
 * Service class serving as a bridge between json data and IntegrationContext objects.
 * 
 * JSON data gets converted into an intermediate POJO that can validate itself and return an
 * IntegrationContext object with a list of any validation (referential) errors.
 * 
 */
@Service
public class IntegrationWorkflowService  extends ServiceInterface.TypedDaoBase<DataIntegrationWorkflow, IntegrationWorkflowDao>{

    @SuppressWarnings("unused")
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private transient SerializationService serializationService;

    @Autowired
    private transient GenericDao genericDao;

    @Transactional
    public IntegrationContext toIntegrationContext(DataIntegrationWorkflow workflow) throws IOException, IntegrationDeserializationException {
        IntegrationWorkflowData workflowData = serializationService.readObjectFromJson(workflow.getJsonData(), IntegrationWorkflowData.class);
        IntegrationContext context = workflowData.toIntegrationContext(genericDao);
        // perform validity checks?
        return context;
    }

    @Transactional(readOnly = false)
    public void saveForController(DataIntegrationWorkflow persistable, IntegrationWorkflowData data, TdarUser authUser) throws IntegrationDeserializationException {
        validateWorkflow(data);
        persistable.markUpdated(authUser);
        genericDao.saveOrUpdate(persistable);
    }

    @Transactional(readOnly = true)
    public void validateWorkflow(IntegrationWorkflowWrapper data) throws IntegrationDeserializationException {
        data.validate(genericDao);
    }
    
    @Transactional(readOnly=true)
    public List<DataIntegrationWorkflow> getWorkflowsForUser(TdarUser authorizedUser) {
        return getDao().getWorkflowsForUser(authorizedUser);
    }

    @Transactional(readOnly=false)
    public void deleteForController(TextProvider provider, DataIntegrationWorkflow persistable, TdarUser authenticatedUser) {
        getDao().delete(persistable);
    }
}
