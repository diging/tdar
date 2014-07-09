package org.tdar.struts.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.junit.Assert;
import org.tdar.TestConstants;
import org.tdar.core.bean.AbstractIntegrationTestCase;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.PersonalFilestoreTicket;
import org.tdar.core.bean.billing.Account;
import org.tdar.core.bean.billing.BillingItem;
import org.tdar.core.bean.billing.Invoice;
import org.tdar.core.bean.billing.Invoice.TransactionStatus;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.collection.ResourceCollection.CollectionType;
import org.tdar.core.bean.entity.AuthorizedUser;
import org.tdar.core.bean.entity.Creator;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.resource.BookmarkedResource;
import org.tdar.core.bean.resource.CodingSheet;
import org.tdar.core.bean.resource.Dataset;
import org.tdar.core.bean.resource.Document;
import org.tdar.core.bean.resource.Image;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.InformationResourceFile.FileAccessRestriction;
import org.tdar.core.bean.resource.InformationResourceFile.FileAction;
import org.tdar.core.bean.resource.Ontology;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.VersionType;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.filestore.personal.PersonalFilestoreFile;
import org.tdar.search.query.SortOption;
import org.tdar.struts.action.account.UserAccountController;
import org.tdar.struts.action.resource.AbstractInformationResourceController;
import org.tdar.struts.action.resource.AbstractSupportingInformationResourceController;
import org.tdar.struts.action.resource.CodingSheetController;
import org.tdar.struts.action.resource.DatasetController;
import org.tdar.struts.action.resource.DocumentController;
import org.tdar.struts.action.resource.ImageController;
import org.tdar.struts.action.resource.OntologyController;
import org.tdar.struts.data.FileProxy;
import org.tdar.utils.Pair;

import com.opensymphony.xwork2.Action;

public abstract class AbstractControllerITCase extends AbstractIntegrationTestCase {

    private static final String PATH = TestConstants.TEST_ROOT_DIR;
    public static final String TESTING_AUTH_INSTIUTION = "testing auth instiution";

    public static final String REASON = "because";


    public void bookmarkResource(Resource r, TdarUser user) {
        bookmarkResource(r, false, user);
    }

    public void removeBookmark(Resource r, TdarUser user) {
        removeBookmark(r, false, user);
    }

    public Account createAccount(TdarUser owner) {
        Account account = new Account("my account");
        account.setDescription("this is an account for : " + owner.getProperName());
        account.setOwner(owner);
        account.markUpdated(owner);
        genericService.saveOrUpdate(account);
        return account;
    }

    // public Account createAccountWithOneItem(Person person) {
    // return createA
    // }

    public Invoice createInvoice(TdarUser person, TransactionStatus status, BillingItem... items) {
        Invoice invoice = new Invoice();
        invoice.setItems(new ArrayList<BillingItem>());
        for (BillingItem item : items) {
            invoice.getItems().add(item);
        }
        invoice.setOwner(person);
        invoice.setTransactionStatus(status);
        genericService.saveOrUpdate(invoice);
        return invoice;
    }

    public void bookmarkResource(Resource r, boolean ajax, TdarUser user) {
        BookmarkResourceController bookmarkController = generateNewInitializedController(BookmarkResourceController.class);
        logger.info("bookmarking " + r.getTitle() + " (" + r.getId() + ")");
        bookmarkController.setResourceId(r.getId());
        if (ajax) {
            bookmarkController.bookmarkResourceAjaxAction();
        } else {
            bookmarkController.bookmarkResourceAction();
        }
        r = resourceService.find(r.getId());
        assertNotNull(r);
        genericService.refresh(user);
        boolean seen = false;
        for (BookmarkedResource b : user.getBookmarkedResources()) {
            if (ObjectUtils.equals(b.getResource(), r)) {
                seen = true;
            }
        }
        Assert.assertTrue("should have seen resource in bookmark list",seen);
    }

