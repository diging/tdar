package org.tdar.struts.action;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.StrutsStatics;
import org.custommonkey.xmlunit.jaxp13.Validator;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.tdar.core.bean.AbstractIntegrationTestCase;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.collection.ResourceCollection.CollectionType;
import org.tdar.core.bean.entity.AuthorizedUser;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.entity.permissions.GeneralPermissions;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.dao.entity.AuthorizedUserDao;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.service.BookmarkedResourceService;
import org.tdar.core.service.EntityService;
import org.tdar.core.service.GenericService;
import org.tdar.core.service.PersonalFilestoreService;
import org.tdar.core.service.ResourceCollectionService;
import org.tdar.core.service.SerializationService;
import org.tdar.core.service.UrlService;
import org.tdar.core.service.external.AuthenticationService;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.core.service.external.EmailService;
import org.tdar.core.service.external.session.SessionData;
import org.tdar.core.service.integration.DataIntegrationService;
import org.tdar.core.service.processes.SendEmailProcess;
import org.tdar.core.service.resource.DataTableService;
import org.tdar.core.service.resource.DatasetService;
import org.tdar.core.service.resource.InformationResourceService;
import org.tdar.core.service.resource.ProjectService;
import org.tdar.core.service.resource.ResourceService;
import org.tdar.core.service.search.SearchIndexService;
import org.tdar.core.service.search.SearchService;
import org.tdar.struts.ErrorListener;
import org.tdar.utils.PersistableUtils;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.LocaleProvider;
import com.opensymphony.xwork2.TextProviderFactory;
import com.opensymphony.xwork2.config.ConfigurationManager;
import com.opensymphony.xwork2.config.providers.XWorkConfigurationProvider;
import com.opensymphony.xwork2.ognl.OgnlValueStackFactory;
import com.opensymphony.xwork2.util.LocalizedTextUtil;
import com.opensymphony.xwork2.util.ValueStack;

@SuppressWarnings("rawtypes")
public abstract class AbstractIntegrationControllerTestCase extends AbstractIntegrationTestCase implements ErrorListener {

    protected HttpServletRequest defaultHttpServletRequest = new MockHttpServletRequest();

    protected HttpServletRequest httpServletRequest = defaultHttpServletRequest;
    protected HttpServletRequest httpServletPostRequest = new MockHttpServletRequest("POST", "/");
    protected HttpServletResponse httpServletResponse = new MockHttpServletResponse();

    protected PlatformTransactionManager transactionManager;
    private TransactionCallback verifyTransactionCallback;
    private TransactionTemplate transactionTemplate;

    @Autowired
    protected SessionFactory sessionFactory;
    @Autowired
    protected ProjectService projectService;
    @Autowired
    protected DatasetService datasetService;
    @Autowired
    protected DataTableService dataTableService;
    @Autowired
    protected DataIntegrationService dataIntegrationService;
    @Autowired
    protected GenericService genericService;
    @Autowired
    protected UrlService urlService;
    @Autowired
    protected ResourceService resourceService;
    @Autowired
    protected EntityService entityService;
    @Autowired
    protected InformationResourceService informationResourceService;
    @Autowired
    protected SearchIndexService searchIndexService;
    @Autowired
    protected SearchService searchService;
    @Autowired
    protected BookmarkedResourceService bookmarkedResourceService;
    @Autowired
    protected PersonalFilestoreService filestoreService;
    @Autowired
    protected AuthorizationService authenticationAndAuthorizationService;
    @Autowired
    protected AuthenticationService authenticationService;
    @Autowired
    private SerializationService serializationService;
    @Autowired
    protected ResourceCollectionService resourceCollectionService;
    @Autowired
    private AuthorizedUserDao authorizedUserDao;

    @Autowired
    public SendEmailProcess sendEmailProcess;

    @Autowired
    protected EmailService emailService;

    private List<String> actionErrors = new ArrayList<>();
    private boolean ignoreActionErrors = false;
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private SessionData sessionData;

    @Rule
    public TestWatcher failWatcher = new TestWatcher() {

        @Override
        protected void failed(Throwable e, Description description) {
            AbstractIntegrationControllerTestCase.this.onFail(e, description);
        }

    };

    @Before
    public void announceTestStarting() {
        setIgnoreActionErrors(false);
        getActionErrors().clear();
    }

    // Called when your test fails. Did I say "when"? I meant "if".
    public void onFail(Throwable e, Description description) {
    }

    @After
    public void announceTestOver() {
        int errorCount = 0;
        if (!isIgnoreActionErrors() && CollectionUtils.isNotEmpty(getActionErrors())) {
            logger.error("action errors {}", getActionErrors());
            errorCount = getActionErrors().size();
        }

        if (errorCount > 0) {
            Assert.fail(String.format("There were %d action errors: \n {} ", errorCount, StringUtils.join(getActionErrors().toArray(new String[0]))));
        }
    }

