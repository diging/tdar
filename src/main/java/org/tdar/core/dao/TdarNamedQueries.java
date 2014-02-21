package org.tdar.core.dao;

public interface TdarNamedQueries {
    public static final String QUERY_DELETE_INFORMATION_RESOURCE_FILE_DERIVATIVES = "informationResourceFileVersion.deleteDerivatives";
    public static final String QUERY_NUMBER_OF_MAPPED_DATA_VALUES_FOR_ONTOLOGY = "ontology.isMapped";
    public static final String QUERY_NUMBER_OF_MAPPED_DATA_VALUES_FOR_COLUMN = "ontology.isMappedToColumn";
    public static final String QUERY_READ_ONLY_FULLUSER_PROJECTS = "fullUser.projects";
    public static final String QUERY_FULLUSER_RESOURCES = "fullUser.resources";
    public static final String QUERY_FULLUSER_DATASET = "fullUser.datasets";
    public static final String QUERY_READUSER_PROJECTS = "readUser.projects";
    public static final String QUERY_SPARSE_RECENT_EDITS = "resources.recent";
    public static final String QUERY_CONTRIBUTORREQUEST_PENDING = "contributorRequest.pending";
    public static final String QUERY_BOOKMARKEDRESOURCE_IS_ALREADY_BOOKMARKED = "bookmarkedResource.isAlreadyBookmarked";
    public static final String QUERY_BOOKMARKEDRESOURCE_REMOVE_BOOKMARK = "bookmarkedResource.removeBookmark";
    public static final String QUERY_BOOKMARKEDRESOURCE_FIND_RESOURCE_BY_PERSON = "bookmarkedResource.findResourcesByPerson";
    public static final String QUERY_DATASET_CAN_LINK_TO_ONTOLOGY = "dataset.canLinkToOntology";
    public static final String QUERY_DATATABLE_RELATED_ID = "dataTable.relatedId";
    public static final String QUERY_DATATABLECOLUMN_WITH_DEFAULT_ONTOLOGY = "dataTableColumn.withDefaultOntology";
    public static final String QUERY_INFORMATIONRESOURCE_FIND_BY_FILENAME = "informationResource.findByFileName";
    public static final String QUERY_ONTOLOGYNODE_ALL_CHILDREN_WITH_WILDCARD = "ontologyNode.allChildrenWithIndexWildcard";
    public static final String QUERY_ONTOLOGYNODE_PARENT = "ontologyNode.parent";
    public static final String QUERY_ONTOLOGYNODE_ALL_CHILDREN = "ontologyNode.allChildren";
    public static final String QUERY_PROJECT_CODINGSHEETS = "project.codingSheets";
    public static final String QUERY_PROJECT_DATASETS = "project.datasets";
    public static final String QUERY_PROJECT_DOCUMENTS = "project.documents";
    public static final String QUERY_PROJECT_INDEPENDENTRESOURCES_PROJECTS = "project.independentResourcesProject";
    public static final String QUERY_PROJECT_EDITABLE = "project.editable";
    public static final String QUERY_PROJECT_VIEWABLE = "project.viewable";
    public static final String QUERY_FILE_STATUS = "file.with.statuses";
    public static final String QUERY_LOGIN_STATS = "admin.userLogin";
    public static final String QUERY_PROJECT_ALL_OTHER = "project.all.other";
    public static final String QUERY_RESOURCE_RESOURCETYPE = "resource.resourceType";
    public static final String QUERY_RESOURCE_MODIFIED_SINCE = "resource.modifiedSince";
    public static final String QUERY_SPARSE_RESOURCES = "all.resources";
    public static final String QUERY_SPARSE_RESOURCES_SUBMITTER = "submitter.resources.sparse";
    public static final String QUERY_RESOURCES_SUBMITTER = "submitter.resources";
    public static final String QUERY_PROJECT_COUNT_INTEGRATABLE_DATASETS = "project.countIntegratableDatasets";
    public static final String QUERY_PROJECTS_COUNT_INTEGRATABLE_DATASETS = "projects.countIntegratableDatasets";
    public static final String QUERY_READ_ONLY_EDITABLE_PROJECTS = "sparse.editableProject";
    public static final String QUERY_MANAGED_ISO_COUNTRIES = "resource.iso_code";
    public static final String QUERY_ACTIVE_RESOURCE_TYPE_COUNT = "resource.typeCounts";
    public static final String QUERY_RESOURCE_COUNT_BY_TYPE_AND_STATUS = "resource.countByTypeAndStatus";
    public static final String QUERY_USER_GET_ALL_RESOURCES_COUNT = "resource.active.count";
    public static final String QUERY_SPARSE_EMPTY_PROJECTS = "projects.empty";
    public static final String QUERY_RESOURCE_EDITABLE = "resource.editable";
    public static final String QUERY_SPARSE_PROJECTS = "project.all.sparse";
    public static final String QUERY_IS_ALLOWED_TO = "resource.isEditable";
    public static final String QUERY_IS_ALLOWED_TO_NEW = "resource.isEditable2";
    public static final String QUERY_IS_ALLOWED_TO_MANAGE = "resourcecollection.isEditable";
    public static final String QUERY_RESOURCE_COUNT_BY_TYPE_AND_STATUS_BY_USER = "dashboard.resourceByPerson";
    public static final String QUERY_COLLECTIONS_YOU_HAVE_ACCESS_TO = "rescol.accessible";
    public static final String QUERY_COLLECTIONS_YOU_HAVE_ACCESS_TO_WITH_NAME = "rescol.accessibleName";
    public static final String QUERY_SPARSE_EDITABLE_RESOURCES = "resource.editable.sparse";
    public static final String QUERY_SPARSE_EDITABLE_SORTED_RESOURCES = "resource.editable.sorted.sparse" ;
    public static final String QUERY_COLLECTION_BY_PARENT = "collection.parent";
    public static final String QUERY_COLLECTIONS_PUBLIC_ACTIVE = "collection.activeId";
    public static final String QUERY_COLLECTION_RESOURCES_WITH_STATUS = "collection.resourcesWithStatus";
    public static final String QUERY_COLLECTION_BY_AUTH_OWNER = "collection.authOwnerId_name";
    public static final String QUERY_COLLECTION_PUBLIC_WITH_HIDDEN_PARENT = "collection.hiddenParent";
    public static final String QUERY_EXTERNAL_ID_SYNC = "resource.externalId";
    public static final String QUERY_KEYWORD_COUNT_CULTURE_KEYWORD_CONTROLLED = "adminStats.cultureKeywordControlled";
    public static final String QUERY_KEYWORD_COUNT_CULTURE_KEYWORD_UNCONTROLLED = "adminStats.cultureKeywordUnControlled";
    public static final String QUERY_KEYWORD_COUNT_GEOGRAPHIC_KEYWORD = "adminStats.geographicKeyword";
    public static final String QUERY_KEYWORD_COUNT_INVESTIGATION_TYPE = "adminStats.investigationType";
    public static final String QUERY_KEYWORD_COUNT_MATERIAL_KEYWORD = "adminStats.materialKeyword";
    public static final String QUERY_KEYWORD_COUNT_OTHER_KEYWORD = "adminStats.otherKeyword";
    public static final String QUERY_KEYWORD_COUNT_SITE_NAME_KEYWORD = "adminStats.siteNameKeyword";
    public static final String QUERY_KEYWORD_COUNT_SITE_TYPE_KEYWORD_CONTROLLED = "adminStats.siteTypeKeywordControlled";
    public static final String QUERY_KEYWORD_COUNT_SITE_TYPE_KEYWORD_UNCONTROLLED = "adminStats.siteTypeKeywordUnControlled";
    public static final String QUERY_KEYWORD_COUNT_TEMPORAL_KEYWORD = "adminStats.temporalKeyword";
    public static final String QUERY_KEYWORD_COUNT_FILE_EXTENSION = "adminStats.fileExtensions";
    public static final String QUERY_RECENT_USERS_ADDED = "adminStats.recentUsers";
    public static final String QUERY_RECENT = "adminStats.recentFiles";
//    public static final String QUERY_MATCHING_FILES = "datasetRelated.Files";
    public static final String QUERY_USAGE_STATS = "adminStats.usage";
    public static final String QUERY_FILE_STATS = "adminStats.fileDetails";
    public static final String QUERY_MAPPED_CODING_RULES = "dataTableColumn.mappedCodingRules";
    public static final String QUERY_RESOURCES_IN_PROJECT = "resources.inProject";
    public static final String QUERY_RESOURCES_IN_PROJECT_WITH_STATUS = "project.informationResourcesWithStatus";
    public static final String QUERY_DASHBOARD = "dashboard.sql";
    public static final String QUERY_RESOURCES_BY_DECADE = "resources.byDecade";
    public static final String QUERY_SPARSE_RESOURCE_LOOKUP = "resource.sparseLookup";
    public static final String QUERY_SPARSE_COLLECTION_LOOKUP = "resourceCollection.sparseLookup";
    public static final String SPACE_BY_PROJECT = "admin.size.project";
    public static final String SPACE_BY_RESOURCE = "admin.size.resource";
    public static final String SPACE_BY_COLLECTION = "admin.size.collection";
    public static final String SPACE_BY_SUBMITTER = "admin.size.submitter";
    public static final String ACCESS_BY = "admin.access";
    public static final String ACCESS_BY_OVERALL = "admin.access.overall";
    public static final String DOWNLOAD_BY = "admin.download";
    public static final String LOGS_FOR_RESOURCE = "admin.logsforResource";
    public static final String FIND_ACTIVE_COUPON = "coupon.active";
    public static final String RESOURCE_ACCESS_HISTORY = "admin.accessHistory";
    public static final String FILE_DOWNLOAD_HISTORY = "admin.fileFileHistory";
    public static final String RESOURCES_WITH_NULL_ACCOUNT_ID = "account.resourceNull";
    public static final String ACCOUNT_QUOTA_INIT = "account.quota.init";
    public static final String RESOURCES_WITH_NON_MATCHING_ACCOUNT_ID = "account.resourceDifferent";
    public static final String ACCOUNT_GROUP_FOR_ACCOUNT = "account.group";
    public static final String ACCOUNTS_FOR_PERSON = "accounts.forPerson";
    public static final String ACCOUNT_GROUPS_FOR_PERSON = "accountgroups.forPerson";
    public static final String QUERY_INFORMATIONRESOURCES_WITH_FILES = "informationResources.withFiles";
    public static final String QUERY_SPARSE_ACTIVE_RESOURCES = "resources.active";
    public static final String INVOICES_FOR_PERSON = "invoices.forPerson";
    public static final String UNASSIGNED_INVOICES_FOR_PERSON = "invoices.unassignedForPerson";
    public static final String FIND_INVOICE_FOR_COUPON = "invoices.coupons";
    public static final String QUERY_SPARSE_CODING_SHEETS_USING_ONTOLOGY = "sparseCodingSheets.ontology";
    public static final String QUERY_FILE_SIZE_TOTAL ="file.total_size";
    public static final String QUERY_RELATED_RESOURCES = "resource.related";
    public static final String FIND_ACCOUNT_FOR_INVOICE = "account.forInvoice";

