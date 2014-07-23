package org.tdar.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.exceptions.ConfigurationException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.tdar.TestConstants;
import org.tdar.core.bean.keyword.CultureKeyword;
import org.tdar.core.bean.resource.Document;
import org.tdar.core.bean.resource.Language;
import org.tdar.core.bean.resource.Project;
import org.tdar.core.service.GenericKeywordService;
import org.tdar.core.service.ImportService;
import org.tdar.core.service.ObfuscationService;
import org.tdar.core.service.ReflectionService;
import org.tdar.core.service.XmlService;
import org.tdar.struts.action.search.AbstractSearchControllerITCase;
import org.tdar.utils.jaxb.JaxbParsingException;
import org.tdar.utils.jaxb.JaxbValidationEvent;
import org.tdar.utils.json.JsonProjectLookupFilter;
import org.xml.sax.SAXException;

public class JAXBITCase extends AbstractSearchControllerITCase {

    private static final String BEDOUIN = "bedouin";

    private static final String NABATAEAN = "Nabataean";

    @Autowired
    XmlService xmlService;

    @Autowired
    ReflectionService reflectionService;

    @Autowired
    ImportService importService;

    @Autowired
    ObfuscationService obfuscationService;

    @Autowired
    GenericKeywordService genericKeywordService;

    @Test
    public void testJAXBDocumentConversion() throws Exception {
        Document document = genericService.find(Document.class, 4232l);
        String xml = xmlService.convertToXML(document);
        logger.info(xml);
    }

    @Test
    public void testJsonExport() throws Exception {
        Document document = genericService.find(Document.class, 4232l);
        StringWriter sw = new StringWriter();
        document.getProject().getCultureKeywords().add(new CultureKeyword(NABATAEAN));
        document.setInheritingCulturalInformation(true);
        xmlService.convertToJson(document, sw, null);
        logger.info(sw.toString());
        assertTrue(sw.toString().contains(NABATAEAN));
        Project project = genericService.find(Project.class, 3805l);
        project.getCultureKeywords().add(new CultureKeyword(BEDOUIN));
        sw = new StringWriter();
        xmlService.convertToJson(project, sw, JsonProjectLookupFilter.class);
        logger.info(sw.toString());
        assertTrue(sw.toString().contains(BEDOUIN));
    }

    @Test
    public void testJAXBProjectConversion() throws Exception {
        Project project = genericService.find(Project.class, 2420l);
        String xml = xmlService.convertToXML(project);
        logger.info(xml);
    }

    @Test
    @Rollback(false)
    public void testJaxbRoundtrip() throws Exception {
        Project project = genericService.find(Project.class, 3805l);

        final String xml = xmlService.convertToXML(project);
        logger.info(xml);
        genericService.detachFromSession(project);

        setVerifyTransactionCallback(new TransactionCallback<Project>() {

            @Override
            public Project doInTransaction(TransactionStatus arg0) {
                boolean exception = false;
                try {
                    Project newProject = (Project) xmlService.parseXml(new StringReader(xml));
                    newProject.markUpdated(getAdminUser());
                    newProject = importService.bringObjectOntoSession(newProject, getAdminUser(), true);
                } catch (Exception e) {
                    exception = true;
                    logger.warn("exception: {}", e);
                }
                assertFalse(exception);
                return null;
            }
        });
    }

    @Test
    @Rollback
    @Ignore("Fixture for testing")
    public void testLoad() throws FileNotFoundException, Exception {
        Project p = new Project();
        String convertToXML = xmlService.convertToXML(p);
        // logger.debug(convertToXML);
        try {
            xmlService.parseXml(new StringReader(convertToXML));
            xmlService.parseXml(new FileReader(new File("c:/Users/abrin/Documents/1979.020.xml"))); // confirm goodxml loads fine
        } catch (Exception e) {
            logger.error("{}", e);
        }

    }

    // make sure we're detecting enum errors.
    @Test
    @Rollback
    public void testLoadWithBadEnumValue() throws Exception {

        Document document = createAndSaveNewInformationResource(Document.class);
        final Language VALID_LANGUAGE = Language.DUTCH;
        final String BAD_LANGUAGE = "Dagnabit!"; // har har
        document.setResourceLanguage(VALID_LANGUAGE);
        String goodXml = xmlService.convertToXML(document);
        Exception parseException = null;
        String badXml = goodXml.replace(
                "<tdar:resourceLanguage>" + VALID_LANGUAGE.name() + "</tdar:resourceLanguage>",
                "<tdar:resourceLanguage>" + BAD_LANGUAGE + "</tdar:resourceLanguage>");

        Object obj = null;
        xmlService.parseXml(new StringReader(goodXml)); // confirm goodxml loads fine
        try {
            obj = xmlService.parseXml(new StringReader(badXml));
        } catch (JaxbParsingException e) {
            parseException = e;
            logger.debug("parsing exception", e);
            logger.debug("exception events:{}", e.getEvents());
            assertEquals(2, e.getEvents().size());
            for (JaxbValidationEvent event : e.getEvents()) {
                Assert.assertTrue(event.getEvent().getMessage().contains(BAD_LANGUAGE));
            }
        }
        logger.debug("{}", obj);
        assertNotNull("this xml has intentional errors and should cause parse exceptions", parseException);
        assertNull(obj);
    }

    @Test
    public void testValidateOAIStatic() throws ConfigurationException, SAXException, IOException {
        testValidXMLResponse(new FileInputStream(new File(TestConstants.TEST_XML_DIR, "oaidc_get_records.xml")),
                "http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd");
    }

    @Test
    public void testValidateSchema() throws ConfigurationException, SAXException, IOException {
        File schemaFile = new File("target/out.xsd");
        try {
            File generateSchema = xmlService.generateSchema();
            FileUtils.copyFile(generateSchema, schemaFile);
            testValidXMLSchemaResponse(FileUtils.readFileToString(generateSchema));
        } catch (Exception e) {
            logger.warn("exception", e);
            assertFalse("I should not exist. fix me, please?", true);
        }
    }

}
