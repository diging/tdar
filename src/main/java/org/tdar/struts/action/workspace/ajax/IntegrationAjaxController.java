package org.tdar.struts.action.workspace.ajax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.resource.CategoryVariable;
import org.tdar.core.bean.resource.Dataset;
import org.tdar.core.bean.resource.Ontology;
import org.tdar.core.bean.resource.OntologyNode;
import org.tdar.core.bean.resource.datatable.DataTable;
import org.tdar.core.bean.resource.datatable.DataTableColumn;
import org.tdar.core.dao.integration.DatasetIntegrationSearchFilter;
import org.tdar.core.dao.integration.OntologyIntegrationSearchFilter;
import org.tdar.core.service.DataIntegrationService;
import org.tdar.core.service.XmlService;
import org.tdar.core.service.resource.DataTableService;
import org.tdar.core.service.resource.OntologyService;
import org.tdar.search.query.SimpleSearchResultHandler;
import org.tdar.search.query.SortOption;
import org.tdar.struts.action.AuthenticationAware;
import org.tdar.struts.action.TdarActionSupport;
import org.tdar.struts.data.intgration.IntegrationColumn;
import org.tdar.struts.data.intgration.IntegrationColumn.ColumnType;

import com.opensymphony.xwork2.Preparable;

/**
 * Created by jimdevos on 10/28/14.
 */
@ParentPackage("secured")
@Namespace("/workspace/ajax")
@Component
@Scope("prototype")
@Results(value = {
        @Result(name = TdarActionSupport.SUCCESS, type = TdarActionSupport.JSONRESULT, params = { "stream", "jsonInputStream" })
})
public class IntegrationAjaxController extends AuthenticationAware.Base implements Preparable, SimpleSearchResultHandler {

    private static final long serialVersionUID = 7550182111626753594L;
    private Long ontologyId;


    //minimize LOC so people don't notice this hack
    class SimpleMap extends HashMap<String, Object> {}
    class SimpleMapList extends ArrayList<HashMap<String, Object>>{
    }

    private OntologyIntegrationSearchFilter ontologyFilter;
    private DatasetIntegrationSearchFilter datasetFilter;
    private InputStream jsonInputStream;
    private Integer startRecord = 0;
    private int recordsPerPage = 10;

    @Autowired
    private transient OntologyService ontologyService;
    @Autowired
    private transient DataIntegrationService integrationService;
    @Autowired
    private transient DataTableService dataTableService;
    @Autowired
    private transient XmlService xmlService;

    private List<Map<String, Object>> results = new ArrayList<>();
    private List<Long> ontologyIds = new ArrayList<>();
    private List<Long> dataTableColumnsIds = new ArrayList<>();
    private List<Long> dataTableIds = new ArrayList<>();

    private ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public void prepare() {
        integrationService.hydrateFilter(ontologyFilter, getAuthenticatedUser());
        integrationService.hydrateFilter(datasetFilter, getAuthenticatedUser());
    }

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Action(value = "find-datasets")
    public String findDatasets() throws IOException {
        if (datasetFilter == null) {
            datasetFilter = new DatasetIntegrationSearchFilter();
        }

        List<DataTable> findDataTables = dataTableService.findDataTables(datasetFilter, startRecord, recordsPerPage);
        for (DataTable result : findDataTables) {
            results.add(setupDatableForJson(result));
        }
        setJsonInputStream(new ByteArrayInputStream(xmlService.convertToJson(results).getBytes()));
        return SUCCESS;
    }

    @Action(value = "get-shared-ontologies")
    public String findSharedOntologies() throws IOException {
        results.addAll(serializeSharedOntologies());
        setJsonInputStream(new ByteArrayInputStream(xmlService.convertToJson(results).getBytes()));
        return SUCCESS;
    }

    private List<Map<String, Object>> serializeSharedOntologies() {
        List<DataTable> dataTables = getGenericService().findAll(DataTable.class, dataTableIds);
        List<Map<String, Object>> shared = new ArrayList<>();

        Map<Ontology, List<DataTable>> suggestions = integrationService.getIntegrationSuggestions(dataTables, true);
        for (Ontology ontology : suggestions.keySet()) {
            Map<String, Object> ont = new HashMap<>();
            ont.put("name", ontology.getTitle());
            ont.put("id", ontology.getId());
            ont.put("nodes", setupOntologyNodesForJson(ontology));
            shared.add(ont);
        }
        return shared;
    }

    private HashMap<String, Object> setupDatableForJson(DataTable result) {
        HashMap<String, Object> map = new HashMap<>();
        Dataset dataset = result.getDataset();
        map.put("dataset_id", dataset.getId());
        map.put("data_table_id", result.getId());
        map.put("data_table_name", result.getName());
        HashMap<String, Object> ds = new HashMap<>();
        ds.put("submitter", dataset.getSubmitter());
        map.put("dataset", ds);

        map.put("dataset_name", dataset.getTitle());
        map.put("dataset_submitter", dataset.getSubmitter().getProperName());
        map.put("dataset_date_created", formatter.format(dataset.getDateCreated()));
        map.put("integratable", dataset.getIntegratableOptions().getBooleanValue());
        return map;
    }

