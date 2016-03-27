package org.tdar.functional;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.tdar.URLConstants.CART_ADD;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdar.URLConstants;
import org.tdar.core.service.external.auth.UserRegistration;
import org.tdar.functional.util.ByLabelText;
import org.tdar.functional.util.WebElementSelection;
import org.tdar.utils.Pair;

import com.opensymphony.xwork2.interceptor.annotations.After;

/**
 * Created by jimdevos on 6/25/14.
 */
public class CartSeleniumWebITCase extends AbstractSeleniumWebITCase {

    Logger logger = LoggerFactory.getLogger(getClass());

    // handle of window created at beginning of test
    String startWindow = null;

    @Before
    public void cartTestBefore() {
        startWindow = getDriver().getWindowHandle();
        force1024x768();
    }

    @After
    public void cleanup() {
        resetSize();
    }

    @Test
    // ideal walk-through of purchase process for a visitor with no mistakes along the way.
    public void testVisitorPurchase() throws InterruptedException {
        // start at the cart page, and click one of the suggested packages
        gotoPage(CART_ADD);
        waitForPageload();
        logger.debug(getText());
        assertLoggedOut();
        selectPackage();
        
        // now we are on the review form (w/ registration/login forms)
        // fill out required user registration fields and submit form
        assertThat(getCurrentUrl(), endsWith(URLConstants.CART_REVIEW_UNAUTHENTICATED));
        UserRegistration reg = createUserRegistration("bob");
        fillOutRegistration(reg);
        // wait for spam check
        Thread.sleep(5000);
        submitForm("#registrationForm .submitButton");

        // now we are on the "choose billing account" page. just click through to next page
        waitForPageload();
        assertThat(getCurrentUrl(), endsWith(URLConstants.CART_REVIEW_PURCHASE));

        submitForm();

        // now we are on the process payment page. click on the button to fire up a new window
        assertThat(getCurrentUrl(), endsWith(URLConstants.CART_PROCESS_PAYMENT_REQUEST));
        find("#btnOpenPaymentWindow").click();

        // sanity check: assert that selenium didn't implicitly switch to popup window (this might be a osx-only thing)
        assertThat(startWindow, equalTo(getDriver().getWindowHandle()));

        switchToNextWindow();
        // popup window is active now. assuming it is the fake payment processor, all we need to do is submit the form to "pay" for the invoice
        waitFor("[type=submit]");
        submitForm();

        // close the popup window

        // even though the popup window is gone, we still need to switch back to the main window
        getDriver().switchTo().window(startWindow);

        // if successful, we are sent to the dashboard
        waitFor("body.dashboard");
        switchToNextWindow();
        find("#btnCloseWindow").click();
        assertThat("nelnet window should be closed / only one window remains", getDriver().getWindowHandles().size(), equalTo(1));
    }

    @Test
    // ideal walkthrough of purchase process for logged-out-user process with no mistakes
    // todo: create By.buttonWithLabel (finds submit input with matching value -or- button with matching text node)
    // todo: create By.inputWithLabel (finds element referred by for-attribute or child elements)
    public void testLoginPurchase() {
        // Starting page
        // go to the cart page and make sure we are logged out
        gotoPage(CART_ADD);
        waitForPageload();
        logger.debug(getText());
        assertLoggedOut();
        selectPackage();

        // review (note that we navigated here via javascript click handler instead of a link or a form submit, so we need to explicitly wait for pageload)
        waitForPageload();
        assertThat(getCurrentUrl(), endsWith(URLConstants.CART_REVIEW_UNAUTHENTICATED));
        find("#loginUsername").val(CONFIG.getUsername());
        find("#loginPassword").val(CONFIG.getPassword());
        submitForm("#loginForm [type=submit]");

        // choose
        assertThat(getCurrentUrl(), endsWith(URLConstants.CART_REVIEW_PURCHASE));
        // we aren't testing billing account customization, so we just advance to the next step
        submitForm();

        // process payment
        assertThat(getCurrentUrl(), endsWith(URLConstants.CART_PROCESS_PAYMENT_REQUEST));
        // open the popup window
        find("#btnOpenPaymentWindow").click();
        switchToNextWindow();
        waitFor("[type=submit]");
        submitForm();

        // close the popup window
        waitFor("#btnCloseWindow").click();
        waitFor(ExpectedConditions.numberOfWindowsToBe(1), 1000);
        assertThat("nelnet window should be closed / only one window remains", getDriver().getWindowHandles().size(), equalTo(1));

        // switch back to polling page
        getDriver().switchTo().window(startWindow);
        waitFor("body.dashboard");
    }

