package org.tdar.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.Rollback;
import org.tdar.core.bean.AbstractIntegrationTestCase;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.service.processes.DoiProcess;
import org.tdar.core.service.processes.SendEmailProcess;
import org.tdar.core.service.resource.ResourceService;
import org.tdar.utils.Pair;

/**
 * $Id$
 * 
 * @author Adam Brin
 */
public class DOIServiceITCase extends AbstractIntegrationTestCase {

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private DoiProcess doiProcess;

    @Autowired
    private SendEmailProcess sendEmailProcess;
    
    public Map<String, List<Pair<Long, String>>> processDois() throws Exception {
        // using mock DAO instead of real service
        doiProcess.setProvider(new MockIdentifierDao());
        // run it once, make sure all are "create", no deletes or updates
        doiProcess.execute();
        return doiProcess.getBatchResults();
    }

    @Test
    @Rollback
    public void testDOIService() throws Exception {
        Map<String, List<Pair<Long, String>>> createAndUpdateDoiInfo = processDois();
        List<Pair<Long, String>> created = createAndUpdateDoiInfo.get(DoiProcess.CREATED);
        List<Pair<Long, String>> updated = createAndUpdateDoiInfo.get(DoiProcess.UPDATED);
        List<Pair<Long, String>> deleted = createAndUpdateDoiInfo.get(DoiProcess.DELETED);
        logger.debug("created:" + created.size());
        logger.debug("updated:" + updated.size());
        logger.debug("deleted:" + deleted.size());
        assertTrue(created.size() > 0);
        assertSame(0, updated.size());
        assertSame(0, deleted.size());
        doiProcess.cleanup();
        // mark one record as deleted, and mark one as updated
        Resource toBeDeleted = resourceService.find(created.get(0).getFirst());
        Resource toBeUpdated = resourceService.find(created.get(1).getFirst());
        toBeDeleted.setStatus(Status.DELETED);
        toBeDeleted.markUpdated(getAdminUser());
        toBeUpdated.markUpdated(getAdminUser());
        toBeDeleted.setDescription(toBeDeleted.getTitle());
        toBeUpdated.setDescription(toBeUpdated.getTitle());
        ((InformationResource) toBeUpdated).setDate(1243);
        ((InformationResource) toBeDeleted).setDate(1243);
        resourceService.saveOrUpdate(toBeUpdated);
        resourceService.saveOrUpdate(toBeDeleted);

        // create new resources (1) without file (1) with file (2) with file, ancient date, and already has DOI
        InformationResource generateInformationResourceWithUser = generateDocumentWithUser();
        InformationResource file = generateDocumentWithFileAndUseDefaultUser();
        InformationResource file2 = generateDocumentWithFileAndUseDefaultUser();
        file2.setDateUpdated(new Date(10000)); // forever ago -- should not register
        file2.setExternalId("1234");
        genericService.saveOrUpdate(file2);
        createAndUpdateDoiInfo = processDois();

        List<Pair<Long, String>> created_ = createAndUpdateDoiInfo.get(DoiProcess.CREATED);
        List<Pair<Long, String>> updated_ = createAndUpdateDoiInfo.get(DoiProcess.UPDATED);
        List<Pair<Long, String>> deleted_ = createAndUpdateDoiInfo.get(DoiProcess.DELETED);
        logger.debug("created:" + created_.size());
        logger.debug("updated:" + updated_.size());
        logger.debug("deleted:" + deleted_.size());
        assertEquals(1, created_.size());
        assertTrue(updated_.size() > 0);
        assertTrue(deleted_.size() > 0);
        sendEmailProcess.execute();
        SimpleMailMessage received = mockMailSender.getMessages().get(0);
        assertTrue(received.getSubject().contains(DoiProcess.SUBJECT));
        assertTrue(received.getText().contains("DOI Daily"));
        assertEquals(received.getFrom(), emailService.getFromEmail());

    }
}