    public void removeBookmark(Resource r, boolean ajax, TdarUser user) {
        BookmarkResourceController bookmarkController = generateNewInitializedController(BookmarkResourceController.class);
        boolean seen = false;
        for (BookmarkedResource b : user.getBookmarkedResources()) {
            if (ObjectUtils.equals(b.getResource(), r)) {
                seen = true;
            }
        }
        
        Assert.assertTrue("should have seen resource in bookmark list",seen);
        logger.info("removing bookmark " + r.getTitle() + " (" + r.getId() + ")");
        bookmarkController.setResourceId(r.getId());
        if (ajax) {
            bookmarkController.removeBookmarkAjaxAction();
        } else {
            bookmarkController.removeBookmarkAction();
        }
        seen = false;
        genericService.synchronize();
        user = genericService.find(TdarUser.class, user.getId());
        for (BookmarkedResource b : user.getBookmarkedResources()) {
            if (ObjectUtils.equals(b.getResource(), r)) {
                seen = true;
            }
        }
        Assert.assertFalse("should not see resource", seen);
    }

    public ResourceCollection generateResourceCollection(String name, String description, CollectionType type, boolean visible, List<AuthorizedUser> users,
            List<? extends Resource> resources, Long parentId)
            throws Exception {
        return generateResourceCollection(name, description, type, visible, users, getUser(), resources, parentId);
    }

    public ResourceCollection generateResourceCollection(String name, String description, CollectionType type, boolean visible, List<AuthorizedUser> users,
            TdarUser owner, List<? extends Resource> resources, Long parentId) throws Exception {
        CollectionController controller = generateNewInitializedController(CollectionController.class, owner);
        controller.setServletRequest(getServletPostRequest());
        controller.prepare();
        // controller.setSessionData(getSessionData());
        logger.info("{}", getUser());
        assertEquals(owner, controller.getAuthenticatedUser());
        ResourceCollection resourceCollection = controller.getResourceCollection();
        resourceCollection.setName(name);
        resourceCollection.setParent(genericService.find(ResourceCollection.class, parentId));
        controller.setParentId(parentId);
        resourceCollection.setType(type);
        controller.setAsync(false);
        resourceCollection.setVisible(visible);
        resourceCollection.setDescription(description);
        if (resources != null) {
            controller.getResources().addAll(resources);
        }

        if (users != null) {
            controller.getAuthorizedUsers().clear();
            controller.getAuthorizedUsers().addAll(users);
        }
        resourceCollection.setSortBy(SortOption.RESOURCE_TYPE);
        controller.setServletRequest(getServletPostRequest());
        String save = controller.save();
        assertTrue(save.equals(Action.SUCCESS));
        return resourceCollection;
    }

    Long uploadFile(String path, String name) {
        String path_ = path;
        String name_ = name;
        if (name_.contains("src/test/")) {
            path_ = FilenameUtils.getPath(name_);
            name_ = FilenameUtils.getName(name_);
        }
        logger.info("name: {} path: {}", name_, path_);
        UploadController controller = generateNewInitializedController(UploadController.class);
        controller.setSessionData(getSessionData());
        controller.grabTicket();
        Long ticketId = controller.getPersonalFilestoreTicket().getId();
        logger.info("ticketId {}", ticketId);
        controller = generateNewInitializedController(UploadController.class);
        controller.setUploadFile(Arrays.asList(new File(path_ + name_)));
        controller.setUploadFileFileName(Arrays.asList(name_));
        controller.setTicketId(ticketId);
        String upload = controller.upload();
        assertEquals(Action.SUCCESS, upload);
        return ticketId;
    }

    public <C> C setupAndLoadResource(String filename, Class<C> cls) {
        return setupAndLoadResource(filename, cls, FileAccessRestriction.PUBLIC, -1L);
    }

    public <C> C setupAndLoadResource(String filename, Class<C> cls, FileAccessRestriction permis) {
        return setupAndLoadResource(filename, cls, permis, -1L);
    }

    public <C> C setupAndLoadResource(String filename, Class<C> cls, Long id) {
        return setupAndLoadResource(filename, cls, FileAccessRestriction.PUBLIC, id);
    }

