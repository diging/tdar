package org.tdar.core.dao;


public interface TdarNamedQueries {
    /**
     * constants to map between the Annotation Keys for HQL queries and the queries in the DAOs
     */
    String QUERY_CURRENT_USER_NOTIFICATIONS = "userNotifications.current";
    String QUERY_USER_NOTIFICATIONS_BY_TYPE = "userNotifications.byType";
    String QUERY_DELETE_INFORMATION_RESOURCE_FILE_DERIVATIVES = "informationResourceFileVersion.deleteDerivatives";
    String QUERY_NUMBER_OF_MAPPED_DATA_VALUES_FOR_ONTOLOGY = "ontology.isMapped";
    String QUERY_NUMBER_OF_MAPPED_DATA_VALUES_FOR_COLUMN = "ontology.isMappedToColumn";
    String QUERY_READ_ONLY_FULLUSER_PROJECTS = "fullUser.projects";
    String QUERY_FULLUSER_RESOURCES = "fullUser.resources";
    String QUERY_FULLUSER_DATASET = "fullUser.datasets";
    String QUERY_READUSER_PROJECTS = "readUser.projects";
    String QUERY_SPARSE_RECENT_EDITS = "resources.recent";
    String QUERY_CONTRIBUTORREQUEST_PENDING = "contributorRequest.pending";
    String QUERY_BOOKMARKEDRESOURCE_IS_ALREADY_BOOKMARKED = "bookmarkedResource.isAlreadyBookmarked";
    String QUERY_BOOKMARKEDRESOURCE_REMOVE_BOOKMARK = "bookmarkedResource.removeBookmark";
    String QUERY_BOOKMARKEDRESOURCE_FIND_RESOURCE_BY_PERSON = "bookmarkedResource.findResourcesByPerson";
    String QUERY_DATASET_CAN_LINK_TO_ONTOLOGY = "dataset.canLinkToOntology";
    String QUERY_DATATABLE_RELATED_ID = "dataTable.relatedId";
    String QUERY_DATATABLECOLUMN_WITH_DEFAULT_ONTOLOGY = "dataTableColumn.withDefaultOntology";
    String QUERY_INFORMATIONRESOURCE_FIND_BY_FILENAME = "informationResource.findByFileName";
    String QUERY_ONTOLOGYNODE_ALL_CHILDREN_WITH_WILDCARD = "ontologyNode.allChildrenWithIndexWildcard";
    String QUERY_ONTOLOGYNODE_PARENT = "ontologyNode.parent";
    String QUERY_ONTOLOGYNODE_ALL_CHILDREN = "ontologyNode.allChildren";
    String QUERY_PROJECT_CODINGSHEETS = "project.codingSheets";
    String QUERY_PROJECT_DATASETS = "project.datasets";
    String QUERY_PROJECT_DOCUMENTS = "project.documents";
    String QUERY_PROJECT_INDEPENDENTRESOURCES_PROJECTS = "project.independentResourcesProject";
    String QUERY_PROJECT_EDITABLE = "project.editable";
    String QUERY_PROJECT_VIEWABLE = "project.viewable";
    String QUERY_FILE_STATUS = "file.with.statuses";
    String QUERY_RESOURCE_FILE_STATUS = "resources.with.file.status";
    String QUERY_LOGIN_STATS = "admin.userLogin";
    String QUERY_PROJECT_ALL_OTHER = "project.all.other";
    String QUERY_RESOURCE_RESOURCETYPE = "resource.resourceType";
    String QUERY_RESOURCE_MODIFIED_SINCE = "resource.modifiedSince";
    String QUERY_SPARSE_RESOURCES = "all.resources";
    String QUERY_SPARSE_RESOURCES_SUBMITTER = "submitter.resources.sparse";
    String QUERY_RESOURCES_SUBMITTER = "submitter.resources";
    String QUERY_PROJECT_COUNT_INTEGRATABLE_DATASETS = "project.countIntegratableDatasets";
    String QUERY_PROJECTS_COUNT_INTEGRATABLE_DATASETS = "projects.countIntegratableDatasets";
    String QUERY_READ_ONLY_EDITABLE_PROJECTS = "sparse.editableProject";
    String QUERY_MANAGED_ISO_COUNTRIES = "resource.iso_code";
    String QUERY_ACTIVE_RESOURCE_TYPE_COUNT = "resource.typeCounts";
    String QUERY_RESOURCE_COUNT_BY_TYPE_AND_STATUS = "resource.countByTypeAndStatus";
    String QUERY_USER_GET_ALL_RESOURCES_COUNT = "resource.active.count";
    String QUERY_SPARSE_EMPTY_PROJECTS = "projects.empty";
    String QUERY_RESOURCE_EDITABLE = "resource.editable";
    String QUERY_SPARSE_PROJECTS = "project.all.sparse";
    String QUERY_IS_ALLOWED_TO = "resource.isEditable";
    String QUERY_IS_ALLOWED_TO_NEW = "resource.isEditable2";
    String QUERY_IS_ALLOWED_TO_MANAGE = "resourcecollection.isEditable";
    String QUERY_RESOURCE_COUNT_BY_TYPE_AND_STATUS_BY_USER = "dashboard.resourceByPerson";
    String QUERY_COLLECTIONS_YOU_HAVE_ACCESS_TO = "rescol.accessible";
    String QUERY_COLLECTIONS_YOU_HAVE_ACCESS_TO_WITH_NAME = "rescol.accessibleName";
    String QUERY_SPARSE_EDITABLE_RESOURCES = "resource.editable.sparse";
    String QUERY_EDITABLE_RESOURCES = "resource.editable";
    String QUERY_SPARSE_EDITABLE_SORTED_RESOURCES = "resource.editable.sorted.sparse";
    String QUERY_COLLECTION_BY_PARENT = "collection.parent";
    String QUERY_COLLECTIONS_PUBLIC_ACTIVE = "collection.activeId";
    String QUERY_COLLECTION_RESOURCES_WITH_STATUS = "collection.resourcesWithStatus";
    String QUERY_COLLECTION_BY_AUTH_OWNER = "collection.authOwnerId_name";
    String QUERY_COLLECTION_PUBLIC_WITH_HIDDEN_PARENT = "collection.hiddenParent";
    String QUERY_EXTERNAL_ID_SYNC = "resource.externalId";
    String QUERY_KEYWORD_COUNT_CULTURE_KEYWORD_CONTROLLED = "adminStats.cultureKeywordControlled";
    String QUERY_KEYWORD_COUNT_CULTURE_KEYWORD_UNCONTROLLED = "adminStats.cultureKeywordUnControlled";
    String QUERY_KEYWORD_COUNT_GEOGRAPHIC_KEYWORD = "adminStats.geographicKeyword";
    String QUERY_KEYWORD_COUNT_INVESTIGATION_TYPE = "adminStats.investigationType";
    String QUERY_KEYWORD_COUNT_MATERIAL_KEYWORD = "adminStats.materialKeyword";
    String QUERY_KEYWORD_COUNT_OTHER_KEYWORD = "adminStats.otherKeyword";
    String QUERY_KEYWORD_COUNT_SITE_NAME_KEYWORD = "adminStats.siteNameKeyword";
    String QUERY_KEYWORD_COUNT_SITE_TYPE_KEYWORD_CONTROLLED = "adminStats.siteTypeKeywordControlled";
    String QUERY_KEYWORD_COUNT_SITE_TYPE_KEYWORD_UNCONTROLLED = "adminStats.siteTypeKeywordUnControlled";
    String QUERY_KEYWORD_COUNT_TEMPORAL_KEYWORD = "adminStats.temporalKeyword";
    String QUERY_KEYWORD_COUNT_FILE_EXTENSION = "adminStats.fileExtensions";
    String QUERY_RECENT_USERS_ADDED = "adminStats.recentUsers";
    String QUERY_RECENT = "adminStats.recentFiles";
    // String QUERY_MATCHING_FILES = "datasetRelated.Files";
    String QUERY_USAGE_STATS = "adminStats.usage";
    String QUERY_FILE_STATS = "adminStats.fileDetails";
    String QUERY_MAPPED_CODING_RULES = "dataTableColumn.mappedCodingRules";
    String QUERY_RESOURCES_IN_PROJECT = "resources.inProject";
    String QUERY_RESOURCES_IN_PROJECT_WITH_STATUS = "project.informationResourcesWithStatus";
    String QUERY_DASHBOARD = "dashboard.sql";
    String QUERY_RESOURCES_BY_DECADE = "resources.byDecade";
    String QUERY_SPARSE_RESOURCE_LOOKUP = "resource.sparseLookup";
    String QUERY_SPARSE_COLLECTION_LOOKUP = "resourceCollection.sparseLookup";
    String SPACE_BY_PROJECT = "admin.size.project";
    String SPACE_BY_RESOURCE = "admin.size.resource";
    String SPACE_BY_COLLECTION = "admin.size.collection";
    String SPACE_BY_SUBMITTER = "admin.size.submitter";
    String ACCESS_BY = "admin.access";
    String ACCESS_BY_OVERALL = "admin.access.overall";
    String DOWNLOAD_BY = "admin.download";
    String LOGS_FOR_RESOURCE = "admin.logsforResource";
    String FIND_ACTIVE_COUPON = "coupon.active";
    String RESOURCE_ACCESS_HISTORY = "admin.accessHistory";
    String FILE_DOWNLOAD_HISTORY = "admin.fileFileHistory";
    String RESOURCES_WITH_NULL_ACCOUNT_ID = "account.resourceNull";
    String ACCOUNT_QUOTA_INIT = "account.quota.init";
    String RESOURCES_WITH_NON_MATCHING_ACCOUNT_ID = "account.resourceDifferent";
    String ACCOUNT_GROUP_FOR_ACCOUNT = "account.group";
    String ACCOUNTS_FOR_PERSON = "accounts.forPerson";
    String ACCOUNT_GROUPS_FOR_PERSON = "accountgroups.forPerson";
    String QUERY_INFORMATIONRESOURCES_WITH_FILES = "informationResources.withFiles";
    String QUERY_SPARSE_ACTIVE_RESOURCES = "resources.active";
    String INVOICES_FOR_PERSON = "invoices.forPerson";
    String UNASSIGNED_INVOICES_FOR_PERSON = "invoices.unassignedForPerson";
    String FIND_INVOICE_FOR_COUPON = "invoices.coupons";
    String QUERY_SPARSE_CODING_SHEETS_USING_ONTOLOGY = "sparseCodingSheets.ontology";
    String QUERY_FILE_SIZE_TOTAL = "file.total_size";
    String QUERY_RELATED_RESOURCES = "resource.related";
    String QUERY_PROXY_RESOURCE_FULL = "resourceProxy.full";
    String QUERY_PROXY_RESOURCE_SHORT = "resourceProxy.short";
    String QUERY_RESOURCE_FIND_OLD_LIST = "resource.old";
    String FIND_ACCOUNT_FOR_INVOICE = "account.forInvoice";
    String DELETE_INFORMATION_RESOURCE_FILE_VERSION_IMMEDIATELY = "irfv.delete";
    String COLLECTION_LIST_WITH_AUTHUSER = "collection.idlest.with.authuser";
    String QUERY_SPARSE_EDITABLE_SORTED_RESOURCES_INHERITED = "query.sparse.editable.sorted.resources.inherited";
    String QUERY_SPARSE_EDITABLE_SORTED_RESOURCES_INHERITED_SORTED = "query.sparse.editable.sorted.resources.inherited.sorted";
    String QUERY_SPARSE_COLLECTION_RESOURCES = "query.sparse.collection.resources";
    String COLLECTION_VIEW = "collection.views";
    String CREATOR_VIEW = "creator.views";
    String QUERY_COLLECTION_CHILDREN = "resourceCollection.allChildren";
    String QUERY_INFORMATION_RESOURCE_FILE_VERSION_VERIFICATION = "versions.verify";
    String QUERY_CLEAR_REFERENCED_ONTOLOGYNODE_RULES = "update.clearOntologyNodeReferences";
    String UPDATE_DATATABLECOLUMN_ONTOLOGIES = "update.dataTableColumnOntologies";
    String FIND_BY_TDAR_YEAR = "query.sparse_by_tdar_year";
    String FIND_BY_TDAR_YEAR_COUNT = "query.sparse_by_tdar_year_count";
    String QUERY_RESOURCE_FILE_EMBARGO_EXIPRED = "query.expired";
    String QUERY_HOSTED_DOWNLOAD_AUTHORIZATION = "query.hosted_download_auth";
    String QUERY_RESOURCE_FILE_EMBARGOING_TOMORROW = "query.expires_tomorrow";
    String QUERY_INTEGRATION_DATA_TABLE = "query.integration_data_table";
    String QUERY_INTEGRATION_ONTOLOGY = "query.integration_ontology";
    String CAN_EDIT_INSTITUTION = "query.authorize_edit_institution";

