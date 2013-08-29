package org.tdar.search.query;

public interface QueryFieldNames {

    public static final String PROJECT_ID = "projectId";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final String STATUS = "status";
    public static final String ACTIVE_START_DATE = "activeCoverageDates.startDate";
    public static final String ACTIVE_END_DATE = "activeCoverageDates.endDate";
    public static final String DATE = "date";
    public static final String DATE_UPDATED = "dateUpdated";
    public static final String DATE_CREATED = "dateCreated";
    public static final String LABEL = "label";
    public static final String DOT = ".";
    static final String DOT_LABEL = DOT + LABEL;
    static final String IR = "informationResources.";
    public static final String TITLE = "title";
    public static final String TITLE_AUTO = "title_auto";
    public static final String PROJECT_TITLE = "project.title";
    public static final String PROJECT_TITLE_AUTO = "project.title_auto";
    public static final String TITLE_SORT = "title_sort";
    public static final String DOCUMENT_TYPE = "documentType";
    public static final String INTEGRATABLE = "integratable";
    public static final String SUBMITTER_ID = "submitter.id";
    public static final String RESOURCE_CREATORS_CREATOR_ID = "resourceCreators.creator.id";
    public static final String RESOURCE_CREATORS_CREATOR_NAME_KEYWORD = "resourceCreators.creator.name_kwd";
    public static final String ACTIVE_SITE_TYPE_KEYWORDS = "activeSiteTypeKeywords";
    public static final String ACTIVE_MATERIAL_KEYWORDS = "activeMaterialKeywords";
    public static final String ACTIVE_CULTURE_KEYWORDS = "activeCultureKeywords";
    public static final String ACTIVE_SITE_NAME_KEYWORDS = "activeSiteNameKeywords";
    public static final String ACTIVE_INVESTIGATION_TYPES = "activeInvestigationTypes";
    public static final String ACTIVE_GEOGRAPHIC_KEYWORDS = "activeGeographicKeywords";
    public static final String ACTIVE_OTHER_KEYWORDS = "activeOtherKeywords";
    public static final String ACTIVE_LATITUDE_LONGITUDE_BOXES = "activeLatitudeLongitudeBoxes";
    
    public static final String MAXX = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "maxx";
    public static final String MAXY = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "maxy";
    public static final String MINY = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "miny";
    public static final String MINX = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "minx";
    public static final String SCALE = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "scale";
    public static final String MINXPRIME = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "minxPrime";
    public static final String MAXXPRIME = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "maxxPrime";

    public static final String ACTIVE_TEMPORAL_KEYWORDS = "activeTemporalKeywords";
    public static final String ACTIVE_SITE_TYPE_KEYWORDS_LABEL = ACTIVE_SITE_TYPE_KEYWORDS + DOT_LABEL;
    public static final String ACTIVE_CULTURE_KEYWORDS_LABEL = ACTIVE_CULTURE_KEYWORDS + DOT_LABEL;
    public static final String ACTIVE_GEOGRAPHIC_KEYWORDS_LABEL = ACTIVE_GEOGRAPHIC_KEYWORDS + DOT_LABEL;
    public static final String IR_ACTIVE_CULTURE_KEYWORDS_LABEL = IR + ACTIVE_CULTURE_KEYWORDS + DOT_LABEL;
    public static final String IR_ACTIVE_SITE_TYPE_KEYWORDS_LABEL = IR + ACTIVE_SITE_TYPE_KEYWORDS + DOT_LABEL;
    public static final String ACTIVE_COVERAGE_TYPE = "activeCoverageDates.dateType";
    public static final String NAME = "name";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String EMAIL = "email";
    public static final String ID = "id";
    public static final String ALL = "all";
    public static final String ALL_PHRASE = "allPhrase";
    public static final String CONTENT = "content";
    public static final String DESCRIPTION = "description";
    public static final String PROJECT_TITLE_SORT = "project_title_sort";
    public static final String LABEL_SORT = "label_sort";
    public static final String FIRST_NAME_SORT = "firstName_sort";
    public static final String LAST_NAME_SORT = "lastName_sort";
    public static final String CREATOR_NAME_SORT = "creator_name_sort";

    public static final String COLLECTION_NAME = TITLE;
    public static final String COLLECTION_NAME_AUTO = TITLE_AUTO;
    public static final String COLLECTION_TYPE = "type";
    public static final String RESOURCE_USERS_WHO_CAN_VIEW = "usersWhoCanView";
    public static final String RESOURCE_USERS_WHO_CAN_MODIFY = "usersWhoCanModify";
    public static final String COLLECTION_USERS_WHO_CAN_ADMINISTER = "usersWhoCanAdminister";
    public static final String COLLECTION_USERS_WHO_CAN_VIEW = RESOURCE_USERS_WHO_CAN_VIEW;
    public static final String COLLECTION_USERS_WHO_CAN_MODIFY = RESOURCE_USERS_WHO_CAN_MODIFY;
//    public static final String RESOURCE_COLLECTION_PUBLIC_IDS = "publicCollectionIds";
    public static final String RESOURCE_COLLECTION_SHARED_IDS = "sharedCollectionIds";
    public static final String RESOURCE_ACCESS_TYPE = "resourceAccessType";
    public static final String PROPER_NAME = "properName";
    public static final String RESOURCE_CREATORS_PROPER_NAME = "resourceCreators.creator." + PROPER_NAME;
    public static final String INFORMATION_RESOURCE_FILES_FILENAME = "informationResourceFiles.filename";
    public static final String RESOURCE_PROVIDER_ID = "resourceProviderInstitution.id";
    public static final String CATEGORY_ID = "categoryVariable.id";
    public static final String CATEGORY_LABEL = "categoryVariable.label";
    public static final String COLLECTION_VISIBLE = "visible";
    public static final String TOP_LEVEL = "topLevel";
    public static final String RESOURCE_TYPE_SORT = "resourceTypeSort";
    public static final String RESOURCE_OWNER = "resourceOwner";
    public static final String DATE_CREATED_DECADE = "decadeCreated";
    public static final String CREATOR_ROLE_IDENTIFIER = "crid";
    public static final String FILENAME = "filename";
    public static final String DATA_VALUE_PAIR = "dataValuePair";
    public static final String COLLECTION_TREE = "collection.parentTree";
    
}
