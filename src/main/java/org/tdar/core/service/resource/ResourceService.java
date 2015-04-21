package org.tdar.core.service.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.HasResource;
import org.tdar.core.bean.cache.HomepageGeographicKeywordCache;
import org.tdar.core.bean.cache.HomepageResourceCountCache;
import org.tdar.core.bean.cache.WeeklyPopularResourceCache;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.collection.ResourceCollection.CollectionType;
import org.tdar.core.bean.coverage.LatitudeLongitudeBox;
import org.tdar.core.bean.entity.AuthorizedUser;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.ResourceCreator;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.keyword.GeographicKeyword;
import org.tdar.core.bean.keyword.Keyword;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.Project;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.ResourceNote;
import org.tdar.core.bean.resource.ResourceNoteType;
import org.tdar.core.bean.resource.ResourceRevisionLog;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.bean.resource.datatable.DataTable;
import org.tdar.core.bean.statistics.AggregateDownloadStatistic;
import org.tdar.core.bean.statistics.AggregateViewStatistic;
import org.tdar.core.bean.statistics.ResourceAccessStatistic;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.GenericDao.FindOptions;
import org.tdar.core.dao.resource.DataTableDao;
import org.tdar.core.dao.resource.DatasetDao;
import org.tdar.core.dao.resource.ProjectDao;
import org.tdar.core.dao.resource.stats.DateGranularity;
import org.tdar.core.dao.resource.stats.ResourceSpaceUsageStatistic;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.exception.TdarRuntimeException;
import org.tdar.core.service.DeleteIssue;
import org.tdar.core.service.EntityService;
import org.tdar.core.service.GenericService;
import org.tdar.core.service.ResourceCreatorProxy;
import org.tdar.core.service.SerializationService;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.search.geosearch.GeoSearchService;
import org.tdar.search.query.SearchResultHandler;
import org.tdar.utils.PersistableUtils;
import org.tdar.utils.ImmutableScrollableCollection;

import com.opensymphony.xwork2.TextProvider;
import com.redfin.sitemapgenerator.GoogleImageSitemapGenerator;

