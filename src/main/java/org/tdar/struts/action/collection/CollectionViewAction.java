package org.tdar.struts.action.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.struts2.convention.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.DisplayOrientation;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.collection.ResourceCollection.CollectionType;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.bean.resource.VersionType;
import org.tdar.core.bean.statistics.ResourceCollectionViewStatistic;
import org.tdar.core.exception.SearchPaginationException;
import org.tdar.core.exception.StatusCode;
import org.tdar.core.service.BookmarkedResourceService;
import org.tdar.core.service.ResourceCollectionService;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.core.service.resource.ResourceService;
import org.tdar.core.service.search.SearchService;
import org.tdar.filestore.Filestore.ObjectType;
import org.tdar.search.query.FacetValue;
import org.tdar.search.query.QueryFieldNames;
import org.tdar.search.query.SearchResultHandler;
import org.tdar.search.query.SortOption;
import org.tdar.search.query.builder.ResourceQueryBuilder;
import org.tdar.struts.action.AbstractPersistableViewableAction;
import org.tdar.struts.action.SlugViewAction;
import org.tdar.struts.action.TdarActionException;
import org.tdar.struts.action.TdarActionSupport;
import org.tdar.struts.data.FacetGroup;
import org.tdar.struts.interceptor.annotation.HttpOnlyIfUnauthenticated;
import org.tdar.utils.PaginationHelper;
import org.tdar.utils.PersistableUtils;

@Component
@Scope("prototype")
@ParentPackage("default")
@Namespace("/collection")
@Results(value = {
        @Result(name = TdarActionSupport.SUCCESS, location = "view.ftl"),
        @Result(name = CollectionViewAction.SUCCESS_WHITELABEL, location = "view-whitelabel.ftl"),
        @Result(name = TdarActionSupport.BAD_SLUG, type = TdarActionSupport.REDIRECT,
                location = "${id}/${persistable.slug}${slugSuffix}", params = { "ignoreParams", "id,slug" }), //removed ,keywordPath
        @Result(name = TdarActionSupport.INPUT, type = TdarActionSupport.HTTPHEADER, params = { "error", "404" })
})
public class CollectionViewAction extends AbstractPersistableViewableAction<ResourceCollection> implements SearchResultHandler<Resource>, SlugViewAction, ResourceFacetedAction {

    private static final long serialVersionUID = 5126290300997389535L;

    public static final String SUCCESS_WHITELABEL = "success_whitelabel";

    /**
     * Threshold that defines a "big" collection (based on imperical evidence by highly-trained tDAR staff). This number
     * refers to the combined count of authorized users +the count of resources associated with a collection. Big
     * collections may adversely affect save/load times as well as cause rendering problems on the client, and so the
     * system may choose to mitigate these effects (somehow)
     */
    public static final int BIG_COLLECTION_CHILDREN_COUNT = 3_000;

    @Autowired
    private transient SearchService searchService;
    @Autowired
    private transient ResourceCollectionService resourceCollectionService;
    @Autowired
    private transient ResourceService resourceService;
    @Autowired
    private transient AuthorizationService authorizationService;
    @Autowired
    private transient BookmarkedResourceService bookmarkedResourceService;

    private Long parentId;
    private List<ResourceCollection> collections = new LinkedList<>();
    private ArrayList<FacetValue> resourceTypeFacets = new ArrayList<>();

    private Long viewCount = 0L;
    private int startRecord = DEFAULT_START;
    private int recordsPerPage = getDefaultRecordsPerPage();
    private int totalRecords;
    private List<Resource> results;
    private SortOption secondarySortField;
    private SortOption sortField;
    private String mode = "CollectionBrowse";
    private PaginationHelper paginationHelper;
    private String parentCollectionName;
    private ArrayList<ResourceType> selectedResourceTypes = new ArrayList<ResourceType>();

    /**
     * Returns a list of all resource collections that can act as candidate parents for the current resource collection.
     * 
     * @return
     */
    public List<ResourceCollection> getCandidateParentResourceCollections() {
        List<ResourceCollection> publicResourceCollections = resourceCollectionService.findPotentialParentCollections(getAuthenticatedUser(),
                getPersistable());
        return publicResourceCollections;
    }

    @Override
    public boolean authorize() throws TdarActionException {
        if (getResourceCollection() == null) {
            throw new TdarActionException(StatusCode.NOT_FOUND, "not found");
        }
        return authorizationService.canViewCollection(getResourceCollection(), getAuthenticatedUser());
    }

    public boolean isVisible() {
        try {
            if (!getResourceCollection().isHidden() || authorize()) {
                return true;
            }
        } catch (TdarActionException e) {
            getLogger().debug("error:", e);
        }
        return false;
    }

    public ResourceCollection getResourceCollection() {
        return getPersistable();
    }

