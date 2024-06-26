package org.tdar.struts.action.api.files;

import java.io.IOException;
import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.billing.BillingAccount;
import org.tdar.core.bean.file.TdarDir;
import org.tdar.core.dao.FileOrder;
import org.tdar.core.service.PersonalFilestoreService;
import org.tdar.struts.action.api.AbstractJsonApiAction;
import org.tdar.utils.PersistableUtils;

@Component
@Scope("prototype")
@ParentPackage("secured")
@Namespace("/api/file")
public class ListDirectoriesAction extends AbstractJsonApiAction {

    private static final long serialVersionUID = -3758560269825040925L;
    private Long accountId;
    private Long parentId;
    private FileOrder sortBy;
    private TdarDir parent;
    private BillingAccount account;

    @Override
    public void prepare() throws Exception {
        super.prepare();
        if (PersistableUtils.isNotNullOrTransient(parentId)) {
            parent = getGenericService().find(TdarDir.class, parentId);
        }
        if (PersistableUtils.isNotNullOrTransient(accountId)) {
            account = getGenericService().find(BillingAccount.class, accountId);
        }
        
    }

    
    @Override
    public void validate() {
        super.validate();
        if (getAuthorizationService().cannotChargeAccount(getAuthenticatedUser(), getAccount())) {
            addActionError("not.allowed");
        }
    }
    @Autowired
    private PersonalFilestoreService personalFilestoreService;

    @Action(value = "listDirs")
    public String execute() throws IOException {
        List<TdarDir> files = personalFilestoreService.listDirectories(parent, account, getAuthenticatedUser());
        setResultObject(files);
        return SUCCESS;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public TdarDir getParent() {
        return parent;
    }

    public void setParent(TdarDir parent) {
        this.parent = parent;
    }

    public BillingAccount getAccount() {
        return account;
    }

    public void setAccount(BillingAccount account) {
        this.account = account;
    }

    public FileOrder getSortBy() {
        return sortBy;
    }

    public void setSortBy(FileOrder sortBy) {
        this.sortBy = sortBy;
    }

}
