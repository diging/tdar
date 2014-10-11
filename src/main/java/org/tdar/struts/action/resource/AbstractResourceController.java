package org.tdar.struts.action.resource;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.tdar.URLConstants;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.Persistable.Sequence;
import org.tdar.core.bean.Sequenceable;
import org.tdar.core.bean.billing.Account;
import org.tdar.core.bean.citation.RelatedComparativeCollection;
import org.tdar.core.bean.citation.SourceCollection;
import org.tdar.core.bean.collection.ResourceCollection;
import org.tdar.core.bean.collection.ResourceCollection.CollectionType;
import org.tdar.core.bean.coverage.CoverageDate;
import org.tdar.core.bean.coverage.CoverageType;
import org.tdar.core.bean.coverage.LatitudeLongitudeBox;
import org.tdar.core.bean.entity.AuthorizedUser;
import org.tdar.core.bean.entity.Creator.CreatorType;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.ResourceCreator;
import org.tdar.core.bean.entity.ResourceCreatorRole;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.entity.permissions.GeneralPermissions;
import org.tdar.core.bean.keyword.CultureKeyword;
import org.tdar.core.bean.keyword.GeographicKeyword;
import org.tdar.core.bean.keyword.InvestigationType;
import org.tdar.core.bean.keyword.MaterialKeyword;
import org.tdar.core.bean.keyword.OtherKeyword;
import org.tdar.core.bean.keyword.SiteNameKeyword;
import org.tdar.core.bean.keyword.SiteTypeKeyword;
import org.tdar.core.bean.keyword.TemporalKeyword;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.ResourceAnnotation;
import org.tdar.core.bean.resource.ResourceAnnotationKey;
import org.tdar.core.bean.resource.ResourceNote;
import org.tdar.core.bean.resource.ResourceNoteType;
import org.tdar.core.bean.resource.ResourceRelationship;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.dao.GenericDao.FindOptions;
import org.tdar.core.dao.external.auth.InternalTdarRights;
import org.tdar.core.service.BookmarkedResourceService;
import org.tdar.core.service.EntityService;
import org.tdar.core.service.GenericKeywordService;
import org.tdar.core.service.ObfuscationService;
import org.tdar.core.service.ResourceCollectionService;
import org.tdar.core.service.XmlService;
import org.tdar.core.service.billing.AccountService;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.core.service.resource.InformationResourceService;
import org.tdar.core.service.resource.ResourceService;
import org.tdar.core.service.resource.ResourceService.ErrorHandling;
import org.tdar.struts.action.AbstractPersistableController;
import org.tdar.struts.action.TdarActionException;
import org.tdar.struts.data.KeywordNode;
import org.tdar.struts.data.ResourceCreatorProxy;
import org.tdar.struts.interceptor.annotation.HttpsOnly;
import org.tdar.struts.interceptor.annotation.PostOnly;
import org.tdar.struts.interceptor.annotation.WriteableSession;
import org.tdar.transform.MetaTag;
import org.tdar.transform.OpenUrlFormatter;
import org.tdar.transform.ScholarMetadataTransformer;
import org.tdar.utils.EmailMessageType;

