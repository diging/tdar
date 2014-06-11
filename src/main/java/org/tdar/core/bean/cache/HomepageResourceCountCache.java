package org.tdar.core.bean.cache;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.apache.commons.lang.ObjectUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.resource.ResourceType;

/**
 * This caches the counts of resource types for the homepage.
 * 
 * @author abrin
 * 
 */
@Entity
@Table(name = "homepage_cache_resource_type")
@Cacheable
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL, region="org.tdar.core.bean.cache.HomepageResourceCountCache")
public class HomepageResourceCountCache extends Persistable.Base implements Comparable<HomepageResourceCountCache>, ResourceCache<ResourceType> {

    private static final long serialVersionUID = 4401314235170180736L;

    @Column(name = "resource_count")
    private Long count;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, unique = true)
    private ResourceType resourceType;

    public HomepageResourceCountCache() {

    }

    public HomepageResourceCountCache(ResourceType resourceType, Long count) {
        this.setResourceType(resourceType);
        this.count = count;
    }

    @Override
    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    @Override
    public Double getLogCount() {
        return Math.log(getCount());
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public int compareTo(HomepageResourceCountCache o) {
        return ObjectUtils.compare(getResourceType(), o.getResourceType());
    }

    @Override
    public String getLabel() {
        return getResourceType().getPlural();
    }

    @Override
    public ResourceType getKey() {
        return getResourceType();
    }

    @Override
    public String getCssId() {
        return this.getKey().name();
    }
}
