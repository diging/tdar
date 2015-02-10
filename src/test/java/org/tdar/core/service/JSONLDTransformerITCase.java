package org.tdar.core.service;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.tdar.core.bean.AbstractIntegrationTestCase;
import org.tdar.core.bean.resource.Resource;
import org.tdar.transform.SchemaOrgMetadataTransformer;

public class JSONLDTransformerITCase extends AbstractIntegrationTestCase {

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    SerializationService serializationService;
    @Autowired
    GenericService genericService;

    @Test
    public void test() throws IOException, ClassNotFoundException {
        SchemaOrgMetadataTransformer transformer = new SchemaOrgMetadataTransformer();

        for (Resource r : genericService.findAll(Resource.class)) {
            logger.debug("//  {} - {}", r.getId(), r.getResourceType());
            logger.debug(transformer.convert(serializationService, r));
        }
    }
}
