package org.tdar.struts.action;

import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.entity.Institution;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.request.ContributorRequest;
import org.tdar.core.service.CrowdService.AuthenticationResult;

import com.opensymphony.xwork2.Preparable;

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
@InterceptorRef("paramsPrepareParamsStack")
@Component
@Scope("prototype")
@Result(name = "new", type = "redirect", location = "new")
public class AccountController extends AuthenticationAware.Base implements Preparable {

    public static final String ERROR_PASSWORDS_DONT_MATCH = "Please make sure your passwords match.";
    public static final String ERROR_MISSING_EMAIL = "Please enter an email address";
    public static final String ERROR_EMAILS_DONT_MATCH = "Please make sure your emails match.";
    public static final String ERROR_CONFIRM_EMAIL = "Please confirm your email to access tDAR.";
    public static final String ERROR_CONFIRM_PASSWORD = "Please confirm your password to access tDAR.";
    public static final String ERROR_CHOOSE_PASSWORD = "Please choose a password to access tDAR.";
    public static final String ERROR_ALREADY_REGISTERED = "This email address is already registered in our system.";
    public static final String ERROR_MAXLENGTH = "The '%s' field accepts a maximum of %s characters.";
    private static final int MAXLENGTH_CONTRIBUTOR = 512;

    private static final long serialVersionUID = 1147098995283237748L;

    private Long personId;
    private Person person;
    private String reminderEmail;
    private String confirmEmail;
    private String password;
    private String confirmPassword;
    private boolean requestingContributorAccess;
    private String institutionName;

    @Action(value = "new", interceptorRefs = @InterceptorRef("basicStack"),
            results = {
                    @Result(name = "success", location = "edit.ftl"),
                    @Result(name = "authenticated", type = "redirect", location = "/project/list") })
    @SkipValidation
    @Override
    public String execute() {
        if (isAuthenticated()) {
            return "authenticated";
        }
        return SUCCESS;
    }

    @Action(value = "recover", interceptorRefs = @InterceptorRef("basicStack"), results = { @Result(name = "success", type = "redirect",
            location = "http://auth.tdar.org/crowd/console/forgottenlogindetails!default.action") })
    @SkipValidation
    public String recover() {
        return SUCCESS;
    }

    @Action("edit")
    @SkipValidation
    public String edit() {
        if (isAuthenticated()) {
            return SUCCESS;
        } else {
            return "new";
        }
    }

    @Action("view")
    @SkipValidation
    public String view() {
        if (isAuthenticated()) {
            return SUCCESS;
        } else {
            return "new";
        }
    }

    // FIXME: not implemented yet.
    @Action(value = "reminder", results = { @Result(name = "success", location = "recover.ftl"), @Result(name = "input", location = "recover.ftl") })
    @SkipValidation
    public String sendNewPassword() {
        Person person = getEntityService().findByEmail(reminderEmail);
        if (person == null) {
            addActionError("Sorry, we didn't find a user with this email.");
            return INPUT;
        }

        // use crowd to handle user management? post to
        // http://dev.tdar.org/crowd/console/forgottenpassword!default.action
        // or just redirect there?
        addActionError("This isn't implemented yet.");
        return SUCCESS;
    }

