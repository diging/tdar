package org.tdar.core.service.external;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.AuthNotice;
import org.tdar.core.bean.HasStatus;
import org.tdar.core.bean.HasSubmitter;
import org.tdar.core.bean.Indexable;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.Viewable;
import org.tdar.core.bean.billing.Invoice;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.entity.AuthenticationToken;
import org.tdar.core.bean.entity.Institution;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.entity.permissions.GeneralPermissions;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.InformationResourceFile;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.bean.util.Email;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.entity.AuthorizedUserDao;
import org.tdar.core.dao.entity.InstitutionDao;
import org.tdar.core.dao.entity.PersonDao;
import org.tdar.core.dao.external.auth.AuthenticationProvider;
import org.tdar.core.dao.external.auth.AuthenticationResult;
import org.tdar.core.dao.external.auth.AuthenticationResult.AuthenticationResultType;
import org.tdar.core.dao.external.auth.InternalTdarRights;
import org.tdar.core.dao.external.auth.TdarGroup;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.struts.action.search.ReservedSearchParameters;
import org.tdar.struts.data.UserRegistration;
import org.tdar.utils.MessageHelper;
import org.tdar.web.SessionData;

/*
 * This service is designed to hide the complexity of users and permissions from the rest of tDAR.  It handles a number different functions including:
 * (a) hiding access to different external authentication systems
 * (b) caching permissions and group memberships
 * (c) getting file access and resource access permissions
 */
@Service
public class AuthenticationAndAuthorizationService implements Accessible {

    public enum AuthenticationStatus {
        AUTHENTICATED,
        ERROR,
        NEW;
    }

    public static final String USERNAME_REGEX = "^[a-zA-Z0-9+@\\.\\-_]";
    public static final String USERNAME_VALID_REGEX = USERNAME_REGEX + "{5,255}$";
    public static final String EMAIL_VALID_REGEX = "^[a-zA-Z0-9+@\\.\\-_]{4,255}$";
    private static final String EMAIL_WELCOME_TEMPLATE = "email-welcome.ftl";

    /*
     * we use a weak hashMap of the group permissions to prevent tDAR from constantly hammering the auth system with the group permissions. The hashMap will
     * track these permissions for short periods of time. Logging out and logging in should reset this
     */
    private final WeakHashMap<Person, TdarGroup> groupMembershipCache = new WeakHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TdarConfiguration tdarConfiguration = TdarConfiguration.getInstance();

    @Autowired
    private AuthorizedUserDao authorizedUserDao;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private EmailService emailService;

    @Autowired
    private InstitutionDao institutionDao;

    private AuthenticationProvider provider;

    @Override
    public List<Resource> findEditableResources(Person person, boolean isAdmin, List<ResourceType> resourceTypes) {
        return authorizedUserDao.findEditableResources(person, resourceTypes, isAdmin);
    }

    /**
     * TdarGroups are represented in the external auth systems, but enable global permissions in tDAR; Admins, Billing Administrators, etc.
     */
    public boolean isMember(TdarUser person, TdarGroup group) {
        return checkAndUpdateCache(person, group);
    }

    public boolean isAdministrator(TdarUser person) {
        return checkAndUpdateCache(person, TdarGroup.TDAR_ADMIN);
    }

    public boolean isBillingManager(TdarUser person) {
        return checkAndUpdateCache(person, TdarGroup.TDAR_BILLING_MANAGER);
    }

    public boolean isEditor(TdarUser person) {
        return checkAndUpdateCache(person, TdarGroup.TDAR_EDITOR);
    }

