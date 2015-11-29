package org.tdar.search.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdar.core.bean.HasName;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.coverage.CoverageDate;
import org.tdar.core.bean.coverage.LatitudeLongitudeBox;
import org.tdar.core.bean.entity.Creator;
import org.tdar.core.bean.keyword.Keyword;
import org.tdar.core.bean.keyword.KeywordType;
import org.tdar.core.bean.resource.Dataset.IntegratableOptions;
import org.tdar.core.bean.resource.DocumentType;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.ResourceAccessType;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.service.ResourceCreatorProxy;
import org.tdar.search.index.analyzer.SiteCodeTokenizingAnalyzer;
import org.tdar.search.query.QueryFieldNames;
import org.tdar.search.query.part.CreatorOwnerQueryPart;
import org.tdar.search.query.part.CreatorQueryPart;
import org.tdar.search.query.part.FieldQueryPart;
import org.tdar.search.query.part.GeneralSearchResourceQueryPart;
import org.tdar.search.query.part.HydrateableKeywordQueryPart;
import org.tdar.search.query.part.PhraseFormatter;
import org.tdar.search.query.part.QueryPartGroup;
import org.tdar.search.query.part.RangeQueryPart;
import org.tdar.search.query.part.SkeletonPersistableQueryPart;
import org.tdar.search.query.part.SpatialQueryPart;
import org.tdar.search.query.part.TemporalQueryPart;
import org.tdar.search.query.part.TitleQueryPart;
import org.tdar.utils.MessageHelper;
import org.tdar.utils.PersistableUtils;
import org.tdar.utils.range.DateRange;
import org.tdar.utils.range.StringRange;

import com.opensymphony.xwork2.TextProvider;

import arq.qparse;

/**
 * This class is meant to capture a group of search terms.
 * 
 * 
 * @author jimdevos
 * 
 */
public class SearchParameters {

    public SearchParameters() {
    }

    public SearchParameters(Operator operator) {
        setOperator(operator);
    }

    
    private List<String> filters = new ArrayList<>();
    private boolean explore = false;
    // user specified status that they do not have permissions to search for. probably because they are not logged in.

    private static final Operator defaultOperator = Operator.AND;
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    private List<SearchFieldType> fieldTypes = new ArrayList<SearchFieldType>();
    private Operator operator = defaultOperator;

    // managed keywords. Ultimately we need list of list of id, but struts only knows how to construct list of list of strings
    private List<List<String>> materialKeywordIdLists = new ArrayList<List<String>>();
    private List<List<String>> approvedSiteTypeIdLists = new ArrayList<List<String>>();
    private List<List<String>> approvedCultureKeywordIdLists = new ArrayList<List<String>>();
    private List<List<String>> investigationTypeIdLists = new ArrayList<List<String>>();

    // freeform keywords
    private List<String> otherKeywords = new ArrayList<String>();
    private List<String> siteNames = new ArrayList<String>();
    private List<String> uncontrolledCultureKeywords = new ArrayList<String>();
    private List<String> uncontrolledMaterialKeywords = new ArrayList<String>();
    private List<String> temporalKeywords = new ArrayList<String>();
    private List<String> geographicKeywords = new ArrayList<String>();
    private List<String> uncontrolledSiteTypes = new ArrayList<String>();

    private List<String> allFields = new ArrayList<String>();
    private List<String> titles = new ArrayList<String>();
    private List<String> contents = new ArrayList<String>();
    private List<String> filenames = new ArrayList<String>();

    private ResourceCreatorProxy creatorOwner;
    private List<ResourceCreatorProxy> resourceCreatorProxies = new ArrayList<ResourceCreatorProxy>();
    // private List<String> creatorRoleIdentifiers = new ArrayList<String>();

    private List<Resource> sparseProjects = new ArrayList<Resource>();
    private List<ResourceCollection> sparseCollections = new ArrayList<ResourceCollection>();

    private List<Long> resourceIds = new ArrayList<Long>();

    private List<DateRange> registeredDates = new ArrayList<DateRange>();
    private List<DateRange> updatedDates = new ArrayList<DateRange>();
    private List<CoverageDate> coverageDates = new ArrayList<CoverageDate>();
    private List<StringRange> createdDates = new ArrayList<StringRange>();
    private List<Integer> creationDecades = new ArrayList<Integer>();

    // parameters. don't render these in the form view.
    private String startingLetter;

