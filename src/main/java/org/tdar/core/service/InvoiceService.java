package org.tdar.core.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.Persistable.Base;
import org.tdar.core.bean.billing.Account;
import org.tdar.core.bean.billing.BillingActivity;
import org.tdar.core.bean.billing.BillingActivity.BillingActivityType;
import org.tdar.core.bean.billing.BillingActivityModel;
import org.tdar.core.bean.billing.BillingItem;
import org.tdar.core.bean.billing.BillingTransactionLog;
import org.tdar.core.bean.billing.Coupon;
import org.tdar.core.bean.billing.Invoice;
import org.tdar.core.bean.billing.Invoice.TransactionStatus;
import org.tdar.core.bean.entity.Address;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.bean.util.Email;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.AccountDao;
import org.tdar.core.dao.GenericDao;
import org.tdar.core.dao.external.payment.PaymentMethod;
import org.tdar.core.dao.external.payment.nelnet.PaymentTransactionProcessor;
import org.tdar.core.dao.external.payment.nelnet.TransactionResponse;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.core.service.external.EmailService;
import org.tdar.struts.data.PricingOption;
import org.tdar.struts.data.PricingOption.PricingType;
import org.tdar.utils.MessageHelper;

@Transactional(readOnly = true)
@Service
public class InvoiceService extends ServiceInterface.TypedDaoBase<Account, AccountDao> {

    private final transient Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private GenericDao genericDao;

    @Autowired
    private AuthorizationService authenticationAndAuthorizationService;
    @Autowired
    private transient XmlService xmlService;

    @Autowired
    private transient EmailService emailService;

    /**
     * Find the account (if exists) associated with the invoice
     * 
     * @param invoice
     * @return
     */
    public Account getAccountForInvoice(Invoice invoice) {
        return getDao().getAccountForInvoice(invoice);
    }

    /**
     * Find all accounts for user: return accounts that are active and have not met their quota
     * 
     * @param user
     * @param statuses
     * @return
     */
    public Set<Account> listAvailableAccountsForUser(Person user, Status... statuses) {
        if (Persistable.Base.isNullOrTransient(user)) {
            return Collections.emptySet();
        }
        return getDao().findAccountsForUser(user, statuses);
    }

    /**
     * Find all accounts for user: return accounts that are active and have not met their quota
     * 
     * @param user
     * @return
     */
    public List<Invoice> listUnassignedInvoicesForUser(Person user) {
        if (Persistable.Base.isNullOrTransient(user)) {
            return Collections.emptyList();
        }
        return getDao().findUnassignedInvoicesForUser(user);
    }

    /**
     * Return defined @link BillingActivity entries that are enabled. A billing activity represents a type of charge (uses ASU Verbage)
     * 
     * @return
     */
    public List<BillingActivity> getActiveBillingActivities() {
        List<BillingActivity> toReturn = new ArrayList<BillingActivity>();
        for (BillingActivity activity : genericDao.findAll(BillingActivity.class)) {
            if (activity.getEnabled()) {
                toReturn.add(activity);
            }
        }
        Collections.sort(toReturn);
        logger.trace("{}", toReturn);
        return toReturn;

    }

