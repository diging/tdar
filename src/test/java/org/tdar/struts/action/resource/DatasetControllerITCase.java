package org.tdar.struts.action.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.struts2.convention.annotation.Action;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.tdar.TestConstants;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.resource.Dataset;
import org.tdar.core.bean.resource.InformationResourceFile;
import org.tdar.core.bean.resource.Ontology;
import org.tdar.core.bean.resource.datatable.DataTable;
import org.tdar.core.bean.resource.datatable.DataTableColumn;
import org.tdar.core.bean.resource.datatable.DataTableColumnEncodingType;
import org.tdar.core.bean.resource.datatable.DataTableColumnType;
import org.tdar.core.service.DownloadService;
import org.tdar.core.service.resource.DataTableService;
import org.tdar.junit.MultipleTdarConfigurationRunner;
import org.tdar.junit.RunWithTdarConfiguration;
import org.tdar.struts.action.AbstractDataIntegrationTestCase;
import org.tdar.struts.action.TdarActionException;
import org.tdar.struts.action.TdarActionSupport;

/**
 * $Id$
 * 
 * Integration test over the DatasetController's action methods.
 * 
 * @author <a href='mailto:allen.lee@asu.edu'>Allen Lee</a>
 * @version $Rev$
 */
@RunWith(MultipleTdarConfigurationRunner.class)
public class DatasetControllerITCase extends AbstractDataIntegrationTestCase {

    private static final String ALEXANDRIA_EXCEL_FILENAME = "qrybonecatalogueeditedkk.xls";
    private static final String TRUNCATED_HARP_EXCEL_FILENAME = "heshfaun-truncated.xls";
    private static final String BELEMENT_COL = "belement";

    private DatasetController controller;
    private Logger logger = Logger.getLogger(getClass());

    @Autowired
    private DataTableService dataTableService;
    @Autowired
    private DownloadService downloadService;

    @Before
    public void setUp() {
        controller = generateNewInitializedController(DatasetController.class);
    }

    @Test
    @Rollback
    public void test() {
        TdarUser p = genericService.find(TdarUser.class, getUser().getId());
        Dataset dataset = genericService.findRandom(Dataset.class, 1).get(0);
        dataset.setTitle("test");
        dataset.setSubmitter(p);
        dataset.markUpdated(controller.getAuthenticatedUser());
        genericService.merge(dataset);
    }

    @Test
    @Rollback
    public void testOntologyMappingCaseSensitivity() throws Exception {
        Dataset dataset = setupAndLoadResource(ALEXANDRIA_EXCEL_FILENAME, Dataset.class);
        controller.setId(dataset.getId());
        Ontology bElementOntology = setupAndLoadResource("fauna-element-updated---default-ontology-draft.owl", Ontology.class);
        DataTableColumn elementColumn = new DataTableColumn();
        elementColumn.setDefaultOntology(bElementOntology);
        elementColumn.setColumnEncodingType(DataTableColumnEncodingType.UNCODED_VALUE);
        elementColumn.setName(BELEMENT_COL);
        DataTable dataTable = dataset.getDataTables().iterator().next();
        mapColumnsToDataset(dataset, dataTable, elementColumn);
        CodingSheetController codingSheetController = generateNewInitializedController(CodingSheetController.class);
        DataTableColumn column = dataTable.getColumnByName(BELEMENT_COL);
        assertNotNull(column.getDefaultCodingSheet());
        assertTrue(column.getDefaultCodingSheet().isGenerated());
        codingSheetController.setId(column.getDefaultCodingSheet().getId());
        codingSheetController.prepare();
        codingSheetController.loadOntologyMappedColumns();
        List<String> findAllDistinctValues = dataTableService.findAllDistinctValues(column);
        List<String> tibias = new ArrayList<String>();
        for (String distinct : findAllDistinctValues) {
            if (distinct.toLowerCase().contains("tibia")) {
                tibias.add(distinct);
            }
        }

        int tibia = -1;
        int Tibia = -1;
        List<String> suggestedTibias = new ArrayList<String>();
        int i = 0;

        for (String key : codingSheetController.getSuggestions().keySet()) {
            if (key.equals("Tibia")) {
                Tibia = i;
            }
            if (key.equals("tibia")) {
                tibia = i;
            }
            i++;
            if (key.toLowerCase().contains("tibia")) {
                suggestedTibias.add(key);
            }
        }

        assertEquals(tibias.size(), suggestedTibias.size());
        assertNotSame(tibia, -1);
        assertNotSame(Tibia, -1);
        assertTrue(String.format("%d < %d", tibia, Tibia), tibia < Tibia);

        logger.info(suggestedTibias);
        Collections.sort(suggestedTibias);
        Collections.sort(tibias);
        assertEquals(tibias, suggestedTibias);
    }