    // raw SQL/HQL queries

    /**
     * Static HQL and SQL queries that cannot be represented as annotations because they are either pure SQL or use String replacement.
     */
    String QUERY_SQL_DASHBOARD =
            "select id, status, resource_type from resource " +
                    "where id in " +
                    "(select resource_id from collection_resource,collection, authorized_user " +
                    "where collection.id=collection_resource.collection_id and collection.id=authorized_user.resource_collection_id " +
                    "and user_id=:submitterId and general_permission_int > :effectivePermissions " +
                    "union select id from resource where updater_id=:submitterId or submitter_id=:submitterId)";

    String QUERY_SQL_COUNT = "SELECT COUNT(*) FROM %1$s";
    String QUERY_FIND_ALL = "FROM %s";
    String QUERY_FIND_ALL_WITH_IDS = "FROM %s WHERE id in (:ids)";
    String QUERY_FIND_ALL_WITH_STATUS = "FROM %s WHERE status in (:statuses)";
    String QUERY_SQL_COUNT_ACTIVE_RESOURCE = "SELECT COUNT(*) FROM %1$s where status='ACTIVE' and resourceType='%2$s' ";
    String QUERY_SQL_COUNT_ACTIVE_RESOURCE_WITH_FILES = "select count(distinct  resource.id) from  resource, information_resource_file where  resource.status='ACTIVE' and resource.resource_type='%1$s' and resource.id=information_resource_id";
    String QUERY_SQL_RESOURCE_INCREMENT_USAGE = "update Resource r set r.accessCounter=accessCounter+1 where r.id=:resourceId";
    String QUERY_SQL_RAW_RESOURCE_STAT_LOOKUP = "select rt.* , "
            + "(select count(*) from resource where resource_type = rt.resource_type and status = 'ACTIVE') as all,  "
            + "(select count(*) from resource where resource_type = rt.resource_type and status = 'DRAFT') as draft, "
            + "(select count(distinct information_resource_id) from information_resource_file irf join resource rr on (rr.id = irf.information_resource_id) where rr.resource_type = rt.resource_type and rr.status = 'ACTIVE') as with_files, "
            + "(select count(distinct information_resource_id) from information_resource_file irf join resource rr on (rr.id = irf.information_resource_id) where rr.resource_type = rt.resource_type and rr.status = 'ACTIVE' and irf.restriction = 'CONFIDENTIAL') as with_conf "
            + "from (select distinct resource_type from resource) as rt";

