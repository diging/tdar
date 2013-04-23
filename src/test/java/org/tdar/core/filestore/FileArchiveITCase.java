/**
 * 
 */
package org.tdar.core.filestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.tdar.TestConstants;
import org.tdar.core.bean.AbstractIntegrationTestCase;
import org.tdar.core.bean.resource.InformationResourceFile;
import org.tdar.core.bean.resource.InformationResourceFile.FileType;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.bean.resource.SensoryData;
import org.tdar.core.service.workflow.MessageService;
import org.tdar.core.service.workflow.workflows.FileArchiveWorkflow;
import org.tdar.core.service.workflow.workflows.Workflow;
import org.tdar.filestore.FileAnalyzer;
import org.tdar.filestore.PairtreeFilestore;

/**
 * @author Adam Brin
 * 
 */
public class FileArchiveITCase extends AbstractIntegrationTestCase {

    @Autowired
    private FileAnalyzer fileAnalyzer;

    @Autowired
    private MessageService messageService;

    protected Logger logger = Logger.getLogger(getClass());

    @Test
    public void testAnalyzerSuggestions() {
        assertEquals(ResourceType.DOCUMENT, fileAnalyzer.suggestTypeForFileExtension("doc", ResourceType.DOCUMENT));
        assertEquals(ResourceType.SENSORY_DATA, fileAnalyzer.suggestTypeForFileExtension("gif", ResourceType.SENSORY_DATA, ResourceType.IMAGE));
        assertEquals(ResourceType.IMAGE, fileAnalyzer.suggestTypeForFileExtension("gif", ResourceType.IMAGE, ResourceType.SENSORY_DATA));
        assertNull(fileAnalyzer.suggestTypeForFileExtension("xls", ResourceType.ONTOLOGY));
        assertEquals(ResourceType.CODING_SHEET, fileAnalyzer.suggestTypeForFileExtension("xls", ResourceType.ONTOLOGY, ResourceType.CODING_SHEET));
    }

    @Test
    @Rollback
    public void testFileAnalyzer() throws Exception {
        PairtreeFilestore store = new PairtreeFilestore(TestConstants.FILESTORE_PATH);
        testArchiveFormat(store, "ark_hm_headpot_scans.tar");
        testArchiveFormat(store, "ark_hm_headpot_scans.zip");
        testArchiveFormat(store, "ark_hm_headpot_scans.tgz");
    }

    public void testArchiveFormat(PairtreeFilestore store, String filename) throws InstantiationException, IllegalAccessException, IOException, Exception {
        File f = new File(TestConstants.TEST_SENSORY_DIR, filename);
        InformationResourceFileVersion originalVersion = generateAndStoreVersion(SensoryData.class, filename, f, store);
        FileType fileType = fileAnalyzer.analyzeFile(originalVersion);
        assertEquals(FileType.FILE_ARCHIVE, fileType);
        Workflow workflow = fileAnalyzer.getWorkflow(originalVersion);
        assertEquals(FileArchiveWorkflow.class, workflow.getClass());
        messageService.sendFileProcessingRequest(originalVersion, workflow);
        InformationResourceFile informationResourceFile = originalVersion.getInformationResourceFile();
        informationResourceFile = genericService.find(InformationResourceFile.class, informationResourceFile.getId());

        for (InformationResourceFileVersion version : informationResourceFile.getLatestVersions()) {
            logger.info(version);
            if (version.isTranslated()) {
                String contents = FileUtils.readFileToString(version.getFile());
                assertTrue(contents.contains("Ark_HM_Headpot_01.txt"));
                assertTrue(contents.contains("Ark_HM_Headpot_mtrx_01.txt"));
            }
        }

        // FIXME: confirm that there is a resulting file, and that the file has the right contents
        // confirm x number of versions, confirm types
        // confirm contents
    }

}
