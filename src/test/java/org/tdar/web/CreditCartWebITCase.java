package org.tdar.web;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.tdar.URLConstants;
import org.tdar.core.bean.billing.BillingActivity;
import org.tdar.core.bean.billing.Invoice.TransactionStatus;
import org.tdar.core.service.AccountService;
import org.tdar.junit.MultipleTdarConfigurationRunner;
import org.tdar.junit.RunWithTdarConfiguration;
import org.tdar.utils.MessageHelper;
import org.tdar.utils.TestConfiguration;

import com.gargoylesoftware.htmlunit.html.HtmlElement;

@RunWith(MultipleTdarConfigurationRunner.class)
@RunWithTdarConfiguration(runWith = { RunWithTdarConfiguration.CREDIT_CARD })
public class CreditCartWebITCase extends AbstractWebTestCase {

    private static final TestConfiguration CFG = TestConfiguration.getInstance();
    private static final String CART_NEW = "/cart/new";
    @Autowired
    AccountService accountService;

    public Long getItemId(String name) {
        for (BillingActivity activity : accountService.getActiveBillingActivities()) {
            if (activity.getName().equalsIgnoreCase(name)) {
                logger.info("{} {} ", activity.getName(), activity.getId());
                return activity.getId();
            }
        }
        return -1L;
    }

    @Test
    public void testCartIncomplete() throws MalformedURLException {
        gotoPage(CART_NEW);
        setInput("invoice.numberOfMb", "0");
        setInput("invoice.numberOfFiles", "0");
        submitForm();
        assertCurrentUrlContains("process-choice");
        assertTextPresentInCode("55 USD");
        assertTextPresentInCode(MessageHelper.getMessage("invoiceService.specify_something"));
    }

    @Test
    public void testCartFilesNoMB() throws MalformedURLException {
        gotoPage(CART_NEW);
        setInput("invoice.numberOfMb", "0");
        setInput("invoice.numberOfFiles", "100");
        submitForm();
        assertTextPresent("50-500:100:$31:$3,100");
        assertTextPresent("total:$3,100");
        loginAndSpecifyCC();
        testAccountPollingResponse("310000", TransactionStatus.TRANSACTION_SUCCESSFUL);

    }

    private void loginAndSpecifyCC() {
        if (getPageText().contains("Log In")) {
            clickLinkOnPage("Log In");
            logger.debug(getCurrentUrlPath());
            completeLoginForm(CFG.getUsername(), CFG.getPassword(), false);
            gotoPage("/cart/review");
        }
    }

    @Test
    public void testCartMBNoFiles() throws MalformedURLException {
        gotoPage(CART_NEW);
        setInput("invoice.numberOfMb", "100");
        setInput("invoice.numberOfFiles", "0");
        submitForm();
        assertTextPresent("100 mb:1:$50:$50");
        assertTextPresent("total:$50");
        loginAndSpecifyCC();
        testAccountPollingResponse("5000", TransactionStatus.TRANSACTION_SUCCESSFUL);
    }

    @Test
    public void testCartSuccess() throws MalformedURLException {
        gotoPage(CART_NEW);
        setInput("invoice.numberOfMb", "2000");
        setInput("invoice.numberOfFiles", "10");
        submitForm();

        assertTextPresent("100 mb:19:$50:$950");
        assertTextPresent("5- 19:10:$40:$400");
        assertTextPresent("total:$1,350");
        loginAndSpecifyCC();
        testAccountPollingResponse("135000", TransactionStatus.TRANSACTION_SUCCESSFUL);
    }

    @Test
    public void testCartWithAccount() throws MalformedURLException {
        gotoPage(CART_NEW);
        setInput("invoice.numberOfMb", "2000");
        setInput("invoice.numberOfFiles", "10");
        submitForm();
        loginAndSpecifyCC();
        String invoiceId = testAccountPollingResponse("135000", TransactionStatus.TRANSACTION_SUCCESSFUL);
        String accountId = addInvoiceToNewAccount(invoiceId, null, null);
        assertTrue(accountId != "-1");
    }