    // reserved terms
    // We don't allow users (at the moment) to have total control over certain fields: maps, resourceTypes, statuses, and documentTypes
    // Instead, we use a single searchParameters instance to hold these terms, which will be populated via the "narrow your search" section
    // as well by selecting a faceted search.
    private List<LatitudeLongitudeBox> latitudeLongitudeBoxes = new ArrayList<LatitudeLongitudeBox>();
    private List<ResourceType> resourceTypes = new ArrayList<ResourceType>();
    private List<IntegratableOptions> integratableOptions = new ArrayList<IntegratableOptions>();
    private List<DocumentType> documentTypes = new ArrayList<DocumentType>();
    private List<ResourceAccessType> resourceAccessTypes = new ArrayList<ResourceAccessType>();

    public List<SearchFieldType> getFieldTypes() {
        return fieldTypes;
    }

    public void setFieldTypes(List<SearchFieldType> fieldTypes) {
        this.fieldTypes = fieldTypes;
    }

    public Operator getOperator() {
        if (operator == null) {
            operator = defaultOperator;
        }
        return operator;
    }

    public boolean isOr() {
        return getOperator() == Operator.OR;
    }

    public void setOperator(Operator operator) {
        if (operator != null) {
            this.operator = operator;
        }
    }

    public List<List<String>> getMaterialKeywordIdLists() {
        return materialKeywordIdLists;
    }

    public void setMaterialKeywordIdLists(List<List<String>> materialKeywordIdLists) {
        this.materialKeywordIdLists = materialKeywordIdLists;
    }

    public List<List<String>> getApprovedSiteTypeIdLists() {
        return approvedSiteTypeIdLists;
    }

    public void setApprovedSiteTypeIdLists(List<List<String>> approvedSiteTypeIdLists) {
        this.approvedSiteTypeIdLists = approvedSiteTypeIdLists;
    }

    public List<List<String>> getApprovedCultureKeywordIdLists() {
        return approvedCultureKeywordIdLists;
    }

    public void setApprovedCultureKeywordIdLists(List<List<String>> approvedCultureKeywordIdLists) {
        this.approvedCultureKeywordIdLists = approvedCultureKeywordIdLists;
    }

    public List<String> getAllFields() {
        return allFields;
    }

    public void setAllFields(List<String> allFields) {
        this.allFields = allFields;
    }

    public List<String> getTitles() {
        return titles;
    }

    public void setTitles(List<String> titles) {
        this.titles = titles;
    }

    public List<ResourceType> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<ResourceType> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public List<String> getOtherKeywords() {
        return otherKeywords;
    }

    public void setOtherKeywords(List<String> otherKeywords) {
        this.otherKeywords = otherKeywords;
    }

    public List<String> getSiteNames() {
        return siteNames;
    }

    public void setSiteNames(List<String> siteNames) {
        this.siteNames = siteNames;
    }

    public List<String> getUncontrolledCultureKeywords() {
        return uncontrolledCultureKeywords;
    }

    public void setUncontrolledCultureKeywords(List<String> uncontrolledCultureKeywords) {
        this.uncontrolledCultureKeywords = uncontrolledCultureKeywords;
    }

    public List<String> getTemporalKeywords() {
        return temporalKeywords;
    }

    public void setTemporalKeywords(List<String> temporalKeywords) {
        this.temporalKeywords = temporalKeywords;
    }

    public List<String> getGeographicKeywords() {
        return geographicKeywords;
    }

    public void setGeographicKeywords(List<String> geographicKeywords) {
        this.geographicKeywords = geographicKeywords;
    }

    public List<String> getUncontrolledSiteTypes() {
        return uncontrolledSiteTypes;
    }

    public void setUncontrolledSiteTypes(List<String> uncontrolledSiteTypes) {
        this.uncontrolledSiteTypes = uncontrolledSiteTypes;
    }

    public List<LatitudeLongitudeBox> getLatitudeLongitudeBoxes() {
        return latitudeLongitudeBoxes;
    }

    public void setLatitudeLongitudeBoxes(List<LatitudeLongitudeBox> latitudeLongitudeBoxs) {
        this.latitudeLongitudeBoxes = latitudeLongitudeBoxs;
    }

    public List<ResourceCreatorProxy> getResourceCreatorProxies() {
        return resourceCreatorProxies;
    }

    public void setResourceCreatorProxies(List<ResourceCreatorProxy> resourceCreatorInstitutions) {
        this.resourceCreatorProxies = resourceCreatorInstitutions;
    }

    public List<List<String>> getInvestigationTypeIdLists() {
        return investigationTypeIdLists;
    }

    public void setInvestigationTypeIdLists(List<List<String>> investigationTypeIdLists) {
        this.investigationTypeIdLists = investigationTypeIdLists;
    }

