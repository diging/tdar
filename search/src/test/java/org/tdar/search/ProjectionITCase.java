package org.tdar.search;

import java.io.IOException;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Test;
import org.springframework.test.annotation.Rollback;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.search.bean.AdvancedSearchQueryObject;
import org.tdar.search.query.QueryFieldNames;
import org.tdar.search.query.SearchResult;
import org.tdar.search.query.SearchResultHandler.ProjectionModel;
import org.tdar.search.query.facet.FacetWrapper;
import org.tdar.utils.MessageHelper;

public class ProjectionITCase extends AbstractResourceSearchITCase {

    @Test
    @Rollback
    public void testExpermientalProjectionModel() throws SolrServerException, IOException, ParseException {
        SearchResult<Resource> result = new SearchResult<>(10000);
        result.setProjectionModel(ProjectionModel.LUCENE_EXPERIMENTAL);
        FacetWrapper facetWrapper = new FacetWrapper();
        facetWrapper.facetBy(QueryFieldNames.RESOURCE_TYPE, ResourceType.class);
        result.setFacetWrapper(facetWrapper);
        AdvancedSearchQueryObject asqo = new AdvancedSearchQueryObject();
//        asqo.getReservedParams().getStatuses().addAll(Arrays.asList(Status.DRAFT,Status.DELETED));
        result.setAuthenticatedUser(getAdminUser());
        resourceSearchService.buildAdvancedSearch(asqo, null, result , MessageHelper.getInstance());
        for (Resource r : result.getResults()) {
        	logger.debug("{} {}", r, r.isViewable());
        	if (r instanceof InformationResource) {
        		InformationResource ir = (InformationResource)r;
        		logger.debug("\t{}", ir.getProject());
        	}
        	logger.debug("\t{}",r.getActiveLatitudeLongitudeBoxes());
        	logger.debug("\t{}",r.getPrimaryCreators());
        }
    }
    
}
