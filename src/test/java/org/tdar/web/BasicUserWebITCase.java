package org.tdar.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.tdar.TestConstants;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.junit.MultipleTdarConfigurationRunner;
import org.tdar.junit.RunWithTdarConfiguration;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;


@RunWith(MultipleTdarConfigurationRunner.class)
@RunWithTdarConfiguration(runWith = { RunWithTdarConfiguration.TDAR, RunWithTdarConfiguration.FAIMS })
public class BasicUserWebITCase extends AbstractAuthenticatedWebTestCase {
    private static final String DESCRIPTION = "descriptionthisisauniquetest";
    private static final String TITLE = "title of a test document";
    private static final String TESTCOLLECTIONNAME = "TESTCOLLECTIONNAME";

    @Test
    public void testDraftPermissions() {
        gotoPage("/document/add");
        HashMap<String, String> docValMap = new HashMap<String, String>();
        docValMap.put(PROJECT_ID_FIELDNAME, "1");
        docValMap.put("document.title", TITLE);
        docValMap.put("document.description", DESCRIPTION);
        docValMap.put("document.documentType", "BOOK");
        docValMap.put("resourceCollections[0].name", TESTCOLLECTIONNAME);
        docValMap.put("document.date", "1923");
        docValMap.put("status", "DRAFT");
        if (TdarConfiguration.getInstance().getCopyrightMandatory()) {
            docValMap.put(TestConstants.COPYRIGHT_HOLDER_PROXY_INSTITUTION_NAME, "Elsevier");
        }
        for (String key : docValMap.keySet()) {
            setInput(key, docValMap.get(key));
        }
        submitForm();
        assertTextPresent(TESTCOLLECTIONNAME);
        clickLinkWithText(TESTCOLLECTIONNAME);
        assertTextPresent(TITLE);
        gotoPage("/search/results?query=" + DESCRIPTION);
        assertTextPresent(TITLE);
        assertTextPresent(DESCRIPTION); // should be in the "search description"
        logout();
        gotoPageWithoutErrorCheck("/search/results?includedStatuses=DRAFT&useSubmitterContext=true&query=" + DESCRIPTION);
        assertTextNotPresent(TITLE);
        assertErrorsPresent();
        assertTextPresent(DESCRIPTION);

    }

    private void chooseFirstBillingAccount() {
        HtmlElement input = null;
        if (getTdarConfiguration().getInstance().isPayPerIngestEnabled()) {
            try {
                input = getInput("accountId");
            } catch (ElementNotFoundException ex) {
            }
        } else {
            assertTrue("there should be no accountId input if pay-per-ingest is enabled", input == null);
        }
        if (input == null)
            return;

        // get the 2nd option which is the first billing account
        Iterator<DomElement> options = input.getChildElements().iterator();
        options.next(); // skip the blank
        DomElement option = options.next();
        String accountId = option.getAttribute("value");
        setInput("accountId", accountId);
    }

    public void assertViewPage() {
        String url = internalPage.getUrl().toString();
        assertTrue("expecting to be on the view page.  actual page is: " + url, url.matches("^.*\\d+$"));
    }
    
    public void assertEditPageForInputResult() {
        String url = internalPage.getUrl().toString();
        assertTrue("expecting to be on the edit page due to INPUT result.  actual page is: " + url, url.matches(".*save.action.*"));
    }

    public void fillOutRequiredfields(ResourceType resourceType) {
        String prefix = resourceType.getFieldName();
        setInput(prefix + ".title", "minimal test");
        if (!resourceType.isProject()) {
            if (getInput("projectId") == null) {
                logger.info(getPageBodyCode());
            }
            setInput("projectId", "-1");
            setInput(prefix + ".date", "2002");
            if (TdarConfiguration.getInstance().getCopyrightMandatory()) {
                setInput(TestConstants.COPYRIGHT_HOLDER_PROXY_INSTITUTION_NAME, "Elsevier");
            }
        }
        setInput(prefix + ".description", "testing");
    }
    

    // create a resource with only required field values. assert that we land on the view page. This will hopefully weed out silly
    // mistakes like omitting necessary form field or duplicating a form field.
    public void createMinimalResource(ResourceType resourceType) {
        createMinimalResource(resourceType, null);
    }

    public void createMinimalResource(ResourceType resourceType, String textInput) {
        String uri = String.format("/%s/add", resourceType.getUrlNamespace());
        gotoPage(uri);
        chooseFirstBillingAccount();
        fillOutRequiredfields(resourceType);
        if (textInput != null) {
            setInput("fileTextInput", textInput);
        }
        submitForm();
        assertViewPage();

    }

    @Test
    public void testMinimalCreate() {
        for (ResourceType resourceType : ResourceType.values()) {
            if (resourceType.isSupporting()) {
                if (resourceType.isCodingSheet()) {
                    createMinimalResource(resourceType, "doh, a female dear\nfa, a long long way to run\n");
                }
                if (resourceType.isOntology()) {
                    createMinimalResource(resourceType, "level1\n\tlevel2\n");
                }
            } else {
                createMinimalResource(resourceType);
            }
        }
    }

    @Test
    public void testTicketIdAfterValidationFail() {
        String ticketId = getPersonalFilestoreTicketId();
        gotoPage("/image/add");
        logger.debug("\n\nbody page \n\n{}\n\n\n");
        fillOutRequiredfields(ResourceType.IMAGE);
        setInput("ticketId", ticketId);
        //set the ticket id, but not necessary to add a file.
        setInput("image.title", "");
        submitForm();
        assertEditPageForInputResult();
        String newTicketId = getInput("ticketId").getAttribute("value");
        assertEquals("ticketId should be same as original edit form", ticketId, newTicketId);
    }

    @Test
    public void testIsGeoLocationToBeUsed() {
        gotoPage("/document/add");
        if (TdarConfiguration.getInstance().isGeoLocationToBeUsed()) {
            assertTextPresentInCode("TDAR.maps.defaults.isGeoLocationToBeUsed = true;");
        } else {
            assertTextPresentInCode("TDAR.maps.defaults.isGeoLocationToBeUsed = false;");
        }
    }
}
