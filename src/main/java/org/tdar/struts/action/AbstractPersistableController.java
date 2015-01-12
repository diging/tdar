package org.tdar.struts.action;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.tdar.core.bean.HasName;
import org.tdar.core.bean.HasStatus;
import org.tdar.core.bean.Indexable;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.Updatable;
import org.tdar.core.bean.Validatable;
import org.tdar.core.bean.XmlLoggable;
import org.tdar.core.bean.entity.AuthorizedUser;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.entity.permissions.GeneralPermissions;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.dao.external.auth.InternalTdarRights;
import org.tdar.core.dao.resource.stats.ResourceSpaceUsageStatistic;
import org.tdar.core.exception.StatusCode;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.service.GenericService;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.core.service.external.RecaptchaService;
import org.tdar.core.service.search.SearchIndexService;
import org.tdar.struts.data.AntiSpamHelper;
import org.tdar.struts.interceptor.annotation.HttpsOnly;
import org.tdar.struts.interceptor.annotation.PostOnly;
import org.tdar.struts.interceptor.annotation.WriteableSession;
import org.tdar.utils.PersistableUtils;
import org.tdar.utils.jaxb.XMLFilestoreLogger;

import com.opensymphony.xwork2.Preparable;

/**
 * $Id$
 * 
 * Provides basic metadata support for controllers that manage subtypes of
 * Resource.
 * 
 * Don't extend this class unless you need this metadata to be set.
 * 
 * 
 * @author Adam Brin, <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
public abstract class AbstractPersistableController<P extends Persistable> extends AuthenticationAware.Base implements Preparable, PersistableLoadingAction<P> {

    public static final String SAVE_SUCCESS_PATH = "/${saveSuccessPath}/${persistable.id}${saveSuccessSuffix}";
    public static final String LIST = "list";
    public static final String DRAFT = "draft";

    @Autowired
    private transient SearchIndexService searchIndexService;
    @Autowired
    private transient RecaptchaService recaptchaService;

    private AntiSpamHelper h = new AntiSpamHelper();
    private static final long serialVersionUID = -559340771608580602L;
    private Long startTime = -1L;
    private String submitAction;
    private P persistable;
    private Long id;
    private Status status;
    private String saveSuccessPath = "view";
    private String saveSuccessSuffix = "";
    @SuppressWarnings("unused")
    private Class<P> persistableClass;
    public final static String msg = "%s is %s %s (%s): %s";
    public final static String REDIRECT_HOME = "REDIRECT_HOME";
    public final static String REDIRECT_PROJECT_LIST = "PROJECT_LIST";
    private boolean asyncSave = true;
    private List<AuthorizedUser> authorizedUsers;
    private List<String> authorizedUsersFullNames = new ArrayList<String>();

    private ResourceSpaceUsageStatistic totalResourceAccessStatistic;
    private ResourceSpaceUsageStatistic uploadedResourceAccessStatistic;
    @Autowired
    private transient GenericService genericService;
    @Autowired
    private transient AuthorizationService authorizationService;

    public static String formatTime(long millis) {
        Date dt = new Date(millis);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        // SimpleDateFormat sdf = new SimpleDateFormat("H'h, 'm'm, 's's, 'S'ms'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // no offset
        return sdf.format(dt);
    }

    protected P loadFromId(final Long id) {
        return getGenericService().find(getPersistableClass(), id);
    }

    /**
     * Override to perform custom save logic for the specific subtype of
     * Resource.
     * 
     * @param persistable
     * @return the String result code to use.
     * @throws TdarActionException
     */
    protected abstract String save(P persistable) throws TdarActionException;

    /**
     * Used to instantiate and return a new specific subtype of Resource to be
     * used by the Struts action and JSP/FTL page. Must be overridden by any
     * subclass of the AbstractResourceController.
     * 
     * @return a new instance of the specific subtype of Resource for which this
     *         ResourceController is managing requests.
     */
    protected P createPersistable() {
        try {
            return getPersistableClass().newInstance();
        } catch (Exception e) {
            addActionErrorWithException("could not instantiate class " + getPersistableClass(), e);
        }
        return null;
    }

    protected void loadListData() {
    }

    @SkipValidation
    @Action(value = LIST)
    public String list() {
        loadListData();
        return SUCCESS;
    }

    @Action(value = SAVE,
            interceptorRefs = { @InterceptorRef("editAuthenticatedStack") },
            results = {
                    @Result(name = SUCCESS, type = TYPE_REDIRECT, location = SAVE_SUCCESS_PATH),
                    @Result(name = SUCCESS_ASYNC, location = "view-async.ftl"),
                    @Result(name = INPUT, location = "edit.ftl")
            })
    @WriteableSession
    @PostOnly
    @HttpsOnly
    public String save() throws TdarActionException {
        // checkSession();
        // genericService.setCacheModeForCurrentSession(CacheMode.REFRESH);
        String actionReturnStatus = SUCCESS;
        logAction("SAVING");
        long currentTimeMillis = System.currentTimeMillis();
        boolean isNew = false;
        try {
            if (isPostRequest()) {
                if (isNullOrNew()) {
                    isNew = true;
                }
                preSaveCallback();
                determineAndUpdateStatus();

                if (persistable instanceof Updatable) {
                    ((Updatable) persistable).markUpdated(getAuthenticatedUser());
                }
                if (persistable instanceof Indexable) {
                    ((Indexable) persistable).setReadyToIndex(false);
                }
                if (persistable instanceof XmlLoggable) {
                    ((XmlLoggable) persistable).setReadyToStore(false);
                }

                actionReturnStatus = save(persistable);

                try {
                    postSaveCallback(actionReturnStatus);
                } catch (TdarRecoverableRuntimeException tex) {
                    addActionErrorWithException(tex.getMessage(), tex);
                }

                // should there not be "one" save at all? I think this should be here
                if (shouldSaveResource()) {
                    if (persistable instanceof XmlLoggable) {
                        ((XmlLoggable) persistable).setReadyToStore(true);
                    }
                    getGenericService().saveOrUpdate(persistable);
                    // NOTE: the below should not be necessary with the hibernate listener, but it seems like the saveOrUpdate above
                    // does not catch the change of the transient readyToStore boolean
                    XMLFilestoreLogger xmlLogger = new XMLFilestoreLogger();
                    xmlLogger.logRecordXmlToFilestore(persistable);
                }

                indexPersistable();
                // who cares what the save implementation says. if there's errors return INPUT
                if (!getActionErrors().isEmpty()) {
                    getLogger().debug("Action errors found {}; Replacing return status of {} with {}", getActionErrors(), actionReturnStatus, INPUT);
                    // FIXME: if INPUT -- should I just "return"?
                    actionReturnStatus = INPUT;
                }
            } else {
                throw new TdarActionException(StatusCode.BAD_REQUEST, getText("abstractPersistableController.bad_request"));
            }
        } catch (TdarActionException exception) {
            throw exception;
        } catch (Exception exception) {
            addActionErrorWithException(getText("abstractPersistableController.unable_to_save", getPersistable()), exception);
            return INPUT;
        } finally {
            // FIXME: make sure this doesn't cause issues with SessionSecurityInterceptor now handling TdarActionExceptions
            postSaveCleanup(actionReturnStatus);
        }
        long saveTime = System.currentTimeMillis() - currentTimeMillis;
        long editTime = System.currentTimeMillis() - getStartTime();
        if (getStartTime() == -1L) {
            editTime = -1L;
        }
        if (isNew && (getPersistable() != null)) {
            getLogger().debug("Created Id: {}", getPersistable().getId());
        }
        getLogger().debug("EDIT TOOK: {} SAVE TOOK: {} (edit:{}  save:{})", new Object[] { editTime, saveTime, formatTime(editTime), formatTime(saveTime) });

        // don't allow SUCCESS response if there are actionErrors, but give the other callbacks leeway in setting their own error message.
        if (CollectionUtils.isNotEmpty(getActionErrors()) && SUCCESS.equals(actionReturnStatus)) {
            return INPUT;
        }
        return actionReturnStatus;
    }

    private void determineAndUpdateStatus() {
        if (persistable instanceof HasStatus) {
            if (getStatus() == null) {
                setStatus(Status.ACTIVE);
            }

            ((HasStatus) getPersistable()).setStatus(getStatus());
        }
    }

    protected void indexPersistable() {
        if (persistable instanceof Indexable) {
            ((Indexable) persistable).setReadyToIndex(true);
            searchIndexService.index((Indexable) persistable);
        }
    }

    private void logAction(String action_) {
        String name_ = "";
        String email_ = "";
        Long id_ = -1L;
        try {
            if (getPersistable() instanceof HasName) {
                name_ = ((HasName) getPersistable()).getName();
            }
        } catch (Exception e) {/* eating, yum */
        }

        try {
            id_ = getPersistable().getId();
        } catch (Exception e) {
        }
        try {
            email_ = getAuthenticatedUser().getEmail().toUpperCase();
        } catch (Exception e) {
            getLogger().debug("something weird happend, authenticated user is null");
        }
        getLogger().info(String.format(msg, email_, action_, getPersistableClass().getSimpleName().toUpperCase(), id_, name_));
    }

    protected void preSaveCallback() {
    }

    protected void postSaveCallback(String actionReturnStatus) {
    }

    /**
     * override if needed
     * 
     * @param actionReturnStatus
     */
    protected void postSaveCleanup(String actionReturnStatus) {
    }

    @SkipValidation
    @Action(value = ADD, results = {
            @Result(name = SUCCESS, location = "edit.ftl")
    })
    @HttpsOnly
    public String add() throws TdarActionException {

        // FIXME:make this a preference...
        if ((getPersistable() instanceof HasStatus) && isEditor() && !isAdministrator()) {
            ((HasStatus) getPersistable()).setStatus(Status.DRAFT);
        }

        logAction("CREATING");
        return loadAddMetadata();
    }

    /**
     * The 'contributor' property only affects which menu items we show (for now). Let non-contributors perform
     * CRUD actions, but send them a reminder about the 'contributor' option in the prefs page
     * 
     * FIXME: this needs to be centralized, as it's not going to be caught in all of the location it exists in ... eg: editColumnMetadata ...
     */
    protected void checkForNonContributorCrud() {
        if (!isContributor()) {
            // FIXME: The html here could benefit from link to the prefs page. Devise a way to hint to the view-layer that certain messages can be decorated
            // and/or replaced.
            addActionMessage(getText("abstractPersistableController.change_profile"));
        }
    }

    protected boolean isAbleToCreateBillableItem() {
        return true;
    }

    public String loadAddMetadata() {
        return SUCCESS;
    }

    @SkipValidation
    @Action(value = EDIT, results = {
            @Result(name = SUCCESS, location = "edit.ftl")
    })
    @HttpsOnly
    public String edit() throws TdarActionException {
        if (PersistableUtils.isNullOrTransient(getPersistable() )) {
            throw new TdarActionException(StatusCode.NOT_FOUND,getText("abstractPersistableController.not_found"));
        }
        logAction("EDITING");
        return loadEditMetadata();
    }

    public abstract String loadEditMetadata() throws TdarActionException;

    public enum RequestType {
        EDIT,
        CREATE,
        DELETE,
        SAVE,
        NONE, VIEW;

        public String getLabel() {
            switch (this) {
                case CREATE:
                    return "CREATING";
                case DELETE:
                    return "DELETING";
                case EDIT:
                    return "EDITING";
                case SAVE:
                    return "SAVING";
                case VIEW:
                    return "VIEWING";
                default:
                    return "";
            }
        }
    }

    @Override
    public P getPersistable() {
        return persistable;
    }

    public void setPersistable(P persistable) {
        this.persistable = persistable;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * This method is invoked when the paramsPrepareParamsInterceptor stack is
     * applied. It allows us to fetch an entity from the database based on the
     * incoming resourceId param, and then re-apply params on that resource.
     * @throws TdarActionException 
     * 
     * @see <a href="http://blog.mattsch.com/2011/04/14/things-discovered-in-struts-2/">Things discovered in Struts 2</a>
     */
    @Override
    public void prepare() throws TdarActionException {
        RequestType type = RequestType.EDIT;
        if (getId() == null && (getCurrentUrl().contains("/add") || StringUtils.isBlank(getCurrentUrl()))) {
            getLogger().debug("setting persistable");
            setPersistable(createPersistable());
            type = RequestType.CREATE;
        }
        prepareAndLoad(this, type);
    }

    protected boolean isPersistableIdSet() {
        return PersistableUtils.isNotNullOrTransient(getPersistable());
    }

    @Override
    public abstract Class<P> getPersistableClass();

    public void setPersistableClass(Class<P> persistableClass) {
        this.persistableClass = persistableClass;
    }

    protected boolean isNullOrNew() {
        return !isPersistableIdSet();
    }

    /**
     * Returns true if we need to checkpoint and save the resource at various stages to handle many-to-one relationships
     * properly (due to cascading not working properly)
     * 
     * @return
     */
    public boolean shouldSaveResource() {
        return true;
    }

    public void setAsync(boolean async) {
        this.asyncSave = async;
    }

    public boolean isAsync() {
        return asyncSave;
    }

    /**
     * @param authorizedUsers
     *            the authorizedUsers to set
     */
    public void setAuthorizedUsers(List<AuthorizedUser> authorizedUsers) {
        this.authorizedUsers = authorizedUsers;
    }

    /**
     * @return the authorizedUsers
     */
    public List<AuthorizedUser> getAuthorizedUsers() {
        if (authorizedUsers == null) {
            authorizedUsers = new ArrayList<AuthorizedUser>();
        }
        return authorizedUsers;
    }

    public AuthorizedUser getBlankAuthorizedUser() {
        AuthorizedUser user = new AuthorizedUser();
        user.setUser(new TdarUser());
        return user;
    }

    public List<GeneralPermissions> getAvailablePermissions() {
        List<GeneralPermissions> permissions = new ArrayList<GeneralPermissions>();
        for (GeneralPermissions permission : GeneralPermissions.values()) {
            if ((permission.getContext() == null) || getPersistable().getClass().isAssignableFrom(permission.getContext())) {
                permissions.add(permission);
            }
        }
        return permissions;
    }

    /**
     * @return the startTime
     */
    public Long getStartTime() {
        return startTime;
    }

    /**
     * @return the startTime
     */
    public Long getCurrentTime() {
        return System.currentTimeMillis();
    }

    /**
     * @param startTime
     *            the startTime to set
     */
    public void setStartTime(Long startTime) {
        getLogger().trace("set start time: " + startTime);
        this.startTime = startTime;
    }

    @Override
    public void validate() {
        reportAnyJavascriptErrors();
        getLogger().debug("validating resource {} - {}", getPersistable(), getPersistableClass().getSimpleName());
        if (getPersistable() == null) {
            getLogger().warn("Null being validated.");
            addActionError(getText("abstractPersistableController.could_not_find"));
            return;
        }
        // String resourceTypeLabel = getPersistable().getResourceType().getLabel();
        if (getPersistable() instanceof Validatable) {
            try {
                boolean valid = ((Validatable) getPersistable()).isValidForController();
                if (!valid) {
                    addActionError(getText("abstractPersistableController.could_not_validate", getPersistable()));
                }
            } catch (Exception e) {
                addActionError(e.getMessage());
            }
        }
    }

    /*
     * This method returns the base URL for where a save should go, in 99% of the cases,
     * this goes to <b>view</b>
     */
    public String getSaveSuccessPath() {
        return saveSuccessPath;
    }

    /*
     * This method sets the base URL for where a save should go, in 99% of the cases,
     * this method should not be needed
     */
    public void setSaveSuccessPath(String successPath) {
        this.saveSuccessPath = successPath;
    }

    public String getSubmitAction() {
        return submitAction;
    }

    public void setSubmitAction(String submitAction) {
        this.submitAction = submitAction;
    }

    public ResourceSpaceUsageStatistic getUploadedResourceAccessStatistic() {
        return uploadedResourceAccessStatistic;
    }

    public void setUploadedResourceAccessStatistic(ResourceSpaceUsageStatistic uploadedResourceAccessStatistic) {
        this.uploadedResourceAccessStatistic = uploadedResourceAccessStatistic;
    }

    public ResourceSpaceUsageStatistic getTotalResourceAccessStatistic() {
        return totalResourceAccessStatistic;
    }

    public void setTotalResourceAccessStatistic(ResourceSpaceUsageStatistic totalResourceAccessStatistic) {
        this.totalResourceAccessStatistic = totalResourceAccessStatistic;
    }

    public Status getStatus() {
        if (status != null) {
            return status;
        }
        if (getPersistable() instanceof HasStatus) {
            return ((HasStatus) getPersistable()).getStatus();
        }
        return null;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<Status> getStatuses() {
        return new ArrayList<Status>(authorizationService.getAllowedSearchStatuses(getAuthenticatedUser()));
    }

    public AntiSpamHelper getH() {
        return h;
    }

    public void setH(AntiSpamHelper h) {
        this.h = h;
    }

    public List<String> getAuthorizedUsersFullNames() {
        return authorizedUsersFullNames;
    }

    public void setAuthorizedUsersFullNames(List<String> authorizedUsersFullNames) {
        this.authorizedUsersFullNames = authorizedUsersFullNames;
    }

    public String getSaveSuccessSuffix() {
        return saveSuccessSuffix;
    }

    public void setSaveSuccessSuffix(String saveSuccessSuffix) {
        this.saveSuccessSuffix = saveSuccessSuffix;
    }
    
    // ideally factor out, but used by the view layer to determine whether to show the edit button or not
    public boolean isEditable() {
        return authorize();
    }
    
    @Override
    public boolean authorize() {
        return true;
    }

    @Override
    public InternalTdarRights getAdminRights() {
        return InternalTdarRights.EDIT_ANYTHING;
    }

}
