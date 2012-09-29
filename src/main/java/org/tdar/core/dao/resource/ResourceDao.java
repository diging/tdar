package org.tdar.core.dao.resource;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.keyword.GeographicKeyword;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.dao.Dao;
import org.tdar.core.dao.NamedNativeQueries;
import org.tdar.core.dao.TdarNamedQueries;
import org.tdar.core.service.CrowdService;
import org.tdar.utils.Pair;

/**
 * $Id$
 * 
 * Base class for resource DAOs providing basic query functionalities for
 * Resource metadata.
 * 
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Rev: 494$
 * @param <E>
 */
public abstract class ResourceDao<E extends Resource> extends Dao.HibernateBase<E> {
    @Autowired
    private CrowdService crowdService;

    public ResourceDao(Class<E> resourceClass) {
        super(resourceClass);
    }

    public List<E> findByTitle(final String title) {
        DetachedCriteria criteria = getOrderedDetachedCriteria();
        criteria.add(Restrictions.eq("title", title));
        return findByCriteria(criteria);
    }

    /**
     * FIXME: replace with HQL if possible
     * 
     * @param submitter
     * @return
     */
    public List<E> findBySubmitter(Person submitter) {
        DetachedCriteria criteria = getOrderedDetachedCriteria();
        if (submitter == null) {
            return Collections.emptyList();
        }
        SimpleExpression eq = Restrictions.eq("submitter.id", submitter.getId());

        if (!crowdService.isAdministrator(submitter)) {
            criteria.add(Restrictions.and(eq, Restrictions.or(
                    Restrictions.eq("status", Status.ACTIVE), Restrictions.eq("status", Status.DRAFT))));
        } else {
            criteria.add(eq);
        }
        return findByCriteria(criteria);
    }

    /*
     * abrin 2010-08-20 FIXME: made changes to enable finding all resources that
     * are available to a user. This works for CodingSheets and for Ontologies,
     * but will not properly work for others because it does not take into
     * account the rights associated by the fullUser table. This is not an issue
     * for the above types because they cannot set permissions to 'confidential'
     * and therefore assigning rights does not matter.
     */
    @SuppressWarnings("unchecked")
    public List<E> findSparseResourceBySubmitterType(Person submitter, ResourceType resourceType) {
        Query query = getCurrentSession().getNamedQuery(QUERY_READ_ONLY_SUBMITTER_RESOURCES);
        query.setLong("submitter", submitter.getId());
        query.setString("resourceType", resourceType.name());
        query.setString("status", Status.ACTIVE.name());
        query.setReadOnly(true);
        return query.list();
    }

    public boolean hasBeenModifiedSince(Resource resource, Date date) {
        Query query = getCurrentSession().getNamedQuery(QUERY_RESOURCE_MODIFIED_SINCE);
        query.setLong("id", resource.getId());
        query.setDate("date", date);
        int numFound = ((Integer) query.iterate().next()).intValue();
        return numFound > 0;
    }

    @Override
    protected String getDefaultOrderingProperty() {
        return "title";
    }

    public ResourceType findResourceType(Number id) {
        Query query = getCurrentSession().getNamedQuery(QUERY_RESOURCE_RESOURCETYPE);
        query.setLong("id", id.longValue());
        return (ResourceType) query.uniqueResult();
    }

    public void incrementAccessCounter(Resource r) {
        Query query = getCurrentSession().createSQLQuery(NamedNativeQueries.incrementAccessCount(r));
        query.executeUpdate();
    }

    public Number countActiveResources(Class<?> resourceClass) {
        Query query = getCurrentSession().createQuery(String.format(TdarNamedQueries.QUERY_SQL_COUNT_ACTIVE_RESOURCE, resourceClass.getSimpleName()));
        return (Number) query.uniqueResult();
    }

    public Map<GeographicKeyword, Long> getISOGeographicCounts() {
        logger.info("executing country count from database");
        Map<GeographicKeyword, Long> countryCount = new HashMap<GeographicKeyword, Long>();
        Query query = getCurrentSession().getNamedQuery(QUERY_MANAGED_ISO_COUNTRIES);
        for (Object o : query.list()) {
            try {
                Object[] objs = (Object[]) o;
                if (objs == null || objs[0] == null)
                    continue;
                countryCount.put((GeographicKeyword) objs[0], (Long) objs[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return countryCount;
    }

    /**
     * @return
     */
    public Map<ResourceType, Pair<Long, Double>> getResourceCounts() {
        logger.info("executing resource count from database");
        Map<ResourceType, Pair<Long, Double>> resourceTypeCounts = new HashMap<ResourceType, Pair<Long, Double>>();
        Query query = getCurrentSession().getNamedQuery(QUERY_ACTIVE_RESOURCE_TYPE_COUNT);

        // initialize with zeros
        for (ResourceType rt : ResourceType.values()) {
            resourceTypeCounts.put(rt, new Pair<Long, Double>(0l, 0.0));
        }

        for (Object o : query.list()) {
            try {
                Object[] objs = (Object[]) o;
                if (objs == null || objs[0] == null)
                    continue;
                resourceTypeCounts.put((ResourceType) objs[1], new Pair<Long,Double>((Long) objs[0], Math.log((Long) objs[0])));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resourceTypeCounts;
    }
}
