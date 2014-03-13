package org.tdar.core.bean.entity;

import org.tdar.core.bean.HasLabel;
import org.tdar.core.bean.Localizable;
import org.tdar.utils.MessageHelper;

public enum UserAffiliation implements HasLabel, Localizable {
    K12_STUDENT("K-12 Student"),
    UNDERGRADUATE_STUDENT("Undergraduate Student"),
    GRADUATE_STUDENT("Graduate Student"),
    K_12_TEACHER("K-12 Teacher"),
    HIGHER_ED_FACULTY("Higher Ed. Faculty"),
    INDEPENDENT_RESEARCHER("Independent Researcher"),
    PUBLIC_AGENCY_ARCH("Public Agency Archaeologist"),
    CRM_ARCHAEOLOGIST("CRM Firm Archaeologist"),
    NON_PROFESSIONAL_ARCH("Nonprofessional/Avocational Archaeologist"),
    GENERAL_PUBLIC("General Public");
    
    private String label;
    
    private UserAffiliation(String label) {
        this.label = label;
    }

    @Override
    public String getLocaleKey() {
        return MessageHelper.formatLocalizableKey(this);
    }

    @Override
    public String getLabel() {
        return label;
    }
    
    
    

}