    @Test
    @Rollback
    public void testNullResourceOperations() throws Exception {
        List<String> successActions = Arrays.asList("add", "list");
        // grab all methods on DatasetController annotated with a conventions plugin @Action
        for (Method method : DatasetController.class.getMethods()) {
            controller = generateNewInitializedController(DatasetController.class);
            controller.prepare();
            if (method.isAnnotationPresent(Action.class)) {
                logger.debug("Invoking action method: " + method.getName());
                try {
                    String result = (String) method.invoke(controller);
                    if (successActions.contains(method.getName())) {
                        assertEquals("DatasetController." + method.getName() + "() should return success", com.opensymphony.xwork2.Action.SUCCESS, result);
                    } else {
                        setIgnoreActionErrors(true);
                        assertNotSame("DatasetController." + method.getName() + "() should not return SUCCESS", com.opensymphony.xwork2.Action.SUCCESS, result);
                    }
                } catch (Exception e) {
                    if (e instanceof TdarActionException) {
                        TdarActionException exception = (TdarActionException) e;
                        setIgnoreActionErrors(true);
                        assertNotSame("DatasetController." + method.getName() + "() should not return SUCCESS", com.opensymphony.xwork2.Action.SUCCESS,
                                exception.getResultName());
                    }

                }
            }
        }
    }

    @Test
    @Rollback
    public void testDatasetReplaceSame() throws TdarActionException {
        Dataset dataset = setupAndLoadResource(ALEXANDRIA_EXCEL_FILENAME, Dataset.class);

        dataset = setupAndLoadResource(ALEXANDRIA_EXCEL_FILENAME, Dataset.class, dataset.getId());

    }

    @Test
    @Rollback
    public void tableAsXmlReturnsErrorIfXmlExportNotEnabled() {
        controller = generateNewInitializedController(DatasetController.class);
        assertSame(com.opensymphony.xwork2.Action.ERROR, controller.getTableAsXml());
    }

