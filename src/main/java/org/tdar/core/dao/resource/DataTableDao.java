package org.tdar.core.dao.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Query;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.datatable.DataTable;
import org.tdar.core.dao.Dao;
import org.tdar.core.dao.integration.DatasetSearchFilter;
import org.tdar.core.dao.resource.integration.DataTableProxy;
import org.tdar.core.dao.resource.integration.IntegrationDataTableSearchResult;

/**
 * $Id$
 * 
 * @author <a href='Yan.Qi@asu.edu'>Yan Qi</a>
 * @version $Revision$
 */
@Component
public class DataTableDao extends Dao.HibernateBase<DataTable> {

    public DataTableDao() {
        super(DataTable.class);
    }

    @SuppressWarnings("unchecked")
    public List<DataTable> findDataTablesUsingResource(Resource resource) {
        if (resource == null) {
            return Collections.emptyList();
        }
        Query query = getCurrentSession().getNamedQuery(QUERY_DATATABLE_RELATED_ID);
        getLogger().trace("Searching for linked resources to {}", resource.getId());
        query.setLong("relatedId", resource.getId());
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public IntegrationDataTableSearchResult findDataTables(DatasetSearchFilter searchFilter) throws IOException {

        // FIXME: rewrite query to run twice, once for total count, and once for the paginated data
        Query query = getCurrentSession().getNamedQuery(QUERY_INTEGRATION_DATA_TABLE);
        query.setProperties(searchFilter);
        query.setMaxResults(searchFilter.getMaxResults());
        query.setFirstResult(searchFilter.getFirstResult());
        query.setReadOnly(true);
        List<DataTableProxy> proxies = new ArrayList<>();
        for (Object[] obj_ : (List<Object[]>) query.list()) {
            DataTable dataTable = (DataTable) obj_[0];
            proxies.add(new DataTableProxy(dataTable));
        }
        IntegrationDataTableSearchResult result = new IntegrationDataTableSearchResult();
        result.getDataTables().addAll(proxies);
        return result;
    }

}
