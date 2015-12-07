package org.tdar.struts.interceptor;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.service.GenericService;
import org.tdar.core.service.external.AuthenticationService;
import org.tdar.core.service.external.session.SessionData;
import org.tdar.core.service.external.session.SessionDataAware;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.interceptor.Interceptor;

public abstract class AbstractAuthenticationInterceptor implements SessionDataAware, Interceptor {

    private static final long serialVersionUID = -89612159086314236L;

    protected TdarConfiguration CONFIG = TdarConfiguration.getInstance();
    @Autowired
    transient AuthenticationService authenticationService;

    private SessionData sessionData;
    
    public SessionData getSessionData() {
        return sessionData;
    }

    @Override
    public void destroy() {
        sessionData = null;
        authenticationService = null;
    }
    
    public void setSessionData(SessionData sessionData) {
        this.sessionData = sessionData;
    }

    Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean validateSsoTokenAndAttachUser(String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }

        logger.debug("checking valid token: {}", token);
        boolean result = authenticationService.checkToken((String) token, getSessionData(), ServletActionContext.getRequest()).getType().isValid();
        logger.debug("token authentication result: {}", result);
        return result;
    }


    protected String getSSoTokenFromParams() {
        return authenticationService.getSsoTokenFromRequest(ServletActionContext.getRequest());
    }
}
