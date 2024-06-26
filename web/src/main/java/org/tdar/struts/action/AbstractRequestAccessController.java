package org.tdar.struts.action;

import java.util.List;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.Indexable;
import org.tdar.core.bean.Persistable;
import org.tdar.core.bean.entity.UserAffiliation;
import org.tdar.core.bean.notification.EmailType;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.core.service.external.auth.AntiSpamHelper;
import org.tdar.struts_base.action.TdarActionSupport;
import org.tdar.utils.PersistableUtils;

import com.opensymphony.xwork2.Preparable;

@Component
@Scope("prototype")
@Results({
        @Result(name = TdarActionSupport.SUCCESS, type = AbstractRequestAccessController.REDIRECT,
                location = AbstractRequestAccessController.SUCCESS_REDIRECT_REQUEST_ACCESS),
        @Result(name = TdarActionSupport.ERROR, type = TdarActionSupport.HTTPHEADER, params = { "error", "404" }),
        @Result(name = TdarActionSupport.INPUT, type = TdarActionSupport.HTTPHEADER, params = { "error", "404" }),
        @Result(name = TdarActionSupport.FORBIDDEN, type = TdarActionSupport.HTTPHEADER, params = { "error", "403" })
})
/**
 * Abstract class for backing unauthenticated requests (Login and Register)
 * 
 * @author abrin
 *
 */
public abstract class AbstractRequestAccessController<P extends Persistable> extends AbstractAuthenticatableAction implements Preparable {

    private static final long serialVersionUID = -3264106556246738465L;

    @Autowired
    private transient AuthorizationService authorizationService;

    private List<UserAffiliation> affiliations = UserAffiliation.getUserSubmittableAffiliations();

    public static final String SUCCESS_REDIRECT_REQUEST_ACCESS = "/${typeNamespace}/request/${id}?type=${type}&messageBody=${messageBody}";
    public static final String FORBIDDEN = "forbidden";
    private Long id;
    private P persistable;
    private String messageBody;
    private EmailType type;

    private AntiSpamHelper h = new AntiSpamHelper();

    public abstract String getTypeNamespace();

    public abstract Class<P> getPersistableClass();

    @Override
    public void prepare() {
        // make sure the Reosurce ID is set
        if (PersistableUtils.isNotNullOrTransient(getId())) {
            setPersistable(getGenericService().find(getPersistableClass(), getId()));
            // bad, but force onto session until better way found
            authorizationService.applyTransientViewableFlag((Indexable) getPersistable(), getAuthenticatedUser());
        }
    }

    @Override
    public void validate() {
        if (PersistableUtils.isNullOrTransient(getId())) {
            addActionError(getText("requestAccessController.specify_what_to_download"));
        }
    }

    public List<UserAffiliation> getAffiliations() {
        return affiliations;
    }

    public void setAffiliations(List<UserAffiliation> affiliations) {
        this.affiliations = affiliations;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AntiSpamHelper getH() {
        return h;
    }

    public void setH(AntiSpamHelper h) {
        this.h = h;
    }

    public EmailType getType() {
        return type;
    }

    public void setType(EmailType type) {
        this.type = type;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public P getPersistable() {
        return persistable;
    }

    public void setPersistable(P persistable) {
        this.persistable = persistable;
    }
}