    @Action(value = "register", results = { @Result(name = "success", type = "redirect", location = "view?personId=${person.id}"),
            @Result(name = "input", location = "edit.ftl") })
    public String create() {
        if (person == null) {
            return INPUT;
        }
        Person person_ = getEntityService().findByEmail(person.getEmail());

        if (person_ != null) {
            if (person_.isRegistered()) {
                addActionError(ERROR_ALREADY_REGISTERED);
                return ERROR;
            }
            person.setId(person_.getId());
            person = getEntityService().merge(person);
        }

        person.setRegistered(true);
        Institution institution = getEntityService().findInstitutionByName(institutionName);
        if (institution == null && !StringUtils.isBlank(institutionName)) {
            institution = new Institution();
            institution.setName(institutionName);
            getEntityService().save(institution);
        }
        person.setInstitution(institution);
        // set password to hash of password + email
        person.setPassword(DigestUtils.shaHex(password + person.getEmail()));

        getEntityService().saveOrUpdate(person);
        // after the person has been saved, create a contributor request for
        // them as needed.
        if (isRequestingContributorAccess()) {
            // create an account request for the administrator..
            ContributorRequest request = new ContributorRequest();
            request.setApplicant(person);
            // FIXME: eventually, this should only happen after being approved (and giving us money)
            person.setContributor(true);
            request.setContributorReason(person.getContributorReason());
            request.setTimestamp(new Date());
            getEntityService().saveOrUpdate(request);
        }
        // add user to Crowd
        getEntityService().saveOrUpdate(person);

        getLogger().debug("Trying to add user to crowd...");
        boolean success = getCrowdService().addUser(person, password);
        if (success) {
            getLogger().debug("Added user to crowd successfully.");
        } else {
            getLogger().debug("user already existed in crowd.  moving on to authentication");
        }
        // log person in.
        AuthenticationResult result = getCrowdService().authenticate(getServletRequest(), getServletResponse(), person.getEmail(), password);
        if (result.isValid()) {
            getLogger().debug("Authenticated successfully with crowd..");
            createAuthenticationToken(person);
            return SUCCESS;
        }

        // pushing error lower for unsuccessful add to CROWD, there could be
        // mulitple reasons for this failure including the fact that the
        // user is already in CROWD
        if (!success) {
            addActionError("a problem occured while trying to create a user");
            return ERROR;
        }
        getLogger().error("Unable to authenticate with the crowd service.");
        addActionError(result.toString());
        return ERROR;
    }

    public boolean isEmailRegistered(String email) {
        logger.trace("testing email:", email);
        if (StringUtils.isBlank(email)) {
            addActionError(ERROR_MISSING_EMAIL);
            return false;
        }
        Person person = getEntityService().findByEmail(email);
        return (person != null && person.isRegistered());
    }

    @Override
    public void validate() {
        logger.trace("calling validate");
        if (StringUtils.length(person.getContributorReason()) > MAXLENGTH_CONTRIBUTOR) {
            // FIXME: should we really be doing this? Or just turn contributorReason into a text field instead?
            logger.debug("contributor reason too long");
            addActionError(String.format(ERROR_MAXLENGTH, "Contributor Reason", MAXLENGTH_CONTRIBUTOR));
        }
        // FIXME: replace with visitor field validation on Person?
        if (StringUtils.isBlank(person.getFirstName())) {
            addActionError("Please enter your first name");
        }
        if (StringUtils.isBlank(person.getLastName())) {
            addActionError("Please enter your last name");
        }
        // validate email + confirmation
        if (isEmailRegistered(person.getEmail())) {
            logger.debug("email was already registered: ", person.getEmail());
            addActionError(ERROR_ALREADY_REGISTERED);
        } else if (StringUtils.isBlank(confirmEmail)) {
            addActionError(ERROR_CONFIRM_EMAIL);
        } else if (!new EqualsBuilder().append(person.getEmail(), confirmEmail).isEquals()) {
            addActionError(ERROR_EMAILS_DONT_MATCH);
        }
        // validate password + confirmation
        if (StringUtils.isBlank(password)) {
            addActionError(ERROR_CHOOSE_PASSWORD);
        } else if (StringUtils.isBlank(confirmPassword)) {
            addActionError(ERROR_CONFIRM_PASSWORD);
        } else if (!new EqualsBuilder().append(password, confirmPassword).isEquals()) {
            addActionError(ERROR_PASSWORDS_DONT_MATCH);
        }
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getConfirmEmail() {
        return confirmEmail;
    }

    public void setConfirmEmail(String email) {
        this.confirmEmail = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public void prepare() {
        if (personId == null || personId == -1L) {
            getLogger().debug("prepare: creating new person");
            person = new Person();
        } else {
            getLogger().debug("prepare: loading new person with person id: " + personId);
            person = getEntityService().find(personId);
            if (person == null) {
                getLogger().error("Couldn't load person with id: " + personId);
            }
        }
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institution) {
        this.institutionName = institution;
    }

    public String getReminderEmail() {
        return reminderEmail;
    }

    public void setReminderEmail(String reminderEmail) {
        this.reminderEmail = reminderEmail;
    }

    public boolean isRequestingContributorAccess() {
        return requestingContributorAccess;
    }

    public void setRequestingContributorAccess(boolean requestingContributorAccess) {
        this.requestingContributorAccess = requestingContributorAccess;
    }

}
