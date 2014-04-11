package org.tdar.core.dao.resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Query;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.resource.BookmarkedResource;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.dao.Dao;

/**
 * $Id$
 * 
 * <p>
 * Provides hibernate DAO access to BookmarkedResources.
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
@Component
public class BookmarkedResourceDao extends Dao.HibernateBase<BookmarkedResource> {

    public BookmarkedResourceDao() {
        super(BookmarkedResource.class);
    }

    /**
     * Returns true if this resource has been bookmarked by this person, false otherwise.
     * 
     * @param resource
     * @param person
     * @return
     */
    public boolean isAlreadyBookmarked(Resource resource, Person person) {
        return findBookmark(resource, person) != null;
    }

    public BookmarkedResource findBookmark(Resource resource, Person person) {
        if ((resource == null) || (person == null)) {
            return null;
        }
        Query query = getCurrentSession().getNamedQuery(QUERY_BOOKMARKEDRESOURCE_IS_ALREADY_BOOKMARKED);
        query.setLong("resourceId", resource.getId());
        query.setLong("personId", person.getId());
        return (BookmarkedResource) query.uniqueResult();
    }

    public void removeBookmark(Resource resource, Person person) {
        if ((resource == null) || (person == null)) {
            return;
        }
        Query query = getCurrentSession().getNamedQuery(QUERY_BOOKMARKEDRESOURCE_REMOVE_BOOKMARK);
        query.setLong("resourceId", resource.getId());
        query.setLong("personId", person.getId());
        query.executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<Resource> findBookmarkedResourcesByPerson(Person person, List<Status> statuses) {
        if (CollectionUtils.isEmpty(statuses)) {
            statuses = Arrays.asList(Status.ACTIVE, Status.DRAFT);
        }
        if (person == null) {
            return Collections.emptyList();
        }
        Query query = getCurrentSession().getNamedQuery(QUERY_BOOKMARKEDRESOURCE_FIND_RESOURCE_BY_PERSON);
        query.setParameterList("statuses", statuses);
        query.setLong("personId", person.getId());
        return query.list();
    }
}