    private void selectPackage() {
        // choose the large package
        find("#MetadataForm_invoice_numberOfFiles").val("10");
        submitForm();
        waitForPageload();
    }

    /**
     * Confirm that the invoice-view page (/cart/view?id=123) works as well as inbound links from other pages
     */
    @Test
    public void testInvoiceView() {
        // ensure that we have at least one valid invoice in the system
        testLoginPurchase();
        logout();
        loginAdmin();
        gotoPage("/billing/listInvoices");

        assertThat(getDriver().getTitle(), is("All Invoices"));

        // links to the invoices (we assume) are in column 1 of the table
        WebElementSelection invoiceLinks = find("#tblAllInvoices tr td:first-child a");
        logger.debug("invoice links:{}", invoiceLinks);
        List<WebElement> list = invoiceLinks.toList();
        assertThat(list, is(not(empty())));

        List<Pair<String, String>> urls = new ArrayList<>();
        // for each invoice page, mak
        for (WebElement element : list) {
            String url = element.getAttribute("href");
            String path = url.substring(url.indexOf("/cart/"));
            urls.add(Pair.create(url, path));
        }

        for (Pair<String, String> pair : urls) {
            logger.debug("url: {}", pair.getFirst());
            gotoPage(pair.getFirst());

            // assert that the this page seems like a legit invoice-view page
            assertThat(getSource(), stringContainsInOrder(asList("Invoice", "Account:", "Transaction Status:")));

            // click on the link to the billing account for this invoice
            find("a.accountLink").first().click();
            waitForPageload();
            Assert.assertTrue(getCurrentUrl().contains("billing"));
            // make sure billing account page also has a link back to the invoice-view page we just verified
            assertThat(getSource(), stringContainsInOrder(asList("Overall Usage", pair.getSecond())));
        }

    }

    @Test
    /**
     * Assert that tdar gracefully handles invalid login in the purchase process.
     */
    public void testInvalidLogin() {
        gotoPage(CART_ADD);
        // choose the large package
        selectPackage();
        
        // try to log in with a blank username (javascript whould catch this, but we want to make sure we handle server-side too
        find(ByLabelText.byPartialLabelText("Username")).val("");

        // bypass jquery validation by calling form.submit() vs. clicking the submit button
        executeJavascript("document.loginForm.submit()");
        waitForPageload();

        assertThat(getSource(), containsString("tDAR encountered the following problems with this submission"));
        assertThat(getCurrentUrl(), endsWith("/cart/process-cart-login"));
    }

    @Test
    /**
     * Assert that tdar gracefully handles invalid login in the purchase process.
     */
    public void testInvalidRegistration() {
        gotoPage(CART_ADD);
        // choose the large package
        selectPackage();
        // make sure at least one required field is blank before submitting form
        find("#username").val("");

        // bypass jquery validation by calling form.submit() vs. clicking the submit button
        executeJavascript("document.registrationForm.submit()");
        waitForPageload();

        assertThat(getSource(), containsString("tDAR encountered the following problems with this submission"));
        assertThat(getCurrentUrl(), endsWith("/cart/process-registration"));
    }


    @Override
    public void waitForPageload() {
    	super.waitForPageload();
    	try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
