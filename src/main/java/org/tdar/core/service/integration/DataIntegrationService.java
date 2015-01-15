package org.tdar.core.service.integration;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.FileProxy;
import org.tdar.core.bean.PersonalFilestoreTicket;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.resource.CodingRule;
import org.tdar.core.bean.resource.CodingSheet;
import org.tdar.core.bean.resource.Ontology;
import org.tdar.core.bean.resource.OntologyNode;
import org.tdar.core.bean.resource.ResourceRevisionLog;
import org.tdar.core.bean.resource.VersionType;
import org.tdar.core.bean.resource.datatable.DataTable;
import org.tdar.core.bean.resource.datatable.DataTableColumn;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.GenericDao;
import org.tdar.core.dao.integration.IntegrationColumnPartProxy;
import org.tdar.core.dao.integration.TableDetailsProxy;
import org.tdar.core.dao.resource.DataTableColumnDao;
import org.tdar.core.dao.resource.OntologyNodeDao;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.service.ExcelService;
import org.tdar.core.service.PersonalFilestoreService;
import org.tdar.core.service.SerializationService;
import org.tdar.core.service.integration.dto.IntegrationDeserializationException;
import org.tdar.core.service.integration.dto.v1.IntegrationWorkflowData;
import org.tdar.core.service.resource.InformationResourceService;
import org.tdar.db.model.abstracts.TargetDatabase;
import org.tdar.filestore.personal.PersonalFilestore;
import org.tdar.utils.Pair;
import org.tdar.utils.PersistableUtils;

import au.com.bytecode.opencsv.CSVWriter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.opensymphony.xwork2.TextProvider;

/**
 * $Id$
 * 
 * Provides data integration functionality for datasets that have been translated and mapped to ontologies.
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Rev$
 */
