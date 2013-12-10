package org.tdar.experimental;

import org.hibernate.Hibernate;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.tdar.TestConstants;
import org.tdar.core.bean.AbstractIntegrationTestCase;
import org.tdar.core.bean.resource.Project;
import org.tdar.core.service.GenericService;

import javax.persistence.Persistence;
import javax.persistence.PersistenceUtil;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: jimdevos
 * Date: 10/17/13
 * Time: 4:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class PersistenceITCase extends AbstractIntegrationTestCase {

    final Long PROJECT_ID = 3L;

    @Autowired
    GenericService genericService;

    PersistenceUtil persistenceUtil;

    public PersistenceITCase() {
        persistenceUtil = Persistence.getPersistenceUtil();
        assertThat(persistenceUtil, notNullValue());
    }


    @Test
    public void testEntityNotInitilized() {
        Project project = genericService.find(Project.class, PROJECT_ID);

        assertThat(project, notNullValue());

        assertThat(persistenceUtil.isLoaded(project), equalTo(true));
        assertThat(persistenceUtil.isLoaded(project, "materialKeywords"), equalTo(false));

        //this should implicitly load the materialKeywords collection (unless 'extra lazy' loading enabled)

        int size = project.getMaterialKeywords().size();
        assertThat(persistenceUtil.isLoaded(project, "materialKeywords"), equalTo(true));
    }

    @Ignore("I don't understand initialize() like I thought I did")
    @Test
    public void testEntityInitialize() {
        Project project = genericService.find(Project.class, PROJECT_ID);
        Hibernate.initialize(project);
        assertThat(persistenceUtil.isLoaded(project, "materialKeywords"), equalTo(true));
    }
}