@Service
public class ResourceService extends GenericService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public enum ErrorHandling {
        NO_VALIDATION,
        VALIDATE_SKIP_ERRORS,
        VALIDATE_WITH_EXCEPTION
    }

    @Autowired
    private transient SerializationService serializationService;

    @Autowired
    private transient EntityService entityService;

    @Autowired
    private DatasetDao datasetDao;
    @Autowired
    private ProjectDao projectDao;
    @Autowired
    private DataTableDao dataTableDao;

    @Autowired
    private AuthorizationService authenticationAndAuthorizationService;
    @Autowired
    private GeoSearchService geoSearchService;

    @Transactional(readOnly = true)
    public List<Resource> findSkeletonsForSearch(boolean trustCache, Long... ids) {
        return datasetDao.findSkeletonsForSearch(trustCache, ids);
    }

    @Transactional(readOnly = true)
    public List<Resource> findOld(Long... ids) {
        return datasetDao.findOld(ids);
    }

    /**
     * Find all @Link Resource Ids submitted by @link Person
     * 
     * @param person
     * @return
     */
    @Transactional(readOnly = true)
    public Set<Long> findResourcesSubmittedByUser(Person person) {
        return datasetDao.findResourcesSubmittedByUser(person);
    }

    /**
     * Find @link Resource by Id only.
     * 
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public <R extends Resource> R find(Long id) {
        if (id == null) {
            return null;
        }
        ResourceType rt = datasetDao.findResourceType(id);
        logger.trace("finding resource " + id + " type:" + rt);
        if (rt == null) {
            return null;
        }
        return (R) datasetDao.find(rt.getResourceClass(), id);
    }

    /**
     * Finds all Resources within tDAR and populates them with sparse data -- Title, Description, Date.
     * 
     * @return
     */
    @Transactional(readOnly = true)
    public List<Resource> findAllSparseActiveResources() {
        return datasetDao.findAllSparseActiveResources();
    }

    /**
     * Finds @link Resource entries that have been modified recently.
     */
    public List<Resource> findRecentlyUpdatedItemsInLastXDays(int days) {
        return datasetDao.findRecentlyUpdatedItemsInLastXDays(days);
    }

    /**
     * Adds a @link ResourceRevisionLog entry for the resource based on the message.
     * 
     * @param <T>
     * @param modifiedResource
     * @param message
     * @param payload
     */
    @Transactional
    public <T extends Resource> void logResourceModification(T modifiedResource, TdarUser person, String message) {
        logResourceModification(modifiedResource, person, message, null);
    }

    /**
     * Adds a @link ResourceRevisionLog entry for the resource based on the message.
     * 
     * @param <T>
     * @param modifiedResource
     * @param message
     * @param payload
     */
    @Transactional
    public <T extends Resource> void logResourceModification(T modifiedResource, TdarUser person, String message, String payload) {
        ResourceRevisionLog log = new ResourceRevisionLog();
        log.setLogMessage(message);
        log.setResource(modifiedResource);
        log.setPerson(person);
        log.setTimestamp(new Date());
        log.setPayload(payload);
        save(log);
    }

    /**
     * Lists all tDAR @link Status entries.
     * 
     * @return
     */
    public List<Status> findAllStatuses() {
        return Arrays.asList(Status.values());
    }

    /**
     * For a given @link Resource, increment the Access Count by creating a @link ResourceAccessStatistic Entry. This is a service-layer function because (a)
     * this may happen when the session is not writable, and we're going to make the explicit bean writable, and (b) because it's a transient (separate) value.
     * 
     * @param r
     */
    @Transactional(readOnly = false)
    public void incrementAccessCounter(Resource r) {
        ResourceAccessStatistic rac = new ResourceAccessStatistic(new Date(), r);
        datasetDao.markWritable(rac);
        save(rac);
    }

    /**
     * Updates the transient access count entry on @link Resource
     * 
     * @param resource
     */
    @Transactional(readOnly = true)
    public void updateTransientAccessCount(Resource resource) {
        resource.setTransientAccessCount(datasetDao.getAccessCount(resource).longValue());
    }

    /**
     * Provides a count of the total number of active resources.
     * 
     * @param type
     * @return
     */
    @Transactional(readOnly = true)
    public Number countActiveResources(ResourceType type) {
        return datasetDao.countActiveResources(type);
    }

    /**
     * Provides a count of the total number of active resources with files.
     * 
     * @param type
     * @return
     */
    @Transactional(readOnly = true)
    public Number countActiveResourcesWithFiles(ResourceType type) {
        return datasetDao.countActiveResourcesWithFiles(type);
    }

    /**
     * Provides counts of Country Codes in tDAR based on the Managed Geographic Keywords which are generated by the tDAR GIS Database and lookups comparing a
     * LatLong to a shapefile. These are used on the homepage map.
     * 
     * @return
     */
    @Transactional(readOnly = true)
    public List<HomepageGeographicKeywordCache> getISOGeographicCounts() {
        return datasetDao.getISOGeographicCounts();
    }

    /**
     * For a given @link Resource and set of @link LatitudeLogitudeBox entries, pass the LatitudeLogitudeBoxes to the @link GeographicSearchService to generate
     * GeographicKeyword entries from the LatitudeLogitudeBox. The implementation will clear the old keywords when creating the new ones. It will use Shapefiles
     * and the tdar_gis database (tdar.support repo) to look up where the LatitudeLogitudeBox is.
     * 
     * @param resource
     * @param allLatLongBoxes
     */
    @Transactional
    public void processManagedKeywords(Resource resource, Collection<LatitudeLongitudeBox> allLatLongBoxes) {
        // needed in cases like the APIController where the collection is not properly initialized
        if (resource.getManagedGeographicKeywords() == null) {
            resource.setManagedGeographicKeywords(new LinkedHashSet<GeographicKeyword>());
        }

        Set<GeographicKeyword> kwds = new HashSet<GeographicKeyword>();
        for (LatitudeLongitudeBox latLong : allLatLongBoxes) {
            Set<GeographicKeyword> managedKeywords = geoSearchService.extractAllGeographicInfo(latLong);
            logger.debug(resource.getId() + " :  " + managedKeywords + " " + managedKeywords.size());
            kwds.addAll(
                    datasetDao.findByExamples(GeographicKeyword.class, managedKeywords, Arrays.asList(Keyword.IGNORE_PROPERTIES_FOR_UNIQUENESS),
                            FindOptions.FIND_FIRST_OR_CREATE));
        }
        PersistableUtils.reconcileSet(resource.getManagedGeographicKeywords(), kwds);
    }

    @Transactional
    /**
     * Given a collection of hibernate-managed beans (the 'current' collection)  and another collection of transient beans (the 'incoming' collection),
     * update the current collection to match the contents of the incoming collection. This method will associate all elements in the incoming collection 
     * with the specified resource.  Contents of both collections should satisfy the HasResource interface.
     * 
     * @param resource  the 'owner' of the elements in the incoming collection.  This method will associate all elements of the incoming collection with this resource.
     * @param shouldSave if true,  this method will persist elements of the incoming collection.
     * @param validateMethod determines what validation steps (if any) to perform on each element of the incoming collection
     * @param incoming_ the incoming collection of HasResource elements.  
     * @param current the current collection of HasResource elements.  This method will modify collection to contain the same elements as the incoming collection.
     * @param cls type of the collection elements.
     */
    public <H extends HasResource<R>, R extends Resource> void saveHasResources(R resource, boolean shouldSave, ErrorHandling validateMethod,
            Collection<H> incoming_,
            Set<H> current, Class<H> cls) {
        if (CollectionUtils.isEmpty(incoming_) && CollectionUtils.isEmpty(current)) {
            // skip a complete no-op
            return;
        }

        if (incoming_ == null) {
            incoming_ = new ArrayList<H>();
        }
        Collection<H> incoming = incoming_;
        // there are cases where current and incoming_ are the same object, if that's the case
        // then we need to copy incoming_ before
        if ((incoming_ == current) && !CollectionUtils.isEmpty(incoming_)) {
            incoming = new ArrayList<H>();
            incoming.addAll(incoming_);
            current.clear();
        }

        // assume everything that's incoming is valid or deduped and tied back into tDAR entities/beans
        logger.debug("Current Collection of {}s ({}) : {} ", new Object[] { cls.getSimpleName(), current.size(), current });

        /*
         * Because we're using ID for the equality and hashCode, we have no way to avoid deleting everything and re-adding it.
         * This is an issue as what'll end up happening otherwise is something like editing a Date results in no persisted change because the
         * "retainAll" below keeps the older version
         */

        current.retainAll(incoming);
        Map<Long, H> idMap = PersistableUtils.createIdMap(current);
        if (CollectionUtils.isNotEmpty(incoming)) {
            logger.debug("Incoming Collection of {}s ({})  : {} ", new Object[] { cls.getSimpleName(), incoming.size(), incoming });
            Iterator<H> incomingIterator = incoming.iterator();
            while (incomingIterator.hasNext()) {
                H hasResource_ = incomingIterator.next();

                if (hasResource_ != null) {

                    // attach the incoming notes to a hibernate session
                    logger.trace("adding {} to {} ", hasResource_, current);
                    H existing = idMap.get(hasResource_.getId());
                    /*
                     * If we're not transient, compare the two beans on all of their local properties (non-recursive) -- if there are differences
                     * copy. otherwise, move on. Question -- it may be more work to compare than to just "copy"... is it worth it?
                     */
                    if (PersistableUtils.isNotNullOrTransient(existing) && !EqualsBuilder.reflectionEquals(existing, hasResource_)) {
                        try {
                            logger.trace("copying bean properties for entry in existing set");
                            BeanUtils.copyProperties(existing, hasResource_);
                        } catch (Exception e) {
                            logger.error("exception setting bean property", e);
                        }
                    }

                    if (validateMethod != ErrorHandling.NO_VALIDATION) {
                        boolean isValid = false;
                        if (hasResource_ instanceof ResourceCreator) {
                            isValid = ((ResourceCreator) hasResource_).isValidForResource(resource);
                        } else {
                            isValid = hasResource_.isValid();
                        }

                        if (!isValid) {
                            logger.debug("skipping: {} - INVALID", hasResource_);
                            if (validateMethod == ErrorHandling.VALIDATE_WITH_EXCEPTION) {
                                throw new TdarRecoverableRuntimeException(hasResource_ + " is not valid");
                            }
                            continue;
                        }
                    }

                    current.add(hasResource_);

                    // if (shouldSave) {
                    // getGenericDao().saveOrUpdate(hasResource_);
                    // }
                }
            }
        }
        logger.debug("Resulting Collection of {}s ({}) : {} ", new Object[] { cls.getSimpleName(), current.size(), current });
    }

    /**
     * Find all of the Resource Counts for the homepage which are cached in @link HomepageResourceCountCache entries because the counts take too long to load
     * dynamically.
     * 
     * @return
     */
    public List<HomepageResourceCountCache> getResourceCounts() {
        return datasetDao.getResourceCounts();
    }

    /**
     * For the Dashboard, create a Map<> of ResourceType to Status & Count for a given @link Person User. This map can be used to generate the two graphs on the
     * dashboard.
     * 
     * @param p
     * @param resourceTypes
     * @return
     */
    public Map<ResourceType, Map<Status, Long>> getResourceCountAndStatusForUser(Person p, List<ResourceType> resourceTypes) {
        return datasetDao.getResourceCountAndStatusForUser(p, resourceTypes);
    }

    /**
     * For a given @link ResourceType and @link Status provide a count of @link Resource entries.
     * 
     * @param resourceType
     * @param status
     * @return
     */
    public Long getResourceCount(ResourceType resourceType, Status status) {
        return datasetDao.getResourceCount(resourceType, status);
    }

    /**
     * Use by the @link BulkUploadService, we use a proxy @link Resource (image) to create a new @link Resource of the specified type.
     * 
     * @param proxy
     * @param resourceClass
     * @return
     */
    @Transactional
    public <T extends Resource> T createResourceFrom(Resource proxy, Class<T> resourceClass) {
        return createResourceFrom(proxy, resourceClass, true);
    }

    /**
     * Use by the @link BulkUploadService, we use a proxy @link Resource (image) to create a new @link Resource of the specified type.
     * 
     * @param proxy
     * @param resourceClass
     * @return
     */
    @Transactional
    public <T extends Resource> T createResourceFrom(Resource proxy, Class<T> resourceClass, boolean save) {
        try {
            T resource = resourceClass.newInstance();
            resource.setTitle(proxy.getTitle());
            resource.setDescription(proxy.getDescription());
            if (StringUtils.isEmpty(resource.getDescription())) {
                resource.setDescription(" ");
            }
            resource.setDateCreated(proxy.getDateCreated());
            resource.markUpdated(proxy.getSubmitter());
            resource.setStatus(proxy.getStatus());
            if (save) {
                datasetDao.save(resource);
            }
            resource.getMaterialKeywords().addAll(proxy.getMaterialKeywords());
            resource.getTemporalKeywords().addAll(proxy.getTemporalKeywords());
            resource.getInvestigationTypes().addAll(proxy.getInvestigationTypes());
            resource.getCultureKeywords().addAll(proxy.getCultureKeywords());
            resource.getOtherKeywords().addAll(proxy.getOtherKeywords());
            resource.getSiteNameKeywords().addAll(proxy.getSiteNameKeywords());
            resource.getSiteTypeKeywords().addAll(proxy.getSiteTypeKeywords());
            resource.getGeographicKeywords().addAll(proxy.getGeographicKeywords());
            resource.getManagedGeographicKeywords().addAll(proxy.getManagedGeographicKeywords());
            // CLONE if internal, otherwise just add

            for (ResourceCollection collection : proxy.getResourceCollections()) {
                if (collection.isInternal()) {
                    logger.info("cloning collection: {}", collection);
                    ResourceCollection newInternal = new ResourceCollection(CollectionType.INTERNAL);
                    newInternal.setName(collection.getName());
                    TdarUser owner = collection.getOwner();
                    refresh(owner);
                    newInternal.markUpdated(owner);
                    if (save) {
                        datasetDao.save(newInternal);
                    }
                    for (AuthorizedUser proxyAuthorizedUser : collection.getAuthorizedUsers()) {
                        AuthorizedUser newAuthorizedUser = new AuthorizedUser(proxyAuthorizedUser.getUser(),
                                proxyAuthorizedUser.getGeneralPermission());
                        newInternal.getAuthorizedUsers().add(newAuthorizedUser);
                    }
                    resource.getResourceCollections().add(newInternal);
                    newInternal.getResources().add(resource);
                } else {
                    logger.info("adding to shared collection : {} ", collection);
                    if (collection.isTransient() && save) {
                        save(collection);
                    }
                    collection.getResources().add(resource);
                    resource.getResourceCollections().add(collection);
                }
            }

            cloneSet(resource, resource.getCoverageDates(), proxy.getCoverageDates());
            cloneSet(resource, resource.getLatitudeLongitudeBoxes(), proxy.getLatitudeLongitudeBoxes());
            cloneSet(resource, resource.getResourceCreators(), proxy.getResourceCreators());
            cloneSet(resource, resource.getResourceAnnotations(), proxy.getResourceAnnotations());
            cloneSet(resource, resource.getResourceNotes(), proxy.getResourceNotes());
            cloneSet(resource, resource.getRelatedComparativeCollections(), proxy.getRelatedComparativeCollections());
            cloneSet(resource, resource.getSourceCollections(), proxy.getSourceCollections());

            if ((resource instanceof InformationResource) && (proxy instanceof InformationResource)) {
                InformationResource proxyInformationResource = (InformationResource) proxy;
                InformationResource informationResource = (InformationResource) resource;
                informationResource.setDate(proxyInformationResource.getDate());
                // force project into the session
                if (PersistableUtils.isNotNullOrTransient(proxyInformationResource.getProject())) {
                    Project project = proxyInformationResource.getProject();
                    // refresh(project);
                    informationResource.setProject(project);
                }
                informationResource.setPublisher(proxyInformationResource.getPublisher());
                informationResource.setCopyrightHolder(proxyInformationResource.getCopyrightHolder());
                informationResource.setLicenseText(proxyInformationResource.getLicenseText());
                informationResource.setLicenseType(proxyInformationResource.getLicenseType());
                informationResource.setPublisherLocation(proxyInformationResource.getPublisherLocation());
                informationResource.setResourceProviderInstitution(proxyInformationResource.getResourceProviderInstitution());
                informationResource.setResourceLanguage(proxyInformationResource.getResourceLanguage());
                informationResource.setMetadataLanguage(proxyInformationResource.getMetadataLanguage());
                informationResource.setInheritingCulturalInformation(proxyInformationResource.isInheritingCulturalInformation());
                informationResource.setInheritingInvestigationInformation(proxyInformationResource.isInheritingInvestigationInformation());
                informationResource.setInheritingMaterialInformation(proxyInformationResource.isInheritingMaterialInformation());
                informationResource.setInheritingOtherInformation(proxyInformationResource.isInheritingOtherInformation());
                informationResource.setInheritingSiteInformation(proxyInformationResource.isInheritingSiteInformation());
                informationResource.setInheritingSpatialInformation(proxyInformationResource.isInheritingSpatialInformation());
                informationResource.setInheritingTemporalInformation(proxyInformationResource.isInheritingTemporalInformation());
                informationResource.setInheritingIdentifierInformation(proxyInformationResource.isInheritingIdentifierInformation());
                informationResource.setInheritingNoteInformation(proxyInformationResource.isInheritingNoteInformation());
                informationResource.setInheritingCollectionInformation(proxyInformationResource.isInheritingCollectionInformation());
                informationResource.setInheritingIndividualAndInstitutionalCredit(proxyInformationResource.isInheritingIndividualAndInstitutionalCredit());
            }
            if (save) {
                datasetDao.saveOrUpdate(resource);
                return datasetDao.merge(resource);
            }
            return resource;
            // NOTE: THIS SHOULD BE THE LAST THING DONE AS IT BRINGS EVERYTHING BACK ONTO THE SESSION PROPERLY
        } catch (Exception exception) {
            throw new TdarRuntimeException(exception);
        }
    }

    /**
     * Given a Set of objects that support @link HasResource, clone the bean and attach it to the new Set
     * 
     * @param resource
     * @param targetCollection
     * @param sourceCollection
     * @return
     */
    @Transactional
    public <T extends HasResource<Resource>> Set<T> cloneSet(Resource resource, Set<T> targetCollection, Set<T> sourceCollection) {
        logger.trace("cloning: " + sourceCollection);
        for (T t : sourceCollection) {
            datasetDao.detachFromSessionAndWarn(t);
            try {
                @SuppressWarnings("unchecked")
                T clone = (T) BeanUtils.cloneBean(t);
                targetCollection.add(clone);
            } catch (Exception e) {
                logger.warn("Exception in clone set: {} ", e);
            }
        }
        // getDao().save(targetCollection);
        return targetCollection;
    }

    @Transactional
    public ResourceSpaceUsageStatistic getResourceSpaceUsageStatistics(List<Long> resourceId, List<Status> status) {
        return datasetDao.getResourceSpaceUsageStatistics(resourceId, status);
    }

    @Transactional
    public ResourceSpaceUsageStatistic getResourceSpaceUsageStatisticsForProject(Long id, List<Status> status) {
        return datasetDao.getResourceSpaceUsageStatisticsForProject(id, status);
    }

    @Transactional
    public ResourceSpaceUsageStatistic getSpaceUsageForCollections(List<Long> collectionId, List<Status> statuses) {
        return datasetDao.getSpaceUsageForCollections(collectionId, statuses);
    }

    @Transactional
    public ResourceSpaceUsageStatistic getResourceSpaceUsageStatisticsForUser(List<Long> accountId, List<Status> status) {
        return datasetDao.getResourceSpaceUsageStatisticsForUser(accountId, status);
    }

    /**
     * Find the count of views for all resources for a given date range, limited by the minimum occurrence count.
     * 
     * @param granularity
     * @param start
     * @param end
     * @param minCount
     * @return
     */
    @Transactional
    public List<AggregateViewStatistic> getAggregateUsageStats(DateGranularity granularity, Date start, Date end, Long minCount) {
        return datasetDao.getAggregateUsageStats(granularity, start, end, minCount);
    }

    /**
     * Find the count of downloads for all InformationResourceFiles for a given date range, limited by the minimum occurrence count.
     * 
     * @param granularity
     * @param start
     * @param end
     * @param minCount
     * @return
     */
    @Transactional
    public List<AggregateViewStatistic> getOverallUsageStats(Date start, Date end, Long max) {
        return datasetDao.getOverallUsageStats(start, end, max);
    }

    @Transactional
    public List<AggregateDownloadStatistic> getAggregateDownloadStats(DateGranularity granularity, Date start, Date end, Long minCount) {
        return datasetDao.getAggregateDownloadStats(granularity, start, end, minCount);
    }

    /**
     * Find the count of downloads for a specified @link InformationResourceFile for a given date range, limited by the minimum occurrence count.
     * 
     * @param granularity
     * @param start
     * @param end
     * @param minCount
     * @param iRFileId
     * @return
     */
    @Transactional
    public List<AggregateDownloadStatistic> getAggregateDownloadStatsForFile(DateGranularity granularity, Date start, Date end, Long minCount, Long iRFileId) {
        return datasetDao.getDownloadStatsForFile(granularity, start, end, minCount, iRFileId);
    }

    /**
     * Find the count of views for the specified resources for a given date range, limited by the minimum occurrence count.
     * 
     * @param granularity
     * @param start
     * @param end
     * @param minCount
     * @param resourceIds
     * @return
     */
    @Transactional
    public List<AggregateViewStatistic> getUsageStatsForResources(DateGranularity granularity, Date start, Date end, Long minCount, List<Long> resourceIds) {
        return datasetDao.getUsageStatsForResource(granularity, start, end, minCount, resourceIds);
    }

    /**
     * Return all resource revision log entries for a specified @link Resource.
     * 
     * @param resource
     * @return
     */
    @Transactional
    public List<ResourceRevisionLog> getLogsForResource(Resource resource) {
        if (PersistableUtils.isNullOrTransient(resource)) {
            return Collections.emptyList();
        }
        return datasetDao.getLogEntriesForResource(resource);
    }

    /**
     * Find all ids of @link InformationResource entries that actually have files.
     * 
     * @return
     */
    @Transactional
    public List<Long> findAllResourceIdsWithFiles() {
        return datasetDao.findAllResourceIdsWithFiles();
    }

    /**
     * Find all @link ResourceType entries that are active within the system (as opposed to exist).
     * 
     * @return
     */
    public List<ResourceType> getAllResourceTypes() {
        ArrayList<ResourceType> arrayList = new ArrayList<>(Arrays.asList(ResourceType.values()));
        if (!TdarConfiguration.getInstance().isVideoEnabled()) {
            arrayList.remove(ResourceType.VIDEO);
            arrayList.remove(ResourceType.AUDIO);
        }

        if (!TdarConfiguration.getInstance().isArchiveFileEnabled()) {
            arrayList.remove(ResourceType.ARCHIVE);
        }
        return arrayList;
    }

    @Transactional
    public int findAllResourcesWithPublicImagesForSitemap(GoogleImageSitemapGenerator gisg) {
        return datasetDao.findAllResourcesWithPublicImagesForSitemap(gisg);

    }

    @Transactional
    public void setupWorldMap(HashMap<String, HomepageGeographicKeywordCache> worldMapData) {
        Long countryTotal = 0l;
        Double countryLogTotal = 0d;
        for (HomepageGeographicKeywordCache item : findAllWithL2Cache(HomepageGeographicKeywordCache.class)) {
            Long count = item.getCount();
            Double logCount = item.getLogCount();
            if (logCount > countryLogTotal) {
                countryLogTotal = logCount;
            }
            if (count > countryTotal) {
                countryTotal = count;
            }
            worldMapData.put(item.getKey(), item);
        }
        for (Entry<String, HomepageGeographicKeywordCache> entrySet : worldMapData.entrySet()) {
            entrySet.getValue().setTotalCount(countryTotal);
            entrySet.getValue().setTotalLogCount(countryLogTotal);
        }
    }

    @Transactional(readOnly = true)
    public List<Resource> findByTdarYear(SearchResultHandler<Resource> resultHandler, int year) {
        return datasetDao.findByTdarYear(resultHandler, year);
    }

    @Transactional(readOnly = true)
    public List<Resource> getWeeklyPopularResources(int count) {
        List<Resource> featured = new ArrayList<>();
        try {
            int cacheCount = 0;
            for (WeeklyPopularResourceCache cache : datasetDao.findAll(WeeklyPopularResourceCache.class)) {
                Resource key = cache.getKey();
                if (key instanceof Resource) {
                    authenticationAndAuthorizationService.applyTransientViewableFlag(key, null);
                }
                if (key.isActive()) {
                    if (cacheCount == count) {
                        break;
                    }
                    cacheCount++;
                    featured.add(key);
                }
            }
        } catch (IndexOutOfBoundsException ioe) {
            logger.debug("no featured resources found");
        }
        return featured;
    }

    @Transactional(readOnly = false)
    public void saveResourceCreatorsFromProxies(Collection<ResourceCreatorProxy> allProxies, Resource resource, boolean shouldSaveResource) {
        logger.info("ResourceCreators before DB lookup: {} ", allProxies);
        int sequence = 0;
        List<ResourceCreator> incomingResourceCreators = new ArrayList<>();
        // convert the list of proxies to a list of resource creators
        for (ResourceCreatorProxy proxy : allProxies) {
            if ((proxy != null) && proxy.isValid()) {
                ResourceCreator resourceCreator = proxy.getResourceCreator();
                resourceCreator.setSequenceNumber(sequence++);
                logger.trace("{} - {}", resourceCreator, resourceCreator.getCreatorType());

                entityService.findOrSaveResourceCreator(resourceCreator);
                incomingResourceCreators.add(resourceCreator);
                logger.trace("{} - {}", resourceCreator, resourceCreator.getCreatorType());
            } else {
                logger.trace("can't create creator from proxy {} {}", proxy);
            }
        }

        // FIXME: Should this throw errors?
        saveHasResources(resource, shouldSaveResource, ErrorHandling.VALIDATE_SKIP_ERRORS, incomingResourceCreators,
                resource.getResourceCreators(), ResourceCreator.class);

    }

    @Transactional(readOnly = true)
    public DeleteIssue getDeletionIssues(TextProvider provider, Resource persistable) {
        DeleteIssue issue = new DeleteIssue();
        switch (persistable.getResourceType()) {
            case PROJECT:
                 ScrollableResults findAllResourcesInProject = projectDao.findAllResourcesInProject((Project) persistable, Status.ACTIVE, Status.DRAFT);
                 Set<InformationResource> inProject = new HashSet<>();
                 for (InformationResource ir : new ImmutableScrollableCollection<InformationResource>(findAllResourcesInProject)) {
                     inProject.add(ir);
                 }
                if (CollectionUtils.isNotEmpty(inProject)) {
                    issue.getRelatedItems().addAll(inProject);
                    issue.setIssue(provider.getText("resourceDeleteController.delete_project"));
                    return issue;
                }
                return null;
            case CODING_SHEET:
            case ONTOLOGY:
                List<DataTable> related = dataTableDao.findDataTablesUsingResource(persistable);
                if (CollectionUtils.isNotEmpty(related)) {
                    for (DataTable table : related) {
                        if (!table.getDataset().isDeleted()) {
                            issue.getRelatedItems().add(table.getDataset());
                        }
                    }
                    issue.setIssue(provider.getText("abstractSupportingInformationResourceController.remove_mappings"));
                    return issue;
                }
                return null;
            default:
                return null;
        }
    }

    @Transactional(readOnly = false)
    public void deleteForController(Resource resource, String reason, TdarUser authUser) {
        if (StringUtils.isNotEmpty(reason)) {
            ResourceNote note = new ResourceNote(ResourceNoteType.ADMIN, reason);
            resource.getResourceNotes().add(note);
            save(note);
        } else {
            reason = "reason not specified";
        }
        String logMessage = String.format("%s id:%s deleted by:%s reason: %s", resource.getResourceType().name(), resource.getId(), authUser, reason);
        logResourceModification(resource, authUser, logMessage);
        delete(resource);
    }

    @Transactional(readOnly=true)
    public ScrollableResults findAllActiveScrollableForSitemap() {
        return datasetDao.findAllActiveScrollableForSitemap();
    }



}
