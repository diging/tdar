package org.tdar.struts.action.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.fileupload.FileUpload;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.tdar.TestConstants;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.entity.AuthorizedUser;
import org.tdar.core.bean.entity.Creator.CreatorType;
import org.tdar.core.bean.entity.permissions.GeneralPermissions;
import org.tdar.core.bean.entity.Institution;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.ResourceCreator;
import org.tdar.core.bean.entity.ResourceCreatorRole;
import org.tdar.core.bean.keyword.CultureKeyword;
import org.tdar.core.bean.keyword.MaterialKeyword;
import org.tdar.core.bean.keyword.SiteNameKeyword;
import org.tdar.core.bean.resource.Document;
import org.tdar.core.bean.resource.Project;
import org.tdar.core.bean.resource.ResourceNote;
import org.tdar.core.bean.resource.ResourceNoteType;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.bean.resource.InformationResourceFile.FileAccessRestriction;
import org.tdar.core.bean.resource.InformationResourceFile.FileAction;
import org.tdar.core.dao.external.auth.InternalTdarRights;
import org.tdar.core.exception.StatusCode;
import org.tdar.struts.action.TdarActionException;
import org.tdar.struts.action.TdarActionSupport;
import org.tdar.struts.action.UploadController;
import org.tdar.struts.data.FileProxy;
import org.tdar.struts.data.ResourceCreatorProxy;

public class DocumentControllerITCase extends AbstractResourceControllerITCase {

    @Autowired
    DocumentController controller;

    @Override
    protected TdarActionSupport getController() {
        return controller;
    }

    public void initControllerFields() {
        controller.prepare();
        controller.setProjectId(TestConstants.PARENT_PROJECT_ID);
    }

    @Test
    public void testShowStatuses() {
        DocumentController dc = generateNewController(DocumentController.class);
        init(dc, getUser());
        List<Status> statuses = controller.getStatuses();
        assertFalse(statuses.isEmpty());
    }

    @Test
    @Rollback
    public void testSubmitterChangeRights() throws TdarActionException {
        //setup document
        Person newUser = createAndSaveNewPerson();
        DocumentController dc = generateNewInitializedController(DocumentController.class, getBasicUser());
        dc.prepare();
        Document doc = dc.getDocument();
        doc.setTitle("test");
        doc.setDate(1234);
        doc.setDescription("my description");
        dc.setServletRequest(getServletPostRequest());
        assertEquals(TdarActionSupport.SUCCESS, dc.save());

        // change the submitter to the admin
        Long id = doc.getId();
        doc = null;
        dc = generateNewInitializedController(DocumentController.class, getBasicUser());
        dc.setId(id);
        dc.prepare();
        dc.edit();
        dc.setSubmitter(newUser);
        dc.setServletRequest(getServletPostRequest());
        assertEquals(TdarActionSupport.SUCCESS, dc.save());

        // try to edit as basic user -- should fail
        dc = generateNewInitializedController(DocumentController.class, getBasicUser());
        dc.setId(id);
        dc.prepare();
        try {
            assertNotEquals(TdarActionSupport.SUCCESS, dc.edit());
        } catch (TdarActionException e) {
            assertEquals(StatusCode.FORBIDDEN.getHttpStatusCode(), e.getStatusCode());
        }
        assertNotEmpty(dc.getActionErrors());
        setIgnoreActionErrors(true);
        
        // try to edit as new user, should work
        doc = null;
        dc = generateNewInitializedController(DocumentController.class, newUser);
        dc.setId(id);
        dc.prepare();
        assertEquals(TdarActionSupport.SUCCESS, dc.edit());

    }