    public void setResourceCollection(ResourceCollection rc) {
        setPersistable(rc);
    }

    @Override
    public Class<ResourceCollection> getPersistableClass() {
        return ResourceCollection.class;
    }

    public List<SortOption> getSortOptions() {
        return SortOption.getOptionsForResourceCollectionPage();
    }

    public List<DisplayOrientation> getResultsOrientations() {
        List<DisplayOrientation> options = Arrays.asList(DisplayOrientation.values());
        return options;
    }

    @Override
    public String loadViewMetadata() {
        setParentId(getPersistable().getParentId());
        if (!isEditor()) {
            ResourceCollectionViewStatistic rcvs = new ResourceCollectionViewStatistic(new Date(), getPersistable());
            getGenericService().saveOrUpdate(rcvs);
        } else {
            setViewCount(resourceCollectionService.getCollectionViewCount(getPersistable()));
        }

        reSortFacets(this, getPersistable());
        return SUCCESS;
    }


    @Override
    public void loadExtraViewMetadata() {
        if (PersistableUtils.isNullOrTransient(getPersistable())) {
            return;
        }
        getLogger().trace("child collections: begin");
        Set<ResourceCollection> findAllChildCollections;

        if (isAuthenticated()) {
            resourceCollectionService.buildCollectionTreeForController(getPersistable(), getAuthenticatedUser(), CollectionType.SHARED);
            findAllChildCollections = getPersistable().getTransientChildren();

            if (isEditor()) {
                List<Long> collectionIds = PersistableUtils.extractIds(getPersistable().getTransientChildren());
                collectionIds.add(getId());
                setUploadedResourceAccessStatistic(resourceService.getSpaceUsageForCollections(collectionIds, Arrays.asList(Status.ACTIVE, Status.DRAFT)));
            }
        } else {
            findAllChildCollections = new LinkedHashSet<ResourceCollection>(resourceCollectionService.findDirectChildCollections(getId(), false,
                    CollectionType.SHARED));
        }
        setCollections(new ArrayList<>(findAllChildCollections));
        getLogger().trace("child collections: sort");
        Collections.sort(collections);
        getLogger().trace("child collections: end");

        // if this collection is public, it will appear in a resource's public collection id list, otherwise it'll be in the shared collection id list
        // String collectionListFieldName = getPersistable().isVisible() ? QueryFieldNames.RESOURCE_COLLECTION_PUBLIC_IDS
        // : QueryFieldNames.RESOURCE_COLLECTION_SHARED_IDS;

        getLogger().trace("lucene: end");
    }


    @HttpOnlyIfUnauthenticated
    @Actions(value = {
            @Action(value = "{id}/{slug}"),
            @Action(value = "{id}")
    })
    public String view() throws TdarActionException {
        String result = super.view();
        if(SUCCESS.equals(result) && getPersistable().isWhiteLabelCollection()) {
            result =  CollectionViewAction.SUCCESS_WHITELABEL;
        }
        return result;
    }



    private void buildLuceneSearch() throws TdarActionException {
        // the visibilty fence should take care of visible vs. shared above
        ResourceQueryBuilder qb = searchService.buildResourceContainedInSearch(QueryFieldNames.RESOURCE_COLLECTION_SHARED_IDS,
                getResourceCollection(), getAuthenticatedUser(), this);
        searchService.addResourceTypeFacetToViewPage(qb, selectedResourceTypes, this);

        setSortField(getPersistable().getSortBy());
        if (getSortField() != SortOption.RELEVANCE) {
            setSecondarySortField(SortOption.TITLE);
            if (getPersistable().getSecondarySortBy() != null) {
                setSecondarySortField(getPersistable().getSecondarySortBy());
            }
        }

        try {
            searchService.handleSearch(qb, this, this);
            bookmarkedResourceService.applyTransientBookmarked(getResults(), getAuthenticatedUser());
        } catch (SearchPaginationException spe) {
            throw new TdarActionException(StatusCode.BAD_REQUEST, spe);
        } catch (Exception e) {
            addActionErrorWithException(getText("collectionController.error_searching_contents"), e);
        }
    }

    @Override
    public SortOption getSortField() {
        return this.sortField;
    }

    @Override
    public SortOption getSecondarySortField() {
        return this.secondarySortField;
    }

    @Override
    public void setTotalRecords(int resultSize) {
        this.totalRecords = resultSize;
    }

    @Override
    public int getStartRecord() {
        return this.startRecord;
    }

    @Override
    public int getRecordsPerPage() {
        return this.recordsPerPage;
    }

    @Override
    public boolean isDebug() {
        return false;
    }

    @Override
    public boolean isShowAll() {
        return false;
    }

    @Override
    public void setStartRecord(int startRecord) {
        this.startRecord = startRecord;
    }

