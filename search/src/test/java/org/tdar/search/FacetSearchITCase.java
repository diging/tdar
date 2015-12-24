package org.tdar.search;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Test;
import org.springframework.test.annotation.Rollback;
import org.tdar.core.bean.keyword.CultureKeyword;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.search.bean.AdvancedSearchQueryObject;
import org.tdar.search.query.Facet;
import org.tdar.search.query.QueryFieldNames;
import org.tdar.search.query.SearchResult;
import org.tdar.search.query.facet.FacetWrapper;
import org.tdar.utils.MessageHelper;

public class FacetSearchITCase extends AbstractResourceSearchITCase {

    @Test
    @Rollback
    public void testFacetByEnum() throws SolrServerException, IOException, ParseException {
        SearchResult<Resource> result = new SearchResult<>();
        FacetWrapper facetWrapper = new FacetWrapper();
        facetWrapper.facetBy(QueryFieldNames.RESOURCE_TYPE, ResourceType.class);
        result.setFacetWrapper(facetWrapper);
        AdvancedSearchQueryObject asqo = new AdvancedSearchQueryObject();
        resourceSearchService.buildAdvancedSearch(asqo, null, result , MessageHelper.getInstance());
        logger.debug("{}", result.getFacetWrapper().getFacetResults());
        List<Facet> list = result.getFacetWrapper().getFacetResults().get(QueryFieldNames.RESOURCE_TYPE);
        assertNotEmpty(list);
        boolean seenDocuments = false;
        boolean seenDatasets = false;
        for (Facet facet : list) {
            if (facet.getLabel().contains("Documents") && facet.getCount() > 0) {
                seenDocuments = true;
            }
            if (facet.getLabel().contains("Datasets") && facet.getCount() > 0) {
                seenDatasets = true;
            }
        }
        assertTrue(seenDatasets);
        assertTrue(seenDocuments);
    }

    @Test
    @Rollback
    public void testFacetByPersistable() throws SolrServerException, IOException, ParseException {
        SearchResult<Resource> result = new SearchResult<>();
        FacetWrapper facetWrapper = new FacetWrapper();
        facetWrapper.facetBy(QueryFieldNames.ACTIVE_CULTURE_KEYWORDS, CultureKeyword.class);
        result.setFacetWrapper(facetWrapper);
        AdvancedSearchQueryObject asqo = new AdvancedSearchQueryObject();
        resourceSearchService.buildAdvancedSearch(asqo, null, result , MessageHelper.getInstance());
        List<Facet> list = result.getFacetWrapper().getFacetResults().get(QueryFieldNames.ACTIVE_CULTURE_KEYWORDS);
        assertNotEmpty(list);
        boolean seenPuebloan = false;
        boolean seenHistoric = false;
        for (Facet facet : list) {
            if (facet.getLabel().contains("Puebloan") && facet.getCount() > 0) {
                seenPuebloan = true;
            }
            if (facet.getLabel().contains("Historic") && facet.getCount() > 0) {
                seenHistoric = true;
            }
        }
        assertTrue(seenHistoric);
        assertTrue(seenPuebloan);
        
        logger.debug("{}", result.getFacetWrapper().getFacetResults());
    }

}
