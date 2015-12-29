package org.tdar.search.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.tdar.AbstractWithIndexIntegrationTestCase;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.resource.Image;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.service.GenericService;
import org.tdar.search.bean.AdvancedSearchQueryObject;
import org.tdar.search.bean.SearchParameters;
import org.tdar.search.query.SearchResult;
import org.tdar.search.service.index.SearchIndexService;
import org.tdar.search.service.query.ResourceSearchService;
import org.tdar.utils.MessageHelper;

public class NestedObjectIdexingITCase extends AbstractWithIndexIntegrationTestCase {

    @Autowired
    SearchIndexService searchIndexService;
    
    @Autowired
    ResourceSearchService resourceSearchService;
    
    @Autowired
    GenericService genericService;
    
    @Override
    public void reindex() {
    }
    
    @Test
    @Rollback(true)
    public void testIndexing() throws SolrServerException, IOException, ParseException {
        ResourceCollection collection = createAndSaveNewResourceCollection(SPITAL_DB_NAME);
        Image image = createAndSaveNewInformationResource(Image.class);
        genericService.synchronize();
        searchIndexService.flushToIndexes();
        logger.debug("===================");
        collection.getResources().add(image);
        image.getResourceCollections().add(collection);
        logger.debug("{}", image);
        genericService.saveOrUpdate(collection);
        genericService.saveOrUpdate(image);
        genericService.synchronize();
        searchIndexService.flushToIndexes();
        SearchResult<Resource> result = new SearchResult<>();
        AdvancedSearchQueryObject asqo = new AdvancedSearchQueryObject();
        SearchParameters params = new SearchParameters();
//        params.getCollections().add(collection);
        params.getResourceIds().add(image.getId());
        asqo.getSearchParameters().add(params);
        resourceSearchService.buildAdvancedSearch(asqo, getAdminUser(), result, MessageHelper.getInstance());
        assertTrue(result.getResults().contains(image));

        result = new SearchResult<>();
        asqo = new AdvancedSearchQueryObject();
        params = new SearchParameters();
        params.getCollections().add(collection);
        asqo.getSearchParameters().add(params);
        resourceSearchService.buildAdvancedSearch(asqo, getAdminUser(), result, MessageHelper.getInstance());
        assertTrue(result.getResults().contains(image));

        collection.getResources().remove(image);
        image.getResourceCollections().remove(collection);
        genericService.saveOrUpdate(collection);
        genericService.saveOrUpdate(image);
        searchIndexService.flushToIndexes();
        genericService.synchronize();

        result = new SearchResult<>();
        asqo = new AdvancedSearchQueryObject();
        params = new SearchParameters();
        params.getCollections().add(collection);
        asqo.getSearchParameters().add(params);
        resourceSearchService.buildAdvancedSearch(asqo, getAdminUser(), result, MessageHelper.getInstance());
        assertFalse(result.getResults().contains(image));

    }
}