    @Ignore("Ignoring because this is an internal performance test, not really a unit-test")
    @Test
    @Rollback
    public void testPerformance() throws InstantiationException, IllegalAccessException, TdarActionException {
        // 42s -- reconcileSet + indexInterceptor @100docs
        // 52s -- reconcileSet + w/o indexInterceptor @100docs
        // 43s -- setter model + w/o indexInterceptor @100docs
        // 41s -- setter model + w indexInterceptor @100docs
        // 54s -- with clearAll + indexInterceptor @100docs
        // 44s -- with clearAll + w/o indexInterceptor @100docs
        Project project = new Project();
        project.setTitle("PerfTest");
        project.setDescription("perfTest");
        project.markUpdated(getUser());
        genericService.saveOrUpdate(project);
        List<Person> fiftyPList = genericService.findRandom(Person.class, 50);
        List<Institution> fiftyIList = genericService.findRandom(Institution.class, 50);
        for (Person person : fiftyPList) {
            ResourceCreator rc = new ResourceCreator(person, ResourceCreatorRole.CONTRIBUTOR);
            project.getResourceCreators().add(rc);
            genericService.saveOrUpdate(rc);
            // project.getResourceCreators().add(rc);
        }
        for (Institution inst : fiftyIList) {
            ResourceCreator rc = new ResourceCreator(inst, ResourceCreatorRole.REPOSITORY);
            project.getResourceCreators().add(rc);
            genericService.saveOrUpdate(rc);
        }
        project.getCultureKeywords().addAll(genericService.findRandom(CultureKeyword.class, 10));
        project.getMaterialKeywords().addAll(genericService.findRandom(MaterialKeyword.class, 10));
        project.getSiteNameKeywords().addAll(genericService.findRandom(SiteNameKeyword.class, 10));
        genericService.saveOrUpdate(project);
        int totalNumOfRecords = 100;

        for (int i = 0; i < totalNumOfRecords; i++) {
            Document doc = createAndSaveNewInformationResource(Document.class, project, getAdminUser(), "perf doc #" + i);
            doc.getCultureKeywords().addAll(genericService.findRandom(CultureKeyword.class, 5));
            doc.setInheritingSiteInformation(true);
            doc.setInheritingMaterialInformation(true);
            for (Person person : genericService.findRandom(Person.class, 10)) {
                ResourceCreator rc = new ResourceCreator(person, ResourceCreatorRole.AUTHOR);
                doc.getResourceCreators().add(rc);
                genericService.saveOrUpdate(rc);
            }
            File file = new File(TestConstants.TEST_DOCUMENT_DIR + TestConstants.TEST_DOCUMENT_NAME);
            addFileToResource(doc, file);
            genericService.saveOrUpdate(doc);
        }
        Long projectId = project.getId();
        genericService.synchronize();
        project = null;

        // this the test...
        ProjectController controller = generateNewInitializedController(ProjectController.class);
        controller.setId(projectId);
        controller.prepare();
        Long time = System.currentTimeMillis();
        controller.setServletRequest(getServletPostRequest());
        controller.edit();
        controller.save();
        logger.info("total save time: " + (System.currentTimeMillis() - time));
    }