    public static final String COLLECTION_LIST_WITH_AUTHUSER = "collection.idlest.with.authuser";
    public static final String QUERY_SPARSE_EDITABLE_SORTED_RESOURCES_INHERITED = "query.sparse.editable.sorted.resources.inherited" ;
    public static final String QUERY_SPARSE_EDITABLE_SORTED_RESOURCES_INHERITED_SORTED = "query.sparse.editable.sorted.resources.inherited.sorted";


    // raw SQL/HQL queries

    public static final String QUERY_SQL_DASHBOARD =
            "select id, status, resource_type from resource " +
                    "where id in " +
                    "(select resource_id from collection_resource,collection, authorized_user " +
                    "where collection.id=collection_resource.collection_id and collection.id=authorized_user.resource_collection_id " +
                    "and user_id=:submitterId and general_permission_int > :effectivePermissions " +
                    "union select id from resource where updater_id=:submitterId or submitter_id=:submitterId)";
    public static final String QUERY_SQL_COUNT = "SELECT COUNT(*) FROM %1$s";
    public static final String QUERY_FIND_ALL_WITH_IDS = "FROM %s WHERE id in (:ids)";
    public static final String QUERY_FIND_ALL_WITH_STATUS = "FROM %s WHERE status in (:statuses)";
    public static final String QUERY_SQL_COUNT_ACTIVE_RESOURCE = "SELECT COUNT(*) FROM %1$s where status='ACTIVE' and resourceType='%2$s' ";
    public static final String QUERY_SQL_COUNT_ACTIVE_RESOURCE_WITH_FILES = "select count(distinct  resource.id) from  resource, information_resource_file where  resource.status='ACTIVE' and resource.resource_type='%1$s' and resource.id=information_resource_id";
    public static final String QUERY_SQL_RESOURCE_INCREMENT_USAGE = "update Resource r set r.accessCounter=accessCounter+1 where r.id=:resourceId";
    public static final String QUERY_SQL_RAW_RESOURCE_STAT_LOOKUP = "select rt.* , "
            + "(select count(*) from resource where resource_type = rt.resource_type and status = 'ACTIVE') as all,  "
            + "(select count(*) from resource where resource_type = rt.resource_type and status = 'DRAFT') as draft, "
            + "(select count(distinct information_resource_id) from information_resource_file irf join resource rr on (rr.id = irf.information_resource_id) where rr.resource_type = rt.resource_type and rr.status = 'ACTIVE') as with_files, "
            + "(select count(distinct information_resource_id) from information_resource_file irf join resource rr on (rr.id = irf.information_resource_id) where rr.resource_type = rt.resource_type and rr.status = 'ACTIVE' and irf.restriction = 'CONFIDENTIAL') as with_conf "
            + "from (select distinct resource_type from resource) as rt";
    // generated HQL formats
    public static final String QUERY_CREATOR_MERGE_ID = "select merge_creator_id from %1$s where id=%2$s";