/**
 * $Id$
 * 
 * Provides basic metadata support for controllers that manage subtypes of
 * Resource.
 * 
 * Don't extend this class unless you need this metadata to be set.
 * 
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
public abstract class AbstractResourceController<R extends Resource> extends AbstractPersistableController<R> {

    public static final String RESOURCE_EDIT_TEMPLATE = "../resource/edit-template.ftl";

    private static final long serialVersionUID = 8620875853247755760L;

    private List<MaterialKeyword> allMaterialKeywords;
    private List<InvestigationType> allInvestigationTypes;
    private List<EmailMessageType> emailTypes = EmailMessageType.valuesWithoutConfidentialFiles();

    private String submitterProperName = "";

    @Autowired
    private XmlService xmlService;

    @Autowired
    private BookmarkedResourceService bookmarkedResourceService;

    @Autowired
    private transient AuthorizationService authorizationService;

    @Autowired
    private ObfuscationService obfuscationService;

    @Autowired
    private EntityService entityService;

    @Autowired
    private GenericKeywordService genericKeywordService;

    @Autowired
    public ResourceCollectionService resourceCollectionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private InformationResourceService informationResourceService;

    @Autowired
    private ResourceService resourceService;

    private KeywordNode<SiteTypeKeyword> approvedSiteTypeKeywords;
    private KeywordNode<CultureKeyword> approvedCultureKeywords;

    private List<ResourceCollection> resourceCollections = new ArrayList<>();
    private List<ResourceCollection> effectiveResourceCollections = new ArrayList<>();

    private List<ResourceRelationship> resourceRelationships = new ArrayList<>();

    // containers for submitted data.
    private List<String> siteNameKeywords;

    private List<Long> materialKeywordIds;
    private List<Long> investigationTypeIds;

    private List<Long> approvedSiteTypeKeywordIds;
    private List<Long> approvedCultureKeywordIds;

    private List<String> uncontrolledSiteTypeKeywords;
    private List<String> uncontrolledCultureKeywords;

    private List<String> otherKeywords;

    private Person submitter;
    private List<String> temporalKeywords;
    private List<String> geographicKeywords;
    private List<LatitudeLongitudeBox> latitudeLongitudeBoxes;
    private List<CoverageDate> coverageDates;
    // citation data.
    // private List<String> sourceCitations;
    private List<SourceCollection> sourceCollections;
    // private List<String> relatedCitations;
    private List<RelatedComparativeCollection> relatedComparativeCollections;
    private Long accountId;
    private List<Account> activeAccounts;

    private List<ResourceNote> resourceNotes;
    private List<ResourceCreatorProxy> authorshipProxies;
    private List<ResourceCreatorProxy> creditProxies;
    private List<ResourceCreatorProxy> contactProxies;

    private List<ResourceAnnotation> resourceAnnotations;

    private List<ResourceCollection> viewableResourceCollections;

    private void initializeResourceCreatorProxyLists(boolean isViewPage) {
        Set<ResourceCreator> resourceCreators = getPersistable().getResourceCreators();
        if (isViewPage) {
            resourceCreators = getPersistable().getActiveResourceCreators();
        }
        if (resourceCreators == null) {
            return;
        }
        authorshipProxies = new ArrayList<>();
        creditProxies = new ArrayList<>();

        // this may be duplicative... check
        for (ResourceCreator rc : resourceCreators) {
            if (getTdarConfiguration().obfuscationInterceptorDisabled()) {
                if ((rc.getCreatorType() == CreatorType.PERSON) && !isAuthenticated()) {
                    obfuscationService.obfuscate(rc.getCreator(), getAuthenticatedUser());
                }
            }

            ResourceCreatorProxy proxy = new ResourceCreatorProxy(rc);
            if (ResourceCreatorRole.getAuthorshipRoles().contains(rc.getRole())) {
                authorshipProxies.add(proxy);
            } else {
                creditProxies.add(proxy);
            }

            if (ResourceCreatorRole.CONTACT == proxy.getRole()) {
                getContactProxies().add(proxy);
            }
        }
    }

    protected void loadCustomMetadata() throws TdarActionException {
    }

    public String getOpenUrl() {
        return OpenUrlFormatter.toOpenURL(getResource());
    }

    public String getGoogleScholarTags() throws Exception {
        ScholarMetadataTransformer trans = new ScholarMetadataTransformer();
        StringWriter sw = new StringWriter();
        for (MetaTag tag : trans.convertResourceToMetaTag(getResource())) {
            xmlService.convertToXMLFragment(MetaTag.class, tag, sw);
            sw.append("\n");
        }
        return sw.toString();
    }

    @Override
    public String loadAddMetadata() {
        if (Persistable.Base.isNotNullOrTransient(getResource())) {
            setSubmitter(getResource().getSubmitter());
        } else {
            setSubmitter(getAuthenticatedUser());
        }

        if (getTdarConfiguration().isPayPerIngestEnabled()) {
            accountService.updateTransientAccountInfo(getResource());
            setActiveAccounts(new ArrayList<>(determineActiveAccounts()));
            if (Persistable.Base.isNotNullOrTransient(getResource()) && Persistable.Base.isNotNullOrTransient(getResource().getAccount())) {
                setAccountId(getResource().getAccount().getId());
            }
            for (Account account : getActiveAccounts()) {
                getLogger().trace(" - active accounts to {} files: {} mb: {}", account, account.getAvailableNumberOfFiles(), account.getAvailableSpaceInMb());
            }
        }
        return SUCCESS;
    }

    // Return list of acceptable billing accounts. If the resource has an account, this method will include it in the returned list even
    // if the user does not have explicit rights to the account (e.g. so that a user w/ edit rights on the resource can modify the resource
    // and maintain original billing account).
    protected List<Account> determineActiveAccounts() {
        List<Account> accounts = new LinkedList<>(accountService.listAvailableAccountsForUser(getAuthenticatedUser()));
        if (getResource() != null) {
            Account resourceAccount = getResource().getAccount();
            if ((resourceAccount != null) && !accounts.contains(resourceAccount)) {
                accounts.add(0, resourceAccount);
            }
        }
        return accounts;
    }

    @Action(value = SAVE,
            interceptorRefs = { @InterceptorRef("editAuthenticatedStack") },
            results = {
                    @Result(name = SUCCESS, type = TYPE_REDIRECT, location = SAVE_SUCCESS_PATH),
                    @Result(name = SUCCESS_ASYNC, location = "view-async.ftl"),
                    @Result(name = INPUT, location = RESOURCE_EDIT_TEMPLATE)
            })
    @WriteableSession
    @PostOnly
    @HttpsOnly
    @Override
    /**
     * FIXME: appears to only override the INPUT result type compared to AbstractPersistableController's declaration,
     * see if it's possible to do this with less duplicatiousness
     * @see org.tdar.struts.action.AbstractPersistableController#save()
     */
    public String save() throws TdarActionException {
        setupSubmitterField();
        setSaveSuccessPath(getResource().getResourceType().getUrlNamespace());
        return super.save();
    }

    @SkipValidation
    @Action(value = ADD, results = {
            @Result(name = SUCCESS, location = RESOURCE_EDIT_TEMPLATE),
            @Result(name = CONTRIBUTOR, type = TYPE_REDIRECT, location = URLConstants.MY_PROFILE),
            @Result(name = BILLING, type = TYPE_REDIRECT, location = URLConstants.CART_ADD)
    })
    @HttpsOnly
    @Override
    public String add() throws TdarActionException {
        accountService.assignOrphanInvoicesIfNecessary(getAuthenticatedUser());
        setupSubmitterField();
        // if user has no invoices/funds to bill against, redirect to cart page
        if (!isAbleToCreateBillableItem()) {
            addActionMessage(getText("resourceController.requires_funds"));
            return BILLING;
        }

        // if user could otherwise create a billable item but isn't a contributor (an unlikely event), redirect to the profile page so that they can change
        // their status.
        if (!isContributor()) {
            addActionMessage(getText("resourceController.must_be_contributor"));
            return CONTRIBUTOR;
        }

        return super.add();
    }

    @SkipValidation
    @Action(value = EDIT, results = {
            @Result(name = SUCCESS, location = RESOURCE_EDIT_TEMPLATE)
    })
    @HttpsOnly
    @Override
    public String edit() throws TdarActionException {
        setupSubmitterField();
        return super.edit();
    }

    private void setupSubmitterField() {
        if (Persistable.Base.isNotNullOrTransient(getSubmitter()) && StringUtils.isNotBlank(getSubmitter().getProperName())) {
            if (getSubmitter().getFirstName() != null && getSubmitter().getLastName() != null)
                setSubmitterProperName(getSubmitter().getProperName());
        } else {
            setSubmitterProperName(getAuthenticatedUser().getProperName());
        }
    }

    @Override
    public String loadEditMetadata() throws TdarActionException {
        loadAddMetadata();
        loadBasicMetadata();
        loadCustomMetadata();
        return SUCCESS;
    }

    @Override
    protected void postSaveCallback(String actionMessage) {
        // if user has single billing account, use that (ignore the form);
        setupAccountForSaving();

        if (SUCCESS.equals(actionMessage)) {
            if (shouldSaveResource()) {
                if (getResource().getStatus() == Status.FLAGGED_ACCOUNT_BALANCE) {
                    getResource().setStatus(getResource().getPreviousStatus());
                }
                updateQuota(getGenericService().find(Account.class, getAccountId()), getResource());
            }
        } else {
            loadAddMetadata();
        }

        if (getResource() != null) { // this will happen with the bulk uploader
            String logMessage = String.format("%s edited and saved by %s:\ttdar id:%s\ttitle:[%s]", getResource().getResourceType().name(),
                    getAuthenticatedUser(), getResource().getId(), StringUtils.left(getResource().getTitle(), 100));
            logModification(logMessage);
        }
    }

    protected void setupAccountForSaving() {
        accountService.updateTransientAccountInfo(getResource());
        List<Account> accounts = determineActiveAccounts();
        if (accounts.size() == 1) {
            setAccountId(accounts.get(0).getId());
        }
    }

    @Override
    public boolean isCreatable() throws TdarActionException {
        // FIXME: this is really an authorization thing...
        // if (!getAuthenticatedUser().getContributor()) {
        // return false;
        // }
        return true;
    }

    /**
     * Returns true if authuser is able to create billable items, contributor status notwithstanding.
     * 
     * @return
     */
    @Override
    public boolean isAbleToCreateBillableItem() {
        return (!getTdarConfiguration().isPayPerIngestEnabled() || accountService.hasSpaceInAnAccount(getAuthenticatedUser(), getResource().getResourceType()));
    }

    // return a persisted annotation based on incoming pojo
    private void resolveAnnotations(Collection<ResourceAnnotation> incomingAnnotations) {
        List<ResourceAnnotation> toAdd = new ArrayList<>();
        for (ResourceAnnotation incomingAnnotation : incomingAnnotations) {
            if (incomingAnnotation == null) {
                continue;
            }
            ResourceAnnotationKey incomingKey = incomingAnnotation.getResourceAnnotationKey();
            ResourceAnnotationKey resolvedKey = getGenericService().findByExample(ResourceAnnotationKey.class, incomingKey, FindOptions.FIND_FIRST_OR_CREATE)
                    .get(0);
            incomingAnnotation.setResourceAnnotationKey(resolvedKey);

            if (incomingAnnotation.isTransient()) {
                List<String> vals = new ArrayList<>();
                vals.add(incomingAnnotation.getValue());
                cleanupKeywords(vals);

                if (vals.size() > 1) {
                    incomingAnnotation.setValue(vals.get(0));
                    for (int i = 1; i < vals.size(); i++) {
                        toAdd.add(new ResourceAnnotation(resolvedKey, vals.get(i)));
                    }
                }
            }
        }
        incomingAnnotations.addAll(toAdd);
    }

    private Boolean editable = null;

    @Override
    public boolean isEditable() {
        if (isNullOrNew()) {
            return false;
        }
        if (editable == null) {
            editable = authorizationService.canEditResource(getAuthenticatedUser(), getPersistable(), GeneralPermissions.MODIFY_METADATA);
        }
        return editable;
    }

    protected void saveKeywords() {
        getLogger().debug("siteNameKeywords=" + siteNameKeywords);
        getLogger().debug("materialKeywords=" + materialKeywordIds);
        getLogger().debug("otherKeywords=" + otherKeywords);
        getLogger().debug("investigationTypes=" + investigationTypeIds);
        Resource res = getPersistable();
        GenericKeywordService gks = genericKeywordService;

        cleanupKeywords(uncontrolledCultureKeywords);
        cleanupKeywords(uncontrolledSiteTypeKeywords);
        cleanupKeywords(siteNameKeywords);
        cleanupKeywords(otherKeywords);
        cleanupKeywords(temporalKeywords);
        Set<CultureKeyword> culKeys = gks.findOrCreateByLabels(CultureKeyword.class, uncontrolledCultureKeywords);
        culKeys.addAll(genericKeywordService.findAll(CultureKeyword.class, approvedCultureKeywordIds));

        Set<SiteTypeKeyword> siteTypeKeys = genericKeywordService.findOrCreateByLabels(SiteTypeKeyword.class, uncontrolledSiteTypeKeywords);
        siteTypeKeys.addAll(genericKeywordService.findAll(SiteTypeKeyword.class, approvedSiteTypeKeywordIds));

        Persistable.Base.reconcileSet(res.getSiteNameKeywords(), gks.findOrCreateByLabels(SiteNameKeyword.class, siteNameKeywords));
        Persistable.Base.reconcileSet(res.getOtherKeywords(), gks.findOrCreateByLabels(OtherKeyword.class, otherKeywords));
        Persistable.Base.reconcileSet(res.getMaterialKeywords(), gks.findAll(MaterialKeyword.class, materialKeywordIds));
        Persistable.Base.reconcileSet(res.getInvestigationTypes(), gks.findAll(InvestigationType.class, investigationTypeIds));

        Persistable.Base.reconcileSet(res.getCultureKeywords(), culKeys);
        Persistable.Base.reconcileSet(res.getSiteTypeKeywords(), siteTypeKeys);
    }

    private void cleanupKeywords(List<String> kwds) {

        if (CollectionUtils.isEmpty(kwds)) {
            return;
        }
        String delim = "||";
        Iterator<String> iter = kwds.iterator();
        Set<String> toAdd = new HashSet<>();
        while (iter.hasNext()) {
            String keyword = iter.next();
            if (StringUtils.isBlank(keyword)) {
                continue;
            }

            if (keyword.contains(delim)) {
                for (String sub : StringUtils.split(keyword, delim)) {
                    sub = StringUtils.trim(sub);
                    if (StringUtils.isNotBlank(sub)) {
                        toAdd.add(sub);
                    }
                }
                iter.remove();
            }
        }
        kwds.addAll(toAdd);
    }

    protected void saveTemporalContext() {
        // calendar and radiocarbon dates are null for Ontologies
        resourceService.saveHasResources((Resource) getPersistable(), shouldSaveResource(), ErrorHandling.VALIDATE_SKIP_ERRORS, coverageDates,
                getResource().getCoverageDates(), CoverageDate.class);
        Persistable.Base.reconcileSet(getPersistable().getTemporalKeywords(),
                genericKeywordService.findOrCreateByLabels(TemporalKeyword.class, temporalKeywords));
    }

    protected void saveSpatialContext() {
        // it won't add a null or incomplete lat-long box.

        resourceService.saveHasResources((Resource) getPersistable(), shouldSaveResource(), ErrorHandling.VALIDATE_SKIP_ERRORS, latitudeLongitudeBoxes,
                getResource().getLatitudeLongitudeBoxes(), LatitudeLongitudeBox.class);

        if (CollectionUtils.isEmpty(getPersistable().getActiveLatitudeLongitudeBoxes()) && !(this instanceof BulkUploadController)
                && !(this instanceof AbstractSupportingInformationResourceController)) {
            addActionMessage(getText("abstractResourceController.no_map", Arrays.asList(getResource().getResourceType().getLabel())));
        }
        Persistable.Base.reconcileSet(getPersistable().getGeographicKeywords(),
                genericKeywordService.findOrCreateByLabels(GeographicKeyword.class, geographicKeywords));

        resourceService.processManagedKeywords(getPersistable(), getPersistable().getLatitudeLongitudeBoxes());
    }

    protected void saveCitations() {
        resourceService.saveHasResources((Resource) getPersistable(), shouldSaveResource(), ErrorHandling.VALIDATE_SKIP_ERRORS,
                relatedComparativeCollections,
                getResource().getRelatedComparativeCollections(), RelatedComparativeCollection.class);
        resourceService.saveHasResources((Resource) getPersistable(), shouldSaveResource(), ErrorHandling.VALIDATE_SKIP_ERRORS, sourceCollections,
                getResource().getSourceCollections(), SourceCollection.class);

    }

    /**
     * Saves keywords, full / read user access, and confidentiality.
     */
    protected void saveBasicResourceMetadata() {

        if (shouldSaveResource()) {
            resourceService.saveOrUpdate(getPersistable());
        }

        if (Persistable.Base.isNotNullOrTransient(getSubmitter())) {
            TdarUser uploader = getGenericService().find(TdarUser.class, getSubmitter().getId());
            getPersistable().setSubmitter(uploader);
            // if I change the owner, and the owner is me, then make sure I don't loose permissions on the record
            // if (uploader.equals(getAuthenticatedUser())) {
            // boolean found = false;
            // for (AuthorizedUser user : getAuthorizedUsers()) {
            // if (user.getUser().equals(uploader)) {
            // found = true;
            // }
            // }
            // // if we're setting the sbumitter
            // if (!found) {
            // getAuthorizedUsers().add(new AuthorizedUser(uploader, GeneralPermissions.MODIFY_RECORD));
            // }
            // }
        }

        // only modify these permissions if the user has the right to
        if (authorizationService.canDo(getAuthenticatedUser(), getResource(), InternalTdarRights.EDIT_ANY_RESOURCE,
                GeneralPermissions.MODIFY_RECORD)) {
            resourceCollectionService.saveAuthorizedUsersForResource(getResource(), getAuthorizedUsers(), shouldSaveResource(), getAuthenticatedUser());
        }

        saveKeywords();
        saveTemporalContext();
        saveSpatialContext();
        saveCitations();

        prepSequence(resourceNotes);
        resourceService.saveHasResources((Resource) getPersistable(), shouldSaveResource(), ErrorHandling.VALIDATE_SKIP_ERRORS, resourceNotes,
                getResource().getResourceNotes(), ResourceNote.class);
        saveResourceCreators();

        resolveAnnotations(getResourceAnnotations());

        resourceService.saveHasResources((Resource) getPersistable(), shouldSaveResource(), ErrorHandling.VALIDATE_SKIP_ERRORS, getResourceAnnotations(),
                getResource().getResourceAnnotations(), ResourceAnnotation.class);
        resourceCollectionService.saveSharedResourceCollections(getResource(), resourceCollections, getResource().getResourceCollections(),
                getAuthenticatedUser(), shouldSaveResource(), ErrorHandling.VALIDATE_SKIP_ERRORS);

    }

    public <T extends Sequenceable<T>> void prepSequence(List<T> list) {
        if (list == null) {
            return;
        }
        if (list.isEmpty()) {
            return;
        }
        list.removeAll(Collections.singletonList(null));
        Sequence.applySequence(list);
    }

    protected void logModification(String message) {
        resourceService.logResourceModification(getPersistable(), getAuthenticatedUser(), message, null);
    }

    protected void saveResourceCreators() {
        List<ResourceCreatorProxy> allProxies = new ArrayList<>();
        if (authorshipProxies != null) {
            allProxies.addAll(authorshipProxies);
        }
        if (creditProxies != null) {
            allProxies.addAll(creditProxies);
        }
        resourceService.saveResourceCreatorsFromProxies(allProxies, getPersistable(), shouldSaveResource());
    }

    public void loadBasicMetadata() {
        // load all keywords

        setMaterialKeywordIds(toIdList(getResource().getMaterialKeywords()));
        setInvestigationTypeIds(toIdList(getResource().getInvestigationTypes()));

        setUncontrolledCultureKeywords(toSortedStringList(getResource().getUncontrolledCultureKeywords()));
        setApprovedCultureKeywordIds(toIdList(getResource().getApprovedCultureKeywords()));

        setUncontrolledSiteTypeKeywords(toSortedStringList(getResource().getUncontrolledSiteTypeKeywords()));
        setApprovedSiteTypeKeywordIds(toIdList(getResource().getApprovedSiteTypeKeywords()));

        setOtherKeywords(toSortedStringList(getResource().getOtherKeywords()));
        setSiteNameKeywords(toSortedStringList(getResource().getSiteNameKeywords()));
        // load temporal / geographic terms
        setTemporalKeywords(toSortedStringList(getResource().getTemporalKeywords()));
        setGeographicKeywords(toSortedStringList(getResource().getGeographicKeywords()));
        // load spatial context
        getLatitudeLongitudeBoxes().addAll(getResource().getLatitudeLongitudeBoxes());

        // load radiocarbon / calendar dates
        getCoverageDates().addAll(getResource().getCoverageDates());
        // load full access users

        getResourceNotes().addAll(getResource().getResourceNotes());
        Collections.sort(getResourceNotes());
        getSourceCollections().addAll(getResource().getSourceCollections());
        getRelatedComparativeCollections().addAll(getResource().getRelatedComparativeCollections());
        getAuthorizedUsers().addAll(resourceCollectionService.getAuthorizedUsersForResource(getResource(), getAuthenticatedUser()));
        for (AuthorizedUser au : getAuthorizedUsers()) {
            String name = null;
            if (au != null && au.getUser() != null) {
                name = au.getUser().getProperName();
            }
            getAuthorizedUsersFullNames().add(name);
        }
        initializeResourceCreatorProxyLists(false);
        getResourceAnnotations().addAll(getResource().getResourceAnnotations());
        loadEffectiveResourceCollections();
    }

    public void loadBasicViewMetadata() {
        getAuthorizedUsers().addAll(resourceCollectionService.getAuthorizedUsersForResource(getResource(), getAuthenticatedUser()));
        initializeResourceCreatorProxyLists(true);
        loadEffectiveResourceCollections();
    }

    private void loadEffectiveResourceCollections() {
        getResourceCollections().addAll(getResource().getSharedResourceCollections());
        getEffectiveResourceCollections().addAll(resourceCollectionService.getEffectiveResourceCollectionsForResource(getResource()));
    }

    public List<String> getSiteNameKeywords() {
        if (CollectionUtils.isEmpty(siteNameKeywords)) {
            siteNameKeywords = new ArrayList<>();
        }
        return siteNameKeywords;
    }

    public List<String> getOtherKeywords() {
        if (CollectionUtils.isEmpty(otherKeywords)) {
            otherKeywords = new ArrayList<>();
        }
        return otherKeywords;
    }

    public List<String> getTemporalKeywords() {
        if (CollectionUtils.isEmpty(temporalKeywords)) {
            temporalKeywords = new ArrayList<>();
        }
        return temporalKeywords;
    }

    public List<String> getGeographicKeywords() {
        if (CollectionUtils.isEmpty(geographicKeywords)) {
            geographicKeywords = new ArrayList<>();
        }
        return geographicKeywords;
    }

    public List<LatitudeLongitudeBox> getLatitudeLongitudeBoxes() {
        if (latitudeLongitudeBoxes == null) {
            latitudeLongitudeBoxes = new ArrayList<>();
        }
        return latitudeLongitudeBoxes;
    }

    public void setLatitudeLongitudeBoxes(List<LatitudeLongitudeBox> longitudeLatitudeBox) {
        this.latitudeLongitudeBoxes = longitudeLatitudeBox;
    }

    public List<Long> getMaterialKeywordIds() {
        if (CollectionUtils.isEmpty(materialKeywordIds)) {
            materialKeywordIds = new ArrayList<>();
        }
        return materialKeywordIds;
    }

    public R getResource() {
        return getPersistable();
    }

    public void setResource(R resource) {
        getLogger().debug("setResource: {}", resource);
        setPersistable(resource);
    }

    public List<MaterialKeyword> getAllMaterialKeywords() {
        if (CollectionUtils.isEmpty(allMaterialKeywords)) {
            allMaterialKeywords = genericKeywordService.findAllWithCache(MaterialKeyword.class);
            Collections.sort(allMaterialKeywords);
        }
        return allMaterialKeywords;
    }

    public List<CoverageDate> getCoverageDates() {
        if (CollectionUtils.isEmpty(coverageDates)) {
            coverageDates = new ArrayList<>();
        }
        return coverageDates;
    }

    public void setCoverageDates(List<CoverageDate> coverageDates) {
        this.coverageDates = coverageDates;
    }

    public List<SourceCollection> getSourceCollections() {
        if (sourceCollections == null) {
            sourceCollections = new ArrayList<>();
        }
        return sourceCollections;
    }

    public List<RelatedComparativeCollection> getRelatedComparativeCollections() {
        if (relatedComparativeCollections == null) {
            relatedComparativeCollections = new ArrayList<>();
        }
        return relatedComparativeCollections;
    }

    public boolean isAbleToViewConfidentialFiles() {
        return authorizationService.canViewConfidentialInformation(getAuthenticatedUser(), getPersistable());
    }

    public List<InvestigationType> getAllInvestigationTypes() {
        if (CollectionUtils.isEmpty(allInvestigationTypes)) {
            allInvestigationTypes = genericKeywordService.findAllWithCache(InvestigationType.class);
            Collections.sort(allInvestigationTypes);
        }
        return allInvestigationTypes;
    }

    public List<Long> getInvestigationTypeIds() {
        if (CollectionUtils.isEmpty(investigationTypeIds)) {
            investigationTypeIds = createListWithSingleNull();
        }
        return investigationTypeIds;
    }

    public KeywordNode<SiteTypeKeyword> getApprovedSiteTypeKeywords() {
        if (approvedSiteTypeKeywords == null) {
            approvedSiteTypeKeywords = KeywordNode.organizeKeywords(genericKeywordService.findAllApprovedWithCache(SiteTypeKeyword.class));
        }
        return approvedSiteTypeKeywords;
    }

    public KeywordNode<CultureKeyword> getApprovedCultureKeywords() {
        if (approvedCultureKeywords == null) {
            approvedCultureKeywords = KeywordNode.organizeKeywords(genericKeywordService.findAllApprovedWithCache(CultureKeyword.class));
        }
        return approvedCultureKeywords;
    }

    public List<Long> getApprovedSiteTypeKeywordIds() {
        if (CollectionUtils.isEmpty(approvedSiteTypeKeywordIds)) {
            approvedSiteTypeKeywordIds = createListWithSingleNull();
        }
        return approvedSiteTypeKeywordIds;
    }

    public List<Long> getApprovedCultureKeywordIds() {
        if (CollectionUtils.isEmpty(approvedCultureKeywordIds)) {
            approvedCultureKeywordIds = createListWithSingleNull();
        }
        return approvedCultureKeywordIds;
    }

    public List<String> getUncontrolledSiteTypeKeywords() {
        if (CollectionUtils.isEmpty(uncontrolledSiteTypeKeywords)) {
            uncontrolledSiteTypeKeywords = createListWithSingleNull();
        }
        return uncontrolledSiteTypeKeywords;
    }

    public List<String> getUncontrolledCultureKeywords() {
        if (CollectionUtils.isEmpty(uncontrolledCultureKeywords)) {
            uncontrolledCultureKeywords = createListWithSingleNull();
        }
        return uncontrolledCultureKeywords;
    }

    public List<CreatorType> getCreatorTypes() {
        // FIXME: move impl to service layer
        return Arrays.asList(CreatorType.values());
    }

    public List<ResourceNote> getResourceNotes() {
        if (resourceNotes == null) {
            resourceNotes = new ArrayList<>();
        }
        return resourceNotes;
    }

    // FIXME: JTd: I think we should make all controller collection setters protected unless service layer absolutely. Confirm w/ others and change signatures
    // or revert this method back to public if there are objections.
    protected void setResourceNotes(List<ResourceNote> resourceNotes) {
        this.resourceNotes = resourceNotes;
    }

    public ResourceNote getBlankResourceNote() {
        return new ResourceNote();
    }

    public List<ResourceNoteType> getNoteTypes() {
        return Arrays.asList(ResourceNoteType.values());
    }

    public List<ResourceCreatorRole> getAllResourceCreatorRoles() {
        // FIXME: move impl to service
        // FIXME: change to SortedSet
        return ResourceCreatorRole.getAll();
    }

    public Set<ResourceAnnotationKey> getAllResourceAnnotationKeys() {
        Set<ResourceAnnotationKey> keys = new HashSet<>();
        if ((getPersistable() != null) && CollectionUtils.isNotEmpty(getPersistable().getActiveResourceAnnotations())) {
            for (ResourceAnnotation ra : getPersistable().getActiveResourceAnnotations()) {
                keys.add(ra.getResourceAnnotationKey());
            }
        }
        return keys;
    }

    public void setSiteNameKeywords(List<String> siteNameKeywords) {
        this.siteNameKeywords = siteNameKeywords;
    }

    public void setApprovedSiteTypeKeywordIds(List<Long> approvedSiteTypeKeywordIds) {
        this.approvedSiteTypeKeywordIds = approvedSiteTypeKeywordIds;
    }

    public void setApprovedCultureKeywordIds(List<Long> approvedCultureKeywordIds) {
        this.approvedCultureKeywordIds = approvedCultureKeywordIds;
    }

    public void setUncontrolledSiteTypeKeywords(List<String> uncontrolledSiteTypeKeywords) {
        this.uncontrolledSiteTypeKeywords = uncontrolledSiteTypeKeywords;
    }

    public void setUncontrolledCultureKeywords(List<String> uncontrolledCultureKeywords) {
        this.uncontrolledCultureKeywords = uncontrolledCultureKeywords;
    }

    public void setOtherKeywords(List<String> otherKeywords) {
        this.otherKeywords = otherKeywords;
    }

    public void setTemporalKeywords(List<String> temporalKeywords) {
        this.temporalKeywords = temporalKeywords;
    }

    public void setGeographicKeywords(List<String> geographicKeywords) {
        this.geographicKeywords = geographicKeywords;
    }

    public void setMaterialKeywordIds(List<Long> materialKeywordIds) {
        this.materialKeywordIds = materialKeywordIds;
    }

    public void setInvestigationTypeIds(List<Long> investigationTypeIds) {
        this.investigationTypeIds = investigationTypeIds;
    }

    public void setSourceCollections(List<SourceCollection> sourceCollections) {
        this.sourceCollections = sourceCollections;
    }

    public void setRelatedComparativeCollections(List<RelatedComparativeCollection> relatedComparativeCitations) {
        this.relatedComparativeCollections = relatedComparativeCitations;
    }

    public List<ResourceCreatorProxy> getAuthorshipProxies() {
        if (CollectionUtils.isEmpty(authorshipProxies)) {
            authorshipProxies = new ArrayList<>();
        }
        return authorshipProxies;
    }

    public List<ResourceCreatorProxy> getContactProxies() {
        if (CollectionUtils.isEmpty(contactProxies)) {
            contactProxies = new ArrayList<>();
        }
        return contactProxies;
    }

    public ResourceCreatorProxy getBlankCreatorProxy() {
        return new ResourceCreatorProxy();
    }

    public CoverageDate getBlankCoverageDate() {
        return new CoverageDate(CoverageType.CALENDAR_DATE);
    }

    public void setAuthorshipProxies(List<ResourceCreatorProxy> authorshipProxies) {
        this.authorshipProxies = authorshipProxies;
    }

    public List<ResourceCreatorProxy> getCreditProxies() {
        if (CollectionUtils.isEmpty(creditProxies)) {
            creditProxies = new ArrayList<>();
        }
        return creditProxies;
    }

    public void setCreditProxies(List<ResourceCreatorProxy> creditProxies) {
        this.creditProxies = creditProxies;
    }

    public List<ResourceCreatorRole> getInstitutionAuthorshipRoles() {
        return ResourceCreatorRole.getAuthorshipRoles(CreatorType.INSTITUTION, getResource().getResourceType());
    }

    public List<ResourceCreatorRole> getInstitutionCreditRoles() {
        return ResourceCreatorRole.getCreditRoles(CreatorType.INSTITUTION, getResource().getResourceType());
    }

    public List<ResourceCreatorRole> getPersonAuthorshipRoles() {
        return ResourceCreatorRole.getAuthorshipRoles(CreatorType.PERSON, getResource().getResourceType());
    }

    public List<ResourceCreatorRole> getPersonCreditRoles() {
        return ResourceCreatorRole.getCreditRoles(CreatorType.PERSON, getResource().getResourceType());
    }

    public List<ResourceAnnotation> getResourceAnnotations() {
        if (resourceAnnotations == null) {
            resourceAnnotations = new ArrayList<>();
        }
        return resourceAnnotations;
    }

    public ResourceAnnotation getBlankResourceAnnotation() {
        return new ResourceAnnotation(new ResourceAnnotationKey(), "");
    }

    public ResourceCollection getBlankResourceCollection() {
        return new ResourceCollection(CollectionType.SHARED);
    }

    public SourceCollection getBlankSourceCollection() {
        return new SourceCollection();
    }

    public RelatedComparativeCollection getBlankRelatedComparativeCollection() {
        return new RelatedComparativeCollection();
    }

    public void setResourceAnnotations(List<ResourceAnnotation> resourceAnnotations) {
        this.resourceAnnotations = resourceAnnotations;
    }

    public List<ResourceType> getAllResourceTypes() {
        return Arrays.asList(ResourceType.values());
    }

    /**
     * @param resourceCollections
     *            the resourceCollections to set
     */
    public void setResourceCollections(List<ResourceCollection> resourceCollections) {
        this.resourceCollections = resourceCollections;
    }

    /**
     * @return the resourceCollections
     */
    public List<ResourceCollection> getResourceCollections() {
        return resourceCollections;
    }

    /**
     * @return the effectiveResourceCollections
     */
    public List<ResourceCollection> getEffectiveResourceCollections() {
        return effectiveResourceCollections;
    }

    /**
     * @param effectiveResourceCollections
     *            the effectiveResourceCollections to set
     */
    public void setEffectiveResourceCollections(List<ResourceCollection> effectiveResourceCollections) {
        this.effectiveResourceCollections = effectiveResourceCollections;
    }

    // return all of the collections that the currently-logged-in user is allowed to view. We define viewable as either shared+visible, or
    // shared+invisible+canEdit
    public List<ResourceCollection> getViewableResourceCollections() {
        if (viewableResourceCollections != null) {
            return viewableResourceCollections;
        }

        // if nobody logged in, just get the shared+visible collections
        Set<ResourceCollection> collections = new HashSet<>(getResource().getSharedVisibleResourceCollections());

        // if authenticated, also add the collections that the user can modify
        if (isAuthenticated()) {
            for (ResourceCollection resourceCollection : getResource().getSharedResourceCollections()) {
                if (authorizationService.canViewCollection(resourceCollection, getAuthenticatedUser())) {
                    collections.add(resourceCollection);
                }
            }
        }

        viewableResourceCollections = new ArrayList<>(collections);
        return viewableResourceCollections;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public List<Account> getActiveAccounts() {
        if (activeAccounts == null) {
            activeAccounts = new ArrayList<>(determineActiveAccounts());
        }
        return activeAccounts;
    }

    public void setActiveAccounts(List<Account> activeAccounts) {
        this.activeAccounts = activeAccounts;
    }

    public boolean isBulkUpload() {
        return false;
    }

    public Person getSubmitter() {
        return submitter;
    }

    public void setSubmitter(Person submitter) {
        this.submitter = submitter;
    }

    public List<ResourceRelationship> getResourceRelationships() {
        return resourceRelationships;
    }

    public void setResourceRelationships(List<ResourceRelationship> resourceRelationships) {
        this.resourceRelationships = resourceRelationships;
    }

    public boolean isUserAbleToReTranslate() {
        if (authorizationService.canEdit(getAuthenticatedUser(), getPersistable())) {
            return true;
        }
        return false;
    }

    public boolean isUserAbleToViewDeletedFiles() {
        return isEditor();
    }

    public boolean isUserAbleToViewUnobfuscatedMap() {
        return isEditor();
    }

    public void updateQuota(Account account, Resource resource) {
        if (getTdarConfiguration().isPayPerIngestEnabled()) {
            accountService.updateQuota(account, resource);
        }
    }

    public List<CoverageType> getAllCoverageTypes() {
        List<CoverageType> coverageTypes = new ArrayList<>();
        coverageTypes.add(CoverageType.CALENDAR_DATE);
        coverageTypes.add(CoverageType.RADIOCARBON_DATE);
        return coverageTypes;
    }

    /**
     * Returns a list of Strings resulting from applying toString to each
     * element of the incoming Collection.
     * 
     * Basically, map( toString, collection ), but since Java doesn't support
     * closures yet...
     * 
     * @param collection
     * @return
     */
    protected List<String> toSortedStringList(Collection<?> collection) {
        ArrayList<String> stringList = new ArrayList<>(collection.size());
        for (Object o : collection) {
            stringList.add(o.toString());
        }
        Collections.sort(stringList);
        return stringList;
    }

    protected <P extends Persistable> List<Long> toIdList(Collection<P> persistables) {
        return Persistable.Base.extractIds(persistables);
    }

    protected void loadCustomViewMetadata() throws TdarActionException {
        // TODO Auto-generated method stub

    }

    public List<EmailMessageType> getEmailTypes() {
        return emailTypes;
    }

    public void setEmailTypes(List<EmailMessageType> emailTypes) {
        this.emailTypes = emailTypes;
    }

    public String getSubmitterProperName() {
        return submitterProperName;
    }

    public void setSubmitterProperName(String submitterProperName) {
        this.submitterProperName = submitterProperName;
    }

}