    @SuppressWarnings("unchecked")
    public <C> C replaceFile(String uploadFile, String replaceFile, Class<C> cls, Long id) throws TdarActionException {
        AbstractInformationResourceController<?> controller = null;
        Long ticketId = -1L;
        if (cls.equals(Ontology.class)) {
            controller = generateNewInitializedController(OntologyController.class);
        } else if (cls.equals(Dataset.class)) {
            controller = generateNewInitializedController(DatasetController.class);
            ticketId = uploadFile(getTestFilePath(), uploadFile);
        } else if (cls.equals(Document.class)) {
            controller = generateNewInitializedController(DocumentController.class);
            ticketId = uploadFile(getTestFilePath(), uploadFile);
        } else if (cls.equals(Image.class)) {
            controller = generateNewInitializedController(ImageController.class);
            ticketId = uploadFile(getTestFilePath(), uploadFile);
        } else if (cls.equals(CodingSheet.class)) {
            controller = generateNewInitializedController(CodingSheetController.class);
        }
        controller.setId(id);
        controller.prepare();
        controller.edit();
        // FileProxy newProxy = new FileProxy(uploadFile, VersionType.UPLOADED, FileAccessRestriction.PUBLIC);
        // newProxy.setAction(FileAction.REPLACE);
        for (FileProxy proxy : controller.getFileProxies()) {
            if (proxy.getFilename().equals(replaceFile)) {
                proxy.setFilename(uploadFile);
                proxy.setAction(FileAction.REPLACE);
                // newProxy.set
            }
        }
        // controller.getFileProxies().add(newProxy);
        controller.setTicketId(ticketId);
        controller.setServletRequest(getServletPostRequest());
        controller.save();
        return (C) controller.getResource();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <C> C setupAndLoadResource(String filename, Class<C> cls, FileAccessRestriction permis, Long id) {

        AbstractInformationResourceController controller = null;
        Long ticketId = -1L;
        if (cls.equals(Ontology.class)) {
            controller = generateNewInitializedController(OntologyController.class);
        } else if (cls.equals(Dataset.class)) {
            controller = generateNewInitializedController(DatasetController.class);
            ticketId = uploadFile(getTestFilePath(), filename);
        } else if (cls.equals(Document.class)) {
            controller = generateNewInitializedController(DocumentController.class);
            ticketId = uploadFile(getTestFilePath(), filename);
        } else if (cls.equals(Image.class)) {
            controller = generateNewInitializedController(ImageController.class);
            ticketId = uploadFile(getTestFilePath(), filename);
        } else if (cls.equals(CodingSheet.class)) {
            controller = generateNewInitializedController(CodingSheetController.class);
        }
        if (controller == null) {
            return null;
        }

        if (Persistable.Base.isNotNullOrTransient(id)) {
            controller.setId(id);
        }
        controller.prepare();
        final Resource resource = controller.getResource();
        resource.setTitle(filename);
        resource.setDescription("This resource was created as a result of a test: " + getClass());
        if ((resource instanceof InformationResource) && TdarConfiguration.getInstance().getCopyrightMandatory()) {
            Creator copyrightHolder = genericService.find(Person.class, 1L);
            ((InformationResource) resource).setCopyrightHolder(copyrightHolder);
        }

        List<File> files = new ArrayList<File>();
        List<String> filenames = new ArrayList<String>();
        if (ticketId != -1) {
            controller.setTicketId(ticketId);
            controller.setFileProxies(Arrays.asList(new FileProxy(FilenameUtils.getName(filename), VersionType.UPLOADED, permis)));
        } else {
            File file = new File(getTestFilePath(), filename);
            assertTrue("file not found:" + getTestFilePath() + "/" + filename, file.exists());
            if (FilenameUtils.getExtension(filename).equals("txt") && (controller instanceof AbstractSupportingInformationResourceController<?>)) {
                AbstractSupportingInformationResourceController<?> asc = (AbstractSupportingInformationResourceController<?>) controller;
                asc.setFileInputMethod(AbstractInformationResourceController.FILE_INPUT_METHOD);
                try {
                    asc.setFileTextInput(FileUtils.readFileToString(file));
                } catch (Exception e) {
                    Assert.fail(e.getMessage());
                }
            } else {
                files.add(file);
                filenames.add(filename);
                controller.setUploadedFiles(files);
                controller.setUploadedFilesFileName(filenames);
            }
        }
        try {
            controller.setServletRequest(getServletPostRequest());
            controller.save();
        } catch (TdarActionException exception) {
            // what now?
            exception.printStackTrace();
        }
        return (C) controller.getResource();
    }

    protected String getTestFilePath() {
        return PATH;
    }

    public Pair<PersonalFilestoreTicket, List<FileProxy>> uploadFilesAsync(List<File> uploadFiles) throws FileNotFoundException {
        UploadController uploadController = generateNewInitializedController(UploadController.class);
        assertEquals(Action.SUCCESS, uploadController.grabTicket());
        PersonalFilestoreTicket ticket = uploadController.getPersonalFilestoreTicket();
        Pair<PersonalFilestoreTicket, List<FileProxy>> toReturn = new Pair<PersonalFilestoreTicket, List<FileProxy>>(ticket, new ArrayList<FileProxy>());
        uploadController = generateNewInitializedController(UploadController.class);
        assertNull(uploadController.getTicketId());

        uploadController.setTicketId(ticket.getId());
        uploadController.setUploadFile(uploadFiles);
        for (File uploadedFile : uploadFiles) {
            uploadController.getUploadFileFileName().add(uploadedFile.getName());
            FileProxy fileProxy = new FileProxy();
            fileProxy.setFilename(uploadedFile.getName());
            fileProxy.setFile(uploadedFile);
            fileProxy.setAction(FileAction.ADD);
            toReturn.getSecond().add(fileProxy);
        }

        assertEquals(Action.SUCCESS, uploadController.upload());
        List<PersonalFilestoreFile> files = filestoreService.retrieveAllPersonalFilestoreFiles(uploadController.getTicketId());
        assertEquals("file count retrieved from personal filestore", uploadFiles.size(), files.size());
        // XXX: potentially assert that md5s and/or filenames are same across both file lists
        for (PersonalFilestoreFile personalFilestoreFile : files) {
            String filename = personalFilestoreFile.getFile().getName();
            boolean equal = false;
            for (File uploadFile : uploadFiles) {
                if (filename.equals(uploadFile.getName())) {
                    equal = true;
                }
            }
            assertTrue(filename + " not found in uploadFiles: " + uploadFiles, equal);
        }
        return toReturn;
    }

    @Override
    protected TdarUser getUser() {
        return getUser(getUserId());
    }

    public String setupValidUserInController(UserAccountController controller) {
        return setupValidUserInController(controller, "testuser@example.com");
    }

    public String setupValidUserInController(UserAccountController controller, String email) {
        TdarUser p = new TdarUser();
        p.setEmail(email);
        p.setUsername(email);
        p.setFirstName("Testing auth");
        p.setLastName("User");
        p.setPhone("212 000 0000");
        controller.getRegistration().setPerson(p);
        controller.getRegistration().setRequestingContributorAccess(true);
        controller.getRegistration().setAcceptTermsOfUse(true);
        controller.getRegistration().setContributorReason(REASON);
        p.setRpaNumber("214");

        return setupValidUserInController(controller, p);
    }

    public String setupValidUserInController(UserAccountController controller, TdarUser p) {
        return setupValidUserInController(controller, p, "password");
    }

    public String setupValidUserInController(UserAccountController controller, TdarUser p, String password) {
        // cleanup crowd if we need to...
        authenticationService.getAuthenticationProvider().deleteUser(p);
        controller.getRegistration().setRequestingContributorAccess(true);
        controller.getRegistration().setInstitutionName(TESTING_AUTH_INSTIUTION);
        controller.getRegistration().setPassword(password);
        controller.getRegistration().setConfirmPassword(password);
        controller.getRegistration().setConfirmEmail(p.getEmail());
        controller.getRegistration().setPerson(p);
        controller.getRegistration().setAcceptTermsOfUse(true);
        controller.setServletRequest(getServletPostRequest());
        controller.setServletResponse(getServletResponse());
        controller.validate();
        String execute = null;
        // technically this is more appropriate -- only call create if validate passes
        if (CollectionUtils.isEmpty(controller.getActionErrors())) {
            execute = controller.create();
        } else {
            logger.error("errors: {} ", controller.getActionErrors());
        }

        return execute;
    }

    @SuppressWarnings("unchecked")
    protected void reindex() {
        searchIndexService.purgeAll();
        searchIndexService.indexAll(getAdminUser(), Resource.class, ResourceCollection.class);
    }
}
