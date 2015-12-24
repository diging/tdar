/**
 * $Id$
 * 
 * @author $Author$
 * @version $Revision$
 */
package org.tdar.struts.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.tdar.core.bean.Indexable;
import org.tdar.core.bean.SortOption;
import org.tdar.core.bean.entity.Creator;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.resource.IntegratableOptions;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.service.ObfuscationService;
import org.tdar.core.service.SerializationService;
import org.tdar.core.service.resource.ResourceService;
import org.tdar.search.bean.ReservedSearchParameters;
import org.tdar.search.index.LookupSource;
import org.tdar.search.query.FacetWrapper;
import org.tdar.search.query.FacetedResultHandler;
import org.tdar.search.service.CreatorSearchService;
import org.tdar.search.service.SearchService;
import org.tdar.search.service.SearchUtils;
import org.tdar.utils.PaginationHelper;
import org.tdar.utils.json.JsonAdminLookupFilter;
import org.tdar.utils.json.JsonLookupFilter;

/**
 * @author Adam Brin
 * 
 */
public abstract class AbstractLookupController<I extends Indexable> extends AuthenticationAware.Base implements FacetedResultHandler<I> {

    private static final long serialVersionUID = 2357805482356017885L;

    private String callback;
    private ProjectionModel projectionModel;
    private int minLookupLength = 3;
    private int recordsPerPage = getDefaultRecordsPerPage();
    private int startRecord = DEFAULT_START;
    private List<I> results = Collections.emptyList();
    private int totalRecords;
    private SortOption sortField;
    private SortOption defaultSort = SortOption.getDefaultSortOption();
    private SortOption secondarySortField = SortOption.TITLE;
    private boolean debug = false;
    private ReservedSearchParameters reservedSearchParameters = new ReservedSearchParameters();
    private InputStream jsonInputStream;
    private Long id = null;
    private String mode;
    private String searchTitle;
    private String searchDescription;
    private FacetWrapper facetWrapper = new FacetWrapper();
    // execute a query even if query is empty

    private LookupSource lookupSource;

    private PaginationHelper paginationHelper;

    @Autowired
    private transient ResourceService resourceService;

    @Autowired
    private transient SearchService searchService;

    @Autowired
    private CreatorSearchService creatorSearchService;

    @Autowired
    ObfuscationService obfuscationService;

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public int getMinLookupLength() {
        return minLookupLength;
    }


    public void setMinLookupLength(int minLookupLength) {
        this.minLookupLength = minLookupLength;
    }

    @Override
    public int getRecordsPerPage() {
        return recordsPerPage;
    }

    @Override
    public void setRecordsPerPage(int recordsPerPage) {
        this.recordsPerPage = recordsPerPage;
    }

    @Override
    public int getStartRecord() {
        return startRecord;
    }

    @Override
    public void setStartRecord(int startRecord) {
        this.startRecord = startRecord;
    }

    @Override
    public SortOption getSortField() {
        if (sortField == null) {
            sortField = getDefaultSort();
        }
        return sortField;
    }

    @Override
    public void setSortField(SortOption sortField) {
        this.sortField = sortField;
    }

    /**
     * @param totalRecords
     *            the totalRecords to set
     */
    @Override
    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    /**
     * @return the totalRecords
     */
    @Override
    public int getTotalRecords() {
        return totalRecords;
    }

    @Override
    public int getNextPageStartRecord() {
        return startRecord + recordsPerPage;
    }

    @Override
    public int getPrevPageStartRecord() {
        return startRecord - recordsPerPage;
    }

    /**
     * @param results
     *            the results to set
     */
    @Override
    public void setResults(List<I> results) {
        this.results = results;
    }

    /**
     * @return the results
     */
    @Override
    public List<I> getResults() {
        return results;
    }

    /*
     * 
     */
    @SuppressWarnings("unchecked")
    public List<Creator> getCreatorResults() {
        return (List<Creator>) results;
    }

    /**
     * @return the debug
     */
    @Override
    public boolean isDebug() {
        return debug;
    }

    /**
     * @param debug
     *            the debug to set
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the secondarySortField
     */
    @Override
    public SortOption getSecondarySortField() {
        return secondarySortField;
    }

    /**
     * @param secondarySortField
     *            the secondarySortField to set
     */
    public void setSecondarySortField(SortOption secondarySortField) {
        this.secondarySortField = secondarySortField;
    }

    /**
     * @param useSubmitterContext
     *            the useSubmitterContext to set
     */
    public void setUseSubmitterContext(boolean useSubmitterContext) {
        getReservedSearchParameters().setUseSubmitterContext(useSubmitterContext);
    }

    /**
     * @return the useSubmitterContext
     */
    public boolean isUseSubmitterContext() {
        return getReservedSearchParameters().isUseSubmitterContext();
    }

    /**
     * @return the mode
     */
    @Override
    public String getMode() {
        return mode;
    }

    /**
     * @param mode
     *            the mode to set
     */
    // TODO: method needs better name... this is just metadata used to describe the caller of handleSearch()
    @Override
    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String getSearchDescription() {
        return searchDescription;
    }

    public void setSearchDescription(String searchDescription) {
        this.searchDescription = searchDescription;
    }