    @Test
    @Rollback
    @RunWithTdarConfiguration(runWith = { RunWithTdarConfiguration.FAIMS })
    public void tableAsXml() throws IOException {
        Dataset dataset = setupAndLoadResource(TRUNCATED_HARP_EXCEL_FILENAME, Dataset.class);
        DataTable dataTable = dataset.getDataTables().iterator().next();
        controller = generateNewInitializedController(DatasetController.class);
        controller.setId(dataset.getId());
        controller.setDataTableId(dataTable.getId());
        controller.prepare();
        assertEquals(com.opensymphony.xwork2.Action.SUCCESS, controller.getTableAsXml());
        InputStream xmlStream = controller.getXmlStream();
        String xml = IOUtils.toString(xmlStream, "UTF-8");
        assertTrue(xml.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""));
    }

    @Test
    @Rollback
    public void testDatasetReplaceWithMappings() throws TdarActionException {
        Dataset dataset = setupAndLoadResource(ALEXANDRIA_EXCEL_FILENAME, Dataset.class);
        controller = generateNewInitializedController(DatasetController.class);

        Ontology bElementOntology = setupAndLoadResource("fauna-element-updated---default-ontology-draft.owl", Ontology.class);
        DataTable alexandriaTable = dataset.getDataTables().iterator().next();
        DataTableColumn elementColumn = alexandriaTable.getColumnByName(BELEMENT_COL);
        elementColumn.setDefaultOntology(bElementOntology);
        Long elementColumnId = elementColumn.getId();
        mapColumnsToDataset(dataset, alexandriaTable, elementColumn);
        mapDataOntologyValues(alexandriaTable, BELEMENT_COL, getElementValueMap(), bElementOntology);
        Map<String, List<Long>> valueToOntologyNodeIdMap = elementColumn.getValueToOntologyNodeIdMap();
        elementColumn = null;
        controller.setId(dataset.getId());
        controller.prepare();
        controller.edit();
        controller.setUploadedFiles(Arrays.asList(new File(TestConstants.TEST_DATA_INTEGRATION_DIR + ALEXANDRIA_EXCEL_FILENAME)));
        controller.setUploadedFilesFileName(Arrays.asList(ALEXANDRIA_EXCEL_FILENAME));
        controller.setServletRequest(getServletPostRequest());
        assertEquals(com.opensymphony.xwork2.Action.SUCCESS, controller.save());
        // FIXME: I believe this causes the NonUniqueObjectException because we're
        // still actually using the same Hibernate Session / thread of execution that we were in initially
        // (when setupAndLoadResource was invoked at the top of the method)
        // flush();
        dataset = controller.getDataset();
        alexandriaTable = dataset.getDataTables().iterator().next();
        DataTableColumn secondElementColumn = alexandriaTable.getColumnByName(BELEMENT_COL);
        assertNotNull(secondElementColumn);
        assertEquals(elementColumnId, secondElementColumn.getId());
        assertEquals(secondElementColumn.getDefaultOntology(), bElementOntology);
        Map<String, List<Long>> incomingValueToOntologyNodeIdMap = secondElementColumn.getValueToOntologyNodeIdMap();
        assertEquals(valueToOntologyNodeIdMap, incomingValueToOntologyNodeIdMap);
    }

    @Test
    @Rollback
    public void testDatasetReplaceDifferentExcel() throws TdarActionException {
        Dataset dataset = setupAndLoadResource(ALEXANDRIA_EXCEL_FILENAME, Dataset.class);
        controller = generateNewInitializedController(DatasetController.class);
        controller.setId(dataset.getId());
        controller.prepare();
        controller.edit();
        String filename = "evmpp-fauna.xls";
        controller.setUploadedFiles(Arrays.asList(new File(TestConstants.TEST_DATA_INTEGRATION_DIR + filename)));
        controller.setUploadedFilesFileName(Arrays.asList(filename));
        controller.setServletRequest(getServletPostRequest());
        assertEquals(com.opensymphony.xwork2.Action.SUCCESS, controller.save());
    }

    @Test
    @Rollback
    public void testDatasetReplaceDifferentColTypes() throws TdarActionException {
        Dataset dataset = setupAndLoadResource("dataset_with_floats.xls", Dataset.class);
        String filename = "dataset_with_floats_to_varchar.xls";
        assertEquals(DataTableColumnType.DOUBLE, dataset.getDataTables().iterator().next().getColumnByName("col2floats").getColumnDataType());
        Long datasetId = dataset.getId();
        dataset = null;
        dataset = replaceFile(filename, "dataset_with_floats.xls", Dataset.class, datasetId);
        assertEquals(DataTableColumnType.VARCHAR, dataset.getDataTables().iterator().next().getColumnByName("col2floats").getColumnDataType());

    }

    @Test
    @Rollback
    public void testDatasetReplaceDifferentMdb() throws TdarActionException {
        Dataset dataset = setupAndLoadResource(ALEXANDRIA_EXCEL_FILENAME, Dataset.class);
        controller = generateNewInitializedController(DatasetController.class);
        controller.setId(dataset.getId());
        controller.prepare();
        controller.edit();
        String filename = SPITAL_DB_NAME;
        controller.setUploadedFiles(Arrays.asList(new File(TestConstants.TEST_DATA_INTEGRATION_DIR + filename)));
        controller.setUploadedFilesFileName(Arrays.asList(filename));
        controller.setServletRequest(getServletPostRequest());
        assertEquals(com.opensymphony.xwork2.Action.SUCCESS, controller.save());
    }

    @Test
    @Rollback(false)
    public void testReprocessDataset() throws Exception {
        Dataset dataset = setupAndLoadResource(TRUNCATED_HARP_EXCEL_FILENAME, Dataset.class);
        final Long datasetId = dataset.getId();
        final DataTable dataTable = dataset.getDataTables().iterator().next();
        final int originalNumberOfRows = tdarDataImportDatabase.getRowCount(dataTable);
        final List<List<String>> originalColumnData = new ArrayList<List<String>>();
        for (DataTableColumn column : dataTable.getSortedDataTableColumns()) {
            // munge column names and rename in tdar data database
            originalColumnData.add(tdarDataImportDatabase.selectAllFrom(column));
            tdarDataImportDatabase.renameColumn(column, column.getDisplayName());
            assertFalse("Column name should be denormalized", tdarDataImportDatabase.normalizeTableOrColumnNames(column.getName()).equals(column.getName()));
            genericService.save(column);
        }
        verifyDataTable(dataTable, originalNumberOfRows, originalColumnData);
        InformationResourceFile file = dataset.getFirstInformationResourceFile();
        controller = generateNewInitializedController(DatasetController.class);
        controller.setId(dataset.getId());
        controller.prepare();
        controller.edit();
        datasetService.reprocess(dataset);
        assertEquals(file, dataset.getFirstInformationResourceFile());
        assertEquals(file.getLatestUploadedVersion(), dataset.getFirstInformationResourceFile().getLatestUploadedVersion());
        setVerifyTransactionCallback(new TransactionCallback<Dataset>() {
            @Override
            public Dataset doInTransaction(TransactionStatus status) {
                Dataset dataset = genericService.find(Dataset.class, datasetId);
                DataTable dataTable = dataset.getDataTables().iterator().next();
                verifyDataTable(dataTable, originalNumberOfRows, originalColumnData);
                for (DataTableColumn column : dataTable.getSortedDataTableColumns()) {
                    assertEquals("Column name should be normalized", tdarDataImportDatabase.normalizeTableOrColumnNames(column.getName()), column.getName());
                }
                return null;
            }
        });
    }

    private void verifyDataTable(final DataTable dataTable, final int expectedNumberOfRows, List<List<String>> expectedColumnData) {
        assertEquals(expectedNumberOfRows, tdarDataImportDatabase.getRowCount(dataTable));
        Iterator<List<String>> expectedColumnDataIterator = expectedColumnData.iterator();
        for (DataTableColumn column : dataTable.getSortedDataTableColumns()) {
            // verify column values
            List<String> expectedValues = expectedColumnDataIterator.next();
            List<String> actualValues = tdarDataImportDatabase.selectAllFrom(column);
            assertEquals(expectedValues, actualValues);
        }
    }

}