    // generated HQL formats
    String QUERY_CREATOR_MERGE_ID = "select merge_creator_id from %1$s where id=%2$s";

    String QUERY_KEYWORD_MERGE_ID = "select merge_keyword_id from %1$s where id=%2$s";

    // e.g."from Resource r1 where exists (from Resource r2 inner join r2.cultureKeywords ck where r2.id = r1.id and ck.id in (:idlist))"
    String QUERY_HQL_MANY_TO_MANY_REFERENCES =
            "from %1$s r1 where exists (from %1$s r2 inner join r2.%2$s ck where r2.id = r1.id and ck.id in (:idlist))";
    // e.g. "from Resource r1 where submitter_id in (:idlist)"
    String QUERY_HQL_MANY_TO_ONE_REFERENCES =
            "from %1$s r1 where %2$s.id in (:idlist)";

    String QUERY_HQL_COUNT_MANY_TO_MANY_REFERENCES =
            "select count(*) as referenceCount from %1$s r1 where exists (from %1$s r2 inner join r2.%2$s ck where r2.id = r1.id and ck.id in (:idlist))";
    String QUERY_HQL_COUNT_MANY_TO_ONE_REFERENCES =
            "select count(*) as referenceCount from %1$s r1 where %2$s.id in (:idlist)";