    @Test
    @Rollback()
    public void testInstitutionResourceCreatorNew() throws Exception {
        initControllerFields();
        // create a document with a single resource creator not currently in the
        // database, then save.
        String EXPECTED_INSTITUTION_NAME = "NewBlankInstitution";

        Long originalId = controller.getResource().getId();
        // FIXME: in reality, struts calls the getter, not the setter, but from
        // there I'm not sure how it's populating the elements.
        controller.getAuthorshipProxies().add(getNewResourceCreator(EXPECTED_INSTITUTION_NAME, -1L, ResourceCreatorRole.REPOSITORY));
        controller.getAuthorshipProxies().add(getNewResourceCreator(EXPECTED_INSTITUTION_NAME, -1L, ResourceCreatorRole.CONTRIBUTOR));
        Document d = controller.getDocument();
        d.setTitle("doc title");
        d.setDescription("desc");
        d.markUpdated(getUser());
        controller.setServletRequest(getServletPostRequest());
        controller.save();
        Long newId = controller.getResource().getId();

        // now reload the document and see if the institution was saved.
        Assert.assertNotSame("resource id should be assigned after insert", originalId, newId);

        controller = generateNewInitializedController(DocumentController.class);
        loadResourceFromId(controller, newId);

        d = controller.getResource();
        Assert.assertEquals(d.getInternalResourceCollection(), null);
        Assert.assertEquals("expecting document IDs to match (save/reloaded)", newId, d.getId());
        Set<ResourceCreator> resourceCreators = d.getResourceCreators();
        Assert.assertTrue(resourceCreators.size() > 0);
        List<ResourceCreator> actualResourceCreators = new ArrayList<ResourceCreator>(d.getResourceCreators());
        Collections.sort(actualResourceCreators);
        ResourceCreator actualCreator = actualResourceCreators.get(0);
        Assert.assertNotNull(actualCreator);
        Assert.assertEquals(CreatorType.INSTITUTION, actualCreator.getCreator().getCreatorType());
        Assert.assertTrue(actualCreator.getCreator().getName().contains(EXPECTED_INSTITUTION_NAME));
        Assert.assertEquals(ResourceCreatorRole.REPOSITORY, actualCreator.getRole());
        setHttpServletRequest(getServletPostRequest());
        controller = generateNewInitializedController(DocumentController.class);
        loadResourceFromId(controller, newId);
        controller.setDelete("delete");
        String deletionReason = "this is a test";
        controller.setDeletionReason(deletionReason);
        controller.delete();

        controller = generateNewInitializedController(DocumentController.class);
        loadResourceFromId(controller, newId);

        Assert.assertEquals("expecting document status to be deleted", Status.DELETED, controller.getDocument().getStatus());
        Assert.assertEquals("expecting controller status to be deleted", Status.DELETED, controller.getStatus());
        boolean seen = false;
        for (ResourceNote note : controller.getDocument().getResourceNotes()) {
            if (note.getType() == ResourceNoteType.ADMIN && note.getNote().equals(deletionReason)) {
                seen = true;
            }
        }
        assertTrue("a deletion note should have been added", seen);
    }

    @Test
    @Rollback()
    public void testPersonResourceCreatorNew() throws Exception {
        initControllerFields();

        getLogger().trace("controller:" + controller);
        getLogger().trace("controller.resource:" + controller.getResource());
        Long originalId = controller.getResource().getId();
        controller.getAuthorshipProxies().add(getNewResourceCreator("newLast", "newFirst", "new@email.com", null, ResourceCreatorRole.AUTHOR));
        controller.getAuthorshipProxies().add(getNewResourceCreator("newLast", "newFirst", "new@email.com", null, ResourceCreatorRole.EDITOR));
        Document d = controller.getDocument();
        d.setTitle("doc title");
        d.setDescription("desc");
        d.markUpdated(getUser());
        controller.setServletRequest(getServletPostRequest());
        controller.save();
        Long newId = controller.getResource().getId();

        // now reload the document and see if the institution was saved.
        Assert.assertNotSame("resource id should be assigned after insert", originalId, newId);

        controller = generateNewInitializedController(DocumentController.class);
        loadResourceFromId(controller, newId);

        d = controller.getResource();
        Assert.assertEquals("expecting document IDs to match (save/reloaded)", newId, d.getId());
        Set<ResourceCreator> resourceCreators = d.getResourceCreators();
        Assert.assertTrue(resourceCreators.size() > 0);
        List<ResourceCreator> actualResourceCreators = new ArrayList<ResourceCreator>(d.getResourceCreators());
        Collections.sort(actualResourceCreators);
        ResourceCreator actualCreator = actualResourceCreators.get(0);
        Assert.assertNotNull(actualCreator);
        Assert.assertEquals(CreatorType.PERSON, actualCreator.getCreator().getCreatorType());
        Assert.assertTrue(actualCreator.getCreator().getName().contains("newLast"));
        Assert.assertEquals(ResourceCreatorRole.AUTHOR, actualCreator.getRole());
        setHttpServletRequest(getServletPostRequest());
        controller = generateNewInitializedController(DocumentController.class);
        loadResourceFromId(controller, newId);
        controller.setDelete("delete");
        controller.delete();
        controller = generateNewInitializedController(DocumentController.class);
        loadResourceFromId(controller, newId);

        Assert.assertEquals("expecting document status to be deleted", Status.DELETED, controller.getDocument().getStatus());
        Assert.assertEquals("expecting controller status to be deleted", Status.DELETED, controller.getStatus());
    }

