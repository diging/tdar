/**
 * 
 * @author Adam Brin
 *
 */

package org.tdar.web.functional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.tdar.TestConstants.TEST_DOCUMENT;
import static org.tdar.TestConstants.TEST_DOCUMENT_NAME;
import static org.tdar.core.bean.entity.permissions.GeneralPermissions.MODIFY_RECORD;
import static org.tdar.core.bean.entity.permissions.GeneralPermissions.VIEW_ALL;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.WebElement;
import org.tdar.core.bean.coverage.CoverageType;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.ResourceCreatorRole;
import org.tdar.core.bean.entity.permissions.GeneralPermissions;
import org.tdar.core.bean.resource.InformationResourceFile.FileAccessRestriction;
import org.tdar.core.bean.resource.Language;
import org.tdar.core.bean.resource.ResourceNoteType;
import org.tdar.web.AbstractWebTestCase;

public class CompleteDocumentSeleniumWebITCase extends AbstractBasicSeleniumWebITCase {
    public static HashMap<String, String> docValMap;
    public static HashMap<String, List<String>> docMultiValMap = new LinkedHashMap<String, List<String>>();
    public static HashMap<String, List<String>> docMultiValMapLab = new LinkedHashMap<String, List<String>>();
    // we will assert the presence of these values, but we don't care what order they appear
    public static Map<String, String> docUnorderdValMap = new HashMap<String, String>();
    public static List<String> alternateTextLookup = new ArrayList<String>();
    public static List<String> alternateCodeLookup = new ArrayList<String>();

    public static String REGEX_DOCUMENT_VIEW = ".+\\/document\\/\\d+$";
    public static Pattern PATTERN_DOCUMENT_VIEW = Pattern.compile(REGEX_DOCUMENT_VIEW);