@Service
public class DataIntegrationService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TargetDatabase tdarDataImportDatabase;

    @Autowired
    private GenericDao genericDao;

    @Autowired
    private DataTableColumnDao dataTableColumnDao;

    @Autowired
    private PersonalFilestoreService filestoreService;

    @Autowired
    private ExcelService excelService;

    @Autowired
    private InformationResourceService informationResourceService;

    @Autowired
    private OntologyNodeDao ontologyNodeDao;

    @Autowired
    private SerializationService serializationService;

    public void setTdarDataImportDatabase(TargetDatabase tdarDataImportDatabase) {
        this.tdarDataImportDatabase = tdarDataImportDatabase;
    }

    /**
     * sets transient booleans on CodingRule to mark that it's mapped
     * 
     * 
     * @param column
     */
    public void updateMappedCodingRules(DataTableColumn column) {
        if (column == null) {
            return;
        }
        logger.debug("col {}", column);
        CodingSheet codingSheet = column.getDefaultCodingSheet();
        if ((codingSheet == null) || CollectionUtils.isEmpty(codingSheet.getCodingRules())) {
            logger.debug("aborting, no coding rules or coding sheet {}", codingSheet);
            return;
        }
        logger.trace("selecting distinct values from column");
        List<String> values = tdarDataImportDatabase.selectDistinctValues(column);
        logger.trace("values: {} ", values);
        logger.info("matching coding rule terms to column values");
        for (CodingRule rule : codingSheet.getCodingRules()) {
            if (values.contains(rule.getTerm())) {
                logger.trace("mapping rule {} to column {}", rule, column);
                rule.setMappedToData(column);
            }
        }
    }

    private void hydrateIntegrationColumn(IntegrationColumn integrationColumn) {
        List<DataTableColumn> dataTableColumns = genericDao.loadFromSparseEntities(integrationColumn.getColumns(), DataTableColumn.class);
        dataTableColumns.removeAll(Collections.singletonList(null));
        integrationColumn.setColumns(dataTableColumns);
        List<OntologyNode> filteredOntologyNodes = integrationColumn.getFilteredOntologyNodes();
        if (CollectionUtils.isNotEmpty(filteredOntologyNodes)) {
            filteredOntologyNodes.removeAll(Collections.singletonList(null));
        }
        integrationColumn.setSharedOntology(genericDao.loadFromSparseEntity(integrationColumn.getSharedOntology(), Ontology.class));

        logger.debug("before: {} - {}", integrationColumn, filteredOntologyNodes);
        filteredOntologyNodes = genericDao.loadFromSparseEntities(filteredOntologyNodes, OntologyNode.class);
        integrationColumn.setFilteredOntologyNodes(filteredOntologyNodes);
        // for each of the integration columns, grab the unique set of all children within an ontology

        // that is, even if child is not selected, should get all children for query and pull up

        integrationColumn.buildNodeChildHierarchy(ontologyNodeDao);

        logger.debug("after: {} - {}", integrationColumn, filteredOntologyNodes);
        logger.info("integration column: {}", integrationColumn);
    }

    /**
     * Convert the integration context to XML for persistance in the @link PersonalFilestore and logging
     * 
     * @param integrationColumns
     * @param creator
     * @return
     * @throws Exception
     */
    public String serializeIntegrationContext(List<IntegrationColumn> integrationColumns, TdarUser creator) throws Exception {
        StringWriter sw = new StringWriter();
        serializationService.convertToXML(new IntegrationContext(creator, integrationColumns), sw);
        return sw.toString();
    }

    /**
     * For a specified @link DataTableColumn, find @link CodingRule entires mapped to the @link CodingSheet and Column that actually have data in the tdardata
     * database.
     * 
     * @param column
     * @return
     */
    @Transactional
    public List<CodingRule> findMappedCodingRules(DataTableColumn column) {
        List<String> distinctColumnValues = tdarDataImportDatabase.selectNonNullDistinctValues(column);
        return dataTableColumnDao.findMappedCodingRules(column, distinctColumnValues);
    }

    /**
     * @see #createGeneratedCodingSheet(DataTableColumn, Person, Ontology)
     * 
     * @param column
     * @param submitter
     * @return
     */
    @Transactional
    public CodingSheet createGeneratedCodingSheet(TextProvider provider, DataTableColumn column, TdarUser submitter) {
        return createGeneratedCodingSheet(provider, column, submitter, column.getDefaultOntology());
    }

    /**
     * When a user maps a @link DataTableColumn to an @link Ontology without a @link CodingSheet specifically chosen, create one on-the-fly from the @link
     * OntologyNode values.
     * 
     * @param column
     * @param submitter
     * @param ontology
     * @return
     */
    @Transactional
    public CodingSheet createGeneratedCodingSheet(TextProvider provider, DataTableColumn column, TdarUser submitter, Ontology ontology) {
        if (column == null) {
            logger.debug("{} tried to create an identity coding sheet for {} with no values", submitter, column);
        }
        CodingSheet codingSheet = new CodingSheet();
        codingSheet.setGenerated(true);
        codingSheet.setAccount(column.getDataTable().getDataset().getAccount());
        codingSheet.setTitle(provider.getText("dataIntegrationService.generated_coding_sheet_title", Arrays.asList(column.getDisplayName(), column.getDataTable().getDataset())));
        codingSheet.markUpdated(submitter);
        codingSheet.setDate(Calendar.getInstance().get(Calendar.YEAR));
        codingSheet.setDefaultOntology(ontology);
        codingSheet.setCategoryVariable(ontology.getCategoryVariable());
        codingSheet.setDescription(provider.getText(
                "dataIntegrationService.generated_coding_sheet_description",
                Arrays.asList(TdarConfiguration.getInstance().getSiteAcronym(), column, column.getDataTable().getDataset().getTitle(),
                        column.getDataTable().getDataset().getId(), codingSheet.getDateCreated())));
        genericDao.save(codingSheet);
        // generate identity coding rules
        List<String> dataColumnValues = tdarDataImportDatabase.selectNonNullDistinctValues(column);
        Set<CodingRule> rules = new HashSet<>();
        for (int index = 0; index < dataColumnValues.size(); index++) {
            String dataValue = dataColumnValues.get(index);
            CodingRule rule = new CodingRule(codingSheet, dataValue);
            rules.add(rule);
        }
        genericDao.save(rules);
        try {
            String baseFileName = codingSheet.getTitle().replace(" ", "_");
            String csvText = convertCodingSheetToCSV(codingSheet, rules);
            FileProxy fileProxy = new FileProxy(baseFileName + ".csv", FileProxy.createTempFileFromString(csvText), VersionType.UPLOADED);
            fileProxy.addVersion(new FileProxy(baseFileName + ".txt", FileProxy.createTempFileFromString(csvText), VersionType.UPLOADED_TEXT));

            informationResourceService.processMetadataForFileProxies(codingSheet, fileProxy);
        } catch (Exception e) {
            logger.debug("could not process coding sheet", e);
        }

        return codingSheet;
    }

    /**
     * @see #convertCodingSheetToCSV(CodingSheet, Collection)
     * 
     * @param sheet
     * @return
     */
    public String convertCodingSheetToCSV(CodingSheet sheet) {
        return convertCodingSheetToCSV(sheet, sheet.getCodingRules());
    }

    /**
     * Given a @link CodingSheet and a set of @link CodingRule entries, create a CSV File
     * 
     * @param sheet
     * @param rules
     * @return
     */
    public String convertCodingSheetToCSV(CodingSheet sheet, Collection<CodingRule> rules) {
        // not all coding sheets have their rules directly attached at the moment (eg the generated ones)
        StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw);
        for (CodingRule rule : rules) {
            writer.writeNext(new String[] { rule.getCode(), rule.getTerm(), rule.getDescription() });
        }
        IOUtils.closeQuietly(writer);
        return sw.toString();
    }

    /**
     * Iterate over every {@link DataTableColumn} in every {@link DataTable} and find ones that have shared {@link Ontology} entries. Return those back in Lists
     * of Lists.
     * 
     * @param selectedDataTables
     * @return
     */
    public List<List<DataTableColumn>> getIntegrationColumnSuggestions(Collection<DataTable> selectedDataTables) {
        // iterate through all of the columns and get a map of the ones associated
        // with any ontology.
        HashMap<Ontology, List<DataTableColumn>> dataTableAutoMap = new HashMap<>();

        for (DataTable table : selectedDataTables) {
            List<DataTableColumn> dataTableColumns;

            // FIXME: not sure if this is correct
            if (TdarConfiguration.getInstance().getLeftJoinDataIntegrationFeatureEnabled()) {
                dataTableColumns = table.getLeftJoinColumns();
            } else {
                dataTableColumns = table.getDataTableColumns();
            }
            for (DataTableColumn column : dataTableColumns) {
                Ontology ontology = column.getDefaultOntology();
                if (ontology != null) {
                    List<DataTableColumn> columns = dataTableAutoMap.get(ontology);
                    if (columns == null) {
                        columns = new ArrayList<>();
                        dataTableAutoMap.put(ontology, columns);
                    }
                    columns.add(column);
                }
            }
        }

        // okay now we have a map of the data table columns,
        List<List<DataTableColumn>> columnAutoList = new ArrayList<>();
        for (Ontology key : dataTableAutoMap.keySet()) {
            Pair<ArrayList<Long>, ArrayList<DataTableColumn>> set1 = new Pair<>(new ArrayList<Long>(), new ArrayList<DataTableColumn>());
            Pair<ArrayList<Long>, ArrayList<DataTableColumn>> set2 = new Pair<>(new ArrayList<Long>(), new ArrayList<DataTableColumn>());

            // go through the hashMap and try and pair out by set of rules assuming that there is one column per table at a time
            // and there might be a case where there are more than one
            for (DataTableColumn column : dataTableAutoMap.get(key)) {
                Long dataTableIld = column.getDataTable().getId();
                if (!set1.getFirst().contains(dataTableIld)) {
                    set1.getSecond().add(column);
                    set1.getFirst().add(dataTableIld);
                } else if (!set2.getFirst().contains(dataTableIld)) {
                    set2.getSecond().add(column);
                    set2.getFirst().add(dataTableIld);
                } // give up
            }

            // might want to tune this to some logic like:
            // if just one table, then anything with an ontology if more than one, just show lists with at least two ontologies
            if (set1.getSecond().size() > 0) {
                columnAutoList.add(set1.getSecond());
            }
            if (set2.getSecond().size() > 0) {
                columnAutoList.add(set2.getSecond());
            }
        }
        return columnAutoList;
    }

    @Transactional(readOnly = true)
    public Map<Ontology, List<DataTable>> getIntegrationSuggestions(Collection<DataTable> bookmarkedDataTables, boolean showOnlyShared) {
        HashMap<Ontology, List<DataTable>> allOntologies = new HashMap<>();
        if (CollectionUtils.isEmpty(bookmarkedDataTables)) {
            return Collections.emptyMap();
        }
        for (DataTable table : bookmarkedDataTables) {
            for (DataTableColumn column : table.getDataTableColumns()) {
                Ontology ontology = column.getMappedOntology();
                if (ontology != null) {
                    List<DataTable> values = allOntologies.get(ontology);
                    if (values == null) {
                        values = new ArrayList<>();
                    }
                    values.add(table);
                    allOntologies.put(ontology, values);
                }
            }
        }
        if (showOnlyShared) {
            HashMap<Ontology, List<DataTable>> toReturn = new HashMap<>();
            for (Entry<Ontology, List<DataTable>> entry : allOntologies.entrySet()) {
                if (entry.getValue().size() == bookmarkedDataTables.size()) {
                    toReturn.put(entry.getKey(), entry.getValue());
                }
            }
            return toReturn;
        }

        return allOntologies;
    }

    @Transactional(readOnly = false)
    public Set<OntologyNode> getFilteredOntologyNodes(IntegrationColumn integrationColumn) {
        List<IntegrationColumnPartProxy> list = getNodeParticipationByColumn(PersistableUtils.extractIds(integrationColumn.getColumns()));
        Set<OntologyNode> nodes = new HashSet<>();
        for (IntegrationColumnPartProxy icp : list) {
            nodes.addAll(icp.getFlattenedNodes());
        }
        return nodes;
    }

    /**
     * Compute the node participation for the DataTableColumns described by the specified list of ID's. The method
     * returns the results in a map of OntologyNode lists by DataTableColumn.
     *
     * @param dataTableColumnIds
     * @return
     */
    @Transactional(readOnly = true)
    public List<IntegrationColumnPartProxy> getNodeParticipationByColumn(List<Long> dataTableColumnIds) {
        List<IntegrationColumnPartProxy> results = new ArrayList<>();
        Set<DataTableColumn> dataTableColumns = new HashSet<>(genericDao.findAll(DataTableColumn.class, dataTableColumnIds));
        Map<Ontology, Set<DataTableColumn>> columnsByOntology = buildOntologyToColumnMap(dataTableColumns);

        // For each ontology, update tdar-data database mappings, and get the list of correlated & mapped ontology nodes 
        for (Ontology ontology : columnsByOntology.keySet()) {
            logger.trace("ontology: {}", ontology);
            for (DataTableColumn col : columnsByOntology.get(ontology)) {
                if (!col.isActuallyMapped()) {
                    continue;
                }
                IntegrationColumnPartProxy icp = new IntegrationColumnPartProxy();
                icp.setDataTableColumn(col);
                icp.setSharedOntology(ontology);
                results.add(icp);
                // get the actual mappings from tdardata
                updateMappedCodingRules(col);
                applyLegacyFilter(ontology, col, icp);

            }
            logger.trace("nodesByColumn: {}", results);
        }
        return results;
    }

    /**
     * Originally ported from IntegrationColumn.flatten(); this method takes the integration column and gets the associated coding sheet
     * and thus, ontology and ontology nodes to find what should be marked as "checked"
     * @param ontology
     * @param col
     * @param icp
     */
    private void applyLegacyFilter(Ontology ontology, DataTableColumn col, IntegrationColumnPartProxy icp) {
        SortedMap<Integer, List<OntologyNode>> ontologyNodeParentChildMap = ontology.toOntologyNodeMap();
        
        // check mapping first to see if the value should be translated a second
        // time to the common ontology format.
        
        // check if distinctValues has any values in common with mapped data values
        // if the two lists are disjoint (nothing in common), then there is no
        // data value if one of the distinct values is already equivalent to the ontology
        // node label, go with that.
        
        
        for (OntologyNode ontologyNode : ontology.getOntologyNodes()) {
            List<OntologyNode> children = ontologyNodeParentChildMap.get(Integer.valueOf(ontologyNode.getIntervalStart()));
            List<CodingRule> rules = col.getDefaultCodingSheet().findRulesMappedToOntologyNode(ontologyNode);

            // Step 1: find direct value matches
            if (CollectionUtils.isNotEmpty(rules)) {
                for (CodingRule rule : rules) {
                    if ((rule != null) && rule.isMappedToData(col)) {
                        markAdded(col, icp, ontologyNode);
                    }
                }
            }

            //NOTE: this can be removed once the legacy filter page is removed
            // check if any of the children of this node matches
            if (CollectionUtils.isNotEmpty(children)) {
                ontologyNode.setParent(true);
            }
        }
    }

    /**
     * convenience method setting mapped to true for an ontologyNode, and thena dding it to the flattened nodes list
     * @param col
     * @param icp
     * @param ontologyNode
     */
    private void markAdded(DataTableColumn col, IntegrationColumnPartProxy icp, OntologyNode ontologyNode) {
        ontologyNode.setMappedDataValues(true);
        ontologyNode.getColumnHasValueMap().put(col, true);
        icp.getFlattenedNodes().add(ontologyNode);
    }

    /**
     * Creates a reverse-map of Ontologies->DataTableColumns for mapped dataTableColumns
     * @param dataTableColumns
     * @return
     */
    private Map<Ontology, Set<DataTableColumn>> buildOntologyToColumnMap(Set<DataTableColumn> dataTableColumns) {
        Map<Ontology, Set<DataTableColumn>> columnsByOntology = new HashMap<>();

        // for each DataTableColumn, create an inverse-map of Ontologies->DataTableColumns
        for (DataTableColumn dataTableColumn : dataTableColumns) {
            Ontology mappedOntology = dataTableColumn.getMappedOntology();

            if (mappedOntology == null) {
                continue;
            }

            Set<DataTableColumn> columns = columnsByOntology.get(mappedOntology);
            if (columns == null) {
                columns = new HashSet<>();
                columnsByOntology.put(mappedOntology, columns);
            }
            columns.add(dataTableColumn);
        }
        return columnsByOntology;
    }

    /**
     * Take the entire Integration Context, hydrate it (if needed) and run the integration
     * @param context
     * @param provider
     * @return
     */
    @Transactional
    @Deprecated
    public ModernIntegrationDataResult generateModernIntegrationResult(IntegrationContext context, TextProvider provider) {
        context.setDataTables(genericDao.loadFromSparseEntities(context.getDataTables(), DataTable.class));
        for (IntegrationColumn integrationColumn : context.getIntegrationColumns()) {
            hydrateIntegrationColumn(integrationColumn);
        }

        ModernIntegrationDataResult result = tdarDataImportDatabase.generateIntegrationResult(context, provider, excelService);
        storeResult(result);
        return result;
    }

    public ModernIntegrationDataResult generateModernIntegrationResult(String integration, TextProvider provider, TdarUser authenticatedUser) throws JsonParseException, JsonMappingException, IOException, IntegrationDeserializationException {
        logger.trace("XXX: DISPLAYING FILTERED RESULTS :XXX");
        IntegrationWorkflowData workflow = serializationService.readObjectFromJson(integration, IntegrationWorkflowData.class);
        IntegrationContext integrationContext = workflow.toIntegrationContext(genericDao);
        integrationContext.setCreator(authenticatedUser);
        ResourceRevisionLog log = new ResourceRevisionLog("display filtered results (payload: tableToDisplayColumns)",null, authenticatedUser);
        log.setTimestamp(new Date());
        log.setPayload(integration);
        genericDao.saveOrUpdate(log);
        logger.trace(integration);
        // ADD ERROR CHECKING LOGIC

        // // ok, at this point we have the integration columns that we're interested in + the ontology
        // // nodes that we want to use to filter values of interest and for aggregation.
        // // getLogger().debug("table columns are: " + tableToIntegrationColumns);
        // // getLogger().debug("ontology node hierarchy map: " + tableToOntologyHierarchyMap);
        // if (CollectionUtils.isEmpty(tableToIntegrationColumns)) {
        // addActionError("Either no integration columns or filter values were selected, please go back and select both");
        // return INPUT;
        // }
        //


        ModernIntegrationDataResult result = tdarDataImportDatabase.generateIntegrationResult(integrationContext, provider, excelService);
        storeResult(result);
        return result;
    }

    /**
     * Take the result of the integration and store it in the personal filestore for retrieval
     * @param result
     * @return
     */
    @Transactional
    public PersonalFilestoreTicket storeResult(ModernIntegrationDataResult result) {
        ModernDataIntegrationWorkbook workbook = result.getWorkbook();
        PersonalFilestoreTicket ticket = workbook.getTicket();
        genericDao.save(ticket);

        try {
            File resultFile = workbook.writeToTempFile();
            PersonalFilestore filestore = filestoreService.getPersonalFilestore(result.getIntegrationContext().getCreator());
            filestore.store(ticket, resultFile, workbook.getFileName());
            result.setTicket(ticket);
        } catch (Exception exception) {
            logger.error("an error occurred when producing the integration excel file", exception);
            throw new TdarRecoverableRuntimeException("dataIntegrationService.could_not_save_file");
        }

        return ticket;
    }

    @Transactional(readOnly = true)
    public TableDetailsProxy getTableDetails(List<Long> dataTableIds) {
        TableDetailsProxy proxy = new TableDetailsProxy();
        proxy.getDataTables().addAll(genericDao.findAll(DataTable.class, dataTableIds));
        Map<Ontology, List<DataTable>> suggestions = getIntegrationSuggestions(proxy.getDataTables(), false);
        proxy.getMappedOntologies().addAll(suggestions.keySet());
        return proxy;
    }
}