    // return a populated "new" resource creator person (i.e. all person fields
    // set but null id)
    public ResourceCreatorProxy getNewResourceCreator(String name, Long id, ResourceCreatorRole role) {
        ResourceCreatorProxy rcp = new ResourceCreatorProxy();
        rcp.getInstitution().setName(name);
        // FIXME: THIS NEEDS TO WORK WITHOUT SETTING AN ID as well as when an ID
        // IS SET
        if (System.currentTimeMillis() % 2 == 0) {
            rcp.getInstitution().setId(-1L);
        }
        rcp.setRole(role);
        return rcp;
    }

    public void setController(DocumentController controller) {
        this.controller = controller;
    }

    @Test
    @Rollback()
    public void testEditResourceCreators() throws Exception {
        initControllerFields();

        getLogger().trace("controller:" + controller);
        getLogger().trace("controller.resource:" + controller.getResource());
        Long originalId = controller.getResource().getId();
        controller.getAuthorshipProxies().add(getNewResourceCreator("newLast", "newFirst", "new@email.com", null, ResourceCreatorRole.AUTHOR));
        Document d = controller.getDocument();
        d.setTitle("doc title");
        d.setDescription("desc");
        d.markUpdated(getUser());
        controller.setServletRequest(getServletPostRequest());
        controller.save();
        Long newId = controller.getResource().getId();

        Assert.assertNotNull(entityService.findByEmail("new@email.com"));
        // now reload the document and see if the institution was saved.
        Assert.assertNotSame("resource id should be assigned after insert", originalId, newId);

        controller = generateNewInitializedController(DocumentController.class);
        loadResourceFromId(controller, newId);

        d = controller.getResource();
        Assert.assertEquals("expecting document IDs to match (save/reloaded)", newId, d.getId());
        Set<ResourceCreator> resourceCreators = d.getResourceCreators();
        Assert.assertTrue(resourceCreators.size() > 0);
        ResourceCreator actualCreator = (ResourceCreator) d.getResourceCreators().toArray()[0];
        Assert.assertNotNull(actualCreator);
        Assert.assertEquals(CreatorType.PERSON, actualCreator.getCreator().getCreatorType());
        Assert.assertTrue(actualCreator.getCreator().getName().contains("newLast"));
        controller.delete(controller.getDocument());

        // FIXME: should add and replace items here to really test

        // FIXME: issues with hydrating resources with Institutions

        controller = generateNewInitializedController(DocumentController.class);
        loadResourceFromId(controller, newId);
        // assert my authorproxies have what i think they should have (rendering
        // edit page)
        controller = generateNewInitializedController(DocumentController.class);
        loadResourceFromId(controller, newId);
        controller.setAuthorshipProxies(new ArrayList<ResourceCreatorProxy>());
        // deleting all authorship resource creators
        controller.setServletRequest(getServletPostRequest());
        controller.save();

        // loading the view page
        controller = generateNewInitializedController(DocumentController.class);
        loadResourceFromId(controller, newId);
        logger.info("{}", controller.getAuthorshipProxies());
        Assert.assertEquals("expecting size zero", 0, controller.getAuthorshipProxies().size());
        logger.debug("{}", controller.getAuthorshipProxies().size());
        Assert.assertTrue("expecting invaled proxy", controller.getAuthorshipProxies().isEmpty());
    }

    @Test
    @Rollback
    // create a simple document, using a pre-existing author with no email address. make sure that we didn't create a new person record.
    public void testForDuplicatePersonWithNoEmail() throws Exception {
        initControllerFields();
        // get person record count.
        int expectedPersonCount = genericService.findAll(Person.class).size();

        ResourceCreatorProxy rcp = getNewResourceCreator("Cressey", "Pamela", null, null, ResourceCreatorRole.AUTHOR);
        rcp.getPerson().setInstitution(null);

        Long originalId = controller.getResource().getId();
        controller.getAuthorshipProxies().add(rcp);
        Document d = controller.getDocument();
        d.setTitle("doc title");
        d.setDescription("testing to see if the system created a person record when it shouldn't have");
        d.markUpdated(getUser());
        controller.setServletRequest(getServletPostRequest());
        controller.save();
        Long newId = controller.getResource().getId();

        // now reload the document and see if the institution was saved.
        Assert.assertNotSame("resource id should be assigned after insert", originalId, newId);

        int actualPersonCount = genericService.findAll(Person.class).size();
        Assert.assertEquals("Person count should be the same after creating new document with an author that already exists", expectedPersonCount,
                actualPersonCount);
    }