    String QUERY_HQL_COUNT_MANY_TO_MANY_REFERENCES_MAP =
            "select new map(ck.id as id, count(*) as referenceCount) from %1$s r2 inner join r2.%2$s ck where ck.id in (:idlist) group by ck.id";
    String QUERY_HQL_COUNT_MANY_TO_ONE_REFERENCES_MAP =
            "select new map(%2$s.id as id, count(*) as referenceCount) from %1$s r1 where %2$s.id in (:idlist) group by %2$s.id";

    String QUERY_HQL_UPDATE_MANY_TO_MANY_REFERENCES = ""; // TODO: //Not possible, I think.
    String QUERY_HQL_UPDATE_MANY_TO_ONE_REFERENCES = ""; // TODO: use many_to_one_count in exists clause.

    String HQL_EDITABLE_RESOURCE_SUFFIX = " FROM Resource as res  where "
            +
            " (TRUE=:allResourceTypes or res.resourceType in (:resourceTypes)) "
            + "and (TRUE=:allStatuses or res.status in (:statuses) )  AND "
            +
            " (res.submitter.id=:userId or exists "
            + "( from ResourceCollection rescol left join rescol.parentIds parentId join rescol.resources as colres where colres.id = res.id and "
            +
            " (TRUE=:admin or rescol.owner.id=:userId or exists ( "
            + "select 1 from ResourceCollection r join r.authorizedUsers as auth where (rescol.id=r.id or parentId=r.id) and auth.user.id=:userId and auth.effectiveGeneralPermission > :effectivePermission)) "
            + ")"
            + ")  ";

