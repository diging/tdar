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
import org.hibernate.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.billing.Account;
import org.tdar.core.bean.billing.AccountGroup;
import org.tdar.core.bean.billing.Coupon;
import org.tdar.core.bean.billing.Invoice;
import org.tdar.core.bean.billing.Invoice.TransactionStatus;
import org.tdar.core.bean.billing.ResourceEvaluator;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.resource.Resource;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.exception.TdarRecoverableRuntimeException;

/**
 * $Id$
 * 
 * DAO access for Accounts
 * 
 * @author <a href='Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
@Component
public class AccountDao extends Dao.HibernateBase<Account> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AccountDao() {
        super(Account.class);
    }

    @SuppressWarnings("unchecked")
    public Set<Account> findAccountsForUser(Person user, Status... statuses) {
        if (ArrayUtils.isEmpty(statuses)) {
            statuses = new Status[] { Status.ACTIVE, Status.FLAGGED_ACCOUNT_BALANCE };
        }
        Set<Account> accountGroups = new HashSet<>();
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.ACCOUNTS_FOR_PERSON);
        query.setParameter("personId", user.getId());
        query.setParameterList("statuses", statuses);
        accountGroups.addAll(query.list());
        for (AccountGroup group : findAccountGroupsForUser(user)) {
            accountGroups.addAll(group.getAccounts());
        }
        return accountGroups;
    }

    @SuppressWarnings("unchecked")
    public List<AccountGroup> findAccountGroupsForUser(Person user) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.ACCOUNT_GROUPS_FOR_PERSON);
        query.setParameter("personId", user.getId());
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
        query.setParameterList("ids", Persistable.Base.extractIds(resourcesToEvaluate));
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<Long> findResourcesWithNullAccount(List<Resource> resourcesToEvaluate) {
        Query query = getCurrentSession().getNamedQuery(TdarNamedQueries.RESOURCES_WITH_NULL_ACCOUNT_ID);
        query.setParameterList("ids", Persistable.Base.extractIds(resourcesToEvaluate));
        return query.list();
    }

    public void updateTransientAccountOnResources(Collection<Resource> resourcesToEvaluate) {
        Map<Long, Resource> resourceIdMap = Persistable.Base.createIdMap(resourcesToEvaluate);
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
        if (Persistable.Base.isNotNullOrTransient(user)) {
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

}
