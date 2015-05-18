package org.tdar.oai.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.Transient;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.queryParser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tdar.core.bean.Indexable;
import org.tdar.core.bean.OaiDcProvider;
import org.tdar.core.bean.Obfuscatable;
import org.tdar.core.bean.Viewable;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.collection.ResourceCollection.CollectionType;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.GenericDao;
import org.tdar.core.exception.OAIException;
import org.tdar.core.exception.OaiErrorCode;
import org.tdar.core.exception.SearchPaginationException;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.service.ObfuscationService;
import org.tdar.core.service.search.SearchService;
import org.tdar.oai.bean.Description;
import org.tdar.oai.bean.generated.DeletedRecordType;
import org.tdar.oai.bean.generated.DescriptionType;
import org.tdar.oai.bean.generated.GetRecordType;
import org.tdar.oai.bean.generated.GranularityType;
import org.tdar.oai.bean.generated.HeaderType;
import org.tdar.oai.bean.generated.IdentifyType;
import org.tdar.oai.bean.generated.ListIdentifiersType;
import org.tdar.oai.bean.generated.ListMetadataFormatsType;
import org.tdar.oai.bean.generated.ListRecordsType;
import org.tdar.oai.bean.generated.ListResponse;
import org.tdar.oai.bean.generated.ListSetsType;
import org.tdar.oai.bean.generated.MetadataFormatType;
import org.tdar.oai.bean.generated.MetadataType;
import org.tdar.oai.bean.generated.RecordType;
import org.tdar.oai.bean.generated.ResumptionTokenType;
import org.tdar.oai.bean.generated.SetType;
import org.tdar.search.query.QueryFieldNames;
import org.tdar.search.query.SearchResult;
import org.tdar.search.query.SearchResultHandler.ProjectionModel;
import org.tdar.search.query.SortOption;
import org.tdar.search.query.builder.InstitutionQueryBuilder;
import org.tdar.search.query.builder.PersonQueryBuilder;
import org.tdar.search.query.builder.QueryBuilder;
import org.tdar.search.query.builder.ResourceCollectionQueryBuilder;
import org.tdar.search.query.builder.ResourceQueryBuilder;
import org.tdar.search.query.part.FieldQueryPart;
import org.tdar.search.query.part.RangeQueryPart;
import org.tdar.struts.data.DateRange;
import org.tdar.struts.data.oai.OAIMetadataFormat;
import org.tdar.struts.data.oai.OAIRecordType;
import org.tdar.struts.data.oai.OAIResumptionToken;
import org.tdar.transform.DcTransformer;
import org.tdar.transform.ModsTransformer;
import org.tdar.utils.MessageHelper;

import com.google.common.base.Objects;

@Service
public class OaiPmhService {

    TdarConfiguration config = TdarConfiguration.getInstance();

    private boolean enableEntities = config.getEnableEntityOai();

    @Autowired
    private GenericDao genericDao;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ObfuscationService obfuscationService;

    @Transient
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    public IdentifyType getIdentifyResponse() {
        IdentifyType ident = new IdentifyType();
        ident.setBaseURL(config.getBaseUrl() + "/oai-pmh/oai");
        ident.setDeletedRecord(DeletedRecordType.NO);
        ident.getAdminEmail().add(config.getSystemAdminEmail());
        ident.setEarliestDatestamp("2008-01-01");
        ident.setGranularity(GranularityType.YYYY_MM_DD);
        ident.setProtocolVersion("2.0");
        ident.setRepositoryName(config.getRepositoryName());
        DescriptionType descr = new DescriptionType();
        descr.setAny(new Description(config.getSystemDescription()));
        ident.getDescription().add(descr);
        return ident;
    }