    public CompleteDocumentSeleniumWebITCase() {
        docValMap = new LinkedHashMap<String, String>();
        // removing inline implementation of HashMap to remove serialization warning
        alternateTextLookup.add(AbstractWebTestCase.RESTRICTED_ACCESS_TEXT);

        docValMap.put("projectId", "-1");
        docValMap.put("document.title", "My Sample Document");
        docValMap.put("document.documentType", "OTHER");
        docValMap.put("authorshipProxies[0].person.firstName", "Jim");
        docValMap.put("authorshipProxies[0].person.lastName", "deVos");
        docValMap.put("authorshipProxies[0].person.email", "testabc123@test.com");
        docValMap.put("authorshipProxies[0].person.institution.name", "A wholly new institution name");
        docValMap.put("authorshipProxies[0].person.id", "");
        docValMap.put("authorshipProxies[0].role", ResourceCreatorRole.AUTHOR.name());
        alternateTextLookup.add(ResourceCreatorRole.AUTHOR.getLabel());
        docValMap.put("authorshipProxies[1].person.institution.name", "SOME INSTITUTION");
        docValMap.put("authorshipProxies[1].person.firstName", "test");
        docValMap.put("authorshipProxies[1].person.lastName", "test");
        docValMap.put("authorshipProxies[1].role", ResourceCreatorRole.AUTHOR.name());
        docValMap.put("authorshipProxies[1].person.email", "testabc1233@test.com");
        docValMap.put("authorshipProxies[1].person.id", "");
        docValMap.put("document.description", "A resource description");
        docValMap.put("document.date", "1923");
        // docValMap.put("authorizedUsers[0].user.id", "121");
        // docValMap.put("authorizedUsers[1].user.id", "5349");
        // docValMap.put("authorizedUsers[0].generalPermission", GeneralPermissions.MODIFY_RECORD.name());
        // docValMap.put("authorizedUsers[1].generalPermission", GeneralPermissions.VIEW_ALL.name());
        // docValMap.put("authorizedUsers[0].user.tempDisplayName", "Michelle Elliott");
        // docValMap.put("authorizedUsers[1].user.tempDisplayName", "Joshua Watts");
        alternateCodeLookup.add(GeneralPermissions.MODIFY_RECORD.name());
        alternateCodeLookup.add(GeneralPermissions.VIEW_ALL.name());
        docValMap.put("document.doi", "doi:10.1016/j.iheduc.2003.11.004");
        docValMap.put("document.isbn", "9780385483995");
        alternateTextLookup.add(Language.SPANISH.getLabel());
        docValMap.put("resourceLanguage", Language.SPANISH.name());
        docValMap.put("document.url", "http://www.tdar.org");
        docValMap.put("publisherName", "test");
        docValMap.put("document.publisherLocation", "new york");
        docValMap.put("document.edition", "3rd");
        docValMap.put("document.seriesName", "series title");
        docValMap.put("document.seriesNumber", "series number");
        docValMap.put("document.copyLocation", "copy location");
        // book title, journal name, issn field not relevant for OTHER documentType
        // docValMap.put("document.bookTitle", "book title");
        // docValMap.put("document.journalName", "journal name");
        // docValMap.put("document.issn", "0002-9114");
        docValMap.put("document.startPage", "mcmxvii");
        docValMap.put("document.endPage", "MMMvii");
        docValMap.put("document.journalNumber", "1");
        docValMap.put("document.volume", "25");

        docValMap.put("siteNameKeywords[0]", "sandy");
        docValMap.put("uncontrolledSiteTypeKeywords[0]", "uncontrolled");
        docValMap.put("otherKeywords[0]", "other");
        docValMap.put("uncontrolledCultureKeywords[0]", "German");
        docValMap.put("geographicKeywords[0]", "Georgia");

        // FIXME: selenium can't manipulate these because they aren't visible. That said, we should do it the way a user would (e.g. 43°11′50″N)
        // docValMap.put("latitudeLongitudeBoxes[0].maximumLatitude", "41.83228739643032");
        // docValMap.put("latitudeLongitudeBoxes[0].maximumLongitude", "-71.39860153198242");
        // docValMap.put("latitudeLongitudeBoxes[0].minimumLatitude", "41.82608370627639");
        // docValMap.put("latitudeLongitudeBoxes[0].minimumLongitude", "-71.41018867492676");
        //

        docValMap.put("temporalKeywords[0]", "before time");
        docValMap.put("coverageDates[0].startDate", "1200");
        docValMap.put("coverageDates[0].endDate", "1500");
        docValMap.put("coverageDates[0].dateType", CoverageType.CALENDAR_DATE.name());
        alternateTextLookup.add(CoverageType.CALENDAR_DATE.getLabel());
        docValMap.put("coverageDates[1].startDate", "1200");
        docValMap.put("coverageDates[1].endDate", "1000");
        docValMap.put("coverageDates[1].dateType", CoverageType.RADIOCARBON_DATE.name());
        alternateTextLookup.add(CoverageType.RADIOCARBON_DATE.getLabel());
        docValMap.put("resourceProviderInstitutionName", "Digital Antiquity");
        // FIXME: notes not maintaining order
        docValMap.put("resourceNotes[0].type", ResourceNoteType.GENERAL.name());
        alternateTextLookup.add(ResourceNoteType.GENERAL.getLabel());
        docValMap.put("resourceNotes[0].note", "A Moose once bit my sister...");
        // introduce a gap in the list (e.g. a user adds notes and then deleted the middle item)
        docValMap.put("resourceNotes[1].type", ResourceNoteType.REDACTION.name());
        alternateTextLookup.add(ResourceNoteType.REDACTION.getLabel());
        docValMap.put("resourceNotes[1].note", "We apologise for the fault in the subtitles. Those responsible have been sacked.");

        docMultiValMap.put("investigationTypeIds", Arrays.asList(new String[] { "1", "2", "3", "5" }));
        docMultiValMap.put("approvedSiteTypeKeywordIds", Arrays.asList(new String[] { "273", "312" }));
        docMultiValMap.put("materialKeywordIds", Arrays.asList(new String[] { "15", "12", "5" }));
        docMultiValMap.put("approvedCultureKeywordIds", Arrays.asList(new String[] { "12", "15", "26" }));

        docMultiValMapLab.put("investigationTypeIds",
                Arrays.asList(new String[] { "Archaeological Overview", "Architectural Survey", "Collections Research", "Data Recovery / Excavation" }));
        docMultiValMapLab.put("approvedSiteTypeKeywordIds", Arrays.asList(new String[] { "Agricultural or Herding", "Ancient church / religious structure" }));
        docMultiValMapLab.put("materialKeywordIds", Arrays.asList(new String[] { "Fire Cracked Rock", "Mineral", "Wood" }));
        docMultiValMapLab.put("approvedCultureKeywordIds", Arrays.asList(new String[] { "Hopewell", "Middle Woodland", "African American" }));

        // look for these values in end product (and the edit form), but the system may alter the form field names (e.g. due to culling nulls in lists)
        docUnorderdValMap.put("resourceNotes[5].type", ResourceNoteType.RIGHTS_ATTRIBUTION.name());
        docUnorderdValMap.put("resourceNotes[5].note", "I'm not internationally known, but I'm known to rock a microphone.");
    }

    private void prepIndexedFields() {
        prepIndexedFields(docValMap.keySet());
        prepIndexedFields(docMultiValMap.keySet());
        prepIndexedFields(docUnorderdValMap.keySet());
    }

