package org.tdar.struts.action;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.resource.Dataset;
import org.tdar.core.bean.resource.datatable.DataTable;
import org.tdar.core.service.XmlService;
import org.tdar.core.service.resource.DatasetService;
import org.tdar.struts.data.ResultMetadataWrapper;

@ParentPackage("secured")
@Component
@Scope("prototype")
@Namespace("/datatable")
public class DataTableBrowseController extends AuthenticationAware.Base {

    private static final long serialVersionUID = -2570627983412022974L;

    private Long id;
    private Integer recordsPerPage = 50;
    private Integer startRecord = 0;
    private List<List<String>> results = Collections.emptyList();
    private String callback;
    private int totalRecords;
    private ResultMetadataWrapper resultsWrapper = new ResultMetadataWrapper();
    private InputStream jsonResult;

    @Autowired
    private transient DatasetService datasetService;
    
    @Autowired
    private transient XmlService xmlService;
    
    @Action(value = "browse",
            interceptorRefs = { @InterceptorRef("unauthenticatedStack") }, results = { @Result(name = SUCCESS, type = "stream",
            params = {
            "contentType", "application/json",
            "inputName", "jsonResult"
    })})
    public String getDataResults() {
        if (Persistable.Base.isNullOrTransient(id)) {
            return ERROR;
        }
        DataTable dataTable = getGenericService().find(DataTable.class, getId());
        Dataset dataset = dataTable.getDataset();
        if (dataset.isPublicallyAccessible() || getAuthenticationAndAuthorizationService().canViewConfidentialInformation(getAuthenticatedUser(), dataset)) {
            ResultMetadataWrapper selectAllFromDataTable = ResultMetadataWrapper.NULL;
            try {
                selectAllFromDataTable = datasetService.selectAllFromDataTable(dataTable, getStartRecord(), getRecordsPerPage(), true,
                        isViewRowSupported());
            } catch (BadSqlGrammarException ex) {
                getLogger().error("Failed to pull datatable results for '{}' (perhaps the table is missing from tdardata schema?)", dataTable.getName());
            }
            setResultsWrapper(selectAllFromDataTable);
            setResults(getResultsWrapper().getResults());
        }
        setJsonResult(new ByteArrayInputStream(xmlService.convertFilteredJsonForStream(getResultsWrapper(), null, getCallback()).getBytes()));
        return SUCCESS;
    }

    public List<List<String>> getResults() {
        return results;
    }

    public void setResults(List<List<String>> results) {
        this.results = results;
    }

    public Integer getStartRecord() {
        return startRecord;
    }

    public void setStartRecord(Integer startRecord) {
        this.startRecord = startRecord;
    }

    public Integer getRecordsPerPage() {
        return recordsPerPage;
    }

    public void setRecordsPerPage(Integer recordsPerPage) {
        this.recordsPerPage = recordsPerPage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ResultMetadataWrapper getResultsWrapper() {
        return resultsWrapper;
    }

    public void setResultsWrapper(ResultMetadataWrapper resultsWrapper) {
        this.resultsWrapper = resultsWrapper;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public InputStream getJsonResult() {
        return jsonResult;
    }

    public void setJsonResult(InputStream jsonResult) {
        this.jsonResult = jsonResult;
    }

}