    public ResumptionTokenType listIdentifiersOrRecords(Date from, Date until, OAIMetadataFormat metadataPrefix, OAIResumptionToken resumptionToken,
            ListResponse response)
            throws OAIException, ParseException {
        ResumptionTokenType token = null;
        Long collectionId = null;
        // start record number (cursor)
        SearchResult search = new SearchResult();
        search.setSortField(SortOption.DATE_UPDATED);
        Date effectiveFrom = from;
        Date effectiveUntil = until;
        OAIMetadataFormat metadataFormat = metadataPrefix;
        if (resumptionToken != null) {
            // ... then this is the second or subsequent page of results.
            // In this case there are no separate "from" and "until" parameters passed by the client;
            // instead all the parameters come packed into a resumption token.
            collectionId = resumptionToken.getSet();
            search.setStartRecord(resumptionToken.getCursor());
            effectiveFrom = resumptionToken.getEffectiveFrom(from);
            effectiveUntil = resumptionToken.getEffectiveUntil(until);
            metadataFormat = resumptionToken.getEffectiveMetadataPrefix(metadataPrefix);
        }

        // now actually build the queries and execute them
        ResourceQueryBuilder resourceQueryBuilder = new ResourceQueryBuilder();
        resourceQueryBuilder.append(new FieldQueryPart<>("status", Status.ACTIVE));
        if (collectionId != null) {
            resourceQueryBuilder.append(new FieldQueryPart<Long>(QueryFieldNames.RESOURCE_COLLECTION_SHARED_IDS, collectionId));
        }
        QueryBuilder personQueryBuilder = new PersonQueryBuilder();
        personQueryBuilder.append(new FieldQueryPart<>("status", Status.ACTIVE));
        InstitutionQueryBuilder institutionQueryBuilder = new InstitutionQueryBuilder();
        institutionQueryBuilder.append(new FieldQueryPart<>("status", Status.ACTIVE));

        int totalPersons = 0;
        int totalInstitutions = 0;
        int totalResources = 0;

        if (enableEntities && !Objects.equal(metadataFormat, OAIMetadataFormat.MODS)) {
            // list people
            totalPersons = populateResult(OAIRecordType.PERSON, personQueryBuilder, metadataFormat, effectiveFrom, effectiveUntil, search, response);
            totalInstitutions = populateResult(OAIRecordType.INSTITUTION, institutionQueryBuilder, metadataFormat, effectiveFrom,
                    effectiveUntil, search, response);
        }

        // list the resources
        totalResources = populateResult(OAIRecordType.RESOURCE, resourceQueryBuilder, metadataFormat, effectiveFrom, effectiveUntil, search, response);

        // if any of the queries returned more than a page of search results, create a resumptionToken to allow
        // the client to continue harvesting from that point
        if ((totalResources > search.getRecordsPerPage()) || (totalPersons > search.getRecordsPerPage()) || (totalInstitutions > search.getRecordsPerPage())) {
            // ... then this is a partial response, and should be terminated with a ResumptionToken
            // which may be empty if this is the last page of results
            // advance the cursor by one page
            int cursor = search.getNextPageStartRecord();
            // check if there would be any resources, persons or institutions in that hypothetical next page
            if ((totalResources > cursor) || (totalPersons > cursor) || (totalInstitutions > cursor)) {
                OAIResumptionToken newResumptionToken = new OAIResumptionToken();
                // ... populate the resumptionToken so the harvester can continue harvesting from the next page
                token = new ResumptionTokenType();
                newResumptionToken = new OAIResumptionToken(cursor, effectiveFrom, effectiveUntil, metadataFormat, collectionId);
                token.setValue(newResumptionToken.getToken());
                if (response instanceof ListIdentifiersType) {
                    response.setResumptionToken(token);
                }
            }
        }

        // if there were no records found, then throw an exception
        if ((totalResources + totalPersons + totalInstitutions) == 0) {
            throw new OAIException(MessageHelper.getInstance().getText("oaiController.no_matches"), OaiErrorCode.NO_RECORDS_MATCH);
        }
        return token;
    }

    private int populateResult(OAIRecordType recordType, QueryBuilder queryBuilder, OAIMetadataFormat metadataFormat, Date effectiveFrom,
            Date effectiveUntil, SearchResult search, ListResponse response) throws ParseException {
        boolean includeRecords = false;
        if (response instanceof ListRecordsType) {
            includeRecords = true;
        }

        List<RecordType> records = new ArrayList<>();
        queryBuilder.append(new RangeQueryPart<Date>(QueryFieldNames.DATE_UPDATED, new DateRange(effectiveFrom, effectiveUntil)));
        int total = 0;
        try {
            switchProjectionModel(search, queryBuilder);
            searchService.handleSearch(queryBuilder, search, MessageHelper.getInstance());
            total = search.getTotalRecords();
            for (Indexable i : search.getResults()) {
                if ((i instanceof Viewable) && !((Viewable) i).isViewable()) {
                    continue;
                }
                OaiDcProvider resource = (OaiDcProvider) i;
                // create OAI metadata for the record
                records.add(createRecord(resource, recordType, metadataFormat, includeRecords));
            }
        } catch (SearchPaginationException spe) {
            logger.debug("an pagination exception happened .. {} ", spe.getMessage());
        } catch (TdarRecoverableRuntimeException e) {
            // this is expected as the cursor follows the "max" results for person/inst/resource so, whatever the max is
            // means that the others will throw this error.
            logger.debug("an exception happened .. {} ", e);
        }

        if (response instanceof ListRecordsType) {
            ((ListRecordsType) response).getRecord().addAll(records);
        }

        if (response instanceof ListIdentifiersType) {
            ListIdentifiersType listIdentifiersType = (ListIdentifiersType) response;
            for (RecordType record : records) {
                listIdentifiersType.getHeader().add(record.getHeader());
            }
        }

        return total;
    }

