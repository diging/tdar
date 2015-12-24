package org.tdar.search.service;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.tdar.AbstractWithIndexIntegrationTestCase;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.collection.ResourceCollection.CollectionType;
import org.tdar.core.bean.entity.AuthorizedUser;
import org.tdar.core.bean.entity.permissions.GeneralPermissions;
import org.tdar.core.bean.resource.Dataset;
import org.tdar.core.service.ScheduledProcessService;
import org.tdar.core.service.processes.SendEmailProcess;
import org.tdar.core.service.processes.daily.DailyEmailProcess;
import org.tdar.search.service.processes.CreatorAnalysisProcess;
import org.tdar.search.service.processes.daily.DailyTimedAccessRevokingProcess;
import org.tdar.search.service.processes.weekly.WeeklyResourcesAdded;

public class SearchScheduledProcessITCase extends AbstractWithIndexIntegrationTestCase {

    @Autowired
    private transient SearchScheduledProcessService sps;

    @Autowired
    private transient ScheduledProcessService scheduledProcessService;

    
    @Test
    @Rollback(true)
    public void cronGenerateWeeklyStats() {
        sps.cronGenerateWeeklyStats();
    }

    @Autowired
    DailyEmailProcess dailyEmailProcess;

    @Test
    @Rollback
    public void testResourceReport() throws SolrServerException, IOException {
        Dataset dataset = createAndSaveNewDataset();
        searchIndexService.index(dataset);
        searchIndexService.flushToIndexes();
        scheduledProcessService.queue(WeeklyResourcesAdded.class);
        scheduledProcessService.runNextScheduledProcessesInQueue();
        assertTrue(dailyEmailProcess.isCompleted());
        scheduledProcessService.queue(SendEmailProcess.class);
        scheduledProcessService.runNextScheduledProcessesInQueue();
        
    }

    @Autowired
    DailyTimedAccessRevokingProcess dtarp;
    
    @Test
    public void testDailyTimedAccessRevokingProcess() {
        Dataset dataset = createAndSaveNewDataset();
        ResourceCollection collection = new ResourceCollection(dataset, getAdminUser());
        collection.setType(CollectionType.SHARED);
        AuthorizedUser e = new AuthorizedUser(getBasicUser(), GeneralPermissions.VIEW_ALL);
        e.setDateExpires(DateTime.now().minusDays(4).toDate());
        collection.setName("test");
        collection.setDescription("test");
        collection.markUpdated(getAdminUser());
        collection.getAuthorizedUsers().add(e);
        collection.getResources().add(dataset);
        genericService.saveOrUpdate(collection);
        genericService.saveOrUpdate(e);
        dataset.getResourceCollections().add(collection);
        genericService.saveOrUpdate(dataset);
        
        dtarp.execute();
    }

    @Autowired
    CreatorAnalysisProcess pqp;
    
    @Test
    @Rollback(true)
    public void testPersonAnalytics() throws InstantiationException, IllegalAccessException {
        pqp.setDaysToRun(3000);
        pqp.execute();
        pqp.cleanup();
        pqp.setAllIds(null);
        // resetting
        pqp.execute();
    }

    @Test
    @Rollback(true)
    public void testWeeklyStats() {
        sps.cronWeeklyAdded();
    }

}