    @Test
    @Rollback
    public void testResourceCreatorSortOrder() throws Exception {
        int numberOfResourceCreators = 20;
        initControllerFields();
        for (int i = 0; i < numberOfResourceCreators; i++) {
            controller.getCreditProxies().add(getNewResourceCreator("Cressey" + i, "Pamela", null, null, ResourceCreatorRole.CONTACT));
        }
        Document d = controller.getDocument();
        d.setTitle("Testing Resource Creator Sort Order");
        d.setDescription("Resource Creator sort order");
        d.markUpdated(getUser());
        controller.setServletRequest(getServletPostRequest());
        controller.save();
        Long documentId = controller.getResource().getId();
        controller = generateNewInitializedController(DocumentController.class);
        loadResourceFromId(controller, documentId);
        for (int i = 0; i < controller.getCreditProxies().size(); i++) {
            ResourceCreatorProxy proxy = controller.getCreditProxies().get(i);
            assertTrue("proxy person " + proxy.getPerson() + "'s last name should end with " + i, proxy.getPerson().getLastName().endsWith("" + i));
            assertEquals("proxy " + proxy + " sequence number should be i", Integer.valueOf(i), proxy.getResourceCreator().getSequenceNumber());
        }
    }

    @Test
    @Rollback
    // create a simple document, using a pre-existing author with no email address. make sure that we didn't create a new person record.
    public void testForDuplicatePersonWithDifferentInstitution() throws Exception {
        initControllerFields();
        // get person record count.
        Person person = new Person();
        person.setFirstName("Pamela");
        person.setLastName("Cressey");
        ResourceCreatorProxy rcp = getNewResourceCreator("Cressey", "Pamela", null, null, ResourceCreatorRole.AUTHOR);
        int expectedPersonCount = genericService.findAll(Person.class).size();

        Long originalId = controller.getResource().getId();
        controller.getAuthorshipProxies().add(rcp);
        rcp.getPerson().getInstitution().setName("testForDuplicatePersonWithDifferentInstitution");

        Document d = controller.getDocument();
        d.setTitle("doc title");
        d.setDescription("testing to see if the system created a person record when it shouldn't have");
        d.markUpdated(getUser());
        controller.setServletRequest(getServletPostRequest());
        controller.save();
        Long newId = controller.getResource().getId();

        // now reload the document and see if the institution was saved.
        Assert.assertNotSame("resource id should be assigned after insert", originalId, newId);

        Set<Person> findByFullName = entityService.findByFullName("Pamela Cressey");
        logger.debug("people: {} ", findByFullName);
        int actualPersonCount = genericService.findAll(Person.class).size();
        Assert.assertEquals("Person count should not be the same after creating new document with an author that already exists", expectedPersonCount + 1,
                actualPersonCount);
    }

    private Long createDocument(String collectionname, String title) throws TdarActionException {
        controller = generateNewInitializedController(DocumentController.class);
        controller.prepare();
        controller.add();
        getLogger().trace("controller:" + controller);
        getLogger().trace("controller.resource:" + controller.getResource());
        Document d = controller.getDocument();
        d.setTitle(title);
        d.setDescription("desc");
        d.markUpdated(getUser());
        d.setDate(1234);
        ResourceCollection collection = new ResourceCollection();
        collection.setName(collectionname);
        controller.getResourceCollections().add(collection);

        controller.setServletRequest(getServletPostRequest());
        controller.save();
        return controller.getResource().getId();
    }

