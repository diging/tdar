package org.tdar.struts.action.search;

import java.util.ArrayList;
import java.util.List;

import org.tdar.core.bean.HasLabel;
import org.tdar.core.bean.keyword.CultureKeyword;
import org.tdar.core.bean.keyword.GeographicKeyword;
import org.tdar.core.bean.keyword.InvestigationType;
import org.tdar.core.bean.keyword.MaterialKeyword;
import org.tdar.core.bean.keyword.OtherKeyword;
import org.tdar.core.bean.keyword.SiteNameKeyword;
import org.tdar.core.bean.keyword.SiteTypeKeyword;
import org.tdar.core.bean.keyword.TemporalKeyword;
import org.tdar.utils.MessageHelper;

public enum SearchFieldType implements HasLabel {
    // basic fields
    ALL_FIELDS("allFields", SearchFieldGroup.BASIC_FIELDS, "All Fields"),
    TITLE("titles", SearchFieldGroup.BASIC_FIELDS, "Title"),
    CONTENTS("contents", SearchFieldGroup.BASIC_FIELDS, "Full-Text"),
    RESOURCE_CREATOR_PERSON("resourceCreatorProxies", SearchFieldGroup.BASIC_FIELDS, "Person", false),
    RESOURCE_CREATOR_INSTITUTION("resourceCreatorProxies", SearchFieldGroup.BASIC_FIELDS, "Institution", false),
    TDAR_ID("resourceIds", SearchFieldGroup.BASIC_FIELDS, "Id"),
    COVERAGE_DATE_CALENDAR("coverageDates", SearchFieldGroup.BASIC_FIELDS, "Calendar Dates", false),
    COVERAGE_DATE_RADIOCARBON("coverageDates", SearchFieldGroup.BASIC_FIELDS, "RadioCarbon Dates", false),
    PROJECT("projects", SearchFieldGroup.BASIC_FIELDS, "Project", false),
    COLLECTION("collections", SearchFieldGroup.BASIC_FIELDS, "Collection", false),
    FILENAME("filenames", SearchFieldGroup.BASIC_FIELDS, "File Name"),

    // freeform keywords
    FFK_GEOGRAPHIC("geographicKeywords", SearchFieldGroup.FREEFORM_KEYWORDS, "Geographic Keywords",GeographicKeyword.class),
    FFK_SITE("siteNames", SearchFieldGroup.FREEFORM_KEYWORDS, "Site Names",SiteNameKeyword.class),
    FFK_SITE_TYPE("uncontrolledSiteTypes", SearchFieldGroup.FREEFORM_KEYWORDS, "Site Type"),
    FFK_CULTURAL("uncontrolledCultureKeywords", SearchFieldGroup.FREEFORM_KEYWORDS, "Culture Keywords"),
    FFK_TEMPORAL("temporalKeywords", SearchFieldGroup.FREEFORM_KEYWORDS, "Temporal Keywords", TemporalKeyword.class),
    FFK_GENERAL("otherKeywords", SearchFieldGroup.FREEFORM_KEYWORDS, "General Keywords",OtherKeyword.class),

    // managed keywords
    KEYWORD_INVESTIGATION("investigationTypeIdLists", SearchFieldGroup.CONTROLLED_KEYWORDS, "Investigation Types", false, InvestigationType.class),
    KEYWORD_SITE("approvedSiteTypeIdLists", SearchFieldGroup.CONTROLLED_KEYWORDS, "Site Type(Controlled)", false,SiteTypeKeyword.class),
    KEYWORD_MATERIAL("materialKeywordIdLists", SearchFieldGroup.CONTROLLED_KEYWORDS, "Material Types", false,MaterialKeyword.class),
    KEYWORD_CULTURAL("approvedCultureKeywordIdLists", SearchFieldGroup.CONTROLLED_KEYWORDS, "Culture Keywords", false,CultureKeyword.class),

    // TODO: add these
    CREATION_DECADE("creationDecades", SearchFieldGroup.EXPLORE, "Creation Decade", false),

    DATE_CREATED("createdDates", SearchFieldGroup.BASIC_FIELDS, "Year", false),
    DATE_REGISTERED("registeredDates", SearchFieldGroup.BASIC_FIELDS, "Date Registered", false),
    DATE_UPDATED("updatedDates", SearchFieldGroup.BASIC_FIELDS, "Date Updated", false);

    private String label = "";
    private SearchFieldGroup fieldGroup;
    private String fieldName = "";
    private boolean simple = true;
    private Class<?> associatedClass;

    private SearchFieldType() {
        this.fieldGroup = SearchFieldGroup.BASIC_FIELDS;
    }

    private SearchFieldType(String fieldName, SearchFieldGroup fieldGroup, String label, boolean simple, Class<?> associatedClass) {
        this(fieldName, fieldGroup, label, simple);
        this.setAssociatedClass(associatedClass);
    }

    private SearchFieldType(String fieldName, SearchFieldGroup fieldGroup, String label, Class<?> associatedClass) {
        this(fieldName, fieldGroup, label);
        this.setAssociatedClass(associatedClass);
    }

    private SearchFieldType(String fieldName, SearchFieldGroup fieldGroup, String label, boolean simple) {
        this(fieldName, fieldGroup, label);
        this.simple = simple;
    }

    private SearchFieldType(String fieldName, SearchFieldGroup fieldGroup, String label) {
        this.label = label;
        this.fieldGroup = fieldGroup;
        this.fieldName = fieldName;
    }

    public static List<SearchFieldType> getSearchFieldTypesByGroup() {
        List<SearchFieldType> types = new ArrayList<SearchFieldType>();
        for (SearchFieldGroup group : SearchFieldGroup.values()) {
            for (SearchFieldType type : SearchFieldType.values()) {
                if (type.getFieldGroup() == group) {
                    types.add(type);
                }
            }
        }
        return types;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    public SearchFieldGroup getFieldGroup() {
        return this.fieldGroup;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public boolean isSimple() {
        return simple;
    }

    public boolean isHidden() {
        if (this.fieldGroup == SearchFieldGroup.EXPLORE) {
            return true;
        }
        return false;
    }

    public Class<?> getAssociatedClass() {
        return associatedClass;
    }

    public void setAssociatedClass(Class<?> associatedClass) {
        this.associatedClass = associatedClass;
    }

}