    protected <T> T generateNewController(Class<T> controllerClass) {
        getAuthorizedUserDao().clearUserPermissionsCache();
        // evictCache();

        T controller = applicationContext.getBean(controllerClass);
        if (controller instanceof AuthenticationAware.Base) {
            TdarActionSupport tas = (TdarActionSupport) controller;
            tas.setServletRequest(getServletRequest());
            tas.setServletResponse(getServletResponse());
            // set the context
        }
        Map<String, Object> contextMap = new HashMap<String, Object>();
        contextMap.put(StrutsStatics.HTTP_REQUEST, getServletRequest());
        ActionContext context = new ActionContext(contextMap);
        context.setLocale(Locale.getDefault());
        // http://mail-archives.apache.org/mod_mbox/struts-user/201001.mbox/%3C637b76e41001151852x119c9cd4vbbe6ff560e56e46f@mail.gmail.com%3E
        ConfigurationManager configurationManager = new ConfigurationManager();
        OgnlValueStackFactory factory = new OgnlValueStackFactory();

        // FIXME: needs to be a better way to handle this
        TextProviderFactory textProviderFactory = new TextProviderFactory();
        String bundle = "Locales/tdar-messages";

        LocalizedTextUtil.addDefaultResourceBundle(bundle);
        factory.setTextProvider(textProviderFactory.createInstance(ResourceBundle.getBundle(bundle), (LocaleProvider) controller));

        configurationManager.addContainerProvider(new XWorkConfigurationProvider());
        configurationManager.getConfiguration().getContainer().inject(factory);
        ValueStack stack = factory.createValueStack();

        context.setValueStack(stack);
        ActionContext.setContext(context);
        return controller;
    }

    protected void init(TdarActionSupport controller, TdarUser user) {
        if (controller != null) {
            TdarUser user_ = null;
            controller.setSessionData(getSessionData());
            if ((user != null) && PersistableUtils.isTransient(user)) {
                throw new TdarRecoverableRuntimeException("can't test this way right now, must persist first");
            } else if (user != null) {
                user_ = genericService.find(TdarUser.class, user.getId());
            } else {
                controller.getSessionData().clearAuthenticationToken();
            }
            controller.getSessionData().setTdarUser(user_);
        }
    }

    protected <T extends ActionSupport> T generateNewInitializedController(Class<T> controllerClass) {
        return generateNewInitializedController(controllerClass, null);
    }

    protected <T extends ActionSupport> T generateNewInitializedController(Class<T> controllerClass, TdarUser user) {
        T controller = generateNewController(controllerClass);
        if (controller instanceof TdarActionSupport) {
            if (user != null) {
                init((TdarActionSupport) controller, user);
            } else {
                init((TdarActionSupport) controller);
            }
            ((TdarActionSupport) controller).registerErrorListener(this);
        }
        return controller;
    }

    protected void init(TdarActionSupport controller) {
        init(controller, getSessionUser());
    }

    protected void initAnonymousUser(TdarActionSupport controller) {
        init(controller, null);
    }

    public SessionData getSessionData() {
        if (sessionData == null) {
            this.sessionData = new SessionData();
        }
        return sessionData;
    }

    protected <T> List<T> createListWithSingleNull() {
        ArrayList<T> list = new ArrayList<T>();
        list.add(null);
        return list;
    }


    public void setHttpServletRequest(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    public HttpServletRequest getServletRequest() {
        return httpServletRequest;
    }

    public HttpServletRequest getServletPostRequest() {
        return httpServletPostRequest;
    }

    public HttpServletRequest getDefaultHttpServletRequest() {
        return defaultHttpServletRequest;
    }

    public void setHttpServletResponse(HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
    }

    public HttpServletResponse getServletResponse() {
        return httpServletResponse;
    }

    public void addAuthorizedUser(Resource resource, TdarUser person, GeneralPermissions permission) {
        AuthorizedUser authorizedUser = new AuthorizedUser(person, permission);
        ResourceCollection internalResourceCollection = resource.getInternalResourceCollection();
        if (internalResourceCollection == null) {
            internalResourceCollection = new ResourceCollection(CollectionType.INTERNAL);
            internalResourceCollection.setOwner(person);
            internalResourceCollection.markUpdated(person);
            resource.getResourceCollections().add(internalResourceCollection);
            genericService.save(internalResourceCollection);
        }
        internalResourceCollection.getAuthorizedUsers().add(authorizedUser);
        logger.debug("{}", internalResourceCollection);
        genericService.saveOrUpdate(internalResourceCollection);
        genericService.saveOrUpdate(authorizedUser);
        genericService.saveOrUpdate(resource);
    }

    /**
     * @param ignoreActionErrors
     *            the ignoreActionErrors to set
     */
    public void setIgnoreActionErrors(boolean ignoreActionErrors) {
        this.ignoreActionErrors = ignoreActionErrors;
    }

    public void ignoreActionErrors(boolean ignoreActionErrors) {
        this.ignoreActionErrors = ignoreActionErrors;
    }

    /**
     * @return the ignoreActionErrors
     */
    public boolean isIgnoreActionErrors() {
        return ignoreActionErrors;
    }

    private TdarUser sessionUser;

    private static Validator v;

    /**
     * @return
     */
    public TdarUser getSessionUser() {
        if (sessionUser != null) {
            return sessionUser;
        }
        return getUser();
    }

    public void setSessionUser(TdarUser user) {
        this.sessionUser = user;
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public void addError(String error) {
        getActionErrors().add(error);
        if (!ignoreActionErrors) {
            fail(error);
        }
    }

    public List<String> getActionErrors() {
        return actionErrors;
    }

    public void setActionErrors(List<String> actionErrors) {
        this.actionErrors = actionErrors;
    }

}
