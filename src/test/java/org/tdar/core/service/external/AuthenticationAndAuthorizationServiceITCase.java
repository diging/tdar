package org.tdar.core.service.external;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.tdar.core.bean.AbstractIntegrationTestCase;
import org.tdar.core.bean.AuthNotice;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.resource.Image;
import org.tdar.core.bean.resource.Status;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.external.auth.AuthenticationProvider;
import org.tdar.core.dao.external.auth.CrowdRestDao;
import org.tdar.core.dao.external.auth.InternalTdarRights;
import org.tdar.core.dao.external.auth.TdarGroup;
import org.tdar.junit.MultipleTdarConfigurationRunner;
import org.tdar.junit.RunWithTdarConfiguration;
import org.tdar.struts.action.UserAgreementController;
import org.tdar.struts.action.account.UserAccountController;

import com.opensymphony.xwork2.Action;

@RunWith(MultipleTdarConfigurationRunner.class)
@RunWithTdarConfiguration(runWith = { RunWithTdarConfiguration.TOS_CHANGE })
public class AuthenticationAndAuthorizationServiceITCase extends AbstractIntegrationTestCase {

    int tosLatestVersion = TdarConfiguration.getInstance().getTosLatestVersion();
    int contributorAgreementLatestVersion = TdarConfiguration.getInstance().getContributorAgreementLatestVersion();

    TdarUser user(boolean contributor, int tosVersion, int creatorAgreementVersion) {
        TdarUser user = new TdarUser("bob", "loblaw", "jim.devos@zombo.com");
        user.setContributor(contributor);
        user.setTosVersion(tosVersion);
        user.setContributorAgreementVersion(creatorAgreementVersion);
        return user;
    }

    @Test
    @Rollback
    public void testBillingAdminRetained() {
        List<Status> list = new ArrayList<>();
        list.add(Status.ACTIVE);
        list.add(Status.DRAFT);
        TdarUser user = getBillingUser();
        logger.debug("groups: {} ", InternalTdarRights.SEARCH_FOR_DRAFT_RECORDS.getPermittedGroups());
        assertTrue(ArrayUtils.contains(InternalTdarRights.SEARCH_FOR_DRAFT_RECORDS.getPermittedGroups(), TdarGroup.TDAR_BILLING_MANAGER));

        authenticationAndAuthorizationService.removeIfNotAllowed(list, Status.DRAFT, InternalTdarRights.SEARCH_FOR_DRAFT_RECORDS, user);
        logger.debug("{}", list);
    }

    @Test
    @Rollback
    public void testUserHasPendingRequirements() throws Exception {
        TdarUser legacyUser = user(false, 0, 0);
        assertThat(authenticationService.userHasPendingRequirements(legacyUser), is(true));

        TdarUser legacyContributor = user(true, 0, 0);
        assertThat(authenticationService.userHasPendingRequirements(legacyContributor), is(true));

        // if user registered after latest version of TOS/CA, they have not pending requirements
        TdarUser newUser = user(false, tosLatestVersion, contributorAgreementLatestVersion);
        assertThat(authenticationService.userHasPendingRequirements(newUser), is(false));

    }

    @Test
    public void testGetUserRequirements() throws Exception {
        // should not meet either requirement
        TdarUser legacyContributor = user(true, 0, 0);
        List<AuthNotice> requirements = authenticationService.getUserRequirements(legacyContributor);
        assertThat(requirements, hasItems(AuthNotice.TOS_AGREEMENT, AuthNotice.CONTRIBUTOR_AGREEMENT));

        // should satisfy all requirements
        TdarUser newUser = user(false, tosLatestVersion, contributorAgreementLatestVersion);
        assertThat(authenticationService.getUserRequirements(newUser), empty());

        // should satisfy all requirements
        TdarUser newContributor = user(true, tosLatestVersion, contributorAgreementLatestVersion);
        assertThat(authenticationService.getUserRequirements(newContributor), empty());
    }