    protected RecordType createRecord(OaiDcProvider resource, OAIRecordType recordType, OAIMetadataFormat metadataFormat, boolean includeRecords) {
        RecordType record = new RecordType();
        HeaderType header = new HeaderType();

        obfuscationService.obfuscate((Obfuscatable) resource, null);
        header.setIdentifier(new OaiIdentifier(recordType, resource.getId()).getOaiId());
        Date updatedDate = resource.getDateUpdated();
        if (updatedDate == null) {
            updatedDate = resource.getDateCreated();
        }
        header.setDatestamp(updatedDate.toString());
        // iso_utc
        record.setHeader(header);
        if (resource instanceof Resource) {
            for (Long id : ((Resource) resource).getSharedCollectionsContaining()) {
                header.getSetSpec().add(Long.toString(id));
            }
        }
        if (includeRecords) {
            MetadataType metadata = new MetadataType();
            switch (metadataFormat) {
                case DC:
                    metadata.setAny(DcTransformer.transformAny((Resource) resource));
                    break;
                case MODS:
                    metadata.setAny(ModsTransformer.transformAny((Resource) resource));
                    break;
                case TDAR:
                    metadata.setAny(resource);
                    break;
            }
            record.setMetadata(metadata);
        }
        return record;
    }

    public ListMetadataFormatsType listMetadataFormats(OaiIdentifier identifier) throws OAIException {
        ListMetadataFormatsType formats = new ListMetadataFormatsType();

        OAIMetadataFormat[] formatList = OAIMetadataFormat.values();
        if (identifier != null) {
            // an identifier was specified, so return a list of the metadata formats which that record can be disseminated in
            formatList = identifier.getRecordType().getMetadataFormats();
        }
        if (!config.enableTdarFormatInOAI()) {
            ArrayUtils.removeElement(formatList, OAIMetadataFormat.TDAR);
        }
        for (OAIMetadataFormat f : formatList) {
            MetadataFormatType type = new MetadataFormatType();
            type.setMetadataNamespace(f.getNamespace());
            type.setMetadataPrefix(f.getPrefix());
            type.setSchema(f.getSchemaLocation());
            formats.getMetadataFormat().add(type);
        }

        // TODO Auto-generated method stub
        return formats;
    }

    public GetRecordType getGetRecordResponse(OaiIdentifier identifier, OAIMetadataFormat requestedFormat) throws OAIException {
        // check that this kind of record can be disseminated in the requested format
        GetRecordType get = new GetRecordType();
        OAIRecordType type = identifier.getRecordType();
        type.checkCanDisseminateFormat(requestedFormat);

        Long id = identifier.getTdarId();
        OaiDcProvider oaiDcObject = (OaiDcProvider) genericDao.find(type.getActualClass(), id);
        RecordType record = createRecord(oaiDcObject, identifier.getRecordType(), requestedFormat, true);
        get.setRecord(record);

        return get;
    }

    private void switchProjectionModel(SearchResult search, QueryBuilder queryBuilder) {
        search.setProjectionModel(ProjectionModel.HIBERNATE_DEFAULT);
        if (queryBuilder instanceof ResourceQueryBuilder) {
            search.setProjectionModel(ProjectionModel.RESOURCE_PROXY);
        }
    }

    public ListRecordsType listRecords(Date from, Date until, OAIMetadataFormat requestedFormat, OAIResumptionToken resumptionToken) throws OAIException,
            ParseException {
        ListRecordsType response = new ListRecordsType();
        ResumptionTokenType token = listIdentifiersOrRecords(from, until, requestedFormat, resumptionToken, response);
        response.setResumptionToken(token);
        return response;
    }

    public ListIdentifiersType listIdentifiers(Date from, Date until, OAIMetadataFormat requestedFormat, OAIResumptionToken resumptionToken)
            throws OAIException, ParseException {
        ListIdentifiersType response = new ListIdentifiersType();
        ResumptionTokenType token = listIdentifiersOrRecords(from, until, requestedFormat, resumptionToken, response);
        response.setResumptionToken(token);
        return response;
    }