    @Test
    public void testAuthUser() {
        gotoPage("/document/add");
        setFieldByName("document.title", "My Sample Document");
        setFieldByName("document.documentType", "OTHER");
        setFieldByName("document.description", "A resource description");
        setFieldByName("document.date", "1923");
        setFieldByName("projectId", "-1");
        find("#accessRightsRecordsAddAnotherButton").click();
        find("#accessRightsRecordsAddAnotherButton").click();
        addAuthuser("authorizedUsers[0].user.tempDisplayName", "authorizedUsers[0].generalPermission", "Michelle Elliott", "michelle.elliott@asu.edu",
                MODIFY_RECORD);
        addAuthuser("authorizedUsers[1].user.tempDisplayName", "authorizedUsers[1].generalPermission", "Joshua Watts", "joshua.watts@asu.edu", VIEW_ALL);
        submitForm();
    }
    
    @Test
    public void testCreateDocumentEditSavehasResource() {
        gotoPage("/document/add");
        WebElement form = find("#metadataForm").first();

        HashMap<String, String> docValMap2 = new HashMap<String, String>();
        docValMap2.put("document.title", "My Sample Document");
        docValMap2.put("document.documentType", "OTHER");
        docValMap2.put("document.description", "A resource description");
        docValMap2.put("document.date", "1923");
        docValMap2.put("projectId", "-1");
        String ORIGINAL_START_DATE = "1200";
        String COVERAGE_START = "coverageDates[0].startDate";
        docValMap2.put(COVERAGE_START, ORIGINAL_START_DATE);
        String ORIGINAL_END_DATE = "1500";
        docValMap2.put("coverageDates[0].endDate", ORIGINAL_END_DATE);
        docValMap2.put("coverageDates[0].dateType", CoverageType.CALENDAR_DATE.name());

        for (String key : docValMap2.keySet()) {
            find(By.name(key)).val(docValMap2.get(key));
        }

        submitForm();

        String path = getDriver().getCurrentUrl();
        logger.info(find("body").getText());
        assertTrue("expecting to be on view page. Actual path:" + path + "\n" + find("body").getText(), path.matches(REGEX_DOCUMENT_VIEW));
        assertTrue("expected value on view page", sourceContains(ORIGINAL_START_DATE));
        assertTrue("expected value on view page", sourceContains(ORIGINAL_END_DATE));

        assertEquals("count of edit buttons", 1, find(By.partialLinkText("EDIT")).size());
        find(By.partialLinkText("EDIT")).click();
        expandAllTreeviews();

        String NEW_START_DATE = "100";
        find(By.name(COVERAGE_START)).val(NEW_START_DATE);
        submitForm();

        path = getDriver().getCurrentUrl();
        logger.info(find("body").getText());
        assertTrue("expecting to be on view page. Actual path:" + path + "\n" + find("body").getText(), path.matches(REGEX_DOCUMENT_VIEW));
        assertTrue(sourceContains(NEW_START_DATE));
        assertFalse(sourceContains(ORIGINAL_START_DATE));
        assertTrue(sourceContains(ORIGINAL_END_DATE));
        logger.trace(find("body").getText());
    }