    @Test
    @Rollback
    public void testSatisfyPrerequisite() throws Exception {
        // a contributor that hasn't signed on since updated TOS and creator agreement
        TdarUser contributor = user(true, 0, 0);
        authenticationService.satisfyPrerequisite(contributor, AuthNotice.TOS_AGREEMENT);
        assertThat(authenticationService.getUserRequirements(contributor), not(hasItem(AuthNotice.TOS_AGREEMENT)));

        authenticationService.satisfyPrerequisite(contributor, AuthNotice.CONTRIBUTOR_AGREEMENT);
        assertThat(authenticationService.getUserRequirements(contributor), not(hasItems(AuthNotice.TOS_AGREEMENT,
                AuthNotice.CONTRIBUTOR_AGREEMENT)));
    }

    @Test
    @Rollback(false)
    public void testSatisfyPrerequisiteWithSession() throws Exception {
        // a contributor that hasn't signed on since updated TOS and creator agreement
        UserAgreementController controller = generateNewController(UserAgreementController.class);
        TdarUser user = getBasicUser();
        user.setContributorAgreementVersion(0);
        init(controller, user);
        assertThat(authenticationService.getUserRequirements(user), hasItem(AuthNotice.CONTRIBUTOR_AGREEMENT));
        List<AuthNotice> list = new ArrayList<>();
        list.add(AuthNotice.CONTRIBUTOR_AGREEMENT);
        list.add(AuthNotice.TOS_AGREEMENT);
        logger.info("userId: {}", controller.getSessionData().getTdarUserId());
        authenticationService.satisfyUserPrerequisites(controller.getSessionData(), list);
        assertThat(authenticationService.getUserRequirements(user), not(hasItem(AuthNotice.CONTRIBUTOR_AGREEMENT)));
        evictCache();
        setVerifyTransactionCallback(new TransactionCallback<Image>() {
            @Override
            public Image doInTransaction(TransactionStatus status) {
                TdarUser user = getBasicUser();
                assertThat(authenticationService.getUserRequirements(user), not(hasItem(AuthNotice.CONTRIBUTOR_AGREEMENT)));
                user.setContributorAgreementVersion(0);
                genericService.saveOrUpdate(user);
                return null;

            }
        });

    }

    @Test
    @Rollback
    public void testCrowdDisconnected() {
        // Create a user ... replace crowd witha "broken crowd" and then
        TdarUser person = new TdarUser("Thomas", "Angell", "tangell@pvd.state.ri.us");
        person.setUsername(person.getEmail());
        person.setContributor(true);

        AuthenticationProvider oldProvider = authenticationService.getProvider();
        authenticationService.getAuthenticationProvider().deleteUser(person);
        Properties crowdProperties = new Properties();
        crowdProperties.put("application.name", "tdar.test");
        crowdProperties.put("application.password", "tdar.test");
        crowdProperties.put("application.login.url", "http://localhost/crowd");
        crowdProperties.put("crowd.server.url", "http://localhost/crowd");

        authenticationService.setProvider(new CrowdRestDao(crowdProperties));

        String password = "super.secret";
        UserAccountController controller = generateNewInitializedController(UserAccountController.class);

        // create account, making sure the controller knows we're legit.
        controller.getH().setTimeCheck(System.currentTimeMillis() - 10000);
        controller.getRegistration().setRequestingContributorAccess(true);
        controller.getRegistration().setPassword(password);
        controller.getRegistration().setConfirmPassword(password);
        controller.getRegistration().setConfirmEmail(person.getEmail());
        controller.getRegistration().setPerson(person);
        controller.getRegistration().setAcceptTermsOfUse(true);
        controller.setServletRequest(getServletPostRequest());
        controller.setServletResponse(getServletResponse());
        String execute = null;
        setIgnoreActionErrors(true);
        try {
            controller.validate();
            // technically this is more appropriate -- only call create if validate passes
            if (CollectionUtils.isEmpty(controller.getActionErrors())) {
                execute = controller.create();
            } else {
                logger.error("errors: {} ", controller.getActionErrors());
            }
        } catch (Exception e) {
            logger.error("{}", e);
        } finally {
            authenticationService.setProvider(oldProvider);
        }
        logger.info("errors: {}", controller.getActionErrors());
        assertEquals("result is not input :" + execute, execute, Action.INPUT);
        logger.info("person:{}", person);
        assertTrue("person should not have an id", Persistable.Base.isTransient(person));
    }

}