    public List<DateRange> getRegisteredDates() {
        return registeredDates;
    }

    public void setRegisteredDates(List<DateRange> registeredDates) {
        this.registeredDates = registeredDates;
    }

    public List<DateRange> getUpdatedDates() {
        return updatedDates;
    }

    public void setUpdatedDates(List<DateRange> updatedDates) {
        this.updatedDates = updatedDates;
    }

    public List<CoverageDate> getCoverageDates() {
        return coverageDates;
    }

    public void setCoverageDates(List<CoverageDate> coverageDates) {
        this.coverageDates = coverageDates;
    }

    public List<Long> getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(List<Long> resourceIds) {
        this.resourceIds = resourceIds;
    }

    TextProvider support = MessageHelper.getInstance();

    // FIXME: where appropriate need to make sure we pass along the operator to any sub queryPart groups
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public QueryPartGroup toQueryPartGroup(TextProvider support_) {
        if (support_ != null) {
            this.support = support_;
        }
        QueryPartGroup queryPartGroup = new QueryPartGroup(getOperator());

        queryPartGroup.append(new GeneralSearchResourceQueryPart(this.getAllFields(), getOperator()));
        queryPartGroup.append(new TitleQueryPart(this.getTitles(), getOperator()));
        queryPartGroup.append(new FieldQueryPart<String>(QueryFieldNames.CONTENT, support.getText("searchParameter.file_contents"), getOperator(), contents));
        queryPartGroup.append(new FieldQueryPart<String>(QueryFieldNames.FILENAME, support.getText("searchParameter.file_name"),
                getOperator(), filenames));

        if (creatorOwner != null) {
            if (PersistableUtils.isNotNullOrTransient(creatorOwner.getPerson())) {
                queryPartGroup.append(new CreatorOwnerQueryPart(creatorOwner.getPerson()));
            }
            if (PersistableUtils.isNotNullOrTransient(creatorOwner.getInstitution())) {
                queryPartGroup.append(new CreatorOwnerQueryPart(creatorOwner.getInstitution()));
            }
        }

        // freeform keywords
        appendKeywordQueryParts(queryPartGroup, KeywordType.OTHER_KEYWORD, Arrays.asList(this.getOtherKeywords()));
        if (CollectionUtils.isNotEmpty(siteNames)) {
            QueryPartGroup subgroup = new QueryPartGroup(Operator.OR);
            for (String q : siteNames) {
                if (StringUtils.isNotBlank(q) && SiteCodeTokenizingAnalyzer.pattern.matcher(q).matches()) {
                    FieldQueryPart<String> siteCodePart = new FieldQueryPart<String>(QueryFieldNames.SITE_CODE, q);
                    siteCodePart.setPhraseFormatters(PhraseFormatter.ESCAPE_QUOTED);
                    siteCodePart.setDisplayName(support.getText("searchParameters.site_code"));
                    subgroup.append(siteCodePart.setBoost(5f));
                }
            }
            appendKeywordQueryParts(subgroup, KeywordType.SITE_NAME_KEYWORD, Arrays.asList(siteNames));
            queryPartGroup.append(subgroup);
        }

        appendKeywordQueryParts(queryPartGroup, KeywordType.CULTURE_KEYWORD, Arrays.asList(this.getUncontrolledCultureKeywords()));
        appendKeywordQueryParts(queryPartGroup, KeywordType.MATERIAL_TYPE, Arrays.asList(this.getUncontrolledMaterialKeywords()));
        appendKeywordQueryParts(queryPartGroup, KeywordType.TEMPORAL_KEYWORD, Arrays.asList(this.getTemporalKeywords()));
        appendKeywordQueryParts(queryPartGroup, KeywordType.GEOGRAPHIC_KEYWORD, Arrays.asList(this.getGeographicKeywords()));
        appendKeywordQueryParts(queryPartGroup, KeywordType.SITE_TYPE_KEYWORD, Arrays.asList(this.getUncontrolledSiteTypes()));

        // managed keywords (in the form of lists of lists of ids)
        appendKeywordQueryParts(queryPartGroup, KeywordType.MATERIAL_TYPE, this.getMaterialKeywordIdLists());
        appendKeywordQueryParts(queryPartGroup, KeywordType.SITE_TYPE_KEYWORD, this.getApprovedSiteTypeIdLists());
        appendKeywordQueryParts(queryPartGroup, KeywordType.SITE_TYPE_KEYWORD, this.getInvestigationTypeIdLists());
        appendKeywordQueryParts(queryPartGroup, KeywordType.CULTURE_KEYWORD, this.getApprovedCultureKeywordIdLists());

        queryPartGroup.append(constructSkeletonQueryPart(QueryFieldNames.PROJECT_ID, support.getText("searchParameter.project"), "project.", Resource.class,
                getOperator(), getProjects()));
        queryPartGroup.append(new FieldQueryPart<Long>(QueryFieldNames.ID, support.getText("searchParameter.id"), Operator.OR, getResourceIds()));

        appendFieldQueryPart(queryPartGroup, QueryFieldNames.RESOURCE_TYPE, support.getText("searchParameter.resource_type"), getResourceTypes(), Operator.OR,
                Arrays.asList(ResourceType.values()));
        appendFieldQueryPart(queryPartGroup, QueryFieldNames.INTEGRATABLE, support.getText("searchParameter.integratable"), getIntegratableOptions(),
                Operator.OR,
                Arrays.asList(IntegratableOptions.values()));

        queryPartGroup.append(new FieldQueryPart<DocumentType>(QueryFieldNames.DOCUMENT_TYPE, support.getText("searchParameter.document_type"), Operator.OR,
                getDocumentTypes()));
        queryPartGroup.append(new FieldQueryPart<ResourceAccessType>(QueryFieldNames.RESOURCE_ACCESS_TYPE, support
                .getText("searchParameter.resource_access_type"), Operator.OR,
                getResourceAccessTypes()));

        queryPartGroup.append(new RangeQueryPart(QueryFieldNames.DATE_CREATED, support.getText("searchParameter.date_created"), getOperator(),
                getRegisteredDates()));
        queryPartGroup.append(new RangeQueryPart(QueryFieldNames.DATE_UPDATED, support.getText("searchParameter.date_updated"), getOperator(),
                getUpdatedDates()));
        queryPartGroup.append(new RangeQueryPart(QueryFieldNames.DATE, support.getText("searchParameter.date"), getOperator(), getCreatedDates()));

        queryPartGroup.append(new TemporalQueryPart(getCoverageDates(), getOperator()));
        SpatialQueryPart spatialQueryPart = new SpatialQueryPart(getLatitudeLongitudeBoxes());
//        getFilters().add(spatialQueryPart.getFilter());
        queryPartGroup.append(spatialQueryPart);
        // NOTE: I AM "SHARED" the autocomplete will supply the "public"

        queryPartGroup.append(constructSkeletonQueryPart(QueryFieldNames.RESOURCE_COLLECTION_SHARED_IDS,
                support.getText("searchParameter.resource_collection"), "resourceCollections.",
                ResourceCollection.class, getOperator(), getCollections()));
        queryPartGroup.append(new CreatorQueryPart<Creator>(QueryFieldNames.CREATOR_ROLE_IDENTIFIER, Creator.class, null, resourceCreatorProxies));

        // explore: decade
        queryPartGroup.append(new FieldQueryPart<Integer>(QueryFieldNames.DATE_CREATED_DECADE, Operator.OR, getCreationDecades()));

        // explore: title starts with
        if (startingLetter != null) {
            FieldQueryPart<String> part = new FieldQueryPart<String>(QueryFieldNames.NAME_SORT, startingLetter.toLowerCase());
            part.setDisplayName(support.getText("searchParameter.title_starts_with", Arrays.asList(startingLetter)));
            part.setPhraseFormatters(PhraseFormatter.WILDCARD);
            queryPartGroup.append(part);
        }

        return queryPartGroup;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <P extends Persistable> SkeletonPersistableQueryPart constructSkeletonQueryPart(String fieldName, String label, String prefix, Class<P> cls,
            Operator operator, List<P> values) {
        SkeletonPersistableQueryPart q = new SkeletonPersistableQueryPart(fieldName, label, cls, values);
        if (HasName.class.isAssignableFrom(cls) && StringUtils.isNotBlank(prefix)) {
            TitleQueryPart tqp = new TitleQueryPart();
            tqp.setPrefix(prefix);
            for (Persistable p : values) {
                HasName name = (HasName) p;
                if ((name != null) && StringUtils.isNotBlank(name.getName())) {
                    tqp.add(name.getName());
                }
            }
            q.setTransientFieldQueryPart(tqp);
            setOperator(operator);
        }
        return q;
    }

    protected <C> void appendFieldQueryPart(QueryPartGroup queryPartGroup, String fieldName, String fieldDisplayName, List<C> incomingList, Operator operator,
            List<C> fullList) {
        Set<C> emptyCheck = new HashSet<C>(fullList);
        for (C item : incomingList) {
            emptyCheck.remove(item);
        }
        if (emptyCheck.isEmpty()) {
            return;
        }
        queryPartGroup.append(new FieldQueryPart<C>(fieldName, fieldDisplayName, operator, incomingList));
    }

    protected <K extends Keyword> void appendKeywordQueryParts(QueryPartGroup group, KeywordType type, List<List<String>> idListList) {
        for (List<String> strings : idListList) {
            if (CollectionUtils.isNotEmpty(strings)) {
                group.append(createKeywordQueryPart(type, strings));
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected <I, K extends Keyword> HydrateableKeywordQueryPart createKeywordQueryPart(KeywordType type, List<I> values) {
        List<K> kwdValues = new ArrayList<K>();
        for (I value : values) {
            if (value == null) {
                continue;
            }
            try {
                K keyword = (K) type.getKeywordClass().newInstance();
                if (value instanceof Keyword) {
                    keyword = (K) value;
                } else if (StringUtils.isNotBlank(value.toString()) && StringUtils.isNumeric(value.toString())) {
                    Long id = Long.valueOf(value.toString());
                    if ((id != null) && (id > -1)) {
                        keyword.setId(id);
                    }
                } else {
                    keyword.setLabel(value.toString());
                }
                logger.trace("kwd: {}", keyword);
                kwdValues.add(keyword);
            } catch (Exception e) {
                throw new TdarRecoverableRuntimeException(e);
            }
        }
        HydrateableKeywordQueryPart hydrateableKeywordQueryPart = new HydrateableKeywordQueryPart(type, kwdValues);
        hydrateableKeywordQueryPart.setIncludeChildren(!explore);
        hydrateableKeywordQueryPart.setDisplayName(support.getText(type.getLocaleKey()));
        return hydrateableKeywordQueryPart;
    }

    public List<DocumentType> getDocumentTypes() {
        return documentTypes;
    }

    public List<ResourceAccessType> getResourceAccessTypes() {
        return resourceAccessTypes;
    }

    // convenience getter for freemarker
    public boolean isMatchingAnyTerms() {
        return getOperator() == Operator.OR;
    }

    // convenience getter for freemarker
    public boolean isMatchingAllTerms() {
        return getOperator() == Operator.AND;
    }

    @Override
    public String toString() {
        try {
            return toQueryPartGroup(null).toString();
        } catch (Exception e) {
            logger.error("error in toString()", e);
            return super.toString();
        }
    }

    public List<StringRange> getCreatedDates() {
        return createdDates;
    }

    public void setCreatedDates(List<StringRange> createdDates) {
        this.createdDates = createdDates;
    }

    public List<Integer> getCreationDecades() {
        return creationDecades;
    }

    public void setCreationDecades(List<Integer> creationDecades) {
        this.creationDecades = creationDecades;
    }

    public String getStartingLetter() {
        return startingLetter;
    }

    public void setStartingLetter(String startingLetter) {
        this.startingLetter = startingLetter;
    }

    public List<Resource> getProjects() {
        return sparseProjects;
    }

    public void setProjects(List<Resource> projects) {
        sparseProjects = projects;
    }

    public List<ResourceCollection> getCollections() {
        return sparseCollections;
    }

    public void setCollections(List<ResourceCollection> resourceCollections) {
        sparseCollections = resourceCollections;
    }

    public List<String> getContents() {
        return contents;
    }

    public void setContents(List<String> contents) {
        this.contents = contents;
    }

    public boolean isExplore() {
        return explore;
    }

    public void setExplore(boolean explore) {
        this.explore = explore;
    }

    public List<IntegratableOptions> getIntegratableOptions() {
        return integratableOptions;
    }

    public void setIntegratableOptions(List<IntegratableOptions> integratableOptions) {
        this.integratableOptions = integratableOptions;
    }

    public List<String> getFilenames() {
        return filenames;
    }

    public void setFilenames(List<String> filenames) {
        this.filenames = filenames;
    }

    public List<List<? extends Persistable>> getSparseLists() {
        List<List<? extends Persistable>> lists = new ArrayList<List<? extends Persistable>>();
        lists.add(sparseProjects);
        lists.add(sparseCollections);
        return lists;
    }

    public ResourceCreatorProxy getCreatorOwner() {
        return creatorOwner;
    }

    public void setCreatorOwner(ResourceCreatorProxy creatorOwner) {
        this.creatorOwner = creatorOwner;
    }

    public List<String> getUncontrolledMaterialKeywords() {
        return uncontrolledMaterialKeywords;
    }

    public void setUncontrolledMaterialKeywords(List<String> uncontrolledMaterialKeywords) {
        this.uncontrolledMaterialKeywords = uncontrolledMaterialKeywords;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

}