    @Test
    public void testCreateDocument() {
        gotoPage("/document/add");
        expandAllTreeviews();
        prepIndexedFields();
        uploadFile(FileAccessRestriction.CONFIDENTIAL, new File(TEST_DOCUMENT));

        docValMap.putAll(docUnorderdValMap);

        // fill in various text fields
        for (Map.Entry<String, String> entry : docValMap.entrySet()) {
            find(By.name(entry.getKey())).val(entry.getValue());
        }

        // check various keyword checkboxes
        for (String key : docMultiValMap.keySet()) {
            for (String val : docMultiValMap.get(key)) {
                try {
                    logger.trace("setting value for field: {} - start", key);
                    find(By.name(key)).val(val);
                    logger.trace("setting value for field: {} - end", key);
                } catch (ElementNotVisibleException en) {
                    logger.error("element not visible: {} {}", key, val);
                    fail("could not find " + key + " because it was not visible");
                }
            }
        }

        // add some authusers
        find("#accessRightsRecordsAddAnotherButton").click();
        find("#accessRightsRecordsAddAnotherButton").click();

        addAuthuser("authorizedUsers[0].user.tempDisplayName", "authorizedUsers[0].generalPermission", "Michelle Elliott", "michelle.elliott@asu.edu",
                MODIFY_RECORD);
        addAuthuser("authorizedUsers[1].user.tempDisplayName", "authorizedUsers[1].generalPermission", "Joshua Watts", "joshua.watts@asu.edu", VIEW_ALL);

        docUnorderdValMap.put("authorizedUsers[0].user.id", "121");
        docUnorderdValMap.put("authorizedUsers[1].user.id", "5349");
        docUnorderdValMap.put("authorizedUsers[0].generalPermission", MODIFY_RECORD.name());
        docUnorderdValMap.put("authorizedUsers[1].generalPermission", VIEW_ALL.name());
        docUnorderdValMap.put("authorizedUsers[0].user.tempDisplayName", "Michelle Elliott");
        docUnorderdValMap.put("authorizedUsers[1].user.tempDisplayName", "Joshua Watts");

        // add a person to satisfy the confidential file requirement
        addPersonWithRole(new Person("loblaw", "robert", "bobloblaw@netflix.com"), "creditProxies[0]", ResourceCreatorRole.CONTACT);

        logger.info(getDriver().getPageSource());
        submitForm();

        String path = getDriver().getCurrentUrl();
        assertTrue("expecting to be on view page. Actual path:" + path + "\n" + find("body").getText(), path.matches(REGEX_DOCUMENT_VIEW));

        logger.trace(find("body").getText());
        for (String key : docValMap.keySet()) {
            // avoid the issue of the fuzzy distances or truncation... use just
            // the top of the lat/long
            if (key.startsWith("latitudeLongitudeBox")) {
                assertTrue(textContains(docValMap.get(key).substring(0, docValMap.get(key).indexOf("."))));
                // these are displayed by "type" or not "displayed"
            } else if (key.equals("document.documentType") || key.equals("resourceLanguage")) {
                assertTrue(textContains(docValMap.get(key)));
            } else if (!key.equals("document.journalName") && !key.equals("document.bookTitle") && !key.startsWith("authorInstitutions")
                    && !key.equals(AbstractWebTestCase.PROJECT_ID_FIELDNAME) && !key.contains("Ids") && !key.contains("Email") && !key.equals("ticketId")
                    && !key.contains("generalPermission")
                    && !key.contains(".id") && !key.contains(".email") && !key.contains(".type") && !key.contains(".dateType") && !key.contains(".licenseType")
                    && !key.contains("role")
                    && !key.contains("person.institution.name")) {
                assertTrue("looking for:" + docValMap.get(key), textContains(docValMap.get(key)));
            }
        }
        for (String alt : alternateTextLookup) {
            assertTrue("looking for '" + alt + "' in source", textContains(alt));
        }
        for (String alt : alternateCodeLookup) {
            assertTrue("looking for '" + alt + "' in source", sourceContains(alt));
        }

        assertFalse(sourceContains("embargo"));
        for (String key : docMultiValMapLab.keySet()) {
            for (String val : docMultiValMapLab.get(key)) {
                assertTrue("looking for '" + val + "' in source", sourceContains(val));
            }
        }

        // go to the edit page and ensure (some) of the form fields and values that we originally created are still present
        find(By.partialLinkText("EDIT")).click();
        expandAllTreeviews();
        logger.debug("----now on edit page----");
        logger.trace(find("body").getText());

        for (String key : docValMap.keySet()) {
            String val = docValMap.get(key);

            // ignore id fields, file uploads, and fields with UPPER CASE VALUES (huh?)
            if (key.contains("Ids") || key.contains("upload") || val.toUpperCase().equals(val))
                continue;

            if (docUnorderdValMap.containsKey(key)) {
                assertTrue("looking for '" + val + "' in text", textContains(val));
            } else {
                assertEquals(find(By.name(key)).val(), val);
            }
        }

        for (String key : docMultiValMap.keySet()) {
            for (String val : docMultiValMap.get(key)) {
                assertTrue(String.format("key:%s  expected val:%s", key, val), find(By.name(key)).vals().contains(val));
            }
        }

        //specific checks for auth users we added earlier
        String sectionText = find("#divAccessRights").getText().toLowerCase();
        logger.debug("\n\n------ access rights text ---- \n" + sectionText);

        assertThat(sectionText, containsString("joshua watts"));
        assertThat(sectionText, containsString("michelle elliott"));
        assertThat(sectionText, containsString(VIEW_ALL.getLabel().toLowerCase()));
        assertThat(sectionText, containsString(MODIFY_RECORD.getLabel().toLowerCase()));

        // make sure our 'async' file was added to the resource
        assertThat(getSource(), containsString(TEST_DOCUMENT_NAME));
    }

    private void addAuthuser(String nameField, String selectField, String name, String email, GeneralPermissions permissions) {

        WebElement blankField = find(By.name(nameField)).first();
        if (!selectAutocompleteValue(blankField, name, email)) {
            String fmt = "Failed to add authuser %s because selenium failed to select a user from the autocomplete " +
                    "dialog.  Either the autocomplete failed to appear or an appropriate value was not in the " +
                    "menu.";
            fail(String.format(fmt, email));
        }
        find(By.name(selectField)).val(permissions.name());

    }

}
