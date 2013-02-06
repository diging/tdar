package org.tdar.struts.action.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.tdar.TestConstants;
import org.tdar.core.bean.AbstractIntegrationTestCase;
import org.tdar.core.bean.resource.CodingSheet;
import org.tdar.core.bean.resource.Dataset;
import org.tdar.core.bean.resource.InformationResourceFile;
import org.tdar.core.bean.resource.InformationResourceFile.FileAccessRestriction;
import org.tdar.core.bean.resource.InformationResourceFile.FileAction;
import org.tdar.core.bean.resource.datatable.DataTable;
import org.tdar.core.bean.resource.datatable.DataTableColumn;
import org.tdar.core.bean.resource.datatable.DataTableColumnEncodingType;
import org.tdar.struts.data.FileProxy;

public class DatasetConfidentialityITCase extends AbstractIntegrationTestCase {

    private static final String TEST_DATA_SET_FILE_PATH = TestConstants.TEST_DATA_INTEGRATION_DIR + "total-number-of-bones-per-period.xlsx";
    private static final File TEST_DATASET_FILE = new File(TEST_DATA_SET_FILE_PATH);
    private static final String EXCEL_FILE_NAME = "periods-modified-sm-01182011.xlsx";
    private static final String EXCEL_FILE_PATH = TestConstants.TEST_DATA_INTEGRATION_DIR + EXCEL_FILE_NAME;

    Long datasetId = null;

    @Test
    @Rollback(false)
    public void testConfidentialityAfterEdit() throws Exception {

        // CodingSheet codingSheet = setupCodingSheet();

        DatasetController datasetController = generateNewInitializedController(DatasetController.class);
        datasetController.prepare();
        Dataset dataset = datasetController.getDataset();
        dataset.setTitle("test dataset");
        dataset.setDescription("test description");
        List<File> uploadedFiles = new ArrayList<File>();
        List<String> uploadedFileNames = new ArrayList<String>();
        uploadedFileNames.add(TEST_DATASET_FILE.getName());
        uploadedFiles.add(TEST_DATASET_FILE);
        datasetController.setUploadedFiles(uploadedFiles);
        datasetController.setUploadedFilesFileName(uploadedFileNames);
        datasetController.setServletRequest(getServletPostRequest());

        // make the file confidential
        FileProxy fileProxy = new FileProxy();
        fileProxy.setFilename(TEST_DATASET_FILE.getName());
        fileProxy.setAction(FileAction.ADD);
        fileProxy.setRestriction(FileAccessRestriction.CONFIDENTIAL);
        datasetController.getFileProxies().add(fileProxy);

        // create the dataset
        datasetController.save();
        genericService.synchronize();
        datasetId = dataset.getId();
        assertNotNull(datasetId);

        CodingSheetController codingSheetController = generateNewInitializedController(CodingSheetController.class);
        codingSheetController.prepare();
        CodingSheet codingSheet = codingSheetController.getCodingSheet();
        codingSheet.setTitle("test coding sheet");
        codingSheet.setDescription("test description");
        List<File> codingFiles = new ArrayList<File>();
        List<String> codingFileNames = new ArrayList<String>();
        File codingFile = new File(TestConstants.TEST_DATA_INTEGRATION_DIR , EXCEL_FILE_NAME);
        codingFiles.add(codingFile);
        codingSheet.setDefaultOntology(null);
        codingFileNames.add("periods-modified-sm-01182011-2.xlsx");
        codingSheetController.setUploadedFilesFileName(codingFileNames);
        codingSheetController.setUploadedFiles(codingFiles);
        codingSheetController.setServletRequest(getServletPostRequest());
        codingSheetController.save();
        Long codingId = codingSheet.getId();

        
        
        // edit column metadata
        genericService.detachFromSession(dataset);
        dataset = null;
        dataset = genericService.find(Dataset.class, datasetId);

        DataTable dataTable = dataset.getDataTables().iterator().next();
//        List<DataTableColumn> dataTableColumns = dataTable.getDataTableColumns();
        DataTableColumn period_ = dataTable.getColumnByDisplayName("Period");
        datasetController = generateNewInitializedController(DatasetController.class);
        datasetController.setId(datasetId);
        datasetController.prepare();
        datasetController.editColumnMetadata();
//        genericService.detachFromSession(dataTable);
//        genericService.detachFromSession(dataTableColumns);
        DataTableColumn cloneBean = (DataTableColumn) BeanUtils.cloneBean(period_);
        cloneBean.setColumnEncodingType(DataTableColumnEncodingType.CODED_VALUE);
        cloneBean.setDefaultCodingSheet(codingSheet);
        logger.info("{}" , cloneBean);
        List<DataTableColumn> list = new ArrayList<DataTableColumn>();
        list.add(cloneBean);
        datasetController.setDataTableColumns(list);
        datasetController.saveColumnMetadata();

        // simulate view, compare the file access
        genericService.detachFromSession(dataset);
        dataset = null;
        genericService.synchronize();
    }

    @AfterTransaction
    public void verifyPostTranslatedData() {
        runInNewTransaction(new TransactionCallback<Dataset>() {

            @Override
            public Dataset doInTransaction(TransactionStatus arg0) {
                Dataset mydataset = genericService.find(Dataset.class, datasetId);
                logger.info("{} {}", datasetId, mydataset);
                Set<InformationResourceFile> informationResourceFiles = mydataset.getInformationResourceFiles();
                assertNotEmpty(informationResourceFiles);
                assertEquals(FileAccessRestriction.CONFIDENTIAL, informationResourceFiles.iterator().next().getRestriction());
                genericService.delete(mydataset);
                return null;
            }
        });
    }
}
