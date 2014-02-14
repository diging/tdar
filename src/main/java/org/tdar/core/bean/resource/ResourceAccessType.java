/**
 * $Id$
 * 
 * @author $Author$
 * @version $Revision$
 */
package org.tdar.core.bean.resource;

import org.tdar.core.bean.HasLabel;
import org.tdar.search.query.QueryFieldNames;

/**
 * Describes the aggregate restrictions on all of the files on the InformationResource.
 * 
 * @author Adam Brin
 * 
 */
@SuppressWarnings("rawtypes")
public enum ResourceAccessType implements HasLabel, Facetable {
    CITATION("Citation Only"),
    PUBLICALLY_ACCESSIBLE("Publicly Accessible Files"),
    PARTIALLY_RESTRICTED("Some Files Restricted"),
    RESTRICTED("Restricted Files");

    private String label;
    private transient Integer count;

    ResourceAccessType(String label) {
        this.setLabel(label);
    }

    @Override
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public Integer getCount() {
        return count;
    }

    @Override
    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public String getLuceneFieldName() {
        return QueryFieldNames.RESOURCE_ACCESS_TYPE;
    }

    @Override
    public ResourceAccessType getValueOf(String val) {
        return valueOf(val);
    }
}