    String HQL_EDITABLE_RESOURCE_SORTED_SUFFIX = HQL_EDITABLE_RESOURCE_SUFFIX + " order by res.title, res.id";
    String QUERY_ACCOUNTS_FOR_RESOURCES = "select id, account_id from resource res where res.id in (%s) ";
    String QUERY_SQL_RESOURCES_BY_YEAR = "select date_part('year', date_registered), count(id) from resource where status='ACTIVE' and date_registered is not null group by date_part('year', date_registered)  order by date_part('year', date_registered)  asc";
    String DISTINCT_SUBMITTERS = "SELECT DISTINCT submitter_id from resource";

    String UPDATE_KEYWORD_OCCURRENCE_CLEAR_COUNT = "update %1$s set occurrence=0";
    String UPDATE_KEYWORD_OCCURRENCE_COUNT_INHERITANCE = "update %1$s set occurrence = occurrence + coalesce((select count(resource_id) from resource_%1$s where %1$s_id =%1$s.id and resource_id in (select project_id from information_resource where %2$s is true) group by %1$s_id),0)";
    String UPDATE_KEYWORD_OCCURRENCE_COUNT = "update %1$s set occurrence =  occurrence + coalesce((select count(resource_id) from resource_%1$s where %1$s_id =%1$s.id group by %1$s_id),0)";
    String UPDATE_CREATOR_OCCURRENCE_CLEAR_COUNT = "update creator set occurrence=0, browse_occurrence=0";
    String UPDATE_CREATOR_OCCURRENCE_RESOURCE_INFORMATION_RESOURCE_PUBLISHER = "update creator set %s=%s + coalesce((select count(information_resource.id) from information_resource where publisher_id=creator.id group by publisher_id),0)";
    String UPDATE_CREATOR_OCCURRENCE_RESOURCE_INFORMATION_RESOURCE_PROVIDER = "update creator set %s=%s + coalesce((select count(information_resource.id) from information_resource where provider_institution_id=creator.id group by provider_institution_id),0)";
    String UPDATE_CREATOR_OCCURRENCE_RESOURCE_INFORMATION_RESOURCE_COPYRIGHT = "update creator set occurrence=occurrence + coalesce((select count(information_resource.id) from information_resource where copyright_holder_id=creator.id group by copyright_holder_id),0) ";
    String UPDATE_CREATOR_OCCURRENCE_RESOURCE_SUBMITTER = "update creator set occurrence=occurrence + coalesce((select count(resource.id) from resource where submitter_id=creator.id group by submitter_id),0)";
    String UPDATE_CREATOR_OCCURRENCE_RESOURCE = "update creator set occurrence = occurrence+ coalesce((select count(resource_id) from resource_creator where creator_id=creator.id group by creator_id),0) ";
    String UPDATE_CREATOR_OCCURRENCE_RESOURCE_INHERITED = "update creator set occurrence = occurrence+ coalesce((select count(resource_id) from resource_creator where creator_id=creator.id and resource_id in (select project_id from information_resource where inheriting_individual_institutional_credit is true)  group by creator_id),0) ";
    String UPDATE_CREATOR_OCCURRENCE_INSTITUTION = "update creator set occurrence = occurrence+ coalesce((select count(person.id) from person where institution_id=creator.id group by institution_id),0) ";
    String DATASETS_USING_NODES = "select id from resource where id in (select dataset_id from data_table where data_table.id in (select data_table_id from data_table_column, coding_rule, coding_sheet where data_table_column.default_coding_sheet_id=coding_sheet_id and coding_rule.coding_sheet_id=coding_sheet.id and  ontology_node_id=%s)) and status = 'ACTIVE'";
    
