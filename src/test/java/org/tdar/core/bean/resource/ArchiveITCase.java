package org.tdar.core.bean.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;

import org.junit.Test;
import org.springframework.test.annotation.Rollback;
import org.tdar.TestConstants;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.service.ErrorTransferObject;
import org.tdar.filestore.FilestoreObjectType;
import org.tdar.struts.action.AbstractIntegrationControllerTestCase;

public class ArchiveITCase extends AbstractIntegrationControllerTestCase {

    public Archive generateArchiveFileAndUser(String archive) {
        setIgnoreActionErrors(true);

        Archive result = createAndSaveNewInformationResource(Archive.class, false);
        assertTrue(result.getResourceType() == ResourceType.ARCHIVE);
        File file = new File(TestConstants.TEST_ARCHIVE_DIR + archive);
        assertTrue("testing " + TestConstants.FAULTY_ARCHIVE + " doesn't exis?", file.exists());
        result = (Archive) addFileToResource(result, file); // now take the file through the work flow.
        return result;
    }

    @Test
    @Rollback
    public void replicateFaultyArchiveIssue() throws Exception {
        // setIgnoreActionErrors(Boolean.TRUE);
        InformationResource ir = generateArchiveFileAndUser(TestConstants.FAULTY_ARCHIVE);
        // Martin: in my scenario, the file results in a processing error.
        final Set<InformationResourceFile> irFiles = ir.getInformationResourceFiles();
        assertEquals(irFiles.size(), 1);

        InformationResourceFile irFile = irFiles.iterator().next();
        irFile = genericService.find(InformationResourceFile.class, irFile.getId());
        assertNotNull("IrFile is null", irFile);
        assertEquals(FileStatus.PROCESSING_WARNING, irFile.getStatus());
        assertEquals(irFile.getInformationResourceFileType(), FileType.FILE_ARCHIVE);

        genericService.saveOrUpdate(irFile);
        evictCache();

        // however, whatever caused the processing error is fixed
        File fileInStore = TdarConfiguration.getInstance().getFilestore().retrieveFile(FilestoreObjectType.RESOURCE, irFile.getLatestUploadedVersion());
        File sourceFile = new File(TestConstants.TEST_ARCHIVE_DIR + TestConstants.GOOD_ARCHIVE);
        fileInStore.setWritable(true);
        org.apache.commons.io.FileUtils.copyFile(sourceFile, fileInStore);

        // and the file is reprocessed
        ErrorTransferObject errors = informationResourceService.reprocessInformationResourceFiles(ir);

        // then in memory, the following is true:
        irFile = genericService.find(InformationResourceFile.class, irFile.getId());
        assertEquals(FileStatus.PROCESSED, irFile.getStatus());

        // However, in the database the file status change has not been persisted...
        // And the transaction around reprocessInformationResourceFiles has been committed
        // And there is no other transaction in progress.
        // I'm not yet sure how to demonstrate this in the test environment,
        // I'll have to play with @AfterTransaction, and make the test properly transactional...
    }

    @Test
    @Rollback(true)
    public void testReprocessFaultyArchive() throws Exception {
        setIgnoreActionErrors(true);
        InformationResource ir = generateArchiveFileAndUser(TestConstants.FAULTY_ARCHIVE);
        final Set<InformationResourceFile> irFiles = ir.getInformationResourceFiles();
        assertEquals(irFiles.size(), 1);
        InformationResourceFile irFile = irFiles.iterator().next();
        irFile = genericService.find(InformationResourceFile.class, irFile.getId());
        assertNotNull("IrFile is null", irFile);
        assertEquals(FileStatus.PROCESSING_WARNING, irFile.getStatus());

        irFile.setStatus(FileStatus.PROCESSED);
        irFile.setErrorMessage("blah");
        genericService.saveOrUpdate(irFile);
        evictCache();
        ErrorTransferObject errors = informationResourceService.reprocessInformationResourceFiles(ir);
        evictCache();

        irFile = genericService.find(InformationResourceFile.class, irFile.getId());
        assertNotNull("IrFile is null", irFile);
        assertEquals(FileStatus.PROCESSING_WARNING, irFile.getStatus());

    }

}