    @Override
    public String getSearchTitle() {
        return searchTitle;
    }

    public void setSearchTitle(String searchTitle) {
        this.searchTitle = searchTitle;
    }

    public ReservedSearchParameters getReservedSearchParameters() {
        return reservedSearchParameters;
    }

    public void setReservedSearchParameters(ReservedSearchParameters reservedSearchParameters) {
        this.reservedSearchParameters = reservedSearchParameters;
    }

    public List<Status> getAllStatuses() {
        return new ArrayList<Status>(Arrays.asList(Status.values()));
    }

    public List<Status> getIncludedStatuses() {
        return getReservedSearchParameters().getStatuses();
    }

    public void setIncludedStatuses(List<Status> statuses) {
        getReservedSearchParameters().setStatuses(statuses);
    }

    public List<ResourceType> getResourceTypes() {
        return getReservedSearchParameters().getResourceTypes();
    }

    public List<ResourceType> getAllResourceTypes() {
        return resourceService.getAllResourceTypes();
    }

    public List<IntegratableOptions> getIntegratableOptions() {
        return getReservedSearchParameters().getIntegratableOptions();
    }

    public void setIntegratableOptions(List<IntegratableOptions> integratableOptions) {
        getReservedSearchParameters().setIntegratableOptions(integratableOptions);
    }

    // REQUIRED IF YOU WANT FACETING TO ACTUALLY WORK
    public void setResourceTypes(List<ResourceType> resourceTypes) {
        getReservedSearchParameters().setResourceTypes(resourceTypes);
    }

    public String findPerson(String term, Person person, boolean registered) throws SolrServerException, IOException {
        this.setLookupSource(LookupSource.PERSON);
        // TODO Auto-generated method stub
            try {
                creatorSearchService.findPerson(person, term, registered, this, this, getMinLookupLength());
                // sanitize results if the user is not logged in
            } catch (ParseException e) {
                addActionErrorWithException(getText("abstractLookupController.invalid_syntax"), e);
                return ERROR;
            }
            if (isEditor()) {
                jsonifyResult(JsonAdminLookupFilter.class);
            } else {
                jsonifyResult(JsonLookupFilter.class);
            }
        return SUCCESS;
    }

    @Autowired
    SerializationService serializationService;

    private Map<String, Object> result = new HashMap<>();

    public void jsonifyResult(Class<?> filter) {
        prepareResult();
        jsonInputStream = new ByteArrayInputStream(serializationService.convertFilteredJsonForStream(getResult(), filter, callback).getBytes());
    }

    protected void prepareResult() {
        List<I> actual = new ArrayList<>();
        for (I obj : results) {
            if (obj == null) {
                continue;
            }
            obfuscationService.obfuscateObject(obj, getAuthenticatedUser());
            actual.add(obj);
        }
        Map<String, Object> status = new HashMap<>();
        getResult().put(getResultsKey(), actual);
        getResult().put("status", status);
        status.put("recordsPerPage", getRecordsPerPage());
        status.put("startRecord", getStartRecord());
        status.put("totalRecords", getTotalRecords());
        status.put("sortField", getSortField());
    }

    protected String getResultsKey() {
        return getLookupSource().getCollectionName();
    }

    public String findInstitution(String institution) throws SolrServerException, IOException {
        this.setLookupSource(LookupSource.INSTITUTION);
        if (SearchUtils.checkMinString(institution, getMinLookupLength())) {
            try {
                creatorSearchService.findInstitution(institution, this, this, getMinLookupLength());
            } catch (ParseException e) {
                addActionErrorWithException(getText("abstractLookupController.invalid_syntax"), e);
                return ERROR;
            }
        }
        jsonifyResult(JsonLookupFilter.class);
        return SUCCESS;
    }

    public LookupSource getLookupSource() {
        return lookupSource;
    }

    public void setLookupSource(LookupSource lookupSource) {
        this.lookupSource = lookupSource;
    }

    public PaginationHelper getPaginationHelper() {
        if (paginationHelper == null) {
            paginationHelper = PaginationHelper.withSearchResults(this);
        }
        return paginationHelper;
    }

    /**
     * indicates whether view layer should hide facet + sort controls
     * 
     * @return
     */
    public boolean isHideFacetsAndSort() {
        return true;
    }

    public SortOption getDefaultSort() {
        return defaultSort;
    }

    public void setDefaultSort(SortOption defaultSort) {
        this.defaultSort = defaultSort;
    }

    @Override
    public ProjectionModel getProjectionModel() {
        return projectionModel;
    }

    public void setProjectionModel(ProjectionModel projectionModel) {
        this.projectionModel = projectionModel;
    }

    public InputStream getJsonInputStream() {
        return jsonInputStream;
    }

    public void setJsonInputStream(InputStream jsonInputStream) {
        this.jsonInputStream = jsonInputStream;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    @Override
    public int getDefaultRecordsPerPage() {
        return DEFAULT_RESULT_SIZE;
    }

    public FacetWrapper getFacetWrapper() {
        return facetWrapper;
    }

    public void setFacetWrapper(FacetWrapper facetWrapper) {
        this.facetWrapper = facetWrapper;
    }

}
