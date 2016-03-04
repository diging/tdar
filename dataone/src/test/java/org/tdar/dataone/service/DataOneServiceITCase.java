package org.tdar.dataone.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.xml.bind.JAXBException;

import org.dataone.service.types.v1.ObjectList;
import org.dspace.foresite.OREException;
import org.dspace.foresite.ORESerialiserException;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.tdar.core.bean.AbstractIntegrationTestCase;
import org.tdar.core.bean.resource.Document;
import org.tdar.core.service.GenericService;

public class DataOneServiceITCase extends AbstractIntegrationTestCase {

    @Autowired
    private DataOneService service;

    @Autowired
    private GenericService genericService;

    @Test
    @Rollback
    public void testOaiORE() throws OREException, URISyntaxException, ORESerialiserException, JDOMException, IOException {
        Document doc = genericService.find(Document.class, 4230L);
        String mapDocument = service.createResourceMap(doc);
        logger.debug(mapDocument.toString());
    }
    
    @Test
    @Rollback
    public void testObjectTotals() throws UnsupportedEncodingException, NoSuchAlgorithmException, OREException, URISyntaxException, ORESerialiserException, JDOMException, IOException, JAXBException {
    	ObjectList listObjectsResponse = service.getListObjectsResponse(new Date(), new Date(), null, null, 0, 10);
    	assertEquals(2, listObjectsResponse.getTotal());
    	assertEquals(2, listObjectsResponse.getObjectInfoList().size());
    }
}
