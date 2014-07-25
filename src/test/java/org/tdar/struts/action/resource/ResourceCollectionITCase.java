package org.tdar.struts.action.resource;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.tdar.core.bean.DisplayOrientation;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.Viewable;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.collection.ResourceCollection.CollectionType;
import org.tdar.core.bean.entity.AuthorizedUser;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.entity.permissions.GeneralPermissions;
import org.tdar.core.bean.resource.Dataset;
import org.tdar.core.bean.resource.Document;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.Project;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.dao.entity.AuthorizedUserDao;
import org.tdar.core.service.EntityService;
import org.tdar.core.service.GenericService;
import org.tdar.core.service.ResourceCollectionService;
import org.tdar.search.query.SortOption;
import org.tdar.struts.action.AbstractPersistableController;
import org.tdar.struts.action.CollectionController;
import org.tdar.struts.action.TdarActionException;
import org.tdar.struts.action.TdarActionSupport;
import org.tdar.struts.action.search.BrowseController;

import com.opensymphony.xwork2.Action;

public class ResourceCollectionITCase extends AbstractResourceControllerITCase {

    @Autowired
    private GenericService genericService;

    @Autowired
    private EntityService entityService;

    @Autowired
    private ResourceCollectionService resourceCollectionService;

    CollectionController controller;

    static int indexCount = 0;

    @Before
    public void setup()
    {
        controller = generateNewInitializedController(CollectionController.class);
        if (indexCount < 1) {
            reindex();
        }
        indexCount++;
    }

    @Test
    @Rollback
    public void testSparseResource() throws Exception {
        ResourceCollection collection = new ResourceCollection("test", "test", SortOption.TITLE, CollectionType.SHARED, true, getAdminUser());
        collection.markUpdated(getAdminUser());
        collection.setResources(new HashSet<Resource>(genericService.findRandom(Resource.class, 20)));
        genericService.saveOrUpdate(collection);
        Long collectionId = collection.getId();
        collection = null;
        collection = genericService.findAll(ResourceCollection.class, Arrays.asList(collectionId)).get(0);
        for (Resource resource : collection.getResources()) {
            logger.info("{} {} ", resource, resource.getSubmitter());
        }

    }

    @Test
    @Rollback
    public void testResourceCollectionController() throws Exception
    {
        TdarUser testPerson = createAndSaveNewPerson("a@basda.com", "1234");
        String name = "test collection";
        String description = "test description";

        InformationResource generateInformationResourceWithFile = generateDocumentWithUser();
        InformationResource generateInformationResourceWithFile2 = generateDocumentWithUser();
        List<AuthorizedUser> users = new ArrayList<AuthorizedUser>(Arrays.asList(new AuthorizedUser(getBasicUser(), GeneralPermissions.ADMINISTER_GROUP),
                new AuthorizedUser(getAdminUser(), GeneralPermissions.MODIFY_RECORD), new AuthorizedUser(testPerson, GeneralPermissions.MODIFY_RECORD)));
        List<Resource> resources = new ArrayList<Resource>(Arrays.asList(generateInformationResourceWithFile, generateInformationResourceWithFile2));
        ResourceCollection collection = generateResourceCollection(name, description, CollectionType.SHARED, true, users, resources, null);
        Long collectionid = collection.getId();
        logger.info("{}", collection.getResources());
        assertFalse(collectionid.equals(-1L));
        collection = null;
        ResourceCollection foundCollection = genericService.find(ResourceCollection.class, collectionid);
        assertNotNull(foundCollection);
        assertEquals(3, foundCollection.getAuthorizedUsers().size());
        assertEquals(2, foundCollection.getResources().size());

        assertEquals(name, foundCollection.getName());
        assertEquals(description, foundCollection.getDescription());
        assertEquals(CollectionType.SHARED, foundCollection.getType());
        assertEquals(SortOption.RESOURCE_TYPE, foundCollection.getSortBy());

        assertTrue(foundCollection.getResources().contains(generateInformationResourceWithFile2));
        assertTrue(foundCollection.getResources().contains(generateInformationResourceWithFile));

        int count = 0;
        for (AuthorizedUser user : foundCollection.getAuthorizedUsers())
        {
            if (user.getUser().equals(testPerson))
            {
                count++;
                assertEquals(GeneralPermissions.MODIFY_RECORD, user.getGeneralPermission());
            }
            if (user.getUser().equals(getAdminUser()))
            {
                count++;
                assertEquals(GeneralPermissions.MODIFY_RECORD, user.getGeneralPermission());
            }
            if (user.getUser().equals(getBasicUser()))
            {
                count++;
                assertEquals(GeneralPermissions.ADMINISTER_GROUP, user.getGeneralPermission());
            }
        }
        assertEquals(3, count);
    }

    @Test
    @Rollback
    public void testResourceCollectionPermissionsController() throws Exception
    {
        TdarUser testPerson = createAndSaveNewPerson("a2@basda.com", "1234");
        String name = "test collection";
        String description = "test description";

        InformationResource generateInformationResourceWithFile = generateDocumentWithUser();
        InformationResource generateInformationResourceWithFile2 = generateDocumentWithUser();

        List<AuthorizedUser> users = new ArrayList<AuthorizedUser>(Arrays.asList(new AuthorizedUser(getBasicUser(), GeneralPermissions.ADMINISTER_GROUP),
                new AuthorizedUser(getAdminUser(), GeneralPermissions.MODIFY_RECORD), new AuthorizedUser(testPerson, GeneralPermissions.MODIFY_RECORD)));
        List<Resource> resources = new ArrayList<Resource>(Arrays.asList(generateInformationResourceWithFile, generateInformationResourceWithFile2));
        ResourceCollection collection = generateResourceCollection(name, description, CollectionType.SHARED, false, users, resources, null);
        Long id = collection.getId();
        collection = null;
        assertFalse(id.equals(-1L));

        ResourceCollection foundCollection = genericService.find(ResourceCollection.class, id);

        assertTrue(authenticationAndAuthorizationService.canEditResource(testPerson, generateInformationResourceWithFile, GeneralPermissions.MODIFY_METADATA));
        assertTrue(authenticationAndAuthorizationService.canEditResource(testPerson, generateInformationResourceWithFile2, GeneralPermissions.MODIFY_METADATA));
        assertTrue(authenticationAndAuthorizationService.canEditResource(getBasicUser(), generateInformationResourceWithFile2,
                GeneralPermissions.MODIFY_METADATA));

        assertTrue(authenticationAndAuthorizationService.canEditCollection(getBasicUser(), foundCollection));
        assertFalse(authenticationAndAuthorizationService.canEditCollection(testPerson, foundCollection));
    }