    public static final String QUERY_KEYWORD_MERGE_ID = "select merge_keyword_id from %1$s where id=%2$s";

    // e.g."from Resource r1 where exists (from Resource r2 inner join r2.cultureKeywords ck where r2.id = r1.id and ck.id in (:idlist))"
    public static final String QUERY_HQL_MANY_TO_MANY_REFERENCES =
            "from %1$s r1 where exists (from %1$s r2 inner join r2.%2$s ck where r2.id = r1.id and ck.id in (:idlist))";
    // e.g. "from Resource r1 where submitter_id in (:idlist)"
    public static final String QUERY_HQL_MANY_TO_ONE_REFERENCES =
            "from %1$s r1 where %2$s.id in (:idlist)";

    public static final String QUERY_HQL_COUNT_MANY_TO_MANY_REFERENCES =
            "select count(*) as referenceCount from %1$s r1 where exists (from %1$s r2 inner join r2.%2$s ck where r2.id = r1.id and ck.id in (:idlist))";
    public static final String QUERY_HQL_COUNT_MANY_TO_ONE_REFERENCES =
            "select count(*) as referenceCount from %1$s r1 where %2$s.id in (:idlist)";

    public static final String QUERY_HQL_COUNT_MANY_TO_MANY_REFERENCES_MAP =
            "select new map(ck.id as id, count(*) as referenceCount) from %1$s r2 inner join r2.%2$s ck where ck.id in (:idlist) group by ck.id";
    public static final String QUERY_HQL_COUNT_MANY_TO_ONE_REFERENCES_MAP =
            "select new map(%2$s.id as id, count(*) as referenceCount) from %1$s r1 where %2$s.id in (:idlist) group by %2$s.id";

