package org.tdar.dataone.dao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.ObjectList;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tdar.core.dao.GenericDao;
import org.tdar.dataone.bean.EntryType;
import org.tdar.dataone.bean.ListObjectEntry;
import org.tdar.dataone.bean.LogEntryImpl;

@Component
public class DataOneDao {

    private static final String LIST_OBJECT_QUERY = "select external_id as \"externalId\", 'D1'   as \"type\", id as \"id\", date_updated as \"dateUpdated\" from resource res where res.external_id is not null and (res.date_updated between :start and :end or res.date_created between :start and :end) and res.status='ACTIVE' and res.resource_type!='PROJECT' and (:identifier is null or res.external_id=:identifier) and (:type is null or   'D1'=:type) union " +
            "select external_id as \"externalId\", 'TDAR' as \"type\", id as \"id\", date_updated as \"dateUpdated\" from resource res where res.external_id is not null and (res.date_updated between :start and :end or res.date_created between :start and :end) and res.status='ACTIVE' and res.resource_type!='PROJECT' and (:identifier is null or res.external_id=:identifier) and (:type is null or 'TDAR'=:type)";

    private static final String LIST_OBJECT_QUERY_COUNT = "select 1 from resource res where res.external_id is not null and (res.date_updated between :start and :end or res.date_created between :start and :end) and res.status='ACTIVE' and res.resource_type!='PROJECT' and (:identifier is null or res.external_id=:identifier) and (:type is null or   'D1'=:type) union " +
            "select 1 from resource res where res.external_id is not null and (res.date_updated between :start and :end or res.date_created between :start and :end) and res.status='ACTIVE' and res.resource_type!='PROJECT' and (:identifier is null or res.external_id=:identifier) and (:type is null or 'TDAR'=:type)";

    @Transient
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private GenericDao genericDao;

    public List<ListObjectEntry> findUpdatedResourcesWithDOIs(Date start, Date end, String formatId, String identifier, ObjectList list) {
        SQLQuery query = setupListObjectQuery(LIST_OBJECT_QUERY_COUNT, start, end, formatId, identifier);

        list.setTotal(query.list().size());
        if (list.getCount() == 0) {
            return new ArrayList<>();
        }

        query = setupListObjectQuery(LIST_OBJECT_QUERY, start, end, formatId, identifier);
        query.setMaxResults(list.getCount());
        query.setFirstResult(list.getStart());
        List<ListObjectEntry> toReturn = new ArrayList<>();
        for (Object wrap : query.list()) {
            Object[] obj = (Object[])wrap;
            toReturn.add(new ListObjectEntry((String)obj[0], (String)obj[1], ((BigInteger)obj[2]).longValue(), (Date)obj[3],null,null,null,null));
        }
        return toReturn;
    }

    private SQLQuery setupListObjectQuery(String sqlQuery, Date fromDate, Date toDate, String formatId, String identifier) {
        SQLQuery query = genericDao.getNativeQuery(sqlQuery);
        
        // if Tier3, use "query.dataone_list_objects_t3"
        initStartEnd(fromDate, toDate, query);
        EntryType type = null;
        if (StringUtils.isNotBlank(formatId)) {
            type = EntryType.getTypeFromFormatId(formatId);
        }
        
        if (type != null) {
            query.setString("type", type.name());
        } else {
            query.setString("type", null);
        }
                
        query.setString("identifier", identifier);
        return query;
    }

    @SuppressWarnings("unchecked")
    public List<LogEntryImpl> findLogFiles(Date fromDate, Date toDate, Event event, String idFilter, int start, int count, Log log) {
        Query query = setupQuery(fromDate, toDate, event, idFilter);
        // FIXME: find better way to handle pagination
        log.setTotal(query.list().size());
        if (count == 0) {
            return new ArrayList<>();
        }

        query = setupQuery(fromDate, toDate, event, idFilter);

        query.setMaxResults(log.getCount());
        query.setFirstResult(log.getStart());
        return query.list();

    }

    private Query setupQuery(Date fromDate, Date toDate, Event event, String idFilter) {
        Query query = genericDao.getNamedQuery("query.dataone_list_logs");
        initStartEnd(fromDate, toDate, query);
        if (event != null) {
            query.setString("event", event.name());
        } else {
            query.setString("event", null);
        }
        query.setString("idFilter", idFilter);
        return query;
    }

    private void initStartEnd(Date fromDate, Date toDate, Query query) {
        Date to = DateTime.now().toDate();
        Date from = new DateTime(1900).toDate();
        if (fromDate != null) {
            from = fromDate;
        }
        if (toDate != null) {
            toDate = to;
        }
        query.setParameter("start", from);
        query.setParameter("end", to);
    }

}
