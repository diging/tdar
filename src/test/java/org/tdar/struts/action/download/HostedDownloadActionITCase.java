package org.tdar.struts.action.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpHeaders.REFERER;

import java.io.File;
import java.io.FileOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.Rollback;
import org.tdar.TestConstants;
import org.tdar.core.bean.collection.DownloadAuthorization;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.collection.ResourceCollection.CollectionType;
import org.tdar.core.bean.resource.Document;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.service.PdfService;
import org.tdar.core.service.download.DownloadService;
import org.tdar.struts.action.AbstractDataIntegrationTestCase;

import com.opensymphony.xwork2.Action;

public class HostedDownloadActionITCase extends AbstractDataIntegrationTestCase {

    private Document doc;

    @Autowired
    DownloadService downloadService;
    int COVER_PAGE_WIGGLE_ROOM = 155_000;

    @Autowired
    PdfService pdfService;


    @Test
    @Rollback
    public void testValidHostedDownload() throws Exception {

        HostedDownloadAction controller = generateNewController(HostedDownloadAction.class);
        init(controller, null);
        controller.setApiKey("test");
        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader("referer", "http://test.tdar.org/blog/this-is-my-test-url");
        controller.setServletRequest(request);
        controller.setInformationResourceFileVersionId(doc.getFirstInformationResourceFile().getLatestPDF().getId());

        controller.prepare();
        assertEquals(Action.SUCCESS, controller.execute());
        assertEquals(TestConstants.TEST_DOCUMENT_NAME, controller.getDownloadTransferObject().getFileName());
        IOUtils.copyLarge(controller.getDownloadTransferObject().getInputStream(), new FileOutputStream(new File("target/out.pdf")));
    }

    @Test
    @Rollback
    public void testInvalidHostedDownloadReferrer() throws Exception {
        setIgnoreActionErrors(true);

        // test bad referrer
        HostedDownloadAction controller = generateNewController(HostedDownloadAction.class);
        init(controller, null);
        controller.setApiKey("test");
        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader(REFERER, "http://tdar.org/blog/this-is-my-test-url");
        controller.setServletRequest(request);
        controller.setInformationResourceFileVersionId(doc.getFirstInformationResourceFile().getLatestPDF().getId());

        controller.prepare();
        assertTrue(CollectionUtils.isNotEmpty(controller.getActionErrors()));
    }

    @Test(expected = TdarRecoverableRuntimeException.class)
    @Rollback
    public void testMissingHostedDownloadReferrer() throws Exception {
        setIgnoreActionErrors(true);
        // test no referrer
        HostedDownloadAction controller = generateNewController(HostedDownloadAction.class);
        init(controller, null);
        controller.setApiKey("test");
        MockHttpServletRequest request = new MockHttpServletRequest();

        controller.setServletRequest(request);
        controller.setInformationResourceFileVersionId(doc.getFirstInformationResourceFile().getLatestPDF().getId());

        controller.prepare();
    }

    @Test
    @Rollback
    public void testInvalidApiKeyHostedDownloadReferrer() throws Exception {
        setIgnoreActionErrors(true);

        HostedDownloadAction controller = generateNewController(HostedDownloadAction.class);
        init(controller, null);
        controller.setApiKey("testasasfasf");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(REFERER, "http://bobs-file-hut.ru/tdar");

        controller.setServletRequest(request);
        controller.setInformationResourceFileVersionId(doc.getFirstInformationResourceFile().getLatestPDF().getId());

        controller.prepare();
        assertTrue(CollectionUtils.isNotEmpty(controller.getActionErrors()));
    }

    @Test
    @Rollback
    public void testMissingApiKeyHostedDownloadReferrer() {
        setIgnoreActionErrors(true);

        HostedDownloadAction controller = generateNewController(HostedDownloadAction.class);
        init(controller, null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(REFERER, "http://invalid-apikey-exchange.biz/archaeology");

        controller.setServletRequest(request);
        controller.setInformationResourceFileVersionId(doc.getFirstInformationResourceFile().getLatestPDF().getId());

        controller.prepare();
        assertTrue(CollectionUtils.isNotEmpty(controller.getActionErrors()));
    }

    @Before
    public void setup() throws InstantiationException, IllegalAccessException {
        doc = generateDocumentWithFileAndUseDefaultUser();
        ResourceCollection collection = new ResourceCollection(CollectionType.SHARED);
        collection.setName("authorized collection");
        collection.setDescription(collection.getName());
        collection.markUpdated(getAdminUser());
        collection.getResources().add(doc);
        genericService.saveOrUpdate(collection);
        doc.getResourceCollections().add(collection);
        genericService.saveOrUpdate(doc);
        DownloadAuthorization downloadAuthorization = new DownloadAuthorization();
        downloadAuthorization.setApiKey("test");
        downloadAuthorization.setResourceCollection(collection);
        downloadAuthorization.getRefererHostnames().add("test.tdar.org");
        downloadAuthorization.getRefererHostnames().add("whatever.tdar.org");
        genericService.saveOrUpdate(downloadAuthorization);
    }

}