    public static final String QUERY_HQL_UPDATE_MANY_TO_MANY_REFERENCES = ""; // TODO: //Not possible, I think.
    public static final String QUERY_HQL_UPDATE_MANY_TO_ONE_REFERENCES = ""; // TODO: use many_to_one_count in exists clause.

    public static final String HQL_EDITABLE_RESOURCE_SUFFIX = "FROM Resource as res  where " +
            " (TRUE=:allResourceTypes or res.resourceType in (:resourceTypes)) and (TRUE=:allStatuses or res.status in (:statuses) )  AND " +
            "(res.submitter.id=:userId or exists (" +
            " from ResourceCollection rescol join rescol.authorizedUsers  as authUser " +
            " join rescol.resources as colres " +
            " where " +
            " colres.id = res.id and " +
            "(TRUE=:admin or authUser.user.id=:userId and authUser.effectiveGeneralPermission > :effectivePermission))) ";
    public static final String HQL_EDITABLE_RESOURCE_SORTED_SUFFIX = HQL_EDITABLE_RESOURCE_SUFFIX + " order by res.title, res.id";
    public static final String QUERY_CLEAR_REFERENCED_ONTOLOGYNODE_RULES = "update.clearOntologyNodeReferences";
    public static final String UPDATE_DATATABLECOLUMN_ONTOLOGIES = "update.dataTableColumnOntologies";
    public static final String QUERY_ACCOUNTS_FOR_RESOURCES = "select id, account_id from resource res where res.id in (%s) ";
    public static final String QUERY_SQL_RESOURCES_BY_YEAR = "select date_part('year', date_registered), count(id) from resource where status='ACTIVE' and date_registered is not null group by date_part('year', date_registered)  order by date_part('year', date_registered)  asc";
    public static final String DISTINCT_SUBMITTERS = "SELECT DISTINCT submitter_id from resource";