    String BROWSE_CREATOR_CREATE_TEMP = "create temporary table rctest (rid bigint, cid bigint, role int);create temporary table rctest_creator (cid bigint, cnt bigint)";
    String BROWSE_CREATOR_ACTIVE_USERS_1 = "insert into rctest select resource.id, submitter_id, 0 from resource where status='ACTIVE'";
    String BROWSE_CREATOR_ROLES_2 = "insert into rctest select resource_id, creator_id, %2$s from resource_creator where resource_id in (select rid from rctest) and role in (%1$s)";
    String BROWSE_CREATOR_IR_ROLES_3 = "insert into rctest select resource_id, creator_id, %2$s from resource_creator where resource_id in (select project_id from rctest, information_resource ir where rid=ir.id and inheriting_individual_institutional_credit=true ) and role in (%1$s)";
    String BROWSE_CREATOR_IR_FIELDS_4 = "insert into rctest select id, provider_institution_id, -1 from information_resource where id in (select rid from rctest) and provider_institution_id is not null union select id, publisher_id, -1 from information_resource where id in (select rid from rctest) and publisher_id is not null";
    String BROWSE_CREATOR_CREATOR_TEMP_5 = "insert into rctest_creator (cid, cnt) select cid, count(rid) from rctest where rid in (select rid from rctest where role >= 0 group by rid having sum(role) = 0 union select rid from rctest where role!=0) group by cid";
    String BROWSE_CREATOR_UPDATE_CREATOR_6 = "update creator SET browse_occurrence = cnt from creator c, rctest_creator where cid=c.id and c.id=creator.id";
    
    String SELECT_RAW_IMAGE_SITEMAP_FILES = "select r.id as resourceId, r.title, r.description as resourceDescription, r.resource_type, irf.description, irfv.id from resource r, information_resource ir, information_resource_file irf, "
            + "information_resource_file_version irfv where r.id=ir.id and ir.id=irf.information_resource_id and "
            + "irf.id=irfv.information_resource_file_id and internal_type='WEB_SMALL' and resource_type in ('IMAGE','SENSORY_DATA','GEOSPATIAL') "
            + "and restriction='PUBLIC' and r.status='ACTIVE'";

