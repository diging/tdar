package org.tdar.core.service;

import java.util.Map;
import java.util.Set;

import org.tdar.core.bean.resource.datatable.DataTable;
import org.tdar.odata.server.AbstractDataRecord;

/**
 * Abstract out the backend databased connection for tdardata for use by the MockDaos
 * 
 * @author
 * 
 */
public interface RowOperations {

    void editRow(DataTable dataTable, Long rowId, Map<?, ?> data);

    Set<AbstractDataRecord> findAllRows(DataTable dataTable);

    void deleteRow(DataTable dataTable, Long rowId);

}