    @Override
    public void setRecordsPerPage(int recordsPerPage) {
        this.recordsPerPage = recordsPerPage;
    }

    public void setCollections(List<ResourceCollection> findAllChildCollections) {
        if (getLogger().isTraceEnabled()) {
            getLogger().trace("child collections: {}", findAllChildCollections);
        }
        this.collections = findAllChildCollections;
    }

    public List<ResourceCollection> getCollections() {
        return this.collections;
    }

    @Override
    public int getTotalRecords() {
        return totalRecords;
    }

    @Override
    public void setResults(List<Resource> toReturn) {
        getLogger().trace("setResults: {}", toReturn);
        this.results = toReturn;
    }

    @Override
    public List<Resource> getResults() {
        return results;
    }

    public void setSecondarySortField(SortOption secondarySortField) {
        this.secondarySortField = secondarySortField;
    }

    @Override
    public void setSortField(SortOption sortField) {
        this.sortField = sortField;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.tdar.struts.search.query.SearchResultHandler#setMode(java.lang.String)
     */
    @Override
    public void setMode(String mode) {
        this.mode = mode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.tdar.struts.search.query.SearchResultHandler#getMode()
     */
    @Override
    public String getMode() {
        return mode;
    }

    @Override
    public int getNextPageStartRecord() {
        return startRecord + recordsPerPage;
    }

    @Override
    public int getPrevPageStartRecord() {
        return startRecord - recordsPerPage;
    }

    @Override
    public String getSearchTitle() {
        return getText("collectionViewAction.search_title", Arrays.asList(getPersistable().getTitle()));
    }

    @Override
    public String getSearchDescription() {
        return getSearchTitle();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<FacetGroup<? extends Enum>> getFacetFields() {
        List<FacetGroup<? extends Enum>> group = new ArrayList<>();
        // List<FacetGroup<?>> group = new ArrayList<FacetGroup<?>>();
        group.add(new FacetGroup<ResourceType>(ResourceType.class, QueryFieldNames.RESOURCE_TYPE, resourceTypeFacets, ResourceType.DOCUMENT));
        return group;
    }

    public PaginationHelper getPaginationHelper() {
        if (paginationHelper == null) {
            paginationHelper = PaginationHelper.withSearchResults(this);
        }
        return paginationHelper;
    }

    public String getParentCollectionName() {
        return parentCollectionName;

    }

    public ArrayList<FacetValue> getResourceTypeFacets() {
        return resourceTypeFacets;
    }

    public void setResourceTypeFacets(ArrayList<FacetValue> resourceTypeFacets) {
        this.resourceTypeFacets = resourceTypeFacets;
    }

    public ArrayList<ResourceType> getSelectedResourceTypes() {
        return selectedResourceTypes;
    }

    public void setSelectedResourceTypes(ArrayList<ResourceType> selectedResourceTypes) {
        this.selectedResourceTypes = selectedResourceTypes;
    }

    @Override
    public ProjectionModel getProjectionModel() {
        return ProjectionModel.RESOURCE_PROXY;
    }

    /**
     * A hint to the view-layer that this resource collection is "big". The view-layer may choose to gracefully degrade the presentation to save on bandwidth
     * and/or
     * client resources.
     * 
     * @return
     */
    public boolean isBigCollection() {
        return (getPersistable().getResources().size() + getAuthorizedUsers().size()) > BIG_COLLECTION_CHILDREN_COUNT;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public boolean isEditable() {
        return authorizationService.canEditCollection(getAuthenticatedUser(), getPersistable());
    }

    @Override
    public void prepare() throws TdarActionException {
        super.prepare();
        if (!isRedirectBadSlug() && PersistableUtils.isNotTransient(getPersistable())) {
            try {
                buildLuceneSearch();
            } catch (Exception e) {
                if (e.getCause() instanceof SearchPaginationException) {
                    getLogger().warn("search pagination issue", e);
                } else {
                    throw e;
                }

            }
        }
    }

    @Override
    public int getDefaultRecordsPerPage() {
        return 100;
    }

    public boolean isLogoAvailable() {
        return checkLogoAvailable(ObjectType.COLLECTION, getId(), VersionType.WEB_SMALL);
    }

    public boolean isSearchHeaderLogoAvailable() {
        //for now, we just look in hosted + collection ID
        String filename =  getResourceCollection().getId() +  "/search-header.jpg";
        return checkHostedFileAvailable(filename);
    }


    /**
     * Indicate to view layer that we should display a search header.
     * @return
     */
    public boolean isSearchHeaderEnabled() {
        return getResourceCollection().isSearchEnabled();
    }

    /**
     * Indicates whether the view layer should show sub-navigation elements.  We turn this off when the 'search header' is enabled.
     *
     */
    @Override
    public boolean isSubnavEnabled() {
        return !isSearchHeaderEnabled();
    }


}
