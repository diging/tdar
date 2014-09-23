package org.tdar.core.bean;

import org.tdar.core.bean.entity.Person;

/**
 * This interface is designed to ensure that fields are available for basic searhing
 * NOTE: HibernateSearch does not have the ability to inherit the @Field annotations
 * 
 * NOTE: THIS IS NOT USED
 */
public interface SimpleSearch extends Persistable {
    enum SimpleSearchType {
        RESOURCE("Resource"),
        COLLECTION("Collection");

        private String label;

        SimpleSearchType(String label) {
            this.setLabel(label);
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    static final String TITLE_SORT_REGEX = "^([\\s\\W]|The |A |An )+";

    String getTitleSort();

    String getTitle();

    String getDescription();

    String getUrlNamespace();

    Person getSubmitter();

}