    @Test
    @Rollback(true)
    public void testDraftResourceIssue() throws Exception
    {
        String email = "a243@basda.com";
        entityService.delete(entityService.findByEmail(email));

        final TdarUser testPerson = createAndSaveNewPerson(email, "1234");
        String name = "test collection";
        String description = "test description";

        InformationResource normal = generateDocumentWithFileAndUseDefaultUser();
        InformationResource draft = generateDocumentWithFileAndUseDefaultUser();
        final Long normalId = normal.getId();
        final Long draftId = draft.getId();
        draft.setStatus(Status.DRAFT);
        genericService.saveOrUpdate(draft);
        List<AuthorizedUser> users = new ArrayList<AuthorizedUser>(Arrays.asList(new AuthorizedUser(getBasicUser(), GeneralPermissions.ADMINISTER_GROUP),
                new AuthorizedUser(getAdminUser(), GeneralPermissions.MODIFY_RECORD)));
        List<Resource> resources = new ArrayList<Resource>(Arrays.asList(normal, draft));
        ResourceCollection collection = generateResourceCollection(name, description, CollectionType.SHARED, false, users, resources, null);

        final Long id = collection.getId();
        collection = null;

        controller = generateNewInitializedController(CollectionController.class, getAdminUser());
        controller.setId(id);
        controller.prepare();
        controller.edit();
        controller.setServletRequest(getServletPostRequest());
        controller.getAuthorizedUsers().add(new AuthorizedUser(testPerson, GeneralPermissions.MODIFY_RECORD));
        controller.setAsync(false);
        controller.save();

        // setVerifyTransactionCallback(new TransactionCallback<Resource>() {
        //
        // @Override
        // public Resource doInTransaction(TransactionStatus arg0) {
        // InformationResource normal = informationResourceService.find(normalId);
        // InformationResource draft = informationResourceService.find(draftId);

        assertTrue(authenticationAndAuthorizationService.canEditResource(testPerson, normal, GeneralPermissions.MODIFY_METADATA));
        assertTrue(authenticationAndAuthorizationService.canEditResource(testPerson, draft, GeneralPermissions.MODIFY_METADATA));
        assertTrue(authenticationAndAuthorizationService.canEditResource(getBasicUser(), draft, GeneralPermissions.MODIFY_METADATA));
        assertTrue(authenticationAndAuthorizationService.canEditResource(getBasicUser(), normal, GeneralPermissions.MODIFY_METADATA));

        assertTrue(authenticationAndAuthorizationService.canViewResource(getBasicUser(), draft));
        assertTrue(authenticationAndAuthorizationService.canViewResource(getBasicUser(), normal));

        controller = generateNewInitializedController(CollectionController.class, testPerson);
        controller.setId(id);
        controller.prepare();
        try {
            controller.view();
        } catch (TdarActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        logger.info("results: {} ", controller.getResults());
        logger.info("results: {} ", controller.getResources());
        assertTrue(controller.getResults().contains(normal));
        assertTrue(controller.getResults().contains(draft));
        genericService.delete(controller.getResourceCollection().getAuthorizedUsers());
        // return null;
        // }
        //
        // });
    }

    @Test
    @Rollback(true)
    public void testLimitedCollectionPermissions() throws Exception
    {
        String email = "a243@basda.com";
        entityService.delete(entityService.findByEmail(email));

        final TdarUser testPerson = createAndSaveNewPerson(email, "1234");
        String name = "test collection";
        String description = "test description";

        InformationResource normal = generateDocumentWithFileAndUseDefaultUser();
        final Long normalId = normal.getId();
        List<AuthorizedUser> users = new ArrayList<AuthorizedUser>(Arrays.asList(new AuthorizedUser(getAdminUser(), GeneralPermissions.ADMINISTER_GROUP),
                new AuthorizedUser(testPerson, GeneralPermissions.MODIFY_METADATA)));
        List<Resource> resources = new ArrayList<Resource>(Arrays.asList(normal));
        ResourceCollection collection = generateResourceCollection(name, description, CollectionType.SHARED, false, users, resources, null);

        final Long id = collection.getId();
        collection = null;

        DocumentController dcontroller = generateNewInitializedController(DocumentController.class, testPerson);
        dcontroller.setId(normalId);
        dcontroller.prepare();
        dcontroller.getDocument().setTitle("TEST 123");
        dcontroller.edit();
        dcontroller.setServletRequest(getServletPostRequest());
        dcontroller.setAsync(false);
        String save = dcontroller.save();
        assertEquals(Action.SUCCESS, save);
    }

    @Autowired
    AuthorizedUserDao authorizedUserDao;

    @Test
    @Rollback
    public void testResourceCollectionPermissionsWithDepthController() throws Exception
    {
        TdarUser testPerson = createAndSaveNewPerson("a@basda.com", "1234");
        String name = "test collection";
        String description = "test description";

        InformationResource generateInformationResourceWithFile = generateDocumentWithUser();

        List<AuthorizedUser> users = new ArrayList<AuthorizedUser>(Arrays.asList(new AuthorizedUser(getBasicUser(), GeneralPermissions.ADMINISTER_GROUP),
                new AuthorizedUser(getAdminUser(), GeneralPermissions.MODIFY_RECORD), new AuthorizedUser(testPerson, GeneralPermissions.MODIFY_RECORD)));
        List<Resource> resources = new ArrayList<Resource>(Arrays.asList(generateInformationResourceWithFile));
        ResourceCollection collection = generateResourceCollection(name, description, CollectionType.SHARED, false, users, null, null);
        ResourceCollection collection2 = generateResourceCollection(name, description, CollectionType.SHARED, false, null, resources, collection.getId());
        logger.info("{}", generateInformationResourceWithFile);

        assertTrue("user can edit based on parent of parent resource collection",
                authenticationAndAuthorizationService.canEditResource(testPerson, generateInformationResourceWithFile, GeneralPermissions.MODIFY_METADATA));
        collection.getAuthorizedUsers().clear();
        genericService.save(collection);
        assertTrue("user can no longer edit",
                authenticationAndAuthorizationService.canEditResource(testPerson, generateInformationResourceWithFile, GeneralPermissions.MODIFY_METADATA));
        authorizedUserDao.clearUserPermissionsCache();
        assertFalse("user can no longer edit",
                authenticationAndAuthorizationService.canEditResource(testPerson, generateInformationResourceWithFile, GeneralPermissions.MODIFY_METADATA));
    }

    @Test
    @Rollback
    public void testRemoveResources() throws Exception
    {
        ResourceCollection resourceCollection = new ResourceCollection(CollectionType.SHARED);
        resourceCollection.setName("a resource collection");
        resourceCollection.setDescription("testing add then remove resources");
        List<Document> docList = new ArrayList<Document>();
        docList.add(createAndSaveNewInformationResource(Document.class));
        docList.add(createAndSaveNewInformationResource(Document.class));
        resourceCollection.getResources().addAll(docList);
        resourceCollection.setDateCreated(new Date());
        TdarUser owner = new TdarUser("bob", "loblaw", "bobloblaw@tdar.net");
        owner.setContributor(true);

        genericService.saveOrUpdate(owner);
        resourceCollection.setOwner(owner);
        genericService.saveOrUpdate(resourceCollection);

        genericService.saveOrUpdate(resourceCollection);
        for (Document doc : docList) {
            doc.getResourceCollections().add(resourceCollection);
            doc.setSubmitter(owner);
            genericService.saveOrUpdate(doc);
        }
        genericService.saveOrUpdate(resourceCollection);

        evictCache();

        // okay, now let's try to remove the resources from the collection via the
        // controller.
        Long rcid = resourceCollection.getId();
        controller.setId(rcid);
        resourceCollection = null;
        init(controller, owner);
        controller.prepare();
        controller.setServletRequest(getServletPostRequest());
        assertNotNull(controller.getPersistable());
        assertTrue("resource list should not be empty", !controller.getPersistable().getResources().isEmpty());

        // clear the list of incoming resources, then save
        controller.getResources().clear(); // strictly speaking this line is not
                                           // necessary.
        controller.save();

        evictCache();

        // now load our resource collection again. the resources should be gone.
        resourceCollection = genericService.find(ResourceCollection.class, rcid);
        assertEquals("resource list should be empty", 0, resourceCollection.getResources().size());
    }

    @Test
    @Rollback
    public void testDeleteResourceCollection() throws Exception
    {
        ResourceCollection resourceCollection = new ResourceCollection(CollectionType.SHARED);
        ResourceCollection resourceCollectionParent = new ResourceCollection(CollectionType.SHARED);
        ResourceCollection resourceCollectionChild = new ResourceCollection(CollectionType.SHARED);
        resourceCollectionChild.setName("child collection");
        resourceCollectionParent.setName("parent collection");
        resourceCollection.setName("a resource collection");
        resourceCollection.setDescription("testing add then remove resources");
        List<Document> docList = new ArrayList<Document>();
        docList.add(createAndSaveNewInformationResource(Document.class));
        docList.add(createAndSaveNewInformationResource(Document.class));
        resourceCollection.getResources().addAll(docList);
        resourceCollection.setDateCreated(new Date());
        TdarUser owner = new TdarUser("bob", "loblaw", "bobloblaw@tdar.net");
        owner.setContributor(true);
        genericService.saveOrUpdate(owner);
        resourceCollection.markUpdated(owner);
        resourceCollectionParent.markUpdated(owner);
        resourceCollectionChild.markUpdated(owner);
        genericService.saveOrUpdate(resourceCollection);
        genericService.saveOrUpdate(resourceCollectionParent);
        genericService.saveOrUpdate(resourceCollectionChild);
        resourceCollectionChild.setParent(resourceCollection);
        resourceCollection.setParent(resourceCollectionParent);
        genericService.saveOrUpdate(resourceCollection);
        genericService.saveOrUpdate(resourceCollectionParent);
        genericService.saveOrUpdate(resourceCollectionChild);
        evictCache();

        Long parentId = resourceCollectionParent.getId();
        Long childId = resourceCollectionChild.getId();

        // okay, now let's try to remove the resources from the collection via the
        // controller.
        Long rcid = resourceCollection.getId();
        controller = generateNewController(CollectionController.class);
        controller.setId(rcid);
        resourceCollection = null;
        init(controller, owner);
        controller.prepare();
        assertNotNull(controller.getPersistable());
        assertTrue("resource list should not be empty", !controller.getPersistable().getResources().isEmpty());
        setHttpServletRequest(getServletPostRequest());
        controller.setDelete(TdarActionSupport.DELETE);
        controller.delete();

        // now load our resource collection again. the resources should be gone.
        resourceCollection = genericService.find(ResourceCollection.class, rcid);
        assertFalse("user should not be able to delete collection", resourceCollection == null);

        for (ResourceCollection child : resourceCollectionService.findDirectChildCollections(rcid, null, CollectionType.SHARED))
        {
            child.setParent(null);
            genericService.saveOrUpdate(child);
        }
        evictCache();

        controller = generateNewController(CollectionController.class);
        controller.setId(rcid);
        resourceCollection = null;
        controller.prepare();
        init(controller, owner);
        assertNotNull(controller.getPersistable());
        assertTrue("resource list should not be empty", !controller.getPersistable().getResources().isEmpty());
        // resourceCollection.setParent(parent)
        setHttpServletRequest(getServletPostRequest());
        controller.setDelete(TdarActionSupport.DELETE);
        controller.delete();
        evictCache();
        assertEquals(0, controller.getDeleteIssues().size());
        resourceCollection = null;
        resourceCollection = genericService.find(ResourceCollection.class, rcid);
        logger.info("{}", genericService.find(ResourceCollection.class, rcid));
        assertTrue("user should be able to delete collection", resourceCollection == null);
        resourceCollectionChild = null;
        resourceCollectionParent = null;
        ResourceCollection child = genericService.find(ResourceCollection.class, childId);
        List<ResourceCollection> children = resourceCollectionService.findDirectChildCollections(parentId, null, CollectionType.SHARED);
        logger.info("child: {}", child.getParent());
        logger.info("children: {}", children);
        assertTrue(child.getParent() == null);
        assertTrue((children == null) || (children.size() == 0));
        evictCache();

    }

    @Test
    @Rollback
    public void testDeleteResourceCollectionWithUser() throws Exception
    {
        ResourceCollection resourceCollection = new ResourceCollection(CollectionType.SHARED);
        resourceCollection.setName("a resource collection");
        resourceCollection.setSortBy(SortOption.DATE);
        resourceCollection.setDescription("testing add then remove resources");
        List<Document> docList = new ArrayList<Document>();
        docList.add(createAndSaveNewInformationResource(Document.class));
        resourceCollection.getResources().addAll(docList);
        resourceCollection.setDateCreated(new Date());
        TdarUser owner = new TdarUser("bob", "loblaw", "bobloblaw@tdar.net");
        owner.setContributor(true);
        genericService.saveOrUpdate(owner);
        resourceCollection.markUpdated(owner);
        AuthorizedUser authorizedUser = new AuthorizedUser(owner, GeneralPermissions.MODIFY_RECORD);
        resourceCollection.getAuthorizedUsers().addAll(Arrays.asList(authorizedUser));
        genericService.saveOrUpdate(resourceCollection);
        evictCache();

        // okay, now let's try to remove the resources from the collection via the
        // controller.
        Long rcid = resourceCollection.getId();
        controller = generateNewController(CollectionController.class);
        controller.setId(rcid);
        resourceCollection = null;
        init(controller, owner);
        controller.prepare();
        assertNotNull(controller.getPersistable());
        assertTrue("resource list should not be empty", !controller.getPersistable().getResources().isEmpty());
        assertTrue("user list should not be empty", !controller.getPersistable().getAuthorizedUsers().isEmpty());
        setHttpServletRequest(getServletPostRequest());
        controller.setDelete(TdarActionSupport.DELETE);
        controller.delete();

        // now load our resource collection again. the resources should be gone.
        resourceCollection = genericService.find(ResourceCollection.class, rcid);
        assertFalse("user should not be able to delete collection", resourceCollection == null);

        controller = generateNewController(CollectionController.class);
        controller.setId(rcid);
        resourceCollection = null;
        init(controller, owner);
        controller.prepare();
        assertNotNull(controller.getPersistable());
        assertTrue("resource list should not be empty", !controller.getPersistable().getResources().isEmpty());
        setHttpServletRequest(getServletPostRequest());
        controller.setDelete(TdarActionSupport.DELETE);
        controller.delete();
        evictCache();
        assertEquals(0, controller.getDeleteIssues().size());
        resourceCollection = null;
        resourceCollection = genericService.find(ResourceCollection.class, rcid);
        logger.info("{}", genericService.find(ResourceCollection.class, rcid));
        assertTrue("user should be able to delete collection", resourceCollection == null);
        evictCache();
    }

    @Test
    @Rollback
    public void testSaveAndDeleteWithRedundantAccessRights() throws TdarActionException {
        controller.prepare();
        controller.add();

        ResourceCollection rc = controller.getResourceCollection();
        rc.setName("test delete w/ redundant rights");
        rc.setDescription("a tragedy in three acts");
        rc.setVisible(true);
        rc.setSortBy(SortOption.ID);
        rc.setOrientation(DisplayOrientation.LIST);

        // Add three authusers. two of the authusers are redundant and should be normalized to the user with
        // the best permissions.
        AuthorizedUser user1Viewer = createAuthUser(GeneralPermissions.VIEW_ALL);
        AuthorizedUser user1Modifier = new AuthorizedUser(user1Viewer.getUser(), GeneralPermissions.MODIFY_METADATA);
        AuthorizedUser user2 = createAuthUser(GeneralPermissions.ADMINISTER_GROUP);
        user2.setTest("1234");
        controller.getAuthorizedUsers().addAll(Arrays.asList(user1Viewer, user1Modifier, user2));

        controller.setServletRequest(getServletPostRequest());
        controller.save();

        Long id = rc.getId();
        controller = generateNewController(CollectionController.class);
        controller.setSessionData(getSessionData());
        controller.setId(id);
        controller.prepare();
        controller.view();

        ResourceCollection rc2 = controller.getResourceCollection();
        assertEquals(rc.getName(), rc2.getName());
        assertEquals("2 redundant authusers should have been normalized", 2, rc2.getAuthorizedUsers().size());

        assertEquals("size should be 2", 2, rc2.getAuthorizedUsers().size());

        // if this list is truly normalized, each queue should be length 1
        HashMap<Long, GeneralPermissions> map = new HashMap<Long, GeneralPermissions>();
        for (AuthorizedUser authuser : rc2.getAuthorizedUsers()) {
            map.put(authuser.getUser().getId(), authuser.getGeneralPermission());
        }
        assertEquals("user 1 should have best permission", GeneralPermissions.MODIFY_METADATA, map.get(user1Modifier.getUser().getId()));
        assertNotNull("only the modifier & admin authusers should remain", map.get(user2.getUser().getId()));
    }

    @Test
    @Rollback
    public void testNormalizeAuthorizedUsers() {
        // Add three authusers. two of the authusers are redundant and should be normalized to the user with
        // the best permissions.
        AuthorizedUser user1Viewer = createAuthUser(GeneralPermissions.VIEW_ALL);
        AuthorizedUser user1Modifier = new AuthorizedUser(user1Viewer.getUser(), GeneralPermissions.MODIFY_METADATA);
        AuthorizedUser user2 = createAuthUser(GeneralPermissions.ADMINISTER_GROUP);
        List<AuthorizedUser> authusers = new ArrayList<AuthorizedUser>(Arrays.asList(user1Viewer, user1Modifier, user2));
        int origCount = authusers.size();
        ResourceCollection.normalizeAuthorizedUsers(authusers);
        int newCount = authusers.size();
        assertThat(newCount, lessThan(origCount));
    }

    private AuthorizedUser createAuthUser(GeneralPermissions permissions) {
        String string = UUID.randomUUID().toString();
        TdarUser person = new TdarUser(string, string, string + "@tdar.net");
        person.setContributor(true);
        genericService.saveOrUpdate(person);
        AuthorizedUser authuser = new AuthorizedUser(person, permissions);
        return authuser;
    }

    @Test
    @Rollback
    public void testBrowseControllerVisibleCollections() throws Exception
    {
        TdarUser testPerson = createAndSaveNewPerson("a@basda.com", "1234");
        List<AuthorizedUser> users = new ArrayList<AuthorizedUser>(Arrays.asList(new AuthorizedUser(testPerson, GeneralPermissions.ADMINISTER_GROUP)));
        ResourceCollection collection1 = generateResourceCollection("test 1", "", CollectionType.INTERNAL, false, new ArrayList<AuthorizedUser>(users),
                new ArrayList<Resource>(), null);
        ResourceCollection collection2 = generateResourceCollection("test 2", "", CollectionType.SHARED, false, new ArrayList<AuthorizedUser>(users),
                new ArrayList<Resource>(), null);
        InformationResource testFile = generateDocumentWithUser();
        ResourceCollection parentCollection = generateResourceCollection("test 3", "", CollectionType.SHARED, true, new ArrayList<AuthorizedUser>(users),
                Arrays.asList(testFile), null);
        ResourceCollection childCollection = generateResourceCollection("test 4", "", CollectionType.SHARED, true, new ArrayList<AuthorizedUser>(),
                new ArrayList<Resource>(), parentCollection.getId());
        ResourceCollection childCollectionHidden = generateResourceCollection("test 5", "", CollectionType.SHARED, false, new ArrayList<AuthorizedUser>(),
                new ArrayList<Resource>(), parentCollection.getId());
        genericService.saveOrUpdate(parentCollection);

        BrowseController controller_ = generateNewInitializedController(BrowseController.class);
        Long fileId = testFile.getId();
        searchIndexService.flushToIndexes();
        searchIndexService.indexAll(getAdminUser(), Resource.class);
        testFile = null;
        evictCache();
        // WHY DOES THE SYNCHRONIZE ON THE INDEX CALL DO ANYTHING HERE VS THE
        // SYNCHRONIZE ABOVE
        testFile = genericService.find(Document.class, fileId);
        logger.info("{} : {}", testFile, testFile.getResourceCollections());

        controller_.setRecordsPerPage(1000);
        assertEquals(Action.SUCCESS, controller_.browseCollections());
        List<ResourceCollection> collections = controller_.getResults();
        assertFalse(collections.contains(collection1));
        assertFalse(collections.contains(collection2));
        // FIXME: @ManyToMany directional issue
        // assertEquals(1,parentCollection.getResources().size());
        assertEquals(1, testFile.getResourceCollections().size());

        assertTrue(parentCollection.isShared());
        assertTrue(parentCollection.isVisible());
        assertTrue(parentCollection.isTopLevel());

        assertTrue(String.format("collections %s should contain %s", collections, parentCollection), collections.contains(parentCollection));
        assertFalse(collections.contains(childCollection));
        assertFalse(collections.contains(childCollectionHidden));
        controller = generateNewController(CollectionController.class);
        // TESTING ANONYMOUS USER
        initAnonymousUser(controller);
        controller.setId(parentCollection.getId());
        controller.prepare();
        assertEquals(Action.SUCCESS, controller.view());
        collections = controller.getCollections();
        assertTrue(collections.contains(childCollection));
        assertFalse(collections.contains(childCollectionHidden));
        assertEquals(1, controller.getResults().size());

        // TESTING MORE ADVANCED VIEW RIGHTS
        logger.info("{}", controller.getActionErrors());
        controller = generateNewController(CollectionController.class);
        init(controller, testPerson);
        controller.setId(parentCollection.getId());
        controller.prepare();
        assertEquals(Action.SUCCESS, controller.view());
        collections = controller.getCollections();
        assertEquals(2, collections.size());
        assertTrue(collections.contains(childCollection));
        assertTrue(collections.contains(childCollectionHidden));

        logger.info("{}", controller.getActionErrors());

        // controller =
        // generateNewInitializedController(CollectionController.class);
        // controller.setId(parentCollection.getId());
        // controller.prepare();
        // assertEquals(TdarActionSupport.SUCCESS, controller.view());
        // assertTrue(collections.contains(childCollection));
        // assertTrue(collections.contains(childCollectionHidden));

        evictCache();
    }

    @Test
    @Rollback
    public void testHiddenParentVisibleChild() throws Exception
    {
        ResourceCollection collection1 = generateResourceCollection("test 1 private", "", CollectionType.SHARED, false, new ArrayList<AuthorizedUser>(),
                new ArrayList<Resource>(), null);
        ResourceCollection collection2 = generateResourceCollection("test 2 public", "", CollectionType.SHARED, true, new ArrayList<AuthorizedUser>(),
                new ArrayList<Resource>(), collection1.getId());
        evictCache();
        searchIndexService.index(collection1, collection2);
        searchIndexService.flushToIndexes();
        BrowseController browseController = generateNewInitializedController(BrowseController.class);
        browseController.setRecordsPerPage(Integer.MAX_VALUE);
        browseController.browseCollections();
        assertTrue("should see child collection of hidden parent", browseController.getResults().contains(collection2));
        assertFalse("should not see hidden collection", browseController.getResults().contains(collection1));
    }

    @Test
    @Rollback
    public void testNestedCollectionEdit() throws Exception
    {
        TdarUser testPerson = createAndSaveNewPerson("a@basda.com", "1234");
        List<AuthorizedUser> users = new ArrayList<AuthorizedUser>(Arrays.asList(new AuthorizedUser(testPerson, GeneralPermissions.ADMINISTER_GROUP)));
        ResourceCollection collection1 = generateResourceCollection("test 1 private", "", CollectionType.SHARED, false, users, new ArrayList<Resource>(), null);
        ResourceCollection collection2 = generateResourceCollection("test 2 public", "", CollectionType.SHARED, true, new ArrayList<AuthorizedUser>(),
                new ArrayList<Resource>(), collection1.getId());

        evictCache();
        assertTrue(authenticationAndAuthorizationService.canEditCollection(testPerson, collection1));
        assertTrue(authenticationAndAuthorizationService.canEditCollection(testPerson, collection2));
    }

    @Test
    @Rollback
    public void testResourceCollectionParentCollectionsFoundProperly() throws Exception
    {
        TdarUser testPerson = createAndSaveNewPerson("a2@basda.com", "1234");
        String name = "test collection";
        String description = "test description";

        Document generateInformationResourceWithFile = generateDocumentWithUser();
        Document generateInformationResourceWithFile2 = generateDocumentWithUser();

        List<AuthorizedUser> users = new ArrayList<AuthorizedUser>(Arrays.asList(new AuthorizedUser(getBasicUser(), GeneralPermissions.ADMINISTER_GROUP),
                new AuthorizedUser(getAdminUser(), GeneralPermissions.MODIFY_RECORD), new AuthorizedUser(testPerson, GeneralPermissions.MODIFY_RECORD)));
        List<Resource> resources = new ArrayList<Resource>(Arrays.asList(generateInformationResourceWithFile, generateInformationResourceWithFile2));
        // use case 1 -- use owner
        ResourceCollection collectionWithUserAsOwner = generateResourceCollection(name, description, CollectionType.SHARED, true, null, getBasicUser(),
                resources, null);
        // use case 2 -- use administrator
        ResourceCollection collectionWithUserAsAdministrator = generateResourceCollection(name, description, CollectionType.SHARED, true, users,
                getAdminUser(), resources, null);

        evictCache();
        searchIndexService.flushToIndexes();

        controller = generateNewInitializedController(CollectionController.class, getBasicUser());
        controller.prepare();
        controller.add();
        logger.info("{}", controller.getCandidateParentResourceCollections());
        assertTrue(controller.getCandidateParentResourceCollections().contains(collectionWithUserAsOwner));
        assertTrue(controller.getCandidateParentResourceCollections().contains(collectionWithUserAsAdministrator));
    }

    @Test
    @Rollback
    public void testDocumentControllerAssigningResourceCollectionsWithoutLocalRights() throws Exception
    {
        ResourceCollection collection1 = generateResourceCollection("test 1 private", "", CollectionType.SHARED, false, null, new ArrayList<Resource>(), null);
        DocumentController controller = generateNewInitializedController(DocumentController.class);
        controller.prepare();
        controller.add();
        Document document = controller.getDocument();
        document.setTitle("test");
        document.setDescription("test");
        document.setDate(1234);
        controller.getResourceCollections().add(collection1);
        controller.setServletRequest(getServletPostRequest());
        assertEquals(Action.SUCCESS, controller.save());
        evictCache();
        ResourceCollection first = document.getResourceCollections().iterator().next();
        assertEquals(1, document.getResourceCollections().size());
        assertEquals(collection1, first);
        assertEquals(getUser(), first.getOwner());
        assertEquals(1, first.getResources().size());
    }

    @Test
    @Rollback
    public void testDocumentControllerAssigningResourceCollections() throws Exception
    {
        ResourceCollection collection1 = generateResourceCollection("test 1 private", "", CollectionType.SHARED, false, null, new ArrayList<Resource>(), null);
        DocumentController controller = generateNewInitializedController(DocumentController.class);
        controller.prepare();
        controller.add();
        Document document = controller.getDocument();
        document.setTitle("test");
        document.setDescription("test");
        document.setDate(1234);
        ResourceCollection fakeIncoming = new ResourceCollection(CollectionType.SHARED);
        fakeIncoming.setName(collection1.getName());
        fakeIncoming.setId(collection1.getId());
        controller.setServletRequest(getServletPostRequest());
        controller.getResourceCollections().add(fakeIncoming);
        assertEquals(Action.SUCCESS, controller.save());
        ResourceCollection first = document.getResourceCollections().iterator().next();
        assertEquals(1, document.getResourceCollections().size());
        assertEquals(collection1, first);
        assertEquals(getUser(), first.getOwner());
        assertEquals(1, first.getResources().size());
    }

    @Test
    @Rollback
    public void testDocumentControllerAssigningResourceCollectionsWithLocalRights() throws Exception
    {
        ResourceCollection collection1 = generateResourceCollection("test 1 private", "", CollectionType.SHARED, true, null, new ArrayList<Resource>(), null);
        DocumentController controller = generateNewInitializedController(DocumentController.class);
        controller.prepare();
        controller.add();
        Document document = controller.getDocument();
        document.setTitle("test");
        document.setDescription("test");
        document.setDate(1234);
        controller.getAuthorizedUsers().add(new AuthorizedUser(getBasicUser(), GeneralPermissions.VIEW_ALL));
        controller.getResourceCollections().add(collection1);
        controller.setServletRequest(getServletPostRequest());
        assertEquals(Action.SUCCESS, controller.save());

        assertEquals(2, document.getResourceCollections().size());
        assertTrue(document.getResourceCollections().contains(collection1));
        assertEquals(1, collection1.getResources().size());
        searchIndexService.index(document);
        CollectionController controller2 = generateNewInitializedController(CollectionController.class);
        controller2.setId(collection1.getId());
        controller2.prepare();
        assertEquals(Action.SUCCESS, controller2.view());
        logger.info("results: {}", controller2.getResults());
        assertTrue(controller2.getResults().contains(document));
    }

    @Test
    @Rollback
    public void testInvalidRightsAssignment() throws Exception
    {
        Document document = generateDocumentWithUser();
        document.setSubmitter(getAdminUser());
        genericService.save(document);
        // try and assign access to aa document that user should not have rights
        // to add, assert that this document cannot be added

        CollectionController controller = generateNewController(CollectionController.class);
        init(controller, getBasicUser());
        controller.add();
        ResourceCollection resourceCollection = controller.getResourceCollection();
        resourceCollection.setType(CollectionType.SHARED);
        resourceCollection.setName("tst");
        resourceCollection.setDescription("tst");
        resourceCollection.markUpdated(getBasicUser());
        resourceCollection.setSortBy(SortOption.ID);
        controller.getResources().add(document);
        controller.setServletRequest(getServletPostRequest());
        String result = controller.save();
        assertFalse(result.equals(Action.SUCCESS));
        controller = generateNewInitializedController(CollectionController.class);
        controller.setId(resourceCollection.getId());
        assertEquals(0, resourceCollection.getResources().size());
        resourceCollection = null;
        controller.prepare();
        controller.edit();
        assertEquals(0, controller.getResources().size());
        assertEquals(0, controller.getResourceCollection().getResources().size());

    }

    @Test
    @Rollback
    public void testRightsEscalation() throws Exception
    {
        // Create document, add user to it with MODIFY_METADATA, have them create a collection, and add it where they're the owner and thus have higher rights
        Document document = generateDocumentWithUser();
        document.setSubmitter(getAdminUser());
        genericService.save(document);
        Long docId = document.getId();
        // document = null;
        DocumentController controller = generateNewInitializedController(DocumentController.class, getAdminUser());
        controller.setId(docId);
        controller.prepare();
        controller.getAuthorizedUsers().add(new AuthorizedUser(getBasicUser(), GeneralPermissions.MODIFY_METADATA));
        controller.setServletRequest(getServletPostRequest());
        controller.save();

        controller = null;
        // try and assign access to aa document that user should not have rights
        // to add, assert that this document cannot be added

        CollectionController cc = generateNewInitializedController(CollectionController.class, getBasicUser());
        cc.prepare();
        cc.getResourceCollection().setName("test");
        cc.getResourceCollection().setDescription("test");
        cc.getResources().add(document);
        assertWeFailedToSave(cc);
    }

    @Test
    @Rollback
    public void testOwnRightsEscalation() throws Exception
    {
        // Create document, add user to it with MODIFY_METADATA, have them create a collection, and add it where they're the owner and thus have higher rights
        Document document = generateDocumentWithUser();
        document.setSubmitter(getAdminUser());
        genericService.save(document);
        Long docId = document.getId();
        DocumentController controller = generateNewInitializedController(DocumentController.class, getAdminUser());
        controller.setId(docId);
        controller.prepare();
        controller.getAuthorizedUsers().add(new AuthorizedUser(getBasicUser(), GeneralPermissions.MODIFY_METADATA));
        controller.setServletRequest(getServletPostRequest());
        controller.save();

        controller = null;
        // try and assign access to aa document that user should not have rights
        // to add, assert that this document cannot be added
        controller = generateNewInitializedController(DocumentController.class, getBasicUser());
        controller.setId(docId);
        controller.prepare();
        controller.edit();
        controller.getAuthorizedUsers().add(new AuthorizedUser(getBasicUser(), GeneralPermissions.MODIFY_RECORD));
        Exception e = null;
        try {
            resourceCollectionService.saveAuthorizedUsersForResource(controller.getDocument(), controller.getAuthorizedUsers(), true, getBasicUser());
        } catch (Exception es) {
            e = es;
        }
        assertNotNull(e);
    }

    private void assertWeFailedToSave(AbstractPersistableController<?> cc) {
        cc.setServletRequest(getServletPostRequest());
        String result = Action.SUCCESS;
        setIgnoreActionErrors(true);
        try {
            result = cc.save();
        } catch (Exception e) {
            logger.error("{}", e);
            result = null;
        }
        assertFalse(Action.SUCCESS.equals(result));
    }

    @Test
    @Rollback
    public void testRightsEscalationUserUpsSelf() throws Exception
    {
        // Create document, add user to it with MODIFY_METADATA, have them edit document and add it to an adhoc collection, then try and add higher rights
        Document document = generateDocumentWithUser();
        document.setSubmitter(getAdminUser());
        genericService.save(document);
        Long docId = document.getId();
        document = null;
        DocumentController controller = generateNewInitializedController(DocumentController.class, getAdminUser());
        controller.setId(docId);
        controller.prepare();
        controller.getAuthorizedUsers().add(new AuthorizedUser(getBasicUser(), GeneralPermissions.MODIFY_METADATA));
        controller.setServletRequest(getServletPostRequest());
        controller.save();

        controller = generateNewInitializedController(DocumentController.class, getBasicUser());
        controller.setId(docId);
        controller.prepare();
        controller.getResourceCollections().add(
                new ResourceCollection("test123", "test123", SortOption.RESOURCE_TYPE, CollectionType.SHARED, true, getBasicUser()));
        controller.setServletRequest(getServletPostRequest());
        controller.save();
        Long id = -1L;
        for (ResourceCollection c : controller.getResourceCollections()) {
            if (c.getTitle().equals("test123")) {
                id = c.getId();
            }
        }
        controller = null;
        // try and assign access to aa document that user should not have rights
        // to add, assert that this document cannot be added

        CollectionController cc = generateNewInitializedController(CollectionController.class, getBasicUser());
        cc.setId(id);
        cc.prepare();
        // controller.getResources().add(document);
        cc.getAuthorizedUsers().add(new AuthorizedUser(getBasicUser(), GeneralPermissions.MODIFY_RECORD));
        assertWeFailedToSave(cc);
    }

    @Test
    @Rollback
    public void testRightsEscalationUserUpsParent() throws Exception
    {
        List<AuthorizedUser> users = Arrays.asList(new AuthorizedUser(getBasicUser(), GeneralPermissions.ADMINISTER_GROUP));
        ResourceCollection parent = generateResourceCollection("parent", "parent", CollectionType.SHARED, true, users, getBasicUser(), Collections.EMPTY_LIST,
                null);
        Long parentId = parent.getId();
        // Create document, add user to it with MODIFY_METADATA, have them edit document and add it to an adhoc collection, then try and add higher rights
        Document document = generateDocumentWithUser();
        document.setSubmitter(getAdminUser());
        genericService.save(document);
        Long docId = document.getId();
        document = null;
        DocumentController controller = generateNewInitializedController(DocumentController.class, getAdminUser());
        controller.setId(docId);
        controller.prepare();
        controller.getAuthorizedUsers().add(new AuthorizedUser(getBasicUser(), GeneralPermissions.MODIFY_METADATA));
        controller.setServletRequest(getServletPostRequest());
        controller.save();

        controller = generateNewInitializedController(DocumentController.class, getBasicUser());
        controller.setId(docId);
        controller.prepare();
        controller.getResourceCollections().add(
                new ResourceCollection("test123", "test123", SortOption.RESOURCE_TYPE, CollectionType.SHARED, true, getBasicUser()));
        controller.setServletRequest(getServletPostRequest());
        controller.save();
        Long id = -1L;
        for (ResourceCollection c : controller.getResourceCollections()) {
            if (c.getTitle().equals("test123")) {
                id = c.getId();
            }
        }
        controller = null;
        // try and assign access to aa document that user should not have rights
        // to add, assert that this document cannot be added

        CollectionController cc = generateNewInitializedController(CollectionController.class, getBasicUser());
        cc.setId(id);
        cc.prepare();
        cc.setParentId(parentId);
        assertWeFailedToSave(cc);
    }

    @Test
    @Rollback
    public void testResourceCollectionDraftDisplay() throws Exception
    {
        Document draftDocument = generateDocumentWithUser();
        Document activeDocument = generateDocumentWithUser();
        draftDocument.setStatus(Status.DRAFT);
        genericService.save(draftDocument);
        evictCache();
        ResourceCollection collection = generateResourceCollection("test collection w/Draft", "testing draft...", CollectionType.SHARED, true, null,
                Arrays.asList(draftDocument, activeDocument), null);
        collection.setOwner(getAdminUser());
        logger.info("DOCUMENT: {} ", draftDocument.getSubmitter());
        searchIndexService.flushToIndexes();
        controller = generateNewInitializedController(CollectionController.class);
        controller.setId(collection.getId());
        controller.prepare();
        logger.info(controller.view());
        assertTrue(controller.getResults().contains(draftDocument));
        assertTrue(controller.getResults().contains(activeDocument));

        controller = generateNewController(CollectionController.class);
        initAnonymousUser(controller);
        controller.setId(collection.getId());
        controller.prepare();
        controller.view();
        assertFalse(controller.getResults().contains(draftDocument));
        assertTrue(controller.getResults().contains(activeDocument));
    }

    @Test
    @Rollback
    public void testSharedResourceCollectionQuery() throws Exception
    {
        TdarUser testPerson = createAndSaveNewPerson("a@basda.com", "1234");
        List<AuthorizedUser> authList = new ArrayList<AuthorizedUser>(Arrays.asList(new AuthorizedUser(testPerson, GeneralPermissions.VIEW_ALL)));

        ResourceCollection collection = generateResourceCollection("test collection w/Draft", "testing draft...", CollectionType.SHARED, true,
                authList, null, null);
        collection.setOwner(getAdminUser());
        List<ResourceCollection> findAccessibleResourceCollections = entityService.findAccessibleResourceCollections(testPerson);
        assertTrue(findAccessibleResourceCollections.contains(collection));
    }

    @Test
    @Rollback
    public void testRemoveResourceCollectionButMaintainSome() throws Exception
    {
        Document doc = generateDocumentWithUser();
        List<AuthorizedUser> users = new ArrayList<AuthorizedUser>(Arrays.asList(new AuthorizedUser(doc.getSubmitter(), GeneralPermissions.ADMINISTER_GROUP)));
        ResourceCollection collection1 = generateResourceCollection("test 1 private", "", CollectionType.SHARED, false, users, Arrays.asList(doc), null);
        ResourceCollection collection2 = generateResourceCollection("test 2 public", "", CollectionType.SHARED, true, new ArrayList<AuthorizedUser>(),
                Arrays.asList(doc), collection1.getId());

        ResourceCollection fake = new ResourceCollection(CollectionType.SHARED);
        fake.setId(collection2.getId());
        fake.setName(collection2.getName());

        DocumentController docController = generateNewInitializedController(DocumentController.class);
        init(docController,(TdarUser) doc.getSubmitter());
        docController.setId(doc.getId());
        docController.prepare();
        docController.edit();
        docController.getResourceCollections().clear();
        docController.getResourceCollections().add(fake);
        docController.setServletRequest(getServletPostRequest());
        assertEquals(Action.SUCCESS, docController.save());
        evictCache();

    }

    @Test
    @Rollback
    public void testFullUser() throws Exception {
        Dataset dataset = createAndSaveNewInformationResource(Dataset.class);
        Long datasetId = dataset.getId();
        addAuthorizedUser(dataset, getAdminUser(), GeneralPermissions.MODIFY_RECORD);
        genericService.save(dataset);
        dataset = null;
        DatasetController controller = generateNewInitializedController(DatasetController.class);
        controller.setId(datasetId);
        controller.prepare();
        controller.edit();

        addAuthorizedUser(controller.getDataset(), getUser(), GeneralPermissions.MODIFY_RECORD);
        controller.setServletRequest(getServletPostRequest());
        controller.save();
        dataset = datasetService.find(datasetId);
        assertEquals(1, dataset.getInternalResourceCollection().getAuthorizedUsers().size());
        assertEquals(getAdminUserId(), dataset.getInternalResourceCollection().getAuthorizedUsers().iterator().next().getUser().getId());
    }

    @Test
    @Rollback
    public void testReadUser() throws Exception {
        Dataset dataset = createAndSaveNewInformationResource(Dataset.class);
        Long datasetId = dataset.getId();
        addAuthorizedUser(dataset, getAdminUser(), GeneralPermissions.VIEW_ALL);
        genericService.save(dataset);
        dataset = null;
        DatasetController controller = generateNewInitializedController(DatasetController.class);
        controller.setId(datasetId);
        controller.prepare();
        controller.edit();
        assertEquals(1, controller.getAuthorizedUsers().size());
        ArrayList<AuthorizedUser> authorizedUsers = new ArrayList<AuthorizedUser>();
        authorizedUsers.add(new AuthorizedUser(getBasicUser(), GeneralPermissions.VIEW_ALL));
        authorizedUsers.add(new AuthorizedUser(getAdminUser(), GeneralPermissions.VIEW_ALL));
        controller.setAuthorizedUsers(authorizedUsers);
        controller.setServletRequest(getServletPostRequest());
        controller.save();
        dataset = datasetService.find(datasetId);
        ResourceCollection internalResourceCollection = dataset.getInternalResourceCollection();
        assertEquals(2, internalResourceCollection.getAuthorizedUsers().size());
        Set<Long> seen = new HashSet<Long>();
        for (AuthorizedUser r : internalResourceCollection.getAuthorizedUsers()) {
            seen.add(r.getUser().getId());
        }
        // FIXME: this fails but clearly, above it works
        // assertTrue(internalResourceCollection.getResources().contains(dataset));
        seen.remove(getUserId());
        seen.remove(getAdminUserId());
        assertTrue("should have seen all user ids already", seen.isEmpty());
    }

    @Test
    @Rollback
    public void testReadUserEmpty() throws Exception {
        Dataset dataset = createAndSaveNewInformationResource(Dataset.class);
        Long datasetId = dataset.getId();
        addAuthorizedUser(dataset, getAdminUser(), GeneralPermissions.VIEW_ALL);
        genericService.save(dataset);
        dataset = null;
        DatasetController controller = generateNewInitializedController(DatasetController.class);
        controller.setId(datasetId);
        controller.prepare();
        controller.edit();
        controller.setAuthorizedUsers(Collections.<AuthorizedUser> emptyList());
        controller.setServletRequest(getServletPostRequest());
        controller.save();
        dataset = datasetService.find(datasetId);
        assertEquals(0, dataset.getInternalResourceCollection().getAuthorizedUsers().size());
    }

    @Test
    @Rollback(true)
    public void testControllerWithActiveResourceThatBecomesDeleted() throws Exception {
        controller = generateNewInitializedController(CollectionController.class, getUser());
        controller.prepare();
        ResourceCollection rc = controller.getPersistable();
        Project project = createAndSaveNewResource(Project.class, getUser(), "test project");
        // not 100% sure why we're using a proxy here, but technically, I think this i closer to what we do in real life
        // Project proxy = new Project(project.getId(), project.getTitle());
        Long pid = project.getId();

        controller.setAuthorizedUsers(Collections.<AuthorizedUser> emptyList());
        controller.getResources().add(project);
        controller.getPersistable().setName("testControllerWithActiveResourceThatBecomesDeleted");
        controller.getPersistable().setDescription("description");
        controller.setServletRequest(getServletPostRequest());
        controller.setAsync(false);
        String result = controller.save();
        Assert.assertEquals(Action.SUCCESS, result);
        Long rcid = rc.getId();
        searchIndexService.flushToIndexes();
        // so, wait, is this resource actually in the collection?
        controller = generateNewInitializedController(CollectionController.class);
        controller.setId(rcid);
        controller.prepare();
        controller.view();
        assertEquals("okay, we should have one resource in this collection now", 1, controller.getResults().size());

        project.getResourceCollections().add(rc);
        genericService.saveOrUpdate(project);
        evictCache();
        // okay now lets delete the resource
        ProjectController projectController = generateNewInitializedController(ProjectController.class);
        projectController.setServletRequest(getServletPostRequest());
        projectController.setId(pid);
        projectController.prepare();
        projectController.setDelete(TdarActionSupport.DELETE);
        projectController.delete();
        searchIndexService.flushToIndexes();

        // go back to the collection's 'edit' page and make sure that we are not displaying the deleted resource
        controller = generateNewInitializedController(CollectionController.class, getUser());
        controller.setId(rcid);
        controller.prepare();
        controller.edit();
        assertEquals("deleted resource should not appear on edit page", 0, controller.getResources().size());

        // so far so good. but lets make sure that the resource *is* actually in the collection
        rc = genericService.find(ResourceCollection.class, rcid);
        assertTrue(rc.getResources().contains(project));
        logger.info("{}", projectController.getProject().getResourceCollections());
    }

    @Test
    @Rollback(true)
    public void testControllerWithDeletedResourceThatBecomesActive() throws Exception {
        Project project = createAndSaveNewResource(Project.class, getUser(), "test project");
        ResourceCollection collection = generateResourceCollection("test collection with deleted", "test", CollectionType.SHARED, true, null, getUser(),
                Arrays.asList(project), null);
        project.setStatus(Status.DELETED);
        project.getResourceCollections().add(collection);
        genericService.saveOrUpdate(project);
        Long rcid = collection.getId();
        Long pid = project.getId();

        searchIndexService.flushToIndexes();
        searchIndexService.index(collection);
        searchIndexService.index(project);
        collection = null;
        project = null;
        // so, wait, is this resource actually in the collection?

        // undelete the project, then make sure that the collection shows up on the collection view page
        ProjectController projectController = generateNewInitializedController(ProjectController.class, getAdminUser());
        projectController.setId(pid);
        projectController.prepare();
        projectController.edit();
        Project project2 = projectController.getPersistable();
        project2.setStatus(Status.ACTIVE);
        projectController.setServletRequest(getServletPostRequest());
        projectController.setAsync(false);
        projectController.save();
        evictCache();
        searchIndexService.flushToIndexes();
        searchIndexService.index(project2);

        logger.info("{}", project2.getResourceCollections());
        assertTrue(Persistable.Base.extractIds(project2.getResourceCollections()).contains(rcid));
        controller = generateNewInitializedController(CollectionController.class);
        controller.setId(rcid);
        controller.prepare();
        controller.view();
        logger.info("{}", controller.getResourceCollection().getResources().iterator().next().getStatus());
        assertTrue("collection should show the newly undeleted project", CollectionUtils.isNotEmpty(controller.getResults()));

        // we should also see the newly-undeleted resource on the edit page
        controller = generateNewInitializedController(CollectionController.class);
        controller.setId(rcid);
        controller.prepare();
        controller.edit();
        logger.info("resources:{}", controller.getResources());
        logger.info("?:{}", controller.getResults());
        logger.info("?:{}", controller.getResourceCollection().getResources());
        assertTrue("collection should show the newly undeleted project", CollectionUtils.isNotEmpty(controller.getResources()));
    }

    @Test
    @Rollback(true)
    public void testDraftResourceVisibleByAuthuser() throws Exception {
        controller = generateNewInitializedController(CollectionController.class, getUser());
        controller.prepare();
        ResourceCollection rc = controller.getPersistable();
        Project project = createAndSaveNewResource(Project.class, getUser(), "test project");
        project.setStatus(Status.DRAFT);
        genericService.saveOrUpdate(project);
        // project = null;
        // Long pid = project.getId();
        Project proxy = new Project(project.getId(), project.getTitle());
        controller.setAuthorizedUsers(Collections.<AuthorizedUser> emptyList());
        controller.getResources().add(proxy);
        controller.getPersistable().setName("testControllerWithActiveResourceThatBecomesDeleted");
        controller.getPersistable().setDescription("description");
        controller.setServletRequest(getServletPostRequest());
        controller.setAsync(false);
        String result = controller.save();
        // searchIndexService.index(proxy);

        Assert.assertEquals(Action.SUCCESS, result);
        Long rcid = rc.getId();

        // confirm resource is viewable by author of collection
        controller = generateNewInitializedController(CollectionController.class);
        controller.setId(rcid);
        controller.prepare();
        controller.view();
        assertEquals("collection should have one resource inside", 1, controller.getResults().size());
        controller = null;
        // make sure it draft resource can't be seen by registered user (but not an authuser)
        TdarUser registeredUser = createAndSaveNewPerson("testDraftResourceVisibleByAuthuser", "foo");
        controller = generateNewInitializedController(CollectionController.class, registeredUser);
        controller.setId(rcid);
        controller.prepare();
        controller.view();
        assertEquals(controller.getAuthenticatedUser(), registeredUser);
        assertTrue("resource should not be viewable", controller.getResults().isEmpty());
        // assertFalse("resource should not be viewable", controller.getResults().get(0).isViewable());

        // now make the user an authorizedUser
        controller = generateNewInitializedController(CollectionController.class);
        controller.setId(rcid);
        controller.prepare();
        AuthorizedUser authUser = new AuthorizedUser(registeredUser, GeneralPermissions.MODIFY_RECORD);
        List<AuthorizedUser> authList = new ArrayList<AuthorizedUser>(Arrays.asList(authUser));
        controller.setAuthorizedUsers(authList);
        controller.getResources().add(proxy);
        controller.setServletRequest(getServletPostRequest());
        controller.setAsync(false);
        controller.save();
        evictCache();
        searchIndexService.flushToIndexes();
        // searchIndexService.indexAll();
        // registered user is now authuser of the collection, and should be able to see the resource
        controller = generateNewInitializedController(CollectionController.class, registeredUser);
        controller.setId(rcid);
        controller.prepare();
        controller.view();
        assertTrue("resource should be viewable", ((Viewable) (controller.getResults().get(0))).isViewable());

        // now make the registeredUser a non-contributor. make sure they can see the resource (TDAR-2028)
        registeredUser.setContributor(false);
        genericService.saveOrUpdate(registeredUser);
        searchIndexService.index(registeredUser);
        controller = generateNewInitializedController(CollectionController.class, registeredUser);
        controller.setId(rcid);
        controller.prepare();
        controller.view();
        assertTrue("resource should be viewable", ((Viewable) controller.getResults().get(0)).isViewable());
    }

    @Test
    public void testResourceCollectionRightsRevoking() throws TdarActionException {
        TdarUser registeredUser = createAndSaveNewPerson("testDraftResourceVisibleByAuthuser", "foo");
        controller = generateNewInitializedController(CollectionController.class, getUser());
        controller.prepare();
        ResourceCollection rc = controller.getPersistable();
        // project = null;
        // Long pid = project.getId();
        controller.getAuthorizedUsers().add(new AuthorizedUser(registeredUser, GeneralPermissions.ADMINISTER_GROUP));
        controller.getPersistable().setName("test");
        controller.getPersistable().setDescription("description");
        controller.setServletRequest(getServletPostRequest());
        controller.setAsync(false);
        String result = controller.save();
        assertEquals(Action.SUCCESS, result);
        Long rcid = controller.getPersistable().getId();
        // confirm resource is viewable by author of collection
        controller = generateNewInitializedController(CollectionController.class, registeredUser);
        controller.setId(rcid);
        controller.prepare();
        controller.edit();
        controller.getAuthorizedUsers().clear();
        controller.setServletRequest(getServletPostRequest());
        controller.setAsync(false);
        result = controller.save();

        // make sure it draft resource can't be seen by registered user (but not an authuser)
        controller = generateNewInitializedController(CollectionController.class, registeredUser);
        controller.setId(rcid);
        controller.prepare();
        boolean seen = false;
        ignoreActionErrors(true);
        try {
            controller.edit();
        } catch (Exception e) {
            seen = true;
            logger.warn("error", e);
        }
        assertTrue(seen);
    }

    @Test
    public void testResourceCollectionRightsRevokingHier() throws TdarActionException {
        TdarUser registeredUser = createAndSaveNewPerson("testDraftResourceVisibleByAuthuser", "foo");
        controller = generateNewInitializedController(CollectionController.class, getUser());
        controller.prepare();
        ResourceCollection rc = controller.getPersistable();
        // project = null;
        // Long pid = project.getId();
        controller.getAuthorizedUsers().add(new AuthorizedUser(registeredUser, GeneralPermissions.ADMINISTER_GROUP));
        controller.getPersistable().setName("test");
        controller.getPersistable().setDescription("description");
        controller.setServletRequest(getServletPostRequest());
        controller.setAsync(false);
        String result = controller.save();
        assertEquals(Action.SUCCESS, result);
        Long rcid = controller.getPersistable().getId();

        controller = generateNewInitializedController(CollectionController.class, getUser());
        controller.prepare();
        ResourceCollection rcChild = controller.getPersistable();
        // project = null;
        // Long pid = project.getId();
        controller.setParentId(rcid);
        controller.getPersistable().setName("test child");
        controller.getPersistable().setDescription("description");
        controller.setServletRequest(getServletPostRequest());
        controller.setAsync(false);
        result = controller.save();
        assertEquals(Action.SUCCESS, result);
        Long rcid2 = controller.getPersistable().getId();

        // confirm resource is viewable by author of collection
        controller = generateNewInitializedController(CollectionController.class, registeredUser);
        controller.setId(rcid2);
        controller.prepare();
        controller.edit();
        controller.setParentId(null);
        controller.setServletRequest(getServletPostRequest());
        controller.setAsync(false);
        result = controller.save();

        // make sure it draft resource can't be seen by registered user (but not an authuser)
        controller = generateNewInitializedController(CollectionController.class, registeredUser);
        controller.setId(rcid2);
        controller.prepare();
        boolean seen = false;
        ignoreActionErrors(true);
        try {
            controller.edit();
        } catch (Exception e) {
            seen = true;
            logger.warn("error", e);
        }
        assertTrue(seen);
    }

    @Test
    public void testResourceCollectionRightsRevokingHierOwnerFails() throws TdarActionException {
        TdarUser registeredUser = createAndSaveNewPerson("testDraftResourceVisibleByAuthuser", "foo");
        controller = generateNewInitializedController(CollectionController.class, registeredUser);
        controller.prepare();
        ResourceCollection rc = controller.getPersistable();
        // project = null;
        // Long pid = project.getId();
        controller.getPersistable().setName("test");
        controller.getPersistable().setDescription("description");
        controller.getAuthorizedUsers().add(new AuthorizedUser(getUser(), GeneralPermissions.ADMINISTER_GROUP));
        controller.setServletRequest(getServletPostRequest());
        controller.setAsync(false);
        String result = controller.save();
        assertEquals(Action.SUCCESS, result);
        Long rcid = controller.getPersistable().getId();

        controller = generateNewInitializedController(CollectionController.class, getUser());
        controller.prepare();
        ResourceCollection rcChild = controller.getPersistable();
        // project = null;
        // Long pid = project.getId();
        controller.setParentId(rcid);
        controller.getPersistable().setName("test child");
        controller.getPersistable().setDescription("description");
        controller.setServletRequest(getServletPostRequest());
        controller.setAsync(false);
        result = controller.save();
        assertEquals(Action.SUCCESS, result);
        Long rcid2 = controller.getPersistable().getId();

        /*
         * This test is expected to fail in-that hierarchical collection "owners" have no rights implicitly.
         * Change this test when we figure out what "should" change in package-info
         */
        controller = generateNewInitializedController(CollectionController.class, registeredUser);
        controller.setId(rcid2);
        controller.prepare();
        boolean seen = false;
        ignoreActionErrors(true);
        try {
            controller.edit();
        } catch (Exception e) {
            seen = true;
            logger.warn("error", e);
        }
        assertTrue(seen);
    }

    private Map<Long, Resource> getIdmap(Set<Resource> resources) {
        Map<Long, Resource> idmap = new HashMap<>();
        for (Resource resource : resources) {
            idmap.put(resource.getId(), resource);
        }
        return idmap;
    }

    @Test
    /**
     *  Assert that the sparse resource list returned by findCollectionSparseResources  matches the persisted list (for
     *  the subset of properties in Resource that we care about)
     */
    public void testfindCollectionSparseResources() {

        // For now we rely on the the init-test and any data created by intervening web tests. In this way this test
        // is brittle. A better idea would be to create our own sample data.
        List<ResourceCollection> allCollections = resourceCollectionService.findAll();
        assertThat("sample data set size", allCollections.size(), greaterThan(5));

        for (ResourceCollection collection : allCollections) {
            // get map of persisted resources
            Map<Long, Resource> persistedResourceMap = getIdmap(collection.getResources());

            // get list of sparse resources, make sure it has same size & contents as the persisted resource list.
            List<Resource> sparseResources = resourceCollectionService.findCollectionSparseResources(collection.getId());
            assertThat(collection.getResources(), hasSize(sparseResources.size()));

            for (Resource sparseResource : sparseResources) {
                logger.trace("evaluating resource:{}", sparseResource);
                assertThat(persistedResourceMap, hasKey(sparseResource.getId()));

                Resource resource = persistedResourceMap.get(sparseResource.getId());
                assertThat(sparseResource.getTitle(), is(resource.getTitle()));
                assertThat(sparseResource.getResourceType(), is(resource.getResourceType()));
                assertThat(sparseResource.getStatus(), is(resource.getStatus()));
                assertThat(sparseResource.getSubmitter(), is(resource.getSubmitter())); // here we assume ID equality
            }
        }

    }
}