    @Action(value = "find-ontologies")
    public String findOntologies() throws IOException {
        if (ontologyFilter == null) {
            ontologyFilter = new OntologyIntegrationSearchFilter();
        }

        List<Ontology> ontologies = ontologyService.findOntologies(ontologyFilter, startRecord, recordsPerPage);
        for (Ontology ontology : ontologies) {
            HashMap<String, Object> map = setupOntologyForJson(ontology);
            results.add(map);
        }
        setJsonInputStream(new ByteArrayInputStream(xmlService.convertToJson(results).getBytes()));
        return SUCCESS;
    }

    private HashMap<String, Object> setupOntologyForJson(Ontology ontology) {
        HashMap<String, Object> map = new HashMap<>();
        CategoryVariable category = ontology.getCategoryVariable();
        map.put("id", ontology.getId());
        map.put("title", ontology.getTitle());
        map.put("category_variable_id", category.getId());
        map.put("category_variable_name", category.getName());
        map.put("ontology_name", ontology.getTitle());
        map.put("ontology_submitter", ontology.getSubmitter().getProperName());
        map.put("ontology_date_created", formatter.format(ontology.getDateCreated()));
        return map;
    }

    ArrayNode setupOntologyNodesForJson(Ontology ontology) {
        Long ontologyId = ontology.getId();
        ArrayNode jsArray = objectMapper.createArrayNode();
        for(OntologyNode node : ontology.getSortedOntologyNodesByImportOrder()) {
            ObjectNode jsObject = objectMapper.createObjectNode()
                    .put("id", node.getId())
                    .put("ontology_id", ontologyId)
                    .put("description", node.getDescription())
                    .put("display_name", node.getDisplayName())
                    .put("index", node.getIndex())
                    .put("interval_start", node.getIntervalStart())
                    .put("interval_end", node.getIntervalEnd())
                    .put("import_order", node.getImportOrder());
            jsArray.add(jsObject);
        }
        return jsArray;
    }

    @Action(value = "table-details")
    public String dataTableDetails() throws IOException {
        HashMap<String, Object> values = new HashMap<>();
        values.put("dataTables", serializeDataTables());
        values.put("sharedOntologies", serializeSharedOntologies());
        results.add(values);
        setJsonInputStream(new ByteArrayInputStream(xmlService.convertToJson(results).getBytes()));
        return SUCCESS;
    }

    private List<HashMap<String, Object>> serializeDataTables() {
        List<HashMap<String, Object>> tables = new ArrayList<>();
        for (DataTable dataTable : dataTableService.findAll(getDataTableIds())) {
            HashMap<String, Object> map = setupDatableForJson(dataTable);
            tables.add(map);
            ArrayList<Map<String, Object>> columns = new ArrayList<Map<String, Object>>();
            map.put("columns", columns);
            for (DataTableColumn col : dataTable.getSortedDataTableColumns()) {
                HashMap<String, Object> colMap = new HashMap<>();
                columns.add(colMap);
                colMap.put("id", col.getId());
                colMap.put("sequence_number", col.getSequenceNumber());
                colMap.put("column_data_type", col.getColumnDataType());
                colMap.put("column_encoding_type", col.getColumnEncodingType());
                colMap.put("name", col.getName());
                colMap.put("display_name", col.getDisplayName());
                colMap.put("description", col.getDescription());
                if (col.getDefaultCodingSheet() != null) {
                    colMap.put("default_coding_sheet_id", col.getDefaultCodingSheet().getId());
                }
                if (col.getDefaultOntology() != null) {
                    colMap.put("default_ontology_id", col.getDefaultOntology().getId());
                }
            }
        }
        return tables;
    }

    @Action(value = "ontology-details")
    public String ontologyDetails() throws IOException {
        Ontology ontology = ontologyService.find(getOntologyId());
        HashMap<String, Object> map = setupOntologyForJson(ontology);
        List<DataTableColumn> columns = getGenericService().findAll(DataTableColumn.class, getDataTableColumnsIds());
        // rehydrate all of the resources being passed in, we just had empty beans with ids
        IntegrationColumn intColumn = new IntegrationColumn(ColumnType.INTEGRATION, columns.toArray(new DataTableColumn[0]));

        // for each DataTableColumn, grab the shared ontology if it exists; setup mappings
        for (DataTableColumn column : columns) {
            getLogger().info("{} ({})", column, column.getDefaultOntology());
            getLogger().info("{} ({})", column, column.getDefaultCodingSheet());
            integrationService.updateMappedCodingRules(column);
        }
        ArrayList<Map<String, Object>> nodeList = new ArrayList<Map<String, Object>>();
        map.put("nodes", nodeList);

        for (OntologyNode node : intColumn.getFlattenedOntologyNodeList()) {
            Map<String, Object> nodeMap = new HashMap<>();
            nodeList.add(nodeMap);
            nodeMap.put("id", node.getId());
            nodeMap.put("displayName", node.getDisplayName());
            nodeMap.put("index", node.getIndex());
            nodeMap.put("indented_label", node.getIndentedLabel());
            nodeMap.put("iri", node.getIri());
            nodeMap.put("interval_start", node.getIntervalStart());
            nodeMap.put("interval_end", node.getIntervalEnd());
            nodeMap.put("import_order", node.getImportOrder());
            nodeMap.put("mapping_list", node.getColumnHasValueArray());
        }
        setJsonInputStream(new ByteArrayInputStream(xmlService.convertToJson(results).getBytes()));
        return SUCCESS;
    }