    /**
     * Group Permissions tend to be hierarchical, hence, you may want to know if a user is a member of any of the nested hierarchy. Eg. EDITOR is a subset of
     * ADMIN
     */
    public boolean isMemberOfAny(TdarUser person, TdarGroup... groups) {
        if ((person == null) || (groups == null)) {
            return false;
        }
        for (TdarGroup group : groups) {
            if (isMember(person, group)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Not currently used; but would allow for the updating of a username in the external auth system by deleting the user and adding them again. In Crowd 2.8
     * this is builtin function; but it might not be for LDAP.
     */
    public void updateUsername(TdarUser person, String newUsername, String password) {
        if (personDao.findByUsername(newUsername.toLowerCase()) != null) {
            throw new TdarRecoverableRuntimeException("auth.username_exists", Arrays.asList(newUsername));
        }

        String[] groupNames = getProvider().findGroupMemberships(person);
        List<TdarGroup> groups = new ArrayList<>();
        for (String groupName : groupNames) {
            groups.add(TdarGroup.valueOf(groupName));
        }
        getProvider().deleteUser(person);
        person.setUsername(newUsername.toLowerCase());
        getProvider().addUser(person, password, groups.toArray(new TdarGroup[groups.size()]));
    }

    /*
     * Checks the current cache for the @link Person and their @linkTdarGroup permissions, if it exists, it returns whether the @link Person is a member of the
     * group. If not, it checks the external authentication and authorization service (CROWD/LDAP) to see what @link TdarGroup Memberships are set for that
     * 
     * @link Person
     * and then updates the cache (HashMap)
     */
    private synchronized boolean checkAndUpdateCache(TdarUser person, TdarGroup requestedPermissionsGroup) {
        TdarGroup greatestPermissionGroup = groupMembershipCache.get(person);
        if (greatestPermissionGroup == null) {
            greatestPermissionGroup = findGroupWithGreatestPermissions(person);
            groupMembershipCache.put(person, greatestPermissionGroup);
        }
        return greatestPermissionGroup.hasGreaterPermissions(requestedPermissionsGroup);
    }

    /*
     * Returns a list of the people in the @link groupMembershipCache which is useful in tracking what's going on with tDAR at a given moment. This would be
     * helpful for
     * a shutdown hook, as well as, for knowing when it's safe to deploy.
     */
    public synchronized List<Person> getCurrentlyActiveUsers() {
        return new ArrayList<>(groupMembershipCache.keySet());
    }

    /*
     * @return all of the resource statuses that a user is allowed to view in a search. Different users have different search permissions in different contexts.
     * A user
     * should be able to see their own DRAFTs, but never DELETED statuss unless they're an admin, for example
     */
    public Set<Status> getAllowedSearchStatuses(TdarUser person) {
        // assumption: ACTIVE always allowed.
        Set<Status> allowed = new HashSet<>(Arrays.asList(Status.ACTIVE));
        if (person == null) {
            return allowed;
        }

        allowed.add(Status.DRAFT);
        if (can(InternalTdarRights.SEARCH_FOR_DELETED_RECORDS, person)) {
            allowed.add(Status.DELETED);
        }

        if (can(InternalTdarRights.SEARCH_FOR_FLAGGED_RECORDS, person)) {
            allowed.add(Status.FLAGGED);
            if (TdarConfiguration.getInstance().isPayPerIngestEnabled()) {
                allowed.add(Status.FLAGGED_ACCOUNT_BALANCE);
            }
        }
        if (can(InternalTdarRights.SEARCH_FOR_DUPLICATE_RECORDS, person)) {
            allowed.add(Status.DUPLICATE);
        }
        return allowed;
    }

    /*
     * The @link AdvancedSearchController's ReservedSearchParameters is a proxy object for handling advanced boolean searches. We initialize it with the search
     * parameters
     * that are AND-ed with the user's search to ensure appropriate search results are returned (such as a Resource's @link Status).
     */
    public void initializeReservedSearchParameters(ReservedSearchParameters reservedSearchParameters, TdarUser user) {
        reservedSearchParameters.setAuthenticatedUser(user);
        reservedSearchParameters.setTdarGroup(findGroupWithGreatestPermissions(user));
        Set<Status> allowedSearchStatuses = getAllowedSearchStatuses(user);
        List<Status> statuses = reservedSearchParameters.getStatuses();

        if (CollectionUtils.isEmpty(statuses)) {
            statuses = new ArrayList<>(Arrays.asList(Status.ACTIVE, Status.DRAFT));
        }

        statuses.retainAll(allowedSearchStatuses);
        reservedSearchParameters.setStatuses(statuses);
        if (statuses.isEmpty()) {
            throw (new TdarRecoverableRuntimeException("auth.search.status.denied"));
        }

    }

    /**
     * Depending on how a person was added to CROWD or LDAP, they may have redundant group permissions (and probably should). Thus, given a set of permissions,
     * we find the one with the greatest rights
     */
    public TdarGroup findGroupWithGreatestPermissions(TdarUser person) {
        if (person == null) {
            return TdarGroup.UNAUTHORIZED;
        }
        String login = person.getUsername();
        if (StringUtils.isBlank(login)) {
            return TdarGroup.UNAUTHORIZED;
        }
        TdarGroup greatestPermissionGroup = TdarGroup.UNAUTHORIZED;
        String[] groups = getProvider().findGroupMemberships(person);
        logger.trace("Found {} memberships for {}", Arrays.asList(groups), login);
        for (String groupString : groups) {
            TdarGroup group = TdarGroup.fromString(groupString);
            if (group.hasGreaterPermissions(greatestPermissionGroup)) {
                greatestPermissionGroup = group;
            }
        }
        return greatestPermissionGroup;
    }

    /**
     * Provides access to the configured @link AuthenticationProvider -- CROWD or LDAP, for example. Consider making private.
     */
    public AuthenticationProvider getAuthenticationProvider() {
        return getProvider();
    }

    /**
     * allow for the clearing of the permissions cache. This is used both by "tests" and by the @link ScheduledProcessService to rest the
     * cache externally on a scheduled basis.
     */
    public synchronized void clearPermissionsCache() {
        logger.debug("Clearing group membership cache of all entries: {}", groupMembershipCache);
        groupMembershipCache.clear();
    }

    /*
     * Removes a specific @link Person from the Permissions cache (e.g. when they log out).
     */
    public synchronized void clearPermissionsCache(Person person) {
        logger.debug("Clearing group membership cache of entry for : {}", person);
        groupMembershipCache.remove(person);
    }

    /**
     * Checks whether a @link Person has the rights to view a given resource. First, checking whether the person's @link TdarGroup permissions grant them
     * additional rights, for example if ADMIN; or if their @link ResourceCollection permissions include GeneralPermission.VIEW_ALL or greater
     * <ol>
     * <li>the person and resource parameters are not null
     * <li>resource.submitter is the same as the person parameter
     * <li>the person has view privileges
     * </ol>
     * 
     * @param person
     * @param resource
     * @return true if person has read permissions on resource according to the above policies, false otherwise.
     */
    public boolean canViewResource(TdarUser person, Resource resource) {
        // is the request valid
        if (person == null) {
            return false;
        }
        if (resource == null) {
            return false;
        }

        // does the user have special privileges to edit resources in any status?
        if (can(InternalTdarRights.VIEW_ANYTHING, person)) {
            return true;
        }

        // finally, check if user has been granted permission
        // FIXME: technically the dao layer is doing some stuff that we should be, but I don't want to mess w/ it right now.
        return authorizedUserDao.isAllowedTo(person, resource, GeneralPermissions.VIEW_ALL);
    }

    /**
     * Checks whether a @link Person has the rights to edit a given resource. First, checking whether the person's @link TdarGroup permissions grant them
     * additional rights, for example if ADMIN; or if their @link ResourceCollection permissions include GeneralPermission.MODIFY_METADATA or greater
     * <ol>
     * <li>the person and resource parameters are not null
     * <li>resource.submitter is the same as the person parameter
     * <li>the person has edit privileges
     * </ol>
     * 
     * @param person
     * @param resource
     * @return true if person has write permissions on resource according to the above policies, false otherwise.
     */
    public boolean canEditResource(TdarUser person, Resource resource, GeneralPermissions basePermission) {
        // is the request valid
        if (person == null) {
            return false;
        }
        if (resource == null) {
            return false;
        }

        // does the user have special privileges to edit resources in any status?
        if (can(InternalTdarRights.EDIT_ANY_RESOURCE, person)) {
            logger.trace("checking if person can edit any resource");
            return true;
        }

        // finally, check if user has been granted permission
        // FIXME: technically the dao layer is doing some stuff that we should be, but I don't want to mess w/ it right now.
        return authorizedUserDao.isAllowedTo(person, resource, basePermission);
    }

    /**
     * Checks whether a @link Person has the rights to edit a @link ResourceCollection. First, checking whether the person's @link TdarGroup permissions grant
     * them
     * additional rights, for example if ADMIN; or if their @link ResourceCollection permissions include GeneralPermission.ADMINISTER_GROUP or greater
     * 
     * @param authenticatedUser
     * @param persistable
     * @return
     */
    @Transactional(readOnly = false)
    public boolean canEditCollection(TdarUser authenticatedUser, ResourceCollection persistable) {
        if (authenticatedUser == null) {
            logger.debug("person is null");
            return false;
        }

        if (can(InternalTdarRights.EDIT_RESOURCE_COLLECTIONS, authenticatedUser) || authenticatedUser.equals(persistable.getOwner())) {
            return true;
        }

        return authorizedUserDao.isAllowedTo(authenticatedUser, persistable, GeneralPermissions.ADMINISTER_GROUP);
    }

    /**
     * This method checks whether a person's group membership allows them to perform an associated right.
     * 
     * The @link InternalTdarRights enum associates global permissions with a @link TdarGroup or set of Groups. These global permissions allow
     * us to simplify permissions management by associating explicit rights with actions in the code, and managing permissions mappings in the enum(s).
     */
    public boolean can(InternalTdarRights rights, TdarUser person) {
        if ((person == null) || (rights == null)) {
            return false;
        }
        if (isMemberOfAny(person, rights.getPermittedGroups())) {
            return true;
        }
        return false;
    }

    /**
     * This method checks whether a person's group membership denies them to perform an associated right @see #can
     */
    public boolean cannot(InternalTdarRights rights, TdarUser person) {
        return !can(rights, person);
    }

    /**
     * Evaluates an <E> (likely resource) in a list and removes it if the @link Person does not have the specified @link InternalTdarRights to perform the
     * action.
     */
    public <E> void removeIfNotAllowed(Collection<E> list, E item, InternalTdarRights permission, TdarUser person) {
        // NOTE: this will FAIL if you use Arrays.asList because that collection is immutable
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        if (cannot(permission, person)) {
            list.remove(item);
        }
    }

    /**
     * Checks whether the @link Person can perform the specified edit action.
     * 
     * Part of the contract of @link AbstractPersistableController is to checks whether a @link Person (user) can View, Edit, Delete a @link Persistable prior
     * to rendering the page.
     * 
     * (non-Javadoc)
     * 
     * @see org.tdar.core.service.external.Accessible#canEdit(org.tdar.core.bean.entity.Person, org.tdar.core.bean.Persistable)
     */
    @Override
    public boolean canEdit(TdarUser authenticatedUser, Persistable item) {
        if (item instanceof Resource) {
            return canEditResource(authenticatedUser, (Resource) item, GeneralPermissions.MODIFY_METADATA);
        } else if (item instanceof ResourceCollection) {
            return canEditCollection(authenticatedUser, (ResourceCollection) item);
        } else {
            return can(InternalTdarRights.EDIT_ANYTHING, authenticatedUser);
        }
    }

    /**
     * Checks whether the @link Person can perform the specified view action.
     * 
     * Part of the contract of @link AbstractPersistableController is to checks whether a @link Person (user) can View, Edit, Delete a @link Persistable prior
     * to rendering the page.
     * 
     * (non-Javadoc)
     * 
     * @see org.tdar.core.service.external.Accessible#canView(org.tdar.core.bean.entity.Person, org.tdar.core.bean.Persistable)
     */
    @Override
    public boolean canView(TdarUser authenticatedUser, Persistable item) {
        if (item instanceof Resource) {
            return canViewResource(authenticatedUser, (Resource) item);
        } else if (item instanceof ResourceCollection) {
            return canViewCollection((ResourceCollection) item, authenticatedUser);
        } else {
            return can(InternalTdarRights.VIEW_ANYTHING, authenticatedUser);
        }
    }

    /**
     * Returns true if the person is privileged, the resource is not restricted in access, or the person is granted @link GeneralPermissions.VIEW_ALL
     * 
     * @param person
     * @return
     */
    public boolean canViewConfidentialInformation(TdarUser person, Resource resource) {
        if (resource instanceof InformationResource) {
            return ((InformationResource) resource).isPublicallyAccessible()
                    || canDo(person, resource, InternalTdarRights.VIEW_AND_DOWNLOAD_CONFIDENTIAL_INFO, GeneralPermissions.VIEW_ALL);
        }
        return true;
    }

    /**
     * Confirms that a @link Person has the rights to edit a @link InformationResource and upload files (@link GeneralPermissions.MODIFY_RECORD (as opposed to
     * 
     * @link GeneralPermissions.MODIFY_METADATA ))
     */
    public boolean canUploadFiles(TdarUser person, Resource resource) {
        return canDo(person, resource, InternalTdarRights.EDIT_ANY_RESOURCE, GeneralPermissions.MODIFY_RECORD);
    }

    /**
     * Pairs the @link InternalTdarRights permission with a @link ResourceCollection's @link GeneralPermission to check whether a user
     * can perform an action. Many of the other checks within this class are reflected as canDo checks or could be refactored as such
     * (a) checks if inputs are NULL
     * (b) checks if user is privileged (admin, etc.)
     * (c) checks if user is allowed to perform action based on @link AuthorizedUser / @link ResourceCollection permissions
     * (d) check's iuf user was submitter
     */
    public boolean canDo(TdarUser person, HasSubmitter resource, InternalTdarRights equivalentAdminRight, GeneralPermissions permission) {
        // This function used to pre-test on the resource, but it doesn't have to and is now more granular
        if (resource == null) {
            return false;
        }

        if (person == null) {
            logger.trace("person is null");
            return false;
        }

        if (Objects.equals(resource.getSubmitter(), person)) {
            logger.trace("person was submitter");
            return true;
        }

        if (can(equivalentAdminRight, person)) {
            logger.trace("person is admin");
            return true;
        }

        if (authorizedUserDao.isAllowedTo(person, resource, permission)) {
            logger.trace("person is an authorized user");
            return true;
        }

        // ab added:12/11/12
        if (Persistable.Base.isTransient(resource) && (resource.getSubmitter() == null)) {
            logger.trace("resource is transient");
            return true;
        }

        logger.trace("returning false... access denied");
        return false;
    }

    /*
     * Checks whether a @link Person has rights to download a given @link InformationResourceFileVersion
     */
    public boolean canDownload(InformationResourceFileVersion irFileVersion, TdarUser person) {
        if (irFileVersion == null) {
            return false;
        }
        return canDownload(irFileVersion.getInformationResourceFile(), person);
    }

    /*
     * Checks whether a @link Person has rights to download a given @link InformationResourceFile
     */
    public boolean canDownload(InformationResourceFile irFile, TdarUser person) {
        if (irFile == null) {
            return false;
        }
        if (irFile.isDeleted() && Persistable.Base.isNullOrTransient(person)) {
            return false;
        }
        if (!irFile.isPublic() && !canViewConfidentialInformation(person, irFile.getInformationResource())) {
            return false;
        }
        return true;
    }

    /*
     * Checks whether a person has the rights to view a collection based on their @link GeneralPermission on the @link ResourceCollection; filters by Shared
     * Visible collections
     */
    public boolean canViewCollection(ResourceCollection collection, Person person) {
        if (collection.isShared() && collection.isVisible()) {
            return true;
        }
        return authorizedUserDao.isAllowedTo(person, collection, GeneralPermissions.VIEW_ALL);
    }

    /*
     * For the view-layer, we add a hint via the @link Viewable interface about whether a @link Resource @link Person or other @link Indexable is viewable or
     * not by
     * the @link Person performing the action.
     * 
     * (a) if the item has a @link Status, and it's Active, ok
     * (b) if not active, then check whether the @link Person can view that status
     * (c) if it's a collection, make sure it's public and shared
     * (d) otherwise, it's probably not
     */
    public void applyTransientViewableFlag(Indexable p, TdarUser authenticatedUser) {
        /*
         * If the Persistable supports the "Viewable" interface, then inject the
         * permissions into the transient property
         */

        // FIXME: it'd be nice if this took an array and could handle multiple lookups at once
        logger.trace("applying transient viewable flag to : {}", p);
        if (p instanceof Viewable) {
            logger.trace("item is a 'viewable': {}", p);
            boolean viewable = false; // by default -- not allowed to view
            Viewable item = (Viewable) p;
            if (item instanceof HasStatus) { // if we have status, then work off that
                logger.trace("item 'has status': {}", p);
                HasStatus status = ((HasStatus) item);
                if (!status.isActive()) { // if not active, check other permissions
                    logger.trace("item 'is not active': {}", p);
                    if (can(InternalTdarRights.VIEW_ANYTHING, authenticatedUser)) {
                        logger.trace("\tuser is special': {}", p);
                        viewable = true;
                    }
                } else {
                    viewable = true;
                }
            }
            if (item instanceof ResourceCollection) {
                logger.trace("item is resource collection: {}", p);
                if (((ResourceCollection) item).isShared() && !((ResourceCollection) item).isInternal()) {
                    viewable = true;
                }
            }

            if (item instanceof InformationResource) {
                logger.trace("item is information resource (download): {}", p);
                setTransientViewableStatus((InformationResource) item, authenticatedUser);
            }

            if (!viewable && canView(authenticatedUser, p)) {
                logger.trace("user can edit: {}", p);
                viewable = true;
            }
            item.setViewable(viewable);
        }
    }

    /*
     * Normalize the username being passed in; we may need to do more than lowercase it, such as run it through a REGEXP.
     */
    public String normalizeUsername(String userName) {
        // for now, we just lowercase it.
        String normalizedUsername = userName.toLowerCase();
        return normalizedUsername;
    }

    /*
     * sets the @link Viewable status on @link InformationResourceFile and @link InformationResourceFileVersion to simplify lookups on the view layer
     * (Freemarker)
     */
    private void setTransientViewableStatus(InformationResource ir, TdarUser p) {
        Boolean viewable = null;
        for (InformationResourceFile irf : ir.getInformationResourceFiles()) {
            // if (viewable == null) {
            viewable = canDownload(irf, p);
            // }

            irf.setViewable(viewable);
            for (InformationResourceFileVersion irfv : irf.getInformationResourceFileVersions()) {
                irfv.setViewable(viewable);
            }
        }
    }

    /*
     * Checks that a username to be added is valid
     */
    public boolean isValidUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }

        return username.matches(USERNAME_VALID_REGEX);
    }

    /*
     * This is separate to ensure that legacy usernames are supported by the system
     */
    public boolean isPossibleValidUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }

        return username.matches(USERNAME_REGEX + "{2,255}$");
    }

    /*
     * Checks that the email is a valid email address
     */
    public boolean isValidEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }

        return email.matches(EMAIL_VALID_REGEX);
    }

    /*
     * Authenticate a web user passing in the Request, Response, username and password. Checks that (a) the username is valid (b) that the user can authenticate
     * (c) that user exists and is valid within tDAR (active);
     * 
     * @param loginUsername - the username of the user to authenticate
     * 
     * @param loginPassword - the user's password
     * 
     * @param request - the @link HttpServletRequest to read cookies from or other information
     * 
     * @param response - the @link HttpServletResponse to set the error code on
     * 
     * @param sessionData - the @SessionData object to intialize with the user's session / cookie information if logged in properly.
     */
    @Transactional
    public AuthenticationStatus authenticatePerson(String loginUsername, String loginPassword, HttpServletRequest request, HttpServletResponse response,
            SessionData sessionData) {
        if (!isPossibleValidUsername(loginUsername)) {
            throw new TdarRecoverableRuntimeException("auth.username.invalid");
        }

        AuthenticationResult result = getAuthenticationProvider().authenticate(request, response, loginUsername, loginPassword);
        if (!result.getType().isValid()) {
            logger.debug("Couldn't authenticate {} - (reason: {})", loginUsername, result);
            throw new TdarRecoverableRuntimeException("auth.couldnt_authenticate", Arrays.asList(result.getType().getMessage()));
        }

        TdarUser person = personDao.findByUsername(loginUsername);
        if (person == null) {
            // FIXME: person exists in Crowd but not in tDAR..
            logger.debug("Person successfully authenticated by authentication service but not present in site database: {}", loginUsername);
            person = new TdarUser();
            person.setUsername(loginUsername);
            // how to pass along authentication information..?
            // username was in Crowd but not in tDAR? Redirect them to the account creation page
            return AuthenticationStatus.NEW;
        }

        if (!person.isActive()) {
            throw new TdarRecoverableRuntimeException("auth.cannot.deleted");
        }

        // enable us to force group cache to be cleared
        clearPermissionsCache(person);

        if (!isMember(person, TdarGroup.TDAR_USERS)) {
            throw new TdarRecoverableRuntimeException("auth.cannot.notmember");
        }

        logger.debug(String.format("%s (%s) logged in from %s using: %s", loginUsername, person.getEmail(), request.getRemoteAddr(),
                request.getHeader("User-Agent")));
        createAuthenticationToken(person, sessionData);
        personDao.registerLogin(person);
        return AuthenticationStatus.AUTHENTICATED;
    }

    /*
     * creates an authentication token (last step in authenticating); that tDAR can use for the entire session
     */
    public void createAuthenticationToken(TdarUser person, SessionData session) {
        AuthenticationToken token = AuthenticationToken.create(person);
        personDao.save(token);
        session.setAuthenticationToken(token);
    }

    /*
     * checks that the specified @link Person can assign an @link Invoice to an @link Account; 1/2 of the check whether the person has the rights to do anything
     * with the
     * Invoive itself
     */
    public boolean canAssignInvoice(Invoice invoice, TdarUser authenticatedUser) {
        if (authenticatedUser.equals(invoice.getTransactedBy()) || authenticatedUser.equals(invoice.getOwner())) {
            return true;
        }
        if (isMember(authenticatedUser, TdarGroup.TDAR_BILLING_MANAGER)) {
            return true;
        }
        return false;
    }

    /*
     * exposes the groups the user is a member of from the external Provider; exposes groups as a String, as the external provider may include other permissions
     * beyond just tDAR groups
     */
    public Collection<String> getGroupMembership(TdarUser person) {
        return Arrays.asList(getAuthenticationProvider().findGroupMemberships(person));
    }

    /**
     * Checks whether a user has pending policy agreements they must accept
     * 
     * @param user
     * @return true if user has pending requirements, otherwise false
     */
    public boolean userHasPendingRequirements(TdarUser user) {
        return !getUserRequirements(user).isEmpty();
    }

    /**
     * @param user
     * @return List containing pending requirements for the specified user
     */
    public List<AuthNotice> getUserRequirements(TdarUser user) {
        List<AuthNotice> notifications = new ArrayList<>();
        // not public static final because don't work in testing
        Integer tosLatestVersion = tdarConfiguration.getTosLatestVersion();
        Integer contributorAgreementLatestVersion = tdarConfiguration.getContributorAgreementLatestVersion();
        if (user.getTosVersion() < tosLatestVersion) {
            notifications.add(AuthNotice.TOS_AGREEMENT);
        }

        if (user.getContributor() && (user.getContributorAgreementVersion() < contributorAgreementLatestVersion)) {
            notifications.add(AuthNotice.CONTRIBUTOR_AGREEMENT);
        }
        return notifications;
    }

    /**
     * Update Person record to indicate that the specified user has satisfied a required task
     * 
     * @param user
     * @param req
     */
    @Transactional(readOnly = false)
    public void satisfyPrerequisite(TdarUser user, AuthNotice req) {
        // not public static final because don't work in testing
        Integer tosLatestVersion = tdarConfiguration.getTosLatestVersion();
        Integer contributorAgreementLatestVersion = tdarConfiguration.getContributorAgreementLatestVersion();
        switch (req) {
            case CONTRIBUTOR_AGREEMENT:
                user.setContributorAgreementVersion(contributorAgreementLatestVersion);
                break;
            case TOS_AGREEMENT:
                user.setTosVersion(tosLatestVersion);
                break;
            case GUEST_ACCOUNT:
                break;
        }
    }

    /*
     * @see #satisfyUserPrerequisites(SessionData sessionData, Collection<AuthNotice> notices)
     */
    @Transactional(readOnly = false)
    void satisfyPrerequisites(TdarUser user, Collection<AuthNotice> notices) {
        for (AuthNotice notice : notices) {
            satisfyPrerequisite(user, notice);
        }
    }

    /**
     * Indicate that the user associated with the specified session has acknowledged/accepted the specified notices
     * (e.g. user agreements)
     * 
     * @param sessionData
     * @param notices
     */
    @Transactional(readOnly = false)
    public void satisfyUserPrerequisites(SessionData sessionData, Collection<AuthNotice> notices) {
        // we actually need to update two person instances: the persisted user record, and the detached user
        // associated with the session. We hide this detail from the caller.
        TdarUser detachedUser = sessionData.getPerson();
        TdarUser persistedUser = personDao.find(TdarUser.class, detachedUser.getId());
        satisfyPrerequisites(detachedUser, notices);
        satisfyPrerequisites(persistedUser, notices);
        personDao.saveOrUpdate(persistedUser);
        logger.trace(" detachedUser:{}, tos:{}, ca:{}", detachedUser, detachedUser.getTosVersion(), detachedUser.getContributorAgreementVersion());
        logger.trace(" persistedUser:{}, tos:{}, ca:{}", persistedUser, persistedUser.getTosVersion(), persistedUser.getContributorAgreementVersion());
    }

    @Transactional(readOnly = false)
    public AuthenticationResult addAndAuthenticateUser(UserRegistration registrationInfo, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse, SessionData sessionData, boolean contributor) {
        return addAndAuthenticateUser(registrationInfo.getPerson(), registrationInfo.getPassword(), registrationInfo.getInstitutionName(),
                servletRequest, servletResponse, sessionData, true);

    }

    /**
     * Authenticate a user, and optionally create the user account prior to authentication.
     * 
     * FIXME: handling of transient reconciliation needs refactoring
     * 
     * @param person
     * @param password
     * @param institutionName
     * @param request
     * @param response
     * @param sessionData
     * @param contributor
     * @return
     */
    @Transactional(readOnly = false)
    public synchronized AuthenticationResult addAndAuthenticateUser(TdarUser person_, String password, String institutionName, HttpServletRequest request,
            HttpServletResponse response, SessionData sessionData, boolean contributor) {
        // FIXME: pointless alias?
        TdarUser person = person_;
        TdarUser findByUsername = personDao.findByUsername(person.getUsername());
        TdarUser findByEmail = personDao.findUserByEmail(person.getEmail());
        // short circut the login process -- if there username and password are registered and valid -- just move on.
        if (Persistable.Base.isNotNullOrTransient(findByUsername)) {
            try {
                AuthenticationStatus status = authenticatePerson(findByUsername.getUsername(), password, request, response, sessionData);
                if (status == AuthenticationStatus.AUTHENTICATED) {
                    return new AuthenticationResult(AuthenticationResultType.VALID);
                }
            } catch (Exception e) {
                logger.warn("could not authenticate", e);
            }
        }
        // FIXME: refactor overwritten reconciliations
        person = reconcilePersonWithTransient(person, findByUsername, MessageHelper.getMessage("userAccountController.error_username_already_registered"));
        // if there's a user with this email, that takes precedence
        person = reconcilePersonWithTransient(person, findByEmail, MessageHelper.getMessage("userAccountController.error_duplicate_email"));
        if (Persistable.Base.isTransient(person)) {
            Person findByEmail2 = personDao.findByEmail(person.getEmail());
            if (Persistable.Base.isNotNullOrTransient(findByEmail2)) {
                person = personDao.findConvertPersonToUser(findByEmail2, person.getUsername());
                logger.info("person: {}", person);
            }
        }
        Institution institution = institutionDao.findByName(institutionName);
        if ((institution == null) && !StringUtils.isBlank(institutionName)) {
            institution = new Institution();
            institution.setName(institutionName);
            personDao.save(institution);
        }
        person.setInstitution(institution);

        AuthenticationResult addResult = getAuthenticationProvider().addUser(person, password);
        if (!addResult.getType().isValid()) {
            throw new TdarRecoverableRuntimeException(addResult.getType().getMessage());
        }
        // after the person has been saved, create a contributor request for
        // them as needed.
        if (contributor) {
            person.setContributor(true);
            satisfyPrerequisite(person, AuthNotice.CONTRIBUTOR_AGREEMENT);
        } else {
            person.setContributor(false);
        }
        satisfyPrerequisite(person, AuthNotice.TOS_AGREEMENT);

        // add user to Crowd
        person.setStatus(Status.ACTIVE);
        personDao.saveOrUpdate(person);

        logger.debug("Trying to add user to auth service...");

        sendWelcomeEmail(person);
        logger.info("Added user to auth service successfully.");
        // } else {
        // // we assume that the add operation failed because user was already in crowd. Common scenario for dev/alpha, but not prod.
        // logger.error("user {} already existed in auth service.  Not unusual unless it happens in prod context ", person);
        // }
        // log person in.
        AuthenticationResult result = getAuthenticationProvider().authenticate(request, response, person.getUsername(), password);
        if (result.getType().isValid()) {
            logger.debug("Authenticated successfully with auth service, registering login and creating authentication token");
            personDao.registerLogin(person);
            createAuthenticationToken(person, sessionData);
        }
        result.setPerson(person);
        return result;

    }

    private void sendWelcomeEmail(Person person) {
        Map<String, Object> result = new HashMap<>();
        final TdarConfiguration config = TdarConfiguration.getInstance();
        result.put("user", person);
        result.put("config", config);
        try {
            String subject = MessageHelper.getMessage("userAccountController.welcome", Arrays.asList(TdarConfiguration.getInstance().getSiteAcronym()));
            Email email = new Email();
            email.setSubject(subject);
            email.addToAddress(person.getEmail());
            emailService.queueWithFreemarkerTemplate(EMAIL_WELCOME_TEMPLATE, result, email);
        } catch (Exception e) {
            // we don't want to ruin the new user's experience with a nasty error message...
            logger.error("Suppressed error that occurred when trying to send welcome email", e);
        }
    }

    private TdarUser reconcilePersonWithTransient(TdarUser incoming, TdarUser resultOfLookup, String error) {
        if ((resultOfLookup != null) && Persistable.Base.isNullOrTransient(incoming)) {
            switch (resultOfLookup.getStatus()) {
                default:
                    logger.debug("user is not valid");
                    throw new TdarRecoverableRuntimeException(error);
                case DRAFT:
                case DUPLICATE:
                    incoming.setStatus(Status.ACTIVE);
                    resultOfLookup.setStatus(Status.ACTIVE);
            }

            incoming.setId(resultOfLookup.getId());
            logger.debug("existing user: {} ", incoming);
            return personDao.merge(incoming);
        }
        return incoming;
    }

    @Autowired
    @Qualifier("AuthenticationProvider")
    public void setProvider(AuthenticationProvider provider) {
        this.provider = provider;
        if (provider != null) {
            logger.debug("Authentication Provider: {}", provider.getClass().getSimpleName());
        } else {
            logger.debug("Authentication Provider: NOT CONFIGURED");
        }
    }

    public AuthenticationProvider getProvider() {
        return provider;
    }

    @Transactional(readOnly = true)
    public void logout(SessionData sessionData, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        AuthenticationToken token = sessionData.getAuthenticationToken();
        token.setSessionEnd(new Date());
        personDao.update(token);
        sessionData.clearAuthenticationToken();
        getAuthenticationProvider().logout(servletRequest, servletResponse);
    }

}
