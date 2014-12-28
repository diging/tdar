package org.tdar.core.dao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.billing.Account;
import org.tdar.core.bean.billing.AccountGroup;
import org.tdar.core.bean.billing.BillingActivityModel;
import org.tdar.core.bean.billing.Coupon;
import org.tdar.core.bean.billing.Invoice;
import org.tdar.core.bean.billing.TransactionStatus;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.exception.TdarQuotaException;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.utils.AccountEvaluationHelper;
import org.tdar.utils.PersistableUtils;

/**
 * $Id$
 * 
 * DAO access for Accounts
 * 
 * @author <a href='Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
@Component
public class BillingAccountDao extends Dao.HibernateBase<Account> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public BillingAccountDao() {
        super(Account.class);
    }

    @SuppressWarnings("unchecked")
    public List<Account> findAccountsForUser(Person user, Status... statuses) {
        if (ArrayUtils.isEmpty(statuses)) {
            statuses = new Status[] { Status.ACTIVE, Status.FLAGGED_ACCOUNT_BALANCE };
        }
        // this does not return unique results
        List<Account> accountGroups = new ArrayList<>();
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.ACCOUNTS_FOR_PERSON);
        query.setParameter("personid", user.getId());
        query.setParameterList("statuses", statuses);
        accountGroups.addAll(query.list());
        for (AccountGroup group : findAccountGroupsForUser(user)) {
            accountGroups.addAll(group.getAccounts());
        }
        return new ArrayList<>(accountGroups);
    }

    @SuppressWarnings("unchecked")
    public List<AccountGroup> findAccountGroupsForUser(Person user) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.ACCOUNT_GROUPS_FOR_PERSON);
        query.setParameter("personid", user.getId());
        query.setParameterList("statuses", Arrays.asList(Status.ACTIVE));
        return query.list();

    }

    public AccountGroup getAccountGroup(Account account) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.ACCOUNT_GROUP_FOR_ACCOUNT);
        query.setParameter("accountId", account.getId());
        return (AccountGroup) query.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<Long> findResourcesWithDifferentAccount(List<Resource> resourcesToEvaluate, Account account) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.RESOURCES_WITH_NON_MATCHING_ACCOUNT_ID);
        query.setParameter("accountId", account.getId());
        query.setParameterList("ids", PersistableUtils.extractIds(resourcesToEvaluate));
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<Long> findResourcesWithNullAccount(List<Resource> resourcesToEvaluate) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.RESOURCES_WITH_NULL_ACCOUNT_ID);
        query.setParameterList("ids", PersistableUtils.extractIds(resourcesToEvaluate));
        return query.list();
    }

    public void updateTransientAccountOnResources(Collection<Resource> resourcesToEvaluate) {
        Map<Long, Resource> resourceIdMap = PersistableUtils.createIdMap(resourcesToEvaluate);
        String sql = String.format(TdarNamedQueries.QUERY_ACCOUNTS_FOR_RESOURCES, StringUtils.join(resourceIdMap.keySet().toArray()));
        if (CollectionUtils.isEmpty(resourceIdMap.keySet()) || ((resourceIdMap.keySet().size() == 1) && resourceIdMap.keySet().contains(-1L))) {
            return;
        }
        Query query = getCurrentSession().createSQLQuery(sql);

        Map<Long, Account> accountIdMap = new HashMap<>();
        for (Object objs : query.list()) {
            Object[] obj = (Object[]) objs;
            Long resourceId = ((BigInteger) obj[0]).longValue();
            Long accountId = null;
            if (obj[1] != null) {
                accountId = ((BigInteger) obj[1]).longValue();
            }
            Account account = accountIdMap.get(accountId);
            if (account == null) {
                account = find(accountId);
                accountIdMap.put(accountId, account);
            }
            Resource resource = resourceIdMap.get(resourceId);
            if (resource != null) {
                logger.trace("setting account {} for resource {}", accountId, resourceId);
                resource.setAccount(account);
            } else {
                logger.error("resource is null somehow for id: {}, account {}", resourceId, account);
            }
        }
    }

    public void updateAccountInfo(Account account, ResourceEvaluator re) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.ACCOUNT_QUOTA_INIT);
        query.setParameter("accountId", account.getId());
        List<Status> statuses = new ArrayList<>(CollectionUtils.disjunction(Arrays.asList(Status.values()), re.getUncountedResourceStatuses()));
        query.setParameterList("statuses", statuses);
        Long totalFiles = 0L;
        Long totalSpaceInBytes = 0L;
        for (Object objs : query.list()) {
            Object[] obj = (Object[]) objs;
            if (obj[0] != null) {
                totalFiles = ((Long) obj[0]).longValue();
            }
            if (obj[1] != null) {
                totalSpaceInBytes = ((Long) obj[1]).longValue();
            }
        }
        for (Coupon coupon : account.getCoupons()) {
            totalFiles += coupon.getNumberOfFiles();
            totalSpaceInBytes += coupon.getNumberOfMb() * Persistable.ONE_MB;
        }
        account.setFilesUsed(totalFiles);
        account.setSpaceUsedInBytes(totalSpaceInBytes);
    }

    @SuppressWarnings("unchecked")
    public List<Invoice> findUnassignedInvoicesForUser(Person user) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.UNASSIGNED_INVOICES_FOR_PERSON);
        query.setParameter("personId", user.getId());
        query.setParameterList("statuses", Arrays.asList(TransactionStatus.TRANSACTION_SUCCESSFUL));
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<Invoice> findInvoicesForUser(Person user) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.INVOICES_FOR_PERSON);
        query.setParameter("personId", user.getId());
        return query.list();
    }

    public Coupon findCoupon(String code, Person user) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.FIND_ACTIVE_COUPON);
        query.setParameter("code", code.toLowerCase());
        if (PersistableUtils.isNotNullOrTransient(user)) {
            query.setParameter("ownerId", user.getId());
        } else {
            query.setParameter("ownerId", null);
        }
        return (Coupon) query.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public void checkCouponStillValidForCheckout(Coupon coupon, Invoice invoice) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.FIND_INVOICE_FOR_COUPON);
        query.setParameter("code", coupon.getCode().toLowerCase());
        for (Invoice inv : (List<Invoice>) query.list()) {
            if (inv.getTransactionStatus().isComplete()) {
                throw new TdarRecoverableRuntimeException("accountDao.coupon_already_used");
            }
        }
        if (!Objects.equals(invoice.getCoupon().getId(), coupon.getId())) {
            throw new TdarRecoverableRuntimeException("accountDao.coupon_assigned_wrong");
        }
    }

    public Account getAccountForInvoice(Invoice invoice) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.FIND_ACCOUNT_FOR_INVOICE);
        query.setParameter("id", invoice.getId());
        return (Account) query.uniqueResult();
    }

    /**
     * Get a pre-configured @link ResourceEvaluator with the specified array of @link Resource entries.
     * 
     * @param resources
     * @return
     */
    public ResourceEvaluator getResourceEvaluator(Resource... resources) {
        return new ResourceEvaluator(getLatestActivityModel(), resources);
    }

    /**
     * Get a pre-configured @link ResourceEvaluator with the specified collection of @link Resource entries.
     * 
     * @param resources
     * @return
     */
    public ResourceEvaluator getResourceEvaluator(Collection<Resource> resources) {
        return new ResourceEvaluator(getLatestActivityModel(), resources.toArray(new Resource[0]));
    }

    /**
     * We know that we will change pricing from time to time, so, a @link BillingActivityModel allows us to represent different models at the same time. Return
     * the current model.
     * 
     * @return
     */
    public BillingActivityModel getLatestActivityModel() {
        List<BillingActivityModel> findAll = findAll(BillingActivityModel.class);
        BillingActivityModel latest = null;
        for (BillingActivityModel model : findAll) {
            if (!model.getActive()) {
                continue;
            }
            if ((latest == null) || (latest.getVersion() == null) || (model.getVersion() > latest.getVersion())) {
                latest = model;
            }
        }
        return latest;
    }

    /**
     * Update quota for account based on an Collection of @link Resource entries. This method works in two different paths:
     * (a) if there's an incremental change that does not bump the quota
     * (b) if the account is overdrawn or manually told to re-evaluate.
     * 
     * If (a) then just adjust the small bit; otherwise if (b) sort the resources chronologically and then evaluate chronologically so only the recent files
     * fail
     * 
     * @param account
     * @param resourcesToEvaluate
     * @return
     */
    public AccountAdditionStatus updateQuota(Account account, Collection<Resource> resourcesToEvaluate) {
        logger.info("updating quota(s) {} {}", account, resourcesToEvaluate);
        logger.trace("model {}", getLatestActivityModel());

        if (PersistableUtils.isNullOrTransient(account)) {
            throw new TdarRecoverableRuntimeException("accountService.account_is_null");
        }
        /* evaluate resources based on the model, and update their counts of files and space */
        ResourceEvaluator resourceEvaluator = getResourceEvaluator(resourcesToEvaluate);
        saveOrUpdate(resourcesToEvaluate);

        /* make sure the account associations are properly set for each resource in the bunch */
        updateTransientAccountOnResources(resourcesToEvaluate);
        AccountEvaluationHelper helper = new AccountEvaluationHelper(account, getLatestActivityModel());

        /* check if any of the resources have been modified (ie. resource.markUpdated() has been called */
        boolean hasUpdates = updateAccountAssociations(account, resourcesToEvaluate, helper);

        /* update the account info in the database */
        updateAccountInfo(account, resourceEvaluator);
        AccountAdditionStatus status = AccountAdditionStatus.CAN_ADD_RESOURCE;
        // init totals
        account.getTotalNumberOfResources();
        helper.updateFromAccount(account);
        logAccountAndHelperState(account, helper);

        boolean overdrawn = isOverdrawn(account, getResourceEvaluator());
        logger.info("overdrawn: {} hasUpdates: {}", overdrawn, hasUpdates);

        if (!hasUpdates || overdrawn) {
            /*
             * If we don't have anything to update (no resource has been marked as "changed" or the account has been overdrawn, then we need to start from
             * scratch with this account. We set it back to the normal state, and we re-evaluate ALL of the resources in the account
             */
            resourcesToEvaluate = account.getResources();

            // start at 0 and re-add everything
            // sort by date updated
            account.reset();

            for (Coupon coupon : account.getCoupons()) {
                account.setFilesUsed(coupon.getNumberOfFiles() + account.getFilesUsed());
                account.setSpaceUsedInBytes((coupon.getNumberOfMb() * Persistable.ONE_MB) + account.getSpaceUsedInBytes());
            }

            helper = new AccountEvaluationHelper(account, getLatestActivityModel());
            // we have to evaluate everything so we can make sure that the transient boolean is set for deleted materials
            getResourceEvaluator().evaluateResources(resourcesToEvaluate);
            logger.trace("s{} f{} r:{} ", account.getAvailableSpaceInBytes(), account.getAvailableNumberOfFiles(), helper.getUnflagged());
            processResourcesChronologically(helper, resourcesToEvaluate);

            status = updateResourceStatusesAndReconcileAccountStatus(helper, status);
            overdrawn = isOverdrawn(account, getResourceEvaluator());
            logger.info("flagged: {} overdrawn:{}", helper.getFlagged(), overdrawn);
            if (CollectionUtils.isNotEmpty(helper.getFlagged()) || overdrawn) {
                account.setStatus(Status.FLAGGED_ACCOUNT_BALANCE);
                logger.info("marking account as FLAGGED {} {}", overdrawn, helper.getFlagged());
            } else {
                if (account.getStatus().equals(Status.FLAGGED_ACCOUNT_BALANCE)) {
                    account.setStatus(Status.ACTIVE);
                }
            }

            saveOrUpdate(resourcesToEvaluate);
            helper.updateAccount();
            updateAccountInfo(account, getResourceEvaluator());
        } else {
            account.setStatus(Status.ACTIVE);
        }

        saveOrUpdate(account);
        helper = null;
        logger.trace("files used: {} ", account.getFilesUsed());
        logger.trace("files avail: {} ", account.getAvailableNumberOfFiles());
        logger.trace("space used: {} ", account.getSpaceUsedInMb());
        logger.trace("space avail: {} ", account.getAvailableSpaceInMb());
        return status;
    }

    private boolean isOverdrawn(Account account, ResourceEvaluator resourceEvaluator) {
        return canAddResource(account, resourceEvaluator) != AccountAdditionStatus.CAN_ADD_RESOURCE;
    }

    public AccountAdditionStatus canAddResource(Account account, ResourceEvaluator re) {
        if (re.evaluatesNumberOfFiles()) {
            logger.debug("available files {} trying to use {}", account.getAvailableNumberOfFiles(), re.getFilesUsed());
            if ((account.getAvailableNumberOfFiles() - re.getFilesUsed()) < 0) {
                return AccountAdditionStatus.NOT_ENOUGH_FILES;
            }
        }

        if (re.evaluatesNumberOfResources()) {
            logger.debug("available resources {} trying to use {}", account.getAvailableResources(), re.getResourcesUsed());
            if ((account.getAvailableResources() - re.getResourcesUsed()) < 0) {
                return AccountAdditionStatus.NOT_ENOUGH_RESOURCES;
            }
        }

        if (re.evaluatesSpace()) {
            logger.debug("available space {} trying to use {}", account.getAvailableSpaceInBytes(), re.getSpaceUsedInBytes());
            if ((account.getAvailableSpaceInBytes() - re.getSpaceUsedInBytes()) < 0) {
                return AccountAdditionStatus.NOT_ENOUGH_SPACE;
            }
        }
        return AccountAdditionStatus.CAN_ADD_RESOURCE;
    }

    /**
     * Marks a @link Resource @link Status to be FLAGGED_ACCOUNT_BALANCE
     * 
     * @param resources
     */
    private void markResourcesAsFlagged(Collection<Resource> resources) {
        for (Resource resource : resources) {
            if (!getResourceEvaluator().getUncountedResourceStatuses().contains(resource.getStatus())) {
                resource.setStatus(Status.FLAGGED_ACCOUNT_BALANCE);
            }
        }

    }

    /**
     * Set the @link Resource back to whatever the resource.getPreviousStatus() was set to; set to Status.ACTIVE if NULL.
     * 
     * @param resources
     */
    private void unMarkResourcesAsFlagged(Collection<Resource> resources) {
        for (Resource resource : resources) {
            if (resource.getStatus() != Status.FLAGGED_ACCOUNT_BALANCE) {
                continue;
            }
            Status status = resource.getPreviousStatus();
            if (status == null) {
                status = Status.ACTIVE;
            }
            resource.setStatus(status);
        }
    }

    /**
     * Once the @link Account has been reconcilled, update all of the @link Status entires for each @link Resource and report back
     * 
     * @param helper
     * @param status
     * @return
     */
    private AccountAdditionStatus updateResourceStatusesAndReconcileAccountStatus(AccountEvaluationHelper helper, AccountAdditionStatus status) {
        markResourcesAsFlagged(helper.getFlagged());
        unMarkResourcesAsFlagged(helper.getUnflagged());
        logger.info("HELPER FINAL: s:{} f:{} r:{} ", helper.getAvailableSpaceInBytes(), helper.getAvailableNumberOfFiles(), helper.getUnflagged());
        if (helper.getFlagged().size() > 0) {
            if (helper.getAvailableSpaceInBytes() < 0) {
                status = AccountAdditionStatus.NOT_ENOUGH_SPACE;
            } else {
                status = AccountAdditionStatus.NOT_ENOUGH_FILES;
            }
        }
        logger.info("ACCOUNT Status: {} resources: {} FLAGGED: {}", status, helper.getUnflagged(), helper.getFlagged());
        return status;
    }

    /**
     * Process through all @link Resource entries chronologically and identify items that should be marked as Status.FLAGGED_FOR_BILLING
     * 
     * @param helper
     * @param resourcesToEvaluate
     */
    private void processResourcesChronologically(AccountEvaluationHelper helper, Collection<Resource> resourcesToEvaluate) {
        List<Resource> resourceList = new ArrayList<Resource>(resourcesToEvaluate);
        PersistableUtils.sortByCreatedDate(resourceList);
        processResourceGroup(helper, resourceList, Mode.ADD);
    }

    /**
     * For each item, determine if the account has space for it, if so, add it, if not, add it to the flagged list.
     * Wait until the very end to add the weight of the flagged resources to try and get as many resources visible
     * and functional as possible.
     * 
     * @param helper
     * @param items
     * @param mode
     */
    private void processResourceGroup(AccountEvaluationHelper helper, List<Resource> items, Mode mode) {
        boolean seenFlagged = false;
        for (Resource resource : items) {
            if (hasSpaceFor(resource, helper, mode)) {
                helper.getUnflagged().add(resource);
                updateMarkers(resource, helper, mode);
            } else {
                if (!seenFlagged) {
                    logger.info("First Flagged item: {}", resource.getId());
                    seenFlagged = true;
                }
                helper.getFlagged().add(resource);
            }
        }
        for (Resource resource : helper.getFlagged()) {
            updateMarkers(resource, helper, mode);
        }
    }

    enum Mode {
        UPDATE,
        ADD;
    }

    /**
     * Update the account files and space settings. Based on Mode. Mode for a full re-evaulation of the account will be
     * ADD whereby the total space used is evaluated. Otherwise, in UPDATE, the differential change between last save and
     * current is used.
     * 
     * @param resource
     * @param helper
     * @param mode
     */
    private void updateMarkers(Resource resource, AccountEvaluationHelper helper, Mode mode) {
        if (!resource.isCountedInBillingEvaluation()) {
            return;
        }
        Long files = resource.getEffectiveFilesUsed();
        Long space = resource.getEffectiveSpaceUsed();
        if (mode == Mode.ADD) {
            files = resource.getFilesUsed();
            space = resource.getSpaceInBytesUsed();
        }
        logger.trace(String.format(" HELPER: space:%s(%s) files:%s(%s) r:%s", helper.getSpaceUsedInBytes(), space, helper.getFilesUsed(), files,
                resource.getId()));
        helper.setSpaceUsedInBytes(helper.getSpaceUsedInBytes() + space);
        helper.setFilesUsed(helper.getFilesUsed() + files);
    }

    /**
     * Check that an @link Account has space for the @link Resource using the metadata in @link AccountEvaluationHelper
     * 
     * @param resource
     * @param helper
     * @param mode
     * @return
     */
    private boolean hasSpaceFor(Resource resource, AccountEvaluationHelper helper, Mode mode) {
        Long files = resource.getEffectiveFilesUsed();
        Long space = resource.getEffectiveSpaceUsed();
        if (mode == Mode.ADD) {
            files = resource.getFilesUsed();
            space = resource.getSpaceInBytesUsed();
        }

        if ((files == 0) && (space == 0)) {
            return true;
        }
        if (!resource.isCountedInBillingEvaluation()) {
            logger.debug("Skipping {} in eval b/c it's not counted", resource.getId());
            return true;
        }
        logger.trace("mode: {}", mode);
        logger.debug("HELPER: space used: {} avail:{} files used: {} avail: {} ++ space: {} files: {} id: {} ({})",
                helper.getSpaceUsedInBytes(), helper.getAvailableSpaceInBytes(), helper.getFilesUsed(),
                helper.getAvailableNumberOfFiles(), space, files, resource.getId(), resource.getStatus());
        // Trivial changes should fall through and not update because they are no-op in terms of effective changes
        if (helper.getModel().getCountingSpace() && ((helper.getAvailableSpaceInBytes() - space) < 0)) {
            logger.debug("OVERAGE ==> space used:{} space available:{} resourceId:{}", space, helper.getAvailableSpaceInBytes(), resource.getId());
            return false;
        }
        if (helper.getModel().getCountingFiles() && ((helper.getAvailableNumberOfFiles() - files) < 0)) {
            logger.trace("files used:{} files available:{} resourceId:{}", files, helper.getAvailableNumberOfFiles(), resource.getId());
            return false;
        }
        return true;
    }

    /**
     * Update a Collection of @link Resource entries to use the specified @link Account and keep track using the @link AccountEvaluationHelper
     * 
     * @param account
     * @param resourcesToEvaluate
     * @param helper
     * @return
     */
    private boolean updateAccountAssociations(Account account, Collection<Resource> resourcesToEvaluate, AccountEvaluationHelper helper) {
        // Account localAccount = account;
        Set<Account> additionalAccountsToCleanup = new HashSet<>();
        boolean hasUpdates = false;

        // try this without the merge
        merge(account);

        for (Resource resource : resourcesToEvaluate) {
            if (resource == null) {
                continue;
            }

            if (resource.isUpdated()) {
                hasUpdates = true;
            }
            account.getResources().add(resource);

            if (PersistableUtils.isNullOrTransient(resource.getAccount()) || account.getResources().contains(resource)) {
                helper.getNewItems().add(resource);
                continue;
            }

            // if we're dealing with multiple accounts ...
            if (!account.equals(resource.getAccount())) {
                Account oldAccount = resource.getAccount();
                additionalAccountsToCleanup.add(oldAccount);
                oldAccount.getResources().remove(resource);
                helper.getNewItems().add(resource);
                continue;
            }
            helper.getExistingItems().add(resource);
        }

        for (Account old : additionalAccountsToCleanup) {
            updateAccountInfo(old, getResourceEvaluator());
        }
        return hasUpdates;
    }

    /**
     * Log out the account info and the identified change
     * 
     * @param account
     * @param helper
     */
    private void logAccountAndHelperState(Account account, AccountEvaluationHelper helper) {
        Object[] log = { account.getSpaceUsedInBytes(), account.getAvailableSpaceInBytes(), account.getFilesUsed(), account.getAvailableNumberOfFiles() };
        logger.info("ACCOUNT: space used: {} avail:{} files used: {} avail {}", log);
        Object[] log2 = { helper.getSpaceUsedInBytes(), helper.getAvailableSpaceInBytes(), helper.getFilesUsed(), helper.getAvailableNumberOfFiles() };
        logger.info("HELPER: space used: {} avail:{} files used: {} avail {}", log2);
        logger.info("CHANGE: existing:{} new:{}", helper.getExistingItems(), helper.getNewItems());
    }

    public Number findCountOfFlaggedResourcesInAccount(Account account) {
        Criteria criteria = getCriteria(Resource.class).add(Restrictions.eq("status", Status.FLAGGED_ACCOUNT_BALANCE)).setProjection(Projections.rowCount());
        Number result = (Number) criteria.uniqueResult();
        return result;
        // TODO Auto-generated method stub

    }

    /*
     * We always update quotas even if a resource overdraws because it's impossible later to reconcile how much something was overdrawn easily...
     * eg. was it because it was a "new resource" or because it was a new file, or 2k over
     */
    public void updateQuotas(Account account, ResourceEvaluator endingEvaluator, Collection<Resource> list) {
        AccountAdditionStatus status = canAddResource(account, endingEvaluator);
        account.getResources().addAll(list);
        account.setFilesUsed(account.getFilesUsed() + endingEvaluator.getFilesUsed());
        account.setResourcesUsed(account.getResourcesUsed() + endingEvaluator.getResourcesUsed());
        account.setSpaceUsedInBytes(account.getSpaceUsedInBytes() + endingEvaluator.getSpaceUsedInBytes());
        if (status != AccountAdditionStatus.CAN_ADD_RESOURCE) {
            throw new TdarQuotaException("account.overdrawn", status);
        }
    }

    public boolean hasMinimumForNewRecord(Account account, ResourceEvaluator resourceEvaluator, ResourceType type) {
        // init totals
        account.getTotalNumberOfResources();
        return (resourceEvaluator.accountHasMinimumForNewResource(account, type));
    }
}
