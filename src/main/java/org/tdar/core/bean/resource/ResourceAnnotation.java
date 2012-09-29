package org.tdar.core.bean.resource;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Field;
import org.tdar.core.bean.HasResource;
import org.tdar.core.bean.Persistable;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * $Id$
 * 
 * A ResourceAnnotation represents a semi-controlled organizational identifier consisting
 * of a semi-controlled key, an arbitrary annotation value, and the resource tagged with
 * the annotation.
 * 
 * @author <a href='mailto:allen.lee@asu.edu'>Allen Lee</a>
 * @version $Rev$
 */
@Entity
@XStreamAlias("resourceAnnotation")
@Table(name = "resource_annotation")
public class ResourceAnnotation extends Persistable.Base implements HasResource<Resource> {

    private static final long serialVersionUID = 8517883471101372051L;

    public ResourceAnnotation() {}
    
    public ResourceAnnotation(ResourceAnnotationKey key,String value) {
        setResourceAnnotationKey(key);
        setValue(value);
    }
    
    @ManyToOne(optional = false)
    private Resource resource;

    @ManyToOne(optional = false)
    // @IndexedEmbedded
    private ResourceAnnotationKey resourceAnnotationKey;

    @Lob
    @Type(type = "org.hibernate.type.StringClobType")
    @Field
    private String value;

    @Temporal(TemporalType.DATE)
    @Column(name = "date_created")
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated")
    private Date lastUpdated;

    public Resource getResource() {
        return resource;
    }

    @Field
    public String getPairedValue() {
        return getResourceAnnotationKey().getKey() + ":" + getValue();
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Transient
    public String getFormattedValue() {
        ResourceAnnotationDataType annotationDataType = resourceAnnotationKey.getAnnotationDataType();
        if (annotationDataType.isFormatString()) {
            // do format string stuff.
            return resourceAnnotationKey.format(value);
        }
        return value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ResourceAnnotationKey getResourceAnnotationKey() {
        return resourceAnnotationKey;
    }

    public void setResourceAnnotationKey(ResourceAnnotationKey resourceAnnotationKey) {
        this.resourceAnnotationKey = resourceAnnotationKey;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return String.format("[%s :: %s (%s)]", resourceAnnotationKey, value, hashCode());
    }

    public boolean isValid() {
        return resourceAnnotationKey != null
                && StringUtils.isNotEmpty(resourceAnnotationKey.getKey())
                && StringUtils.isNotEmpty(value);
    }

}
