package org.tdar.core.dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.keyword.GeographicKeyword;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.util.FileUtils;

import freemarker.ext.dom.NodeModel;

@Component
public class FileSystemResourceDao {

    @Autowired
    ResourceLoader resourceLoader;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String TESTING_PATH_FOR_INCLUDES_DIRECTORY = "target/ROOT/";
    private XPathFactory xPathFactory = XPathFactory.newInstance();

    private String wroTempDirName;

    public static Boolean wroExists = null;

    public boolean testWRO() {
        if (wroExists != null) {
            return wroExists;
        }
        try {
            Document dom = getWroDom();
            XPathFactory xPathFactory = XPathFactory.newInstance();
            // Create XPath object from XPathFactory
            XPath xpath = xPathFactory.newXPath();
            XPathExpression xPathExpr = xpath.compile(".//groups/group");
            NodeList nodes = (NodeList) xPathExpr.evaluate(dom, XPathConstants.NODESET);
            if (nodes.getLength() > 0) {
                Node group = nodes.item(0).getAttributes().getNamedItem("name");
                String wroFile = getWroDir() + "/" + group.getTextContent() + ".js";
                logger.debug("wroFile: {}", wroFile);
                Resource resource = resourceLoader.getResource(wroFile);
                wroExists = resource.exists();
                if (wroExists) {
                    logger.debug("WRO found? true");
                    return true;
                }
            } else {
                logger.debug("wro does not exist");
                wroExists = false;
            }

        } catch (Exception e) {
            logger.error("{}", e);
        }
        logger.debug("WRO found? false");
        return false;
    }

    private Document getWroDom() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // use the factory to take an instance of the document builder
        DocumentBuilder db = dbf.newDocumentBuilder();
        // parse using the builder to get the DOM mapping of the
        // XML file
        Document dom = db.parse(getClass().getClassLoader().getResourceAsStream("wro.xml"));
        return dom;
    }

    // helper to load the PDF Template for the cover page
    public File loadTemplate(String path) throws IOException, FileNotFoundException {
        Resource resource = resourceLoader.getResource(path);
        File template = null;
        if (resource.exists()) {
            template = resource.getFile();
        } else {
            resource = resourceLoader.getResource(".");
            String parent = resource.getFile().getParent();
            logger.debug("{} {}", resource, parent);
            // TO SUPPORT TESTING
            template = new File(TESTING_PATH_FOR_INCLUDES_DIRECTORY + path);
            logger.debug("{}", path);
            if (!template.exists()) {
                throw new FileNotFoundException(template.getAbsolutePath());
            }
        }
        return template;
    }

    public List<String> parseWroXML(String prefix) {
        List<String> toReturn = new ArrayList<>();
        try {
            Document dom = getWroDom();
            // Create XPath object from XPathFactory
            XPath xpath = xPathFactory.newXPath();
            XPathExpression xPathExpr = xpath.compile(".//" + prefix);
            NodeList nodes = (NodeList) xPathExpr.evaluate(dom, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                toReturn.add(nodes.item(i).getTextContent());
            }
        } catch (Exception e) {
            throw new TdarRecoverableRuntimeException("fileSystemResourceDao.wro_missing", e);
        }
        return toReturn;
    }

    public Document openCreatorInfoLog(File filename) throws SAXException, IOException, ParserConfigurationException {
        logger.info("opening {}", filename);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // use the factory to take an instance of the document builder
        DocumentBuilder db = dbf.newDocumentBuilder();
        // parse using the builder to get the DOM mapping of the XML file
        Document dom = null;
        if (filename.exists()) {
            dom = db.parse(filename);
        }
        return dom;
    }

    public List<NodeModel> parseCreatorInfoLog(String prefix, boolean limit, float mean, int sidebarValuesToShow, Document dom) {
        List<NodeModel> toReturn = new ArrayList<>();
        if (dom == null) {
            return toReturn;
        }
        try {
            // Create XPath object from XPathFactory
            XPath xpath = xPathFactory.newXPath();
            XPathExpression xPathExpr = xpath.compile(prefix);
            NodeList nodes = (NodeList) xPathExpr.evaluate(dom, XPathConstants.NODESET);
            logger.trace("xpath returned: {}", nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String name = node.getAttributes().getNamedItem("name").getTextContent();
                Float count = Float.parseFloat(node.getAttributes().getNamedItem("count").getTextContent());
                if (sidebarValuesToShow < toReturn.size()) {
                    return toReturn;
                }
                if (limit || count < mean) {
                    if (StringUtils.contains(name, GeographicKeyword.Level.COUNTRY.getLabel()) ||
                            StringUtils.contains(name, GeographicKeyword.Level.CONTINENT.getLabel()) ||
                            StringUtils.contains(name, GeographicKeyword.Level.FIPS_CODE.getLabel())) {
                        continue;
                    }
                }

                toReturn.add(NodeModel.wrap(nodes.item(i)));
            }
        } catch (Exception e) {
            throw new TdarRecoverableRuntimeException("browseController.parse_creator_log", e);
        }
        return toReturn;
    }

    public String getWroDir() {
        if (wroTempDirName != null) {
            return wroTempDirName;
        }
        try {
            String file = FileUtils.readWholeFileAsUTF8(getClass().getClassLoader().getResourceAsStream("version.txt"));
            file = StringUtils.replace(file, "+", "");
            wroTempDirName = "/wro/" + file.trim();
            return wroTempDirName;
        } catch (Exception e) {
            logger.error("{}", e);
        }
        return null;
    }

}
