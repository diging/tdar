package org.tdar.struts.action.download;

import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.dao.external.auth.AuthenticationResult;
import org.tdar.core.service.GenericService;
import org.tdar.core.service.external.AuthenticationService;
import org.tdar.struts.action.TdarActionSupport;
import org.tdar.struts.interceptor.annotation.DoNotObfuscate;
import org.tdar.struts.interceptor.annotation.HttpsOnly;
import org.tdar.struts.interceptor.annotation.PostOnly;
import org.tdar.struts.interceptor.annotation.WriteableSession;

import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;

@ParentPackage("default")
@Namespace("/filestore")
@Component
@Scope("prototype")
public class DownloadRegistrationController extends AbstractDownloadController implements Validateable, Preparable {

    private static final long serialVersionUID = -893535919691607147L;

    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private GenericService genericService;

    @Action(value = "process-download-registration",
            interceptorRefs = { @InterceptorRef("csrfDefaultStack") },
            results = { @Result(name = INPUT, location = "../filestore/download-unauthenticated.ftl"),
                    @Result(name = SUCCESS, type = TdarActionSupport.REDIRECT, location = "${downloadRegistration.returnUrl}")
            })
    @HttpsOnly
    @PostOnly
    @WriteableSession
    @DoNotObfuscate(reason = "getPerson() may have not been set on the session before sent to obfuscator, so don't want to wipe email")
    public String create() {
        if (getDownloadRegistration() == null || getDownloadRegistration().getPerson() == null || !isPostRequest()) {
            return INPUT;
        }
        AuthenticationResult result = null;
        try {
            result = authenticationService.addAndAuthenticateUser(
                    getDownloadRegistration(), getServletRequest(), getServletResponse(), getSessionData());
            if (result.getType().isValid()) {
                getDownloadRegistration().setPerson(result.getPerson());
                addActionMessage(getText("userAccountController.successful_registration_message"));
                return TdarActionSupport.SUCCESS;
            }
        } catch (Throwable e) {
            addActionError(e.getLocalizedMessage());
        }
        setupLoginRegistrationBeans();
        return TdarActionSupport.INPUT;
    }
   

    @Override
    public void prepare() {
        setInformationResource(genericService.find(InformationResource.class, getDownloadRegistration().getResource().getId()));
        setInformationResourceFileVersion(getDownloadRegistration().getVersion());
    }

    @Override
    public void validate() {        
        getLogger().debug("validating registration request");
        List<String> errors = getDownloadRegistration().validate(this, authenticationService);
        getLogger().debug("found errors {}", errors);
        addActionErrors(errors);
    }
}
