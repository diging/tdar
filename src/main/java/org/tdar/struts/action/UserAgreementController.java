package org.tdar.struts.action;

import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.AuthNotice;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.service.external.AuthenticationService;
import org.tdar.struts.interceptor.annotation.PostOnly;
import org.tdar.struts.interceptor.annotation.WriteableSession;

import com.opensymphony.xwork2.Preparable;

@Component
@Scope("prototype")
@Namespace("/")
@ParentPackage("secured")
public class UserAgreementController extends AuthenticationAware.Base implements Preparable {

    private static final String ACCEPT = "accept";
    private static final String DECLINE = "decline";
    private static final long serialVersionUID = 5992094345280080761L;
    private List<AuthNotice> authNotices = new ArrayList<>();
    private List<AuthNotice> acceptedAuthNotices = new ArrayList<>();
    private String userResponse = "";
    private TdarUser user;

    @Autowired
    private transient AuthenticationService authenticationService;

    @Override
    public void prepare() {
        getLogger().trace("acceptedAuthNotices: {}", acceptedAuthNotices);
        getLogger().trace("userResponse:{}", userResponse);
        user = getAuthenticatedUser();
        authNotices.addAll(authenticationService.getUserRequirements(user));
    }

    @WriteableSession
    @Action(value = "agreement-response", results = {
            @Result(name = TdarActionSupport.SUCCESS, type = "redirect", location = "/dashboard"),
            @Result(name = TdarActionSupport.NONE, type = "redirect", location = "/logout"),
            @Result(name = TdarActionSupport.INPUT, type = "redirect", location = "/show-notices")
    })
    @PostOnly
    public String agreementResponse() {
        if (!isAuthenticated()) {
            return LOGIN;
        }

        if (DECLINE.equals(userResponse)) {
            String fmt = getText("userAgreementController.decline_message");
            addActionMessage(String.format(fmt, getSiteAcronym()));
            getLogger().debug("agreements declined,  redirecting to logout page");
            return NONE;
        }

        if (ACCEPT.equals(userResponse)) {
            if (processResponse()) {
                getLogger().debug("all requirements met,  success!! returning success");
                return SUCCESS;
            } else {
                getLogger().debug("some requirements remain, returning input");
                addActionError(getText("userAgreementController.please_choose"));
                return INPUT;
            }
        } else {
            // unexpected response. bail out!
            return BAD_REQUEST;
        }
    }

    boolean processResponse() {
        getLogger().trace(" pending notices:{}", authNotices);
        getLogger().trace("accepted notices:{}", acceptedAuthNotices);
        authenticationService.satisfyUserPrerequisites(getSessionData(), acceptedAuthNotices);
        boolean allRequirementsMet = !authenticationService.userHasPendingRequirements(user);
        return allRequirementsMet;
    }

    @Action(value = "show-notices")
    public String showNotices() {
        if (!isAuthenticated()) {
            return LOGIN;
        }
        return SUCCESS;
    }

    public List<AuthNotice> getAuthNotices() {
        return authNotices;
    }

    public List<AuthNotice> getAcceptedAuthNotices() {
        return acceptedAuthNotices;
    }

    public void setAcceptedAuthNotices(List<AuthNotice> value) {
        acceptedAuthNotices = value;
    }

    public void setSubmit(String value) {
        userResponse = value;
    }

    public boolean isTosAcceptanceRequired() {
        return authNotices.contains(AuthNotice.TOS_AGREEMENT);
    }

    public boolean isContributorAgreementAcceptanceRequired() {
        return authNotices.contains(AuthNotice.CONTRIBUTOR_AGREEMENT);
    }

    public String getTosUrl() {
        return getTdarConfiguration().getTosUrl();
    }

    public String getContributorAgreementUrl() {
        return getTdarConfiguration().getContributorAgreementUrl();
    }

}
