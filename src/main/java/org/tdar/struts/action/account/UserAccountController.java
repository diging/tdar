package org.tdar.struts.action.account;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Actions;
import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.URLConstants;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.external.auth.AuthenticationResult;
import org.tdar.core.service.EntityService;
import org.tdar.core.service.external.AuthenticationService;
import org.tdar.core.service.external.RecaptchaService;
import org.tdar.struts.action.AuthenticationAware;
import org.tdar.struts.action.TdarActionSupport;
import org.tdar.struts.data.AntiSpamHelper;
import org.tdar.struts.data.UserRegistration;
import org.tdar.struts.interceptor.annotation.CacheControl;
import org.tdar.struts.interceptor.annotation.DoNotObfuscate;
import org.tdar.struts.interceptor.annotation.HttpsOnly;
import org.tdar.struts.interceptor.annotation.PostOnly;
import org.tdar.struts.interceptor.annotation.WriteableSession;

import com.opensymphony.xwork2.ValidationAware;

/**
 * $Id$
 * 
 * Manages web requests for CRUD-ing user accounts, providing account management
 * functionality.
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */

@ParentPackage("default")
@Namespace("/account")
@Component
@Scope("prototype")
@HttpsOnly
@CacheControl
public class UserAccountController extends AuthenticationAware.Base implements ValidationAware {

    private static final long serialVersionUID = 1147098995283237748L;

    public static final long ONE_HOUR_IN_MS = 3_600_000;

    private String url;
    private String passwordResetURL;

    @Autowired
    private transient RecaptchaService reCaptchaService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private EntityService entityService;
    private String reminderEmail;
    private AntiSpamHelper h = new AntiSpamHelper(reCaptchaService);
    private UserRegistration registration = new UserRegistration(h);

    public boolean isUsernameRegistered(String username) {
        getLogger().debug("testing username:", username);
        if (StringUtils.isBlank(username)) {
            addActionError(getText("userAccountController.error_missing_username"));
            return true;
        }
        TdarUser person = entityService.findByUsername(username);
        return ((person != null) && person.isRegistered());
    }

    @Action(value = "new",
            results = {
                    @Result(name = SUCCESS, location = "edit.ftl"),
                    @Result(name = AUTHENTICATED, type = TYPE_REDIRECT, location = URLConstants.DASHBOARD) })
    @SkipValidation
    @Override
    @HttpsOnly
    public String execute() {
        if (isAuthenticated()) {
            return AUTHENTICATED;
        }

        if (StringUtils.isNotBlank(TdarConfiguration.getInstance().getRecaptchaPrivateKey())) {
            setH(new AntiSpamHelper(reCaptchaService));
        }
        return SUCCESS;
    }

    @Action(value = "recover",
            results = { @Result(name = SUCCESS, type = TYPE_REDIRECT, location = "${passwordResetURL}") })
    @SkipValidation
    @HttpsOnly
    public String recover() {
        setPasswordResetURL(authenticationService.getAuthenticationProvider().getPasswordResetURL());
        return SUCCESS;
    }

    @Action(value = "edit", results = { @Result(name = SUCCESS, type = TYPE_REDIRECT, location = "/entity/person/${person.id}/edit") })
    @SkipValidation
    @HttpsOnly
    public String edit() {
        if (isAuthenticated()) {
            return SUCCESS;
        }
        return "new";
    }

    // FIXME: not implemented yet.
    @Action(value = "reminder"
            , results = { @Result(name = SUCCESS, location = "recover.ftl"), @Result(name = "input", location = "recover.ftl") })
    @SkipValidation
    @HttpsOnly
    public String sendNewPassword() {
        Person person = entityService.findByEmail(reminderEmail);
        if (person == null || !(person instanceof TdarUser)) {
            addActionError(getText("userAccountController.email_invalid"));
            return INPUT;
        }

        // use crowd to handle user management? post to
        // http://dev.tdar.org/crowd/console/forgottenpassword!default.action
        // or just redirect there?
        addActionError(getText("userAccountController.not_implemented"));
        return SUCCESS;
    }

    @Actions({
            @Action(value = "register",
//                    interceptorRefs = { @InterceptorRef("csrfDefaultStack") },
                    results = { @Result(name = SUCCESS, type = TYPE_REDIRECT, location = URLConstants.DASHBOARD),
                            @Result(name = ADD, type = TYPE_REDIRECT, location = "/account/add"),
                            @Result(name = INPUT, location = "edit.ftl") })
    })
    @HttpsOnly
    @PostOnly
    @WriteableSession
    @DoNotObfuscate(reason = "getPerson() may have not been set on the session before sent to obfuscator, so don't want to wipe email")
    public String create() {
        if (registration == null || registration.getPerson() == null || !isPostRequest()) {
            return INPUT;
        }
        try {
            AuthenticationResult result = authenticationService.addAndAuthenticateUser(registration, getServletRequest(), getServletResponse(), getSessionData());
            if (result.getType().isValid()) {
                registration.setPerson(result.getPerson());
                addActionMessage(getText("userAccountController.successful_registration_message"));
                return TdarActionSupport.SUCCESS;
            }
        } catch (Throwable e) {
            addActionError(e.getLocalizedMessage());
            return TdarActionSupport.INPUT;
        }
        return TdarActionSupport.INPUT;
    }

    public String getPasswordResetURL()
    {
        return passwordResetURL;
    }

    public void setPasswordResetURL(String url)
    {
        this.passwordResetURL = url;
    }

    public String getTosUrl() {
        return getTdarConfiguration().getTosUrl();
    }

    public String getContributorAgreementUrl() {
        return getTdarConfiguration().getContributorAgreementUrl();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getReminderEmail() {
        return reminderEmail;
    }

    public void setReminderEmail(String reminderEmail) {
        this.reminderEmail = reminderEmail;
    }

    public UserRegistration getRegistration() {
        return registration;
    }

    public UserRegistration getReg() {
        return registration;
    }

    public void setRegistration(UserRegistration registration) {
        this.registration = registration;
    }

    public AntiSpamHelper getH() {
        return h;
    }

    public void setH(AntiSpamHelper h) {
        this.h = h;
    }

    // if form submittal takes too long we assume spambot. expose the timeout value to view layer so that we can make sure
    // actual humans get a form that is never too old while still locking out spambots.
    public long getRegistrationTimeout() {
        return ONE_HOUR_IN_MS;
    }

    @Override
    public void validate() {
        getLogger().debug("validating registration request");
        List<String> errors = registration.validate(this, authenticationService);
        getLogger().debug("found errors {}", errors);
        addActionErrors(errors);
    }

}