    public ListSetsType listSets(Date from, Date until, OAIMetadataFormat requestedFormat, OAIResumptionToken resumptionToken) throws OAIException {
        // Sort results by dateUpdated ascending and filter
        // by dates, either supplied in OAI-PMH 'from' and 'to' parameters,
        // or encoded as parts of the 'resumptionToken'
        // Optionally filter results by date range
        // In Lucene, dates are stored as "yyyyMMddHHmmssSSS" in UTC time zone
        // see http://docs.jboss.org/hibernate/search/3.4/reference/en-US/html_single/#d0e3510
        // but in OAI-PMH, date parameters are ISO8601 dates, so we must remove the punctuation.
        SearchResult search = new SearchResult();
        ListSetsType response = new ListSetsType();
        Date effectiveFrom = from;
        Date effectiveUntil = until;
        if (resumptionToken != null) {
            // ... then this is the second or subsequent page of results.
            // In this case there are no separate "from" and "until" parameters passed by the client;
            // instead all the parameters come packed into a resumption token.
            search.setStartRecord(resumptionToken.getCursor());
            effectiveFrom = resumptionToken.getEffectiveFrom(from);
            effectiveUntil = resumptionToken.getEffectiveUntil(until);
        }
        OAIMetadataFormat metadataFormat = resumptionToken.getEffectiveMetadataPrefix(requestedFormat);

        search.setSortField(SortOption.DATE_UPDATED);

        // now actually build the queries and execute them
        ResourceCollectionQueryBuilder collectionQueryBuilder = new ResourceCollectionQueryBuilder();
        collectionQueryBuilder.append(new FieldQueryPart<>(QueryFieldNames.COLLECTION_TYPE, CollectionType.SHARED));

        collectionQueryBuilder.append(new RangeQueryPart<Date>(QueryFieldNames.DATE_UPDATED, new DateRange(effectiveFrom, effectiveUntil)));
        int total = 0;
        Collection<SetType> setList = new ArrayList<>();
        try {
            switchProjectionModel(search, collectionQueryBuilder);
            searchService.handleSearch(collectionQueryBuilder, search, MessageHelper.getInstance());
            total = search.getTotalRecords();
            List<Long> ids = new ArrayList<>();
            for (Indexable i : search.getResults()) {
                logger.debug("{}, {}", i, ((Viewable) i).isViewable());
                if ((i instanceof Viewable) && !((Viewable) i).isViewable()) {
                    continue;
                }
                SetType set = new SetType();
                ResourceCollection coll = (ResourceCollection) i;
                set.setSetName(coll.getName());
                DescriptionType descr = new DescriptionType();
                descr.setAny(new Description(coll.getDescription()));
                set.getSetDescription().add(descr);
                set.setSetSpec(Long.toString(coll.getId()));
                setList.add(set);
            }
            logger.info("ALL IDS: {}", ids);
        } catch (SearchPaginationException spe) {
            logger.debug("an pagination exception happened .. {} ", spe.getMessage());
        } catch (TdarRecoverableRuntimeException e) {
            // this is expected as the cursor follows the "max" results for person/inst/resource so, whatever the max is
            // means that the others will throw this error.
            logger.debug("an exception happened .. {} ", e);
        } catch (ParseException e) {
            logger.debug("an exception happened .. {} ", e);
        }

        // if any of the queries returned more than a page of search results, create a resumptionToken to allow
        // the client to continue harvesting from that point
        int recordsPerPage = search.getRecordsPerPage();
        if (total > recordsPerPage) {
            // ... then this is a partial response, and should be terminated with a ResumptionToken
            // which may be empty if this is the last page of results
            // advance the cursor by one page
            int cursor = search.getNextPageStartRecord();
            // check if there would be any resources, persons or institutions in that hypothetical next page

            // check if there would be any resources, persons or institutions in that hypothetical next page
            if (total > cursor) {
                // ... populate the resumptionToken so the harvester can continue harvesting from the next page
                OAIResumptionToken newResumptionToken = new OAIResumptionToken();
                ResumptionTokenType token = new ResumptionTokenType();
                newResumptionToken = new OAIResumptionToken(cursor, effectiveFrom, effectiveUntil, metadataFormat, null);
                token.setValue(newResumptionToken.getToken());
                response.setResumptionToken(token);
            }
        }
        response.getSet().addAll(setList);
        return response;
    }

}
