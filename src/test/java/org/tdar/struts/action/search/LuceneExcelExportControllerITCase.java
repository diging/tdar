package org.tdar.struts.action.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.queryParser.ParseException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.exception.StatusCode;
import org.tdar.core.service.ExcelService;
import org.tdar.core.service.search.SearchIndexService;
import org.tdar.struts.action.TdarActionException;
import org.tdar.web.SessionData;

import com.opensymphony.xwork2.Action;

@Transactional
public class LuceneExcelExportControllerITCase extends AbstractSearchControllerITCase {

    // the first few rows of the export have stats, column names, spacing, yada yada...
    private static final int EXCEL_EXPORT_HEADER_ROWCOUNT = 5;

    @Autowired
    SearchIndexService searchIndexService;

    @Autowired
    ExcelService excelService;

    private TdarUser currentUser = null;

    @Test
    @Rollback(true)
    public void testExcelExport() throws InstantiationException, IllegalAccessException, ParseException, FileNotFoundException, IOException,
            InvalidFormatException, TdarActionException {
        searchIndexService.indexAll(getAdminUser(), Resource.class);
        // currentUser = getBasicUser();
        AdvancedSearchDownloadAction controller = generateNewInitializedController(AdvancedSearchDownloadAction.class, genericService.find(TdarUser.class, getBasicUserId()));

        controller.setServletRequest(getServletRequest());
        doSearch("");
        assertEquals(Action.SUCCESS, controller.viewExcelReport());
        assertFalse(controller.getSearchPhrase() + " should not have bold tag", controller.getSearchPhrase().toLowerCase().contains("<b>"));
        File tempFile = File.createTempFile("report", ".xls");
        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        long copyLarge = IOUtils.copyLarge(controller.getInputStream(), fileOutputStream);

        fileOutputStream.close();
        logger.debug("tempFile: {}", tempFile);

        Workbook workbook = WorkbookFactory.create(new FileInputStream(tempFile));
        Sheet sheet = workbook.getSheet("results");
        Assert.assertEquals(TdarConfiguration.getInstance().getSearchExcelExportRecordMax(), sheet.getLastRowNum() - EXCEL_EXPORT_HEADER_ROWCOUNT);
    }

    @Test
    @Rollback(true)
    public void testExcelFailUnauthenticatedExport() throws InstantiationException, IllegalAccessException, ParseException, FileNotFoundException, IOException,
            TdarActionException {
        setIgnoreActionErrors(true);
        searchIndexService.indexAll(getAdminUser(), Resource.class);
        currentUser = null;
        AdvancedSearchDownloadAction controller = generateNewInitializedController(AdvancedSearchDownloadAction.class, genericService.find(TdarUser.class, getBasicUserId()));
        controller.setSessionData(new SessionData()); // create unauthenticated session
        getServletRequest().setAttribute("RequestURI", "http://www.test.com");
//        controller = generateNewInitializedController(AdvancedSearchController.class);

        controller.setServletRequest(getServletRequest());
        doSearch("");
        TdarActionException except = null;
        try {
            controller.viewExcelReport();
        } catch (TdarActionException e) {
            except = e;
        }
        assertNotNull(except);
        assertEquals(StatusCode.UNAUTHORIZED.getHttpStatusCode(), except.getStatusCode());
    }

    @Override
    public TdarUser getSessionUser() {
        return currentUser;
    }

}