    @Action(value = "ontology-list-details")
    public String ontologyListDetails() throws IOException {
        List<Ontology> ontologies = ontologyService.findAll(getOntologyIds());
        SimpleMapList maplist= new SimpleMapList();
        getLogger().debug("ontologies found: {}", getOntologyIds());
        for(Ontology ontology : ontologies) {
            getLogger().debug("grabbing detail for ontology: {}", ontology);
            HashMap<String, Object> map = setupOntologyForJson(ontology);
            List<DataTableColumn> columns = getGenericService().findAll(DataTableColumn.class, getDataTableColumnsIds());

            //FIXME: let caller get ontology details even if caller does not provide datatable information.
            // rehydrate all of the resources being passed in, we just had empty beans with ids
            IntegrationColumn intColumn = new IntegrationColumn(ColumnType.INTEGRATION, columns.toArray(new DataTableColumn[0]));

            // for each DataTableColumn, grab the shared ontology if it exists; setup mappings
            for (DataTableColumn column : columns) {
                getLogger().info("{} ({})", column, column.getDefaultOntology());
                getLogger().info("{} ({})", column, column.getDefaultCodingSheet());
                integrationService.updateMappedCodingRules(column);
            }
            ArrayList<Map<String, Object>> nodeList = new ArrayList<Map<String, Object>>();
            map.put("nodes", nodeList);

            for (OntologyNode node : intColumn.getFlattenedOntologyNodeList()) {
                Map<String, Object> nodeMap = new HashMap<>();
                nodeList.add(nodeMap);
                nodeMap.put("id", node.getId());
                nodeMap.put("displayName", node.getDisplayName());
                nodeMap.put("index", node.getIndex());
                nodeMap.put("indented_label", node.getIndentedLabel());
                nodeMap.put("iri", node.getIri());
                nodeMap.put("interval_start", node.getIntervalStart());
                nodeMap.put("interval_end", node.getIntervalEnd());
                nodeMap.put("import_order", node.getImportOrder());
                nodeMap.put("mapping_list", node.getColumnHasValueArray());
            }
            maplist.add(map);
        }

        results.clear();
        results.addAll(maplist);
        setJsonInputStream(new ByteArrayInputStream(xmlService.convertToJson(results).getBytes()));
        return SUCCESS;
    }



    @Override
    public SortOption getSortField() {
        return null;
    }

    @Override
    public void setSortField(SortOption sortField) {
    }

    @Override
    public int getStartRecord() {
        return startRecord;
    }

    @Override
    public void setStartRecord(int startRecord) {
        this.startRecord = startRecord;
    }

    @Override
    public int getRecordsPerPage() {
        return recordsPerPage;
    }

    @Override
    public void setRecordsPerPage(int recordsPerPage) {
        this.recordsPerPage = recordsPerPage;

    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public OntologyIntegrationSearchFilter getOntologyFilter() {
        return ontologyFilter;
    }

    public void setOntologyFilter(OntologyIntegrationSearchFilter ontologyFilter) {
        this.ontologyFilter = ontologyFilter;
    }

    public DatasetIntegrationSearchFilter getDatasetFilter() {
        return datasetFilter;
    }

    public void setDatasetFilter(DatasetIntegrationSearchFilter datasetFilter) {
        this.datasetFilter = datasetFilter;
    }

    public InputStream getJsonInputStream() {
        return jsonInputStream;
    }

    public void setJsonInputStream(InputStream jsonInputStream) {
        this.jsonInputStream = jsonInputStream;
    }

    public List<Long> getDataTableColumnsIds() {
        return dataTableColumnsIds;
    }

    public void setDataTableColumnsIds(List<Long> dataTableColumnsIds) {
        this.dataTableColumnsIds = dataTableColumnsIds;
    }

    public List<Long> getOntologyIds() {
        return ontologyIds;
    }

    public void setOntologyIds(List<Long> ontologyIds) {
        this.ontologyIds = ontologyIds;
    }

    public List<Long> getDataTableIds() {
        return dataTableIds;
    }

    public void setDataTableIds(List<Long> dataTableIds) {
        this.dataTableIds = dataTableIds;
    }

    public Long getOntologyId() {
        return ontologyId;
    }

    public void setOntologyId(Long ontologyId) {
        this.ontologyId = ontologyId;
    }

}
