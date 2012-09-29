package org.tdar.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.tdar.TestConstants;

public class ThumbnailITCase extends AbstractAdminAuthenticatedWebTestCase {

    public static HashMap<String, String> docValMap = new HashMap<String, String>();
    public static HashMap<String, List<String>> docMultiValMap = new HashMap<String, List<String>>();
    public static HashMap<String, List<String>> docMultiValMapLab = new HashMap<String, List<String>>();
    public static String PROJECT_ID_FIELDNAME = "projectId";
    public static String IMAGE_TITLE_FIELDNAME = "image.title";
    public static String DESCRIPTION_FIELDNAME = "image.description";
    public static final String TEST_IMAGE_NAME = "handbook_of_archaeology.jpg";
    public static final String TEST_IMAGE = TestConstants.TEST_IMAGE_DIR + TEST_IMAGE_NAME;

    public static String PROJECT_ID = "2";
    public static String IMAGE_TITLE = "thumb test";
    public static String DESCRIPTION = "this is a test";

    public static String REGEX_IMAGE_VIEW = "\\/image\\/\\d+$";

    public ThumbnailITCase() {
    }

    @Test
    // create image as confidential, then log out and see if we see the image.
    public void testThumbnailOnViewPage() {

        // simulate an async file upload
        String ticketId = getPersonalFilestoreTicketId();
        uploadFileToPersonalFilestore(ticketId, TEST_IMAGE);

        gotoPage("/image/add");
        setInput(PROJECT_ID_FIELDNAME, PROJECT_ID);
        setInput("ticketId", ticketId);
        setInput(IMAGE_TITLE_FIELDNAME, IMAGE_TITLE);
        setInput(DESCRIPTION_FIELDNAME, DESCRIPTION);
        // FIXME: need to create input
        addFileProxyFields(0, true, TEST_IMAGE_NAME);
        setInput("resourceAvailability", "Public");
        submitForm();

        // the logged in creator should be able to see the image
        String path = internalPage.getUrl().getPath().toLowerCase();
        assertTrue("expecting to be on view page. Actual path:" + path, path.matches(REGEX_IMAGE_VIEW));
        String viewPage = path;
        String editPage = path + "/edit";
        logger.debug("view:" + viewPage);
        logger.debug("edit:" + editPage);
        logger.info(getPageText());
        assertTextPresent("/thumbnail");
        String pageCode = getPageCode();
        Pattern p = Pattern.compile("/filestore/(\\d+)(/?)");
        Matcher m = p.matcher(pageCode);
        List<Long> irFileVersionIds = new ArrayList<Long>();
        while (m.find()) {
            // logger.info(m.group(1));
            irFileVersionIds.add(Long.parseLong(m.group(1)));
        }

        // ONCE WE LOG OUT THE THUMBNAIL SHOULDN'T BE PRESENT BECAUSE THE RESOURCE IS CONFIDENTIAL
        logout();
        gotoPage(viewPage);
        assertTextNotPresent("/thumbnail");

        // LOG IN, BUT AS A USER THAT SHOULDN'T HAVE RIGHTS TO THE RESOURCE. NO THUMBNAIL.
        login(TestConstants.USERNAME, TestConstants.PASSWORD);
        gotoPage(viewPage);
        assertTextNotPresent("/thumbnail");

        assertDeniedAccess(irFileVersionIds);

        logout();
        // LOGIN, CHANGE FROM CONFIDENTIAL TO PUBLIC THEN LOGOUT... WE SHOULD SEE THE THUMBNAIL
        loginAdmin();
        gotoPage(editPage);
        setInput("fileProxies[0].action", "MODIFY_METADATA");
        setInput("fileProxies[0].confidential", "false");
        submitForm();
        logout();
        gotoPage(viewPage);
        assertTextNotPresent("/thumbnail");

        assertLoginPrompt(irFileVersionIds);

        // LOG IN, AS A USER THAT SHOULD HAVE RIGHTS TO THE RESOURCE. THUMBNAIL.
        login(TestConstants.USERNAME, TestConstants.PASSWORD);
        gotoPage(viewPage);
        assertTextPresent("/thumbnail");

        assertAllowedToViewIRVersionIds(irFileVersionIds);

        logout();

        // NOW MAKE THE PAGE EMBARGED -- THE THUMBNAIL SHOULD NOT BE VISIBLE
        loginAdmin();
        gotoPage(editPage);
        setInput("resourceAvailability", "Embargoed");
        submitForm();
        logout();
        gotoPage(viewPage);
        assertTextNotPresent("/thumbnail");

        assertLoginPrompt(irFileVersionIds);

        gotoPage(editPage);
        // LOG IN, BUT AS A USER THAT SHOULDN'T HAVE RIGHTS TO THE RESOURCE. NO THUMBNAIL.
        login(TestConstants.USERNAME, TestConstants.PASSWORD);
        gotoPage(viewPage);
        assertTextNotPresent("/thumbnail");

        assertDeniedAccess(irFileVersionIds);

    }

    public void assertDeniedAccess(List<Long> irFileVersionIds) {
        for (Long id : irFileVersionIds) {
            String pth = getBaseUrl() + "/filestore/" + id + "/get";
            int status = gotoPageWithoutErrorCheck(pth);
            logger.info(pth + ":" + status + " -" + getCurrentUrlPath());
            assertEquals("should not be allowed", 403, status);
        }
    }

    public void assertLoginPrompt(List<Long> irFileVersionIds) {
        for (Long id : irFileVersionIds) {
            String pth = getBaseUrl() + "/filestore/" + id + "/get";
            int status = gotoPageWithoutErrorCheck(pth);
            logger.info(pth + ":" + status + " -" + getCurrentUrlPath());
            assertFalse("Should always be a login prompt", getCurrentUrlPath().equals(pth));
        }
    }

    public void assertAllowedToViewIRVersionIds(List<Long> irFileVersionIds) {
        for (Long id : irFileVersionIds) {
            String pth = getBaseUrl() + "/filestore/" + id + "/get";
            int status = gotoPageWithoutErrorCheck(pth);
            logger.info(pth + ":" + status + " -" + getCurrentUrlPath());
            assertEquals("Should always be allowed", 200, status);
        }
    }

    // TODO: add tests for hacked urls (e.g. using /image/xyx/thumnail where xyz is restricted and/or is not actually a thumbnail)

}