    @Test
    public void testAddCartToSameAccount() throws MalformedURLException {
        gotoPage(CART_NEW);
        setInput("invoice.numberOfMb", "2000");
        setInput("invoice.numberOfFiles", "10");
        submitForm();
        loginAndSpecifyCC();
        String accountId = testAccountPollingResponse("135000", TransactionStatus.TRANSACTION_SUCCESSFUL);
        assertTrue(accountId != "-1");
        gotoPage(CART_NEW);
        setInput("invoice.numberOfMb", "10000");
        setInput("invoice.numberOfFiles", "12");
        submitForm();
        String invoiceId2 = testAccountPollingResponse("543000", TransactionStatus.TRANSACTION_SUCCESSFUL, false);
//        assertEquals(account, accountId);
        assertTextPresent("10,020");
        assertTextPresent("2,000");
        assertTextPresent("10");
        assertTextPresent("12");
        assertTextPresent("$5,430");
        assertTextPresent("$1,350");
        logger.trace(getPageText());

    }

    @Test
    public void testAddPaymentsToMultipleAccount() throws MalformedURLException {
        gotoPage(CART_NEW);
        setInput("invoice.numberOfMb", "2000");
        setInput("invoice.numberOfFiles", "10");
        submitForm();
        loginAndSpecifyCC();
        String invoiceId = testAccountPollingResponse("135000", TransactionStatus.TRANSACTION_SUCCESSFUL);
        String accountName = "test account 1";
        String accountId = addInvoiceToNewAccount(invoiceId, null, accountName);
        assertTrue(accountId != "-1");
        gotoPage(CART_NEW);
        setInput("invoice.numberOfMb", "10000");
        setInput("invoice.numberOfFiles", "12");
        submitForm();
        String invoiceId2 = testAccountPollingResponse("543000", TransactionStatus.TRANSACTION_SUCCESSFUL);
        String accountName2 = "test account 2";
        String account = addInvoiceToNewAccount(invoiceId2, null, accountName2);
        assertTextPresent(accountName2);
        assertTextNotPresent(accountName);
        assertNotEquals(account, accountId);
        gotoPage(URLConstants.DASHBOARD);
        assertTextPresent(accountName);
        assertTextPresent(accountName2);
        logger.trace(getPageText());

    }

    @Test
    public void testCartError() throws MalformedURLException {
        login(CFG.getAdminUsername(), CFG.getAdminPassword());
        gotoPage(CART_NEW);
        setInput("invoice.numberOfMb", "2000");
        setInput("invoice.numberOfFiles", "10");
        setExtraItem("error", "1");

        submitForm();

        assertTextPresent("100 mb:19:$50:$950");
        assertTextPresent("5- 19:10:$40:$400");
        assertTextPresent("total:$1,405.21");
        testAccountPollingResponse("140521", TransactionStatus.TRANSACTION_FAILED);
    }

    @Test
    public void testCartUnknown() throws MalformedURLException {
        login(CFG.getAdminUsername(), CFG.getAdminPassword());
        gotoPage(CART_NEW);
        setInput("invoice.numberOfMb", "2000");
        setInput("invoice.numberOfFiles", "10");
        setExtraItem("unknown", "1");

        submitForm();

        assertTextPresent("100 mb:19:$50:$950");
        assertTextPresent("5- 19:10:$40:$400");
        assertTextPresent("total:$1,405.31");
//        loginAndSpecifyCC();
        testAccountPollingResponse("140531", TransactionStatus.TRANSACTION_FAILED);
    }

    private void setExtraItem(String name, String val) {
        for (int i = 0; i < 100; i++) {
            try {
                HtmlElement input = getInput(String.format("extraItemIds[%s]", i));
                if (input != null) {
                    String string = getItemId(name).toString();
                    logger.info(" {}|{} ", input.getAttribute("value"), string);
                    if (input.getAttribute("value").equals(string)) {
                        setInput(String.format("extraItemQuantities[%s]", i), val);
                        logger.info("setting value {} {}", input.toString(), i);
                    }
                }
            } catch (NoSuchElementException e) {
                logger.warn("{}", e.getMessage());
            } catch (Exception e) {
                logger.warn("exception:", e);
            }
        }
    }

    @Test
    public void testCartDecline() throws MalformedURLException {
        login(CFG.getAdminUsername(), CFG.getAdminPassword());
        gotoPage(CART_NEW);
        setInput("invoice.numberOfMb", "2000");
        setInput("invoice.numberOfFiles", "10");

        setExtraItem("decline", "1");

        submitForm();

        assertTextPresent("100 mb:19:$50:$950");
        assertTextPresent("5- 19:10:$40:$400");
        assertTextPresent("total:$1,405.11");
        testAccountPollingResponse("140511", TransactionStatus.TRANSACTION_FAILED);
    }

}
