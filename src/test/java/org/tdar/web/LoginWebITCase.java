/**
 * 
 */
package org.tdar.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.tdar.core.dao.external.auth.AuthenticationResult;
import org.tdar.junit.MultipleTdarConfigurationRunner;
import org.tdar.junit.RunWithTdarConfiguration;
import org.tdar.utils.MessageHelper;

/**
 * @author Adam Brin
 * 
 */
@RunWith(MultipleTdarConfigurationRunner.class)
public class LoginWebITCase extends AbstractAuthenticatedWebTestCase {

    @Test
    public void testAbstractLogin() {
        assertTextPresentInPage("Welcome back,");
    }

    @Test
    @RunWithTdarConfiguration(runWith = { RunWithTdarConfiguration.TOS_CHANGE })
    public void testLoginWithPrompt() {
        logger.trace(getPageBodyCode());
        assertTextPresent("User Agreements");
        setInput("acceptedAuthNotices", "TOS_AGREEMENT");
        setInput("acceptedAuthNotices", "CONTRIBUTOR_AGREEMENT");
        clickElementWithId("accept");
        assertTextPresentInPage("Welcome back,");
    }

    @Test
    @RunWithTdarConfiguration(runWith = { RunWithTdarConfiguration.TOS_CHANGE })
    public void testLoginDeclineWithPrompt() {
        logger.trace(getPageBodyCode());
        assertTextPresent("User Agreements");
        clickElementWithId("decline");
        assertTextPresentInPage("What can you dig up");
    }

    @Test
    public void testSecondLogin() {
        gotoPage("/login");
        assertTextPresentInPage("Featured Content");
    }

    @Test
    public void testInvalidLogin() {
        logout();
        login("BAD_USERNAME", "BAD_PASSWORD", true);
        assertTextPresent(AuthenticationResult.INVALID_PASSWORD.getMessage());
        assertTextNotPresent("Your submitted projects");
    }

    @Test
    public void testInvalidLoginInvalidEmail() {
        logout();
        login("BAD USERNAME", "BAD PASSWORD", true);
        assertTextPresent(MessageHelper.getMessage("auth.username.invalid"));
        assertTextNotPresent("Your submitted projects");
    }

    @Test
    public void testLogout() {
        logout();
        assertTextPresentInPage("Log In");
    }
}
