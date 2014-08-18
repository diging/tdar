/**
 * $Id$
 * 
 * @author $Author$
 * @version $Revision$
 */
package org.tdar.core.service.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.tdar.TestConstants;
import org.tdar.core.bean.resource.CodingSheet;
import org.tdar.core.bean.resource.InformationResourceFile;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.bean.resource.VersionType;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.parser.CodingSheetParserException;
import org.tdar.filestore.Filestore.ObjectType;
import org.tdar.struts.action.AbstractControllerITCase;
import org.tdar.struts.action.TdarActionSupport;

/**
 * @author Adam Brin
 * 
 */
public class CodingSheetServiceITCase extends AbstractControllerITCase {

    @Autowired
    CodingSheetService codingSheetService;

    @Rollback
    @Test
    public void testCodingSheetFindAll() {
        List<CodingSheet> sparseCodingSheets = codingSheetService.findSparseCodingSheetList();
        assertTrue(!sparseCodingSheets.isEmpty());
    }

    @Rollback
    @Test
    public void testNonUniqueCodes() throws IOException {
        CodingSheet sheet = new CodingSheet();
        sheet.markUpdated(getBasicUser());
        sheet.setTitle("test");
        sheet.setDescription("test");
        Throwable e = null;
        File content = new File(TestConstants.TEST_CODING_SHEET_DIR, "nonuniquecodes.csv");
        InformationResourceFile irFile = new InformationResourceFile();
        irFile.setId(100L);
        InformationResourceFileVersion version = new InformationResourceFileVersion();
        version.setFilename(content.getName());
        version.setInformationResourceFile(irFile);
        version.setInformationResourceId(100L);
        version.setFileVersionType(VersionType.UPLOADED_TEXT);
        TdarConfiguration.getInstance().getFilestore().store(ObjectType.RESOURCE, content, version);
        try {
            codingSheetService.parseUpload(sheet, version);
        } catch (Throwable ex) {
            ex.printStackTrace();
            e = ex;
        }
        assertEquals(CodingSheetParserException.class, e.getClass());
        logger.debug("dupes:{}", ((CodingSheetParserException) e).getContributingFactors());
        assertTrue(((CodingSheetParserException) e).getContributingFactors().contains("CODE2"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.tdar.struts.action.AbstractControllerITCase#getController()
     */
    @Override
    protected TdarActionSupport getController() {
        return null;
    }
}
