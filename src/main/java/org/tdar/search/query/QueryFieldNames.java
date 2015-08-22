package org.tdar.search.query;

public interface QueryFieldNames {

    String PROJECT_ID = "projectId";
    String RESOURCE_TYPE = "resourceType";
    String STATUS = "status";
    String ACTIVE_START_DATE = "activeCoverageDates.startDate";
    String ACTIVE_END_DATE = "activeCoverageDates.endDate";
    String DATE = "date";
    String DATE_UPDATED = "dateUpdated";
    String DATE_CREATED = "dateCreated";
    String LABEL = "label";
    String DOT = ".";
    String DOT_LABEL = DOT + LABEL;
    String IR = "informationResources.";
    String TITLE = "title";
    String TITLE_AUTO = "title_auto";
    String PROJECT_TITLE = "project.title";
    String PROJECT_TITLE_AUTO = "project.title_auto";
    String TITLE_SORT = "title_sort";
    String DOCUMENT_TYPE = "documentType";
    String INTEGRATABLE = "integratable";
    String SUBMITTER_ID = "submitter.id";
    String RESOURCE_CREATORS_CREATOR_ID = "activeResourceCreators.creator.id";
    String RESOURCE_CREATORS_CREATOR_NAME_KEYWORD = "activeResourceCreators.creator.name_kwd";
    String ACTIVE_SITE_TYPE_KEYWORDS = "activeSiteTypeKeywords";
    String ACTIVE_MATERIAL_KEYWORDS = "activeMaterialKeywords";
    String ACTIVE_CULTURE_KEYWORDS = "activeCultureKeywords";
    String ACTIVE_SITE_NAME_KEYWORDS = "activeSiteNameKeywords";
    String ACTIVE_INVESTIGATION_TYPES = "activeInvestigationTypes";
    String ACTIVE_GEOGRAPHIC_KEYWORDS = "activeGeographicKeywords";
    String ACTIVE_OTHER_KEYWORDS = "activeOtherKeywords";
    String ACTIVE_LATITUDE_LONGITUDE_BOXES = "activeLatitudeLongitudeBoxes";

    String MAXX = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "maxx";
    String MAXY = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "maxy";
    String MINY = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "miny";
    String MINX = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "minx";
    String SCALE = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "scale";
    String MINXPRIME = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "minxPrime";
    String MAXXPRIME = ACTIVE_LATITUDE_LONGITUDE_BOXES + DOT + "maxxPrime";

    String ACTIVE_TEMPORAL_KEYWORDS = "activeTemporalKeywords";
    String ACTIVE_SITE_TYPE_KEYWORDS_LABEL = ACTIVE_SITE_TYPE_KEYWORDS + DOT_LABEL;
    String ACTIVE_CULTURE_KEYWORDS_LABEL = ACTIVE_CULTURE_KEYWORDS + DOT_LABEL;
    String ACTIVE_GEOGRAPHIC_KEYWORDS_LABEL = ACTIVE_GEOGRAPHIC_KEYWORDS + DOT_LABEL;
    String IR_ACTIVE_CULTURE_KEYWORDS_LABEL = IR + ACTIVE_CULTURE_KEYWORDS + DOT_LABEL;
    String IR_ACTIVE_SITE_TYPE_KEYWORDS_LABEL = IR + ACTIVE_SITE_TYPE_KEYWORDS + DOT_LABEL;
    String ACTIVE_COVERAGE_TYPE = "activeCoverageDates.dateType";
    String NAME = "name";
    String FIRST_NAME = "firstName";
    String LAST_NAME = "lastName";
    String EMAIL = "email";
    String ID = "id";
    String ALL = "all";
    String ALL_PHRASE = "allPhrase";
    String CONTENT = "content";
    String DESCRIPTION = "description";
    String PROJECT_TITLE_SORT = "project_title_sort";
    String LABEL_SORT = "label_sort";
    String FIRST_NAME_SORT = "firstName_sort";
    String LAST_NAME_SORT = "lastName_sort";
    String CREATOR_NAME_SORT = "creator_name_sort";

    String COLLECTION_NAME = TITLE;
    String COLLECTION_NAME_AUTO = TITLE_AUTO;
    String COLLECTION_TYPE = "type";
    String RESOURCE_USERS_WHO_CAN_VIEW = "usersWhoCanView";
    String RESOURCE_USERS_WHO_CAN_MODIFY = "usersWhoCanModify";
    String COLLECTION_USERS_WHO_CAN_ADMINISTER = "usersWhoCanAdminister";
    String COLLECTION_USERS_WHO_CAN_VIEW = RESOURCE_USERS_WHO_CAN_VIEW;
    String COLLECTION_USERS_WHO_CAN_MODIFY = RESOURCE_USERS_WHO_CAN_MODIFY;
    // String RESOURCE_COLLECTION_PUBLIC_IDS = "publicCollectionIds";
    String RESOURCE_COLLECTION_SHARED_IDS = "sharedCollectionIds";
    String RESOURCE_ACCESS_TYPE = "resourceAccessType";
    String PROPER_NAME = "properName";
    String RESOURCE_CREATORS_PROPER_NAME = "activeResourceCreators.creator." + PROPER_NAME;
    String INFORMATION_RESOURCE_FILES_FILENAME = "informationResourceFiles.filename";
    String RESOURCE_PROVIDER_ID = "resourceProviderInstitution.id";
    String CATEGORY_ID = "categoryVariable.id";
    String CATEGORY_LABEL = "categoryVariable.label";
    String COLLECTION_HIDDEN = "hidden";
    String TOP_LEVEL = "topLevel";
    String RESOURCE_TYPE_SORT = "resourceTypeSort";
    String RESOURCE_OWNER = "resourceOwner";
    String DATE_CREATED_DECADE = "decadeCreated";
    String CREATOR_ROLE_IDENTIFIER = "crid";
    String IR_CREATOR_ROLE_IDENTIFIER = IR + CREATOR_ROLE_IDENTIFIER;
    String FILENAME = "filename";
    String DATA_VALUE_PAIR = "dataValuePair";
    String COLLECTION_TREE = "collection.parentTree";
    String SITE_CODE = "siteCode";
    String TITLE_PHRASE = "title.phrase";
    String DESCRIPTION_PHRASE = "description.phrase";
    String COLLECTION_NAME_PHRASE = TITLE_PHRASE;
    String NAME_PHRASE = "name_phrase";
    String NAME_TOKEN = "name_token";
    String USERNAME = "username";
    String PROPER_AUTO = "proper_name_auto";
    String COLLECTION_HIDDEN_WITH_RESOURCES = "visibleInSearch";

    String RESOURCE_COLLECTION_DIRECT_SHARED_IDS = "directSharedCollectionIds";
    String LAST_NAME_AUTO = "lastName_auto";
    String FIRST_NAME_AUTO = "firstName_auto";
}