    @Test
    @Rollback()
    public void testResourceAdhocCollection() throws Exception {
        initControllerFields();
        String collectionname = "my collection";
        Long newId = createDocument(collectionname, "test 1");
        ResourceCollection collection = controller.getResource().getSharedResourceCollections().iterator().next();
        Long collectionId = collection.getId();
        logger.info("{}", collection);
        Long newId2 = createDocument(collectionname, "test 2");
        ResourceCollection collection2 = controller.getResource().getSharedResourceCollections().iterator().next();
        Long collectionId2 = collection2.getId();
        assertEquals(collectionId, collectionId2);

    }

    
    @Test
    @Rollback
    public void testUserPermIssuesUsers() throws TdarActionException {
        //setup document
        Person newUser = createAndSaveNewPerson();
        DocumentController dc = generateNewInitializedController(DocumentController.class, getBasicUser());
        dc.prepare();
        Document doc = dc.getDocument();
        doc.setTitle("test");
        doc.setDate(1234);
        doc.setDescription("my description");
        dc.getAuthorizedUsers().add(new AuthorizedUser(newUser, GeneralPermissions.MODIFY_METADATA));
        dc.setServletRequest(getServletPostRequest());
        assertEquals(TdarActionSupport.SUCCESS, dc.save());

        // change the submitter to the admin
        Long id = doc.getId();
        doc = null;
        dc = generateNewInitializedController(DocumentController.class, newUser);
        dc.setId(id);
        dc.prepare();
        dc.edit();
        dc.getAuthorizedUsers().add(new AuthorizedUser(newUser, GeneralPermissions.ADMINISTER_GROUP));
        dc.setServletRequest(getServletPostRequest());
        assertEquals(TdarActionSupport.SUCCESS, dc.save());

        genericService.synchronize();


    }
    
    
    @Test
    @Rollback
    public void testUserPermIssUpload() throws TdarActionException {
        //setup document
        Person newUser = createAndSaveNewPerson();
        DocumentController dc = generateNewInitializedController(DocumentController.class, getBasicUser());
        dc.prepare();
        Document doc = dc.getDocument();
        doc.setTitle("test");
        doc.setDate(1234);
        doc.setDescription("my description");
        dc.getAuthorizedUsers().add(new AuthorizedUser(newUser, GeneralPermissions.MODIFY_METADATA));
        dc.setServletRequest(getServletPostRequest());
        assertEquals(TdarActionSupport.SUCCESS, dc.save());

        // change the submitter to the admin
        Long id = doc.getId();
        doc = null;

        genericService.synchronize();
        UploadController uc= generateNewInitializedController(UploadController.class, newUser);
        uc.getUploadFile().add(new File(TestConstants.TEST_DOCUMENT_DIR, TestConstants.TEST_DOCUMENT_NAME));
        uc.getUploadFileFileName().add(TestConstants.TEST_DOCUMENT_NAME);
        uc.upload();
        Long ticketId = uc.getTicketId();
        assertFalse(authenticationAndAuthorizationService.canDo(newUser, dc.getDocument(), InternalTdarRights.EDIT_ANY_RESOURCE, GeneralPermissions.ADMINISTER_GROUP));
        assertEquals(1,dc.getDocument().getInternalResourceCollection().getAuthorizedUsers().size() );
        // try to edit as basic user -- should fail
        dc = generateNewInitializedController(DocumentController.class, newUser);
        dc.setId(id);
        dc.prepare();
        try {
            dc.edit();
            FileProxy fileProxy = new FileProxy();
            fileProxy.setFilename(TestConstants.TEST_DOCUMENT_NAME);
            fileProxy.setAction(FileAction.ADD);
            fileProxy.setRestriction(FileAccessRestriction.CONFIDENTIAL);
            dc.getFileProxies().add(fileProxy);
            dc.setTicketId(ticketId);
            dc.save();
        } catch (TdarActionException e) {
            assertEquals(StatusCode.UNAUTHORIZED.getHttpStatusCode(), e.getStatusCode());
        }
        assertNotEmpty(dc.getActionErrors());
        setIgnoreActionErrors(true);
        

    }

}