    /**
     * We know that we will change pricing from time to time, so, a @link BillingActivityModel allows us to represent different models at the same time. Return
     * the current model.
     * 
     * @return
     */
    public BillingActivityModel getLatestActivityModel() {
        List<BillingActivityModel> findAll = getDao().findAll(BillingActivityModel.class);
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
     * Iterate through all active @link BillingActivity entries and find the first that is only MB.
     * 
     * @return
     */
    public BillingActivity getSpaceActivity() {
        for (BillingActivity activity : getActiveBillingActivities()) {
            if (activity.getActivityType() == BillingActivityType.TEST) {
                continue;
            }
            if (((activity.getNumberOfFiles() == null) || (activity.getNumberOfFiles() == 0))
                    && ((activity.getNumberOfResources() == null) || (activity.getNumberOfResources() == 0)) && (activity.getNumberOfMb() != null)
                    && (activity.getNumberOfMb() > 0)) {
                return activity;
            }
        }
        return null;
    }

    /**
     * As we can bill by different things, # of Files, # of MB, we want to find out what the cheapest method is by # of files either using exact entries in
     * tiers, or even jumping to the next tier which may be cheaper.
     * 
     * @param numFiles_
     * @param numMb_
     * @param exact
     * @return
     */
    public PricingOption getCheapestActivityByFiles(Long numFiles_, Long numMb_, boolean exact) {
        Long numFiles = 0L;
        Long numMb = 0L;
        if (numFiles_ != null) {
            numFiles = numFiles_;
        }
        if (numMb_ != null) {
            numMb = numMb_;
        }

        if ((numFiles == 0) && (numMb == 0)) {
            return null;
        }

        PricingOption option = new PricingOption(PricingType.SIZED_BY_FILE_ONLY);
        if (!exact) {
            option.setType(PricingType.SIZED_BY_FILE_ABOVE_TIER);
        }
        List<BillingItem> items = new ArrayList<BillingItem>();
        logger.info("files: {} mb: {}", numFiles, numMb);

        for (BillingActivity activity : getActiveBillingActivities()) {
            int calculatedNumberOfFiles = numFiles.intValue(); // Don't use test activities or activities that are Just about MB
            if ((activity.getActivityType() == BillingActivityType.TEST) || !activity.supportsFileLimit()) {
                continue;
            }
            logger.trace("n:{} min:{}", numFiles, activity.getMinAllowedNumberOfFiles());
            if (exact && (numFiles < activity.getMinAllowedNumberOfFiles())) {
                continue;
            }

            // 2 cases (1) exact value; (2) where the next step up might actually be cheaper
            if (!exact && (activity.getMinAllowedNumberOfFiles() >= numFiles)) {
                calculatedNumberOfFiles = activity.getMinAllowedNumberOfFiles().intValue();
            }

            BillingItem e = new BillingItem(activity, calculatedNumberOfFiles);
            logger.trace(" -- {} ({})", e.getActivity().getName(), e);
            items.add(e);
        }
        logger.trace("{} {}", option, items);
        // finding the cheapest
        BillingItem lowest = null;
        for (BillingItem item : items) {
            if (lowest == null) {
                lowest = item;
            } else if (lowest.getSubtotal() > item.getSubtotal()) {
                lowest = item;
            } else if (lowest.getSubtotal().equals(item.getSubtotal())) {
                /*
                 * FIXME: if two items have the SAME price, but one has more "stuff" we should choose the one with more "stuff"
                 * Caution: there are many corner cases whereby we may not know about how to determine what's the better pricing decision when we're providing a
                 * per-file and per-mb price. eg: 8 files and 200 MB or 10 files and 150? There's no way to choose.
                 */
                logger.info("{} =??= {} ", lowest, item);
            }
        }
        option.getItems().add(lowest);
        BillingActivity spaceActivity = getSpaceActivity();
        if (lowest == null) {
            logger.warn("no options found for f:{} m:{} ", numFiles, numMb);
            return null;
        }
        Long spaceAvailable = lowest.getQuantity() * lowest.getActivity().getNumberOfMb();
        Long spaceNeeded = numMb - spaceAvailable;
        logger.info("adtl. space needed: {} avail: {} ", spaceNeeded, spaceAvailable);
        logger.info("space act: {} ", getSpaceActivity());
        calculateSpaceActivity(option, spaceActivity, spaceNeeded);

        if ((option.getTotalMb() < numMb) || (option.getTotalFiles() < numFiles)) {
            return null;
        }

        return option;
    }

    /**
     * Calculate rate purely on space
     * 
     * @param option
     * @param spaceActivity
     * @param spaceNeeded
     */
    public void calculateSpaceActivity(PricingOption option, BillingActivity spaceActivity, Long spaceNeeded) {
        BillingItem extraSpace;
        if ((spaceNeeded > 0) && (spaceActivity != null)) {
            int qty = (int) Base.divideByRoundUp(spaceNeeded, spaceActivity.getNumberOfMb());
            extraSpace = new BillingItem(spaceActivity, qty);
            option.getItems().add(extraSpace);
        }
    }

    /**
     * As we can bill by different things, # of Files, # of MB, we want to find out what the cheapest method by MB.
     * 
     * @param numFiles_
     * @param spaceInMb_
     * @return
     */
    public PricingOption getCheapestActivityBySpace(Long numFiles_, Long spaceInMb_) {
        Long numFiles = 0L;
        Long spaceInMb = 0L;
        if (numFiles_ != null) {
            numFiles = numFiles_;
        }
        if (spaceInMb_ != null) {
            spaceInMb = spaceInMb_;
        }

        if ((numFiles.longValue() == 0L) && (spaceInMb.longValue() == 0L)) {
            return null;
        }

        PricingOption option = new PricingOption(PricingType.SIZED_BY_MB);
        List<BillingItem> items = new ArrayList<BillingItem>();
        BillingActivity spaceActivity = getSpaceActivity();
        if ((spaceActivity != null) && ((numFiles == null) || (numFiles.intValue() == 0))) {
            calculateSpaceActivity(option, spaceActivity, spaceInMb);
            return option;
        }

        for (BillingActivity activity : getActiveBillingActivities()) {
            if (activity.getActivityType() == BillingActivityType.TEST) {
                continue;
            }

            if (activity.supportsFileLimit()) {
                Long total = Base.divideByRoundUp(spaceInMb, activity.getNumberOfMb());
                Long minAllowedNumberOfFiles = activity.getMinAllowedNumberOfFiles();
                if (minAllowedNumberOfFiles == null) {
                    minAllowedNumberOfFiles = 0L;
                }

                if ((total * activity.getNumberOfFiles()) < minAllowedNumberOfFiles) {
                    total = minAllowedNumberOfFiles;
                }

                if (total < (numFiles / activity.getNumberOfFiles())) {
                    total = numFiles;
                }
                items.add(new BillingItem(activity, total.intValue()));
            }
        }
        BillingItem lowest = null;
        for (BillingItem item : items) {
            if (lowest == null) {
                lowest = item;
            } else if (lowest.getSubtotal() > item.getSubtotal()) {
                lowest = item;
            }
        }
        option.getItems().add(lowest);
        if ((option == null) || (option.getTotalMb() < spaceInMb) || (option.getTotalFiles() < numFiles)) {
            return null;
        }

        return option;
    }

    /**
     * Given an @link Invoice calculate the cheapeset Pricing Option
     * 
     * @param invoice
     * @return
     */
    public PricingOption calculateCheapestActivities(Invoice invoice) {
        PricingOption lowestByMB = getCheapestActivityBySpace(invoice.getNumberOfFiles(), invoice.getNumberOfMb());
        PricingOption lowestByFiles = getCheapestActivityByFiles(invoice.getNumberOfFiles(), invoice.getNumberOfMb(), false);

        // If we are using the ok amount of space for that activity...
        if (lowestByFiles != null) {
            logger.info("lowest by files: {}", lowestByFiles.getSubtotal());
        }
        if (lowestByMB != null) {
            logger.info("lowest by space: {} ", lowestByMB.getSubtotal());
        }
        if ((lowestByMB == null) || ((lowestByFiles != null) && (lowestByFiles.getSubtotal() < lowestByMB.getSubtotal()))) {
            return lowestByFiles;
        }
        return lowestByMB;
    }

    /**
     * Calculate @link PricingOption based on @link PricingType calculation
     * 
     * @param invoice
     * @param pricingType
     * @return
     */
    public PricingOption calculateActivities(Invoice invoice, PricingType pricingType) {
        switch (pricingType) {
            case SIZED_BY_FILE_ABOVE_TIER:
                return getCheapestActivityBySpace(invoice.getNumberOfFiles(), invoice.getNumberOfMb());
            case SIZED_BY_FILE_ONLY:
                return getCheapestActivityByFiles(invoice.getNumberOfFiles(), invoice.getNumberOfMb(), false);
            case SIZED_BY_MB:
                return getCheapestActivityBySpace(invoice.getNumberOfFiles(), invoice.getNumberOfMb());
        }
        return null;
    }

    /**
     * Confirm that @link Coupon can be used (Not assigned, not expired)
     * 
     * @param coupon
     * @param invoice
     */
    @Transactional
    public void checkCouponStillValidForCheckout(Coupon coupon, Invoice invoice) {
        getDao().checkCouponStillValidForCheckout(coupon, invoice);
    }

    /**
     * Apply a @link Coupon to a @link Invoice, if the coupon is for more than an invoice, then we bump the cost of the invoice to match the value of the coupon
     * code
     * 
     * @param persistable
     * @param user
     * @param code
     */
    @Transactional
    public void redeemCode(Invoice invoice, TdarUser user, String code) {
        if (StringUtils.isEmpty(code)) {
            return;
        }
        // find and validate the coupon
        Coupon coupon = locateRedeemableCoupon(code, user);
        if (coupon == null) {
            throw new TdarRecoverableRuntimeException("invoiceService.cannot_redeem_coupon");
        }
        if (Persistable.Base.isNotNullOrTransient(invoice.getCoupon())) {
            if (Persistable.Base.isEqual(coupon, invoice.getCoupon())) {
                return;
            } else {
                throw new TdarRecoverableRuntimeException("invoiceService.coupon_already_applied");
            }
        }
        if (coupon.getDateExpires().before(new Date())) {
            throw new TdarRecoverableRuntimeException("invoiceService.coupon_has_expired");
        }

        // assign the coupon to invoice, and 'redeem' it
        invoice.setCoupon(coupon);
        coupon.setUser(user);
        coupon.setDateRedeemed(new Date());

        // make sure the invoice cancels out the coupon by using all of it, i.e. if the invoice is for 1 file, but the coupon
        // is for 5, the invoice will be changed to be for 5, as we can't break up coupons.

        Long files = invoice.getNumberOfFiles();
        Long mb = invoice.getNumberOfMb();
        if ((files == null) || (coupon.getNumberOfFiles() > files.longValue())) {
            invoice.setNumberOfFiles(coupon.getNumberOfFiles());
        }
        if ((mb == null) || (coupon.getNumberOfMb() > mb.longValue())) {
            invoice.setNumberOfMb(coupon.getNumberOfMb());
        }

        getDao().saveOrUpdate(coupon);
    }

    /**
     * Find the @link Coupon based on the String code.
     * 
     * @param code
     * @param user
     * @return
     */
    @Transactional(readOnly = true)
    public Coupon locateRedeemableCoupon(String code, TdarUser user) {
        logger.debug("locate coupon: {} for: {} ", code, user);
        if (StringUtils.isBlank(code)) {
            return null;
        }
        return getDao().findCoupon(code, user);
    }

    /**
     * Takes the controller specified list of Extra BillingItem(s) and quantities listed by ID and returns
     * 
     * @param extraItemIds
     * @param extraItemQuantities
     * @return
     */
    @Transactional
    public Collection<BillingItem> lookupExtraBillingActivities(List<Long> extraItemIds, List<Integer> extraItemQuantities) {
        Map<Long, BillingActivity> actIdMap = Persistable.Base.createIdMap(getActiveBillingActivities());
        Set<BillingItem> items = new HashSet<>();
        for (int i = 0; i < extraItemIds.size(); i++) {
            BillingActivity act = actIdMap.get(extraItemIds.get(i));
            Integer quantity = extraItemQuantities.get(i);
            getLogger().trace("{} {} {}", extraItemIds.get(i), act, quantity);
            if ((act == null) || (quantity == null) || (quantity < 1)) {
                continue;
            }
            items.add(new BillingItem(act, quantity));
        }
        return items;

    }

    /**
     * For a given invoice object, clear it and apply the appropriate BillingItems based on:
     * 1) the PricingType evaluation (files, MB, etc)
     * 2) any extra BillingItem(s) specfied by admins or for testing
     * 3) a Coupon Code, if specified
     * 
     * @param invoice
     * @param authenticatedUser
     * @param code
     * @param extraItems
     * @param pricingType
     * @param accountId
     * @return
     */
    @Transactional(readOnly = false)
    public Invoice processInvoice(Invoice invoice, TdarUser authenticatedUser, String code, Collection<BillingItem> extraItems, PricingType pricingType,
            Long accountId) {
        boolean billingManager = authenticationAndAuthorizationService.isBillingManager(authenticatedUser);
        if (!invoice.hasValidValue() && StringUtils.isBlank(code) && !billingManager) {
            throw new TdarRecoverableRuntimeException("invoiceService.specify_something");
        }

        // setup BillingItem(s) for Invoice
        invoice.getItems().clear();
        invoice.getItems().addAll(extraItems);

        // redeem coupon code, we do this before calculating costs because the redemption may change the files and space
        redeemCode(invoice, invoice.getOwner(), code);
        List<BillingItem> items = new ArrayList<BillingItem>();
        if (pricingType != null) {
            items = calculateActivities(invoice, pricingType).getItems();
        } else {
            PricingOption activities2 = calculateCheapestActivities(invoice);
            if (activities2 != null) {
                items = activities2.getItems();
            }
        }
        if (CollectionUtils.isNotEmpty(items)) {
            invoice.getItems().addAll(items);
        }

        // if somehow we have absolutely nothing in the invoice, error out
        if (CollectionUtils.isEmpty(invoice.getItems())) {
            throw new TdarRecoverableRuntimeException("cartController.no_items_found");
        }

        invoice.setTransactedBy(authenticatedUser);

        // reconcile the Invoice owner, which could be different if you're an admin and running an invoice for someone else
        updateInvoiceOwner(invoice, authenticatedUser, billingManager);
        invoice.markUpdated(authenticatedUser);

        // if invoice is persisted it will be read-only, so make it writable
        if (Persistable.Base.isNotNullOrTransient(invoice)) {
            genericDao.markUpdatable(invoice);
            genericDao.markUpdatable(invoice.getItems());
        }
        getDao().saveOrUpdate(invoice);
        if (Persistable.Base.isNotNullOrTransient(accountId)) {
            Account account = genericDao.find(Account.class, accountId);
            account.getInvoices().add(invoice);
        }

        return invoice;
    }

    /**
     * reconcile the Invoice owner, which could be different if you're an admin and running an invoice for someone else
     * 
     * @param invoice
     * @param authenticatedUser
     * @param billingManager
     */
    @Transactional(readOnly = false)
    private void updateInvoiceOwner(Invoice invoice, TdarUser authenticatedUser, boolean billingManager) {
        TdarUser owner = invoice.getOwner();
        // if we have an owner
        if (billingManager && Persistable.Base.isNotNullOrTransient(owner)) {
            invoice.setOwner(getDao().find(TdarUser.class, owner.getId()));
        } else {
            // if we're logged in
            if (authenticatedUser != null) {
                invoice.setOwner(authenticatedUser);
            } else {
                // just in case, clear it as we may have a transient instance id=-1
                invoice.setOwner(null);
            }
        }
    }

    /**
     * Finalizes a payment -- given an incoming transient invoice the service will find the existing invoice, and:
     *  (a) update the Invoice with any payment info that it needs such as method, or reason
     *  (b) mark it as final, so it cannot be modified
     *  (c) confirm that the coupon is still valid
     *  (d) change the transaction status:
     *      1) if we have a coupon and that coupon is for the entire amount, complete transaciton
     *      2) set status to PENDING_TRANSACTION
     * 
     * @param invoice_
     * @param paymentMethod
     * @return
     */
    @Transactional(readOnly = false)
    public Invoice finalizePayment(Invoice invoice_, PaymentMethod paymentMethod) {

        Address address = getDao().loadFromSparseEntity(invoice_.getAddress(), Address.class);
        if (Persistable.Base.isNotNullOrTransient(address)) {
            invoice_.setAddress(address);
        }

        String otherReason = invoice_.getOtherReason();
        Invoice invoice = getDao().loadFromSparseEntity(invoice_, Invoice.class);
        invoice.setPaymentMethod(paymentMethod);
        invoice.setOtherReason(otherReason);
        // finalize the cost and cache it
        invoice.markFinal();
        getDao().saveOrUpdate(invoice);
        getLogger().info("USER: {} IS PROCESSING TRANSACTION FOR: {} ", invoice_.getId(), invoice_.getTotal());

        // if the discount brings the total cost down to 0, then skip the credit card process
        if ((invoice.getTotal() <= 0) && CollectionUtils.isNotEmpty(invoice.getItems())) {
            if (Persistable.Base.isNotNullOrTransient(invoice.getCoupon())) {
                // accountService.redeemCode(invoice, invoice.getOwner(), invoice.getCoupon().getCode());
                checkCouponStillValidForCheckout(invoice.getCoupon(), invoice);
            }
            invoice.setTransactionStatus(TransactionStatus.TRANSACTION_SUCCESSFUL);
            getDao().saveOrUpdate(invoice);
        } else {
            invoice.setTransactionStatus(TransactionStatus.PENDING_TRANSACTION);
        }

        return invoice;
    }

    /**
     * Processes the results of a NelNet transaction, first validating the response hash, logging the response, and then updating the invoice with all of the 
     * information available, and finally sends an email.
     * @param response
     * @param paymentTransactionProcessor
     * @throws IOException
     */
    @Transactional(readOnly = false)
    public void processTransactionResponse(TransactionResponse response, PaymentTransactionProcessor paymentTransactionProcessor) throws IOException {
        if (paymentTransactionProcessor.validateResponse(response)) {
            getDao().markWritable();
            Invoice invoice = paymentTransactionProcessor.locateInvoice(response);

            BillingTransactionLog billingResponse = new BillingTransactionLog(xmlService.convertToJson(response), response.getTransactionId());
            billingResponse = getDao().markWritable(billingResponse);
            getDao().saveOrUpdate(billingResponse);
            if (invoice != null) {
                // if invoice has an address this will throw an exception if it is the same as one of the person adresses. (cascaded merge introduces transient
                // item in p.addresses)
                invoice = getDao().markWritable(invoice);
                updateAddresses(response, invoice);
                paymentTransactionProcessor.updateInvoiceFromResponse(response, invoice);
                invoice.setResponse(billingResponse);
                getLogger().info("processing payment response: {}  -> {} ", invoice, invoice.getTransactionStatus());
                sendSuccessEmail(invoice);
                getDao().saveOrUpdate(invoice);
            }
        }
    }

    /**
     * Sends a email to the user when the transaction is successful
     * @param invoice
     */
    private void sendSuccessEmail(Invoice invoice) {
        Map<String, Object> map = new HashMap<>();
        map.put("invoice", invoice);
        map.put("date", new Date());
        try {
            List<Person> people = new ArrayList<>();
            for (String email : StringUtils.split(TdarConfiguration.getInstance().getBillingAdminEmail(), ";")) {
                if (StringUtils.isBlank(email)) {
                    continue;
                }
                Person person = new Person("Billing", "Info", email.trim());
                getDao().markReadOnly(person);
                people.add(person);
            }
            Email email = new Email();
            email.setSubject(MessageHelper.getMessage("cartController.subject"));
            for (Person person : people) {
                email.addToAddress(person.getEmail());
            }
            emailService.queueWithFreemarkerTemplate("transaction-complete-admin.ftl", map, email);
        } catch (Exception e) {
            getLogger().error("could not send email: {} ", e);
        }
    }

    /**
     * updates a person's address to include the billing address from the response
     * @param response
     * @param invoice
     */
    private void updateAddresses(TransactionResponse response, Invoice invoice) {
        TdarUser p = invoice.getOwner();
        boolean found = false;
        Address addressToSave = response.getAddress();
        for (Address address : p.getAddresses()) {
            if (address.isSameAs(addressToSave)) {
                found = true;
            }
        }

        // if user provided an address to nelnet, add that address to user's list of addresses
        // fixme: This is sketchy behavior. Just because I gave the payment processor my billing address does not imply I want to give it to tDAR.
        if (!found) {
            p.getAddresses().add(addressToSave);
            getLogger().info(addressToSave.getAddressSingleLine());
            // this will always fail(you can't save a transient addresss directly because it is a child relation of person). It's also unnecessary: If p
            // is on the session hibernate will save the address.
            // getDao().saveOrUpdate(addressToSave);
            invoice.setAddress(addressToSave);
        }
    }

}