    String CONVERT_PERSON_TO_USER = "INSERT INTO tdar_user (id, username) VALUES(%s, '%s')";
    String DAILY_RESOURCE_UPDATE = "INSERT INTO resource_access_day_agg (resource_id, year, month, date_accessed, count) select resource_id, date_part('year', date_accessed), date_part('month', date_accessed), date_trunc('day',date_accessed), count(id) from resource_access_statistics where date_trunc('day',date_accessed)='%1$tF' group by resource_id, date_part('year', date_accessed), date_part('month', date_accessed), date_trunc('day', date_accessed)";
    String DAILY_DOWNLOAD_UPDATE = "INSERT INTO file_download_day_agg (information_resource_file_id, year, month, date_accessed, count) select information_resource_file_id, date_part('year', date_accessed), date_part('month', date_accessed), date_trunc('day',date_accessed), count(id) from information_resource_file_download_statistics where date_trunc('day',date_accessed)='%1$tF' group by information_resource_file_id, date_part('year', date_accessed), date_part('month', date_accessed), date_trunc('day', date_accessed)";

    String FIND_ACTIVE_PERSISTABLE_BY_ID = "select id from %s where status in ('ACTIVE')";
    String FIND_ACTIVE_PERSON_BY_ID = "select id from %s where status in ('ACTIVE') and browse_occurrence > 0 and hidden=false";
    String FIND_ACTIVE_INSTITUTION_BY_ID = "select id from %s where status in ('ACTIVE') and browse_occurrence > 0 and hidden=false";
    String WEEKLY_EMAIL_STATS = "stats.weekly_emails";

    String RESOURCE_ACCESS_COUNT_SQL = "select coalesce((select count(ras.id)  from resource_access_statistics ras where ras.resource_id='%1$s' and ras.date_accessed > '%2$tY-%2$tm-%2$td') ,0) + coalesce((select sum(rad.count) from resource_access_day_agg rad where rad.resource_id='%1$s'),0)";
    String DOWNLOAD_COUNT_SQL = "select coalesce((select count(irfds.id)  from information_resource_file_download_statistics irfds where irfds.information_resource_file_id='%1$s' and irfds.date_accessed > '%2$tY-%2$tm-%2$td') ,0) + coalesce((select sum(fda.count) from file_download_day_agg fda where fda.information_resource_file_id='%1$s'),0)";
    String ANNUAL_ACCESS_SKELETON = "select id, title, resource_type, status, %s %s from resource where id in (:ids)";

    String ANNUAL_VIEW_PART = "(select sum(count) from resource_access_day_agg where resource_id=resource.id and year='%1$s') as \"%1$s Views\"";
    String ANNUAL_DOWNLAOD_PART = "(select sum(count) from file_download_day_agg, information_resource_file where information_resource_id=resource.id and information_resource_file.id=file_download_day_agg.information_resource_file_id and year='%1$s') as \"%1$s Downloads\"";
    String MONTH_VIEW_PART = "(select sum(count) from resource_access_day_agg where resource_id=resource.id and year='%1$s' and month='%2$s') as \"%1$s-%2$s Views\"";
    String MONTH_DOWNLAOD_PART = "(select sum(count) from file_download_day_agg, information_resource_file where information_resource_id=resource.id and information_resource_file.id=file_download_day_agg.information_resource_file_id and year='%1$s' and month='%2$s') as \"%1$s-%2$s Downloads\"";
    String DAY_VIEW_PART = "(select sum(count) from resource_access_day_agg where resource_id=resource.id and date_accessed = '%1$s') as \"%1$s Views\"";
    String DAY_DOWNLAOD_PART = "(select sum(count) from file_download_day_agg, information_resource_file where information_resource_id=resource.id and information_resource_file.id=file_download_day_agg.information_resource_file_id and date_accessed = '%1$s') as \"%1$s Downloads\"";
}
