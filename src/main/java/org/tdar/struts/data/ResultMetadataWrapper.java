package org.tdar.struts.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.tdar.core.bean.JsonModel;
import org.tdar.core.bean.resource.datatable.DataTableColumn;

public class ResultMetadataWrapper extends JsonModel.Base {

    private static final long serialVersionUID = 1524243095172930161L;

    private Integer recordsPerPage = 50;
    private Integer startRecord = 0;
    private Integer totalRecords = 0;
    private List<List<String>> results = Collections.emptyList();
    private List<DataTableColumn> fields = Collections.emptyList();
    private String sColumns;

    public static final ResultMetadataWrapper NULL = new ResultMetadataWrapper() {
        private static final long serialVersionUID = 6430529822235933806L;
    };

    public Integer getRecordsPerPage() {
        return recordsPerPage;
    }

    public void setRecordsPerPage(Integer recordsPerPage) {
        this.recordsPerPage = recordsPerPage;
    }

    public Integer getStartRecord() {
        return startRecord;
    }

    public void setStartRecord(Integer startRecord) {
        this.startRecord = startRecord;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    @Override
    protected String[] getIncludedJsonProperties() {
        return new String[] { "sColumns", "name", "displayName", "columnEncodingType", "startRecord", "recordsPerPage", "fields", "totalRecords", "results" };
    }

    public List<List<String>> getResults() {
        return results;
    }

    public void setResults(List<List<String>> results) {
        this.results = results;
    }

    public List<DataTableColumn> getFields() {
        return fields;
    }

    public String getSColumns() {
        if (StringUtils.isEmpty(sColumns)) {
            List<String> tmp = new ArrayList<String>();
            for (DataTableColumn field : fields) {
                // technically this replace should never be needed, but...
                tmp.add(field.getJsSimpleName());
            }
            sColumns = StringUtils.join(tmp, ',');
        }
        return sColumns;
    }

    public void setFields(List<DataTableColumn> fields) {
        this.fields = fields;
    }

    public String getsColumns() {
        return getSColumns();
    }
}