    public static final String UPDATE_KEYWORD_OCCURRENCE_CLEAR_COUNT= "update %1$s set occurrence=0";
    public static final String UPDATE_KEYWORD_OCCURRENCE_COUNT_INHERITANCE = "update %1$s set occurrence = occurrence + coalesce((select count(resource_id) from resource_%1$s where %1$s_id =%1$s.id and resource_id in (select project_id from information_resource where %2$s is true) group by %1$s_id),0)";
    public static final String UPDATE_KEYWORD_OCCURRENCE_COUNT= "update %1$s set occurrence =  occurrence + coalesce((select count(resource_id) from resource_%1$s where %1$s_id =%1$s.id group by %1$s_id),0)";
    public static final String UPDATE_CREATOR_OCCURRENCE_CLEAR_COUNT = "update creator set occurrence=0";
    public static final String UPDATE_CREATOR_OCCURRENCE_RESOURCE_INFORMATION_RESOURCE_PUBLISHER = "update creator set occurrence=occurrence + coalesce((select count(information_resource.id) from information_resource where publisher_id=creator.id group by publisher_id),0)";
    public static final String UPDATE_CREATOR_OCCURRENCE_RESOURCE_INFORMATION_RESOURCE_PROVIDER = "update creator set occurrence=occurrence + coalesce((select count(information_resource.id) from information_resource where provider_institution_id=creator.id group by provider_institution_id),0)";
    public static final String UPDATE_CREATOR_OCCURRENCE_RESOURCE_INFORMATION_RESOURCE_COPYRIGHT = "update creator set occurrence=occurrence + coalesce((select count(information_resource.id) from information_resource where copyright_holder_id=creator.id group by copyright_holder_id),0) ";
    public static final String UPDATE_CREATOR_OCCURRENCE_RESOURCE_SUBMITTER = "update creator set occurrence=occurrence + coalesce((select count(resource.id) from resource where submitter_id=creator.id group by submitter_id),0)";
    public static final String UPDATE_CREATOR_OCCURRENCE_RESOURCE = "update creator set occurrence = occurrence+ coalesce((select count(resource_id) from resource_creator where creator_id=creator.id group by creator_id),0) ";
    public static final String DATASETS_USING_NODES = "select id from resource where id in (select dataset_id from data_table where data_table.id in (select data_table_id from data_table_column, coding_rule, coding_sheet where data_table_column.default_coding_sheet_id=coding_sheet_id and coding_rule.coding_sheet_id=coding_sheet.id and  ontology_node_id=%s)) and status = 'ACTIVE'";
    public static final String HQL_DELETE_INFORMATION_RESOURCE_FILE_VERSION_IMMEDIATELY = "delete InformationResourceFileVersion where id = :id";
    
    public static final String SELECT_RAW_IMAGE_SITEMAP_FILES = "select r.id as resourceId, r.title, r.description as resourceDescription, r.resource_type, irf.description, irfv.id from resource r, information_resource ir, information_resource_file irf, "
            + "information_resource_file_version irfv where r.id=ir.id and ir.id=irf.information_resource_id and "
            + "irf.id=irfv.information_resource_file_id and internal_type='WEB_SMALL' and resource_type in ('IMAGE','SENSORY_DATA','GEOSPATIAL') "
            + "and restriction='PUBLIC' and r.status='ACTIVE'";

    public static final String QUERY_SPARSE_COLLECTION_RESOURCES = "query.sparse.collection.resources";
}
