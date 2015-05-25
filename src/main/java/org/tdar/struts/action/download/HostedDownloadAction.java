package org.tdar.struts.action.download;

import org.apache.commons.lang.StringUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.resource.InformationResourceFile;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.service.download.DownloadResult;
import org.tdar.core.service.download.DownloadService;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.struts.interceptor.annotation.HttpOnlyIfUnauthenticated;
import org.tdar.utils.PersistableUtils;

import com.opensymphony.xwork2.Preparable;

/**
 * Created by jimdevos on 9/23/14. This action is designed to facilitate un-authenticated downloads if resources are part of a given collection and the host
 * name matches one in the allowed downloads, as well as the resource being public and the valid API-key being passed.
 * 
 * NOTE: This action is somewhat brittle based on HTTP/HTTPS referrer rules. This seems like it will only work from HTTP->HTTP and when HTTPS is added to the
 * mix it'll likely break, or that breakage will be browser-dependent.
 * http://webmasters.stackexchange.com/questions/47405/how-can-i-pass-referrer-header-from-my-https-domain-to-http-domains
 * 
 */
@Namespace("/download/hosted")
@Component
@Scope("prototype")
@ParentPackage("default")
@HttpOnlyIfUnauthenticated
public class HostedDownloadAction extends AbstractDownloadController implements Preparable {

    private static final long serialVersionUID = -7618237747050278959L;
    private String apiKey = "";
    @Autowired
    private transient AuthorizationService authorzationService;
    @Autowired
    private transient DownloadService downloadService;

    private Long informationResourceFileId;
    private InformationResourceFile informationResourceFile;

    @Action(value = "{informationResourceFileId}/{apiKey}")
    public String execute() {
        setDownloadTransferObject(downloadService.validateFilterAndSetupDownload(getAuthenticatedUser(),
                informationResourceFile, isCoverPageIncluded(), this, null, true));

        if (getDownloadTransferObject().getResult() != DownloadResult.SUCCESS) {
            return getDownloadTransferObject().getResult().name().toLowerCase();
        }
        return getDownloadTransferObject().getResult().name().toLowerCase();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void prepare() {
        super.prepare();
        informationResourceFile = getGenericService().find(InformationResourceFile.class, informationResourceFileId);
        if (StringUtils.isBlank(apiKey)) {
            addActionError("hostedDownloadController.api_key_required");
        }

        if (PersistableUtils.isNotNullOrTransient(informationResourceFile)) {
            InformationResourceFileVersion fileVersion = informationResourceFile.getLatestUploadedVersion();

            if (!authorzationService.checkValidUnauthenticatedDownload(fileVersion, getApiKey(), getServletRequest())) {
                addActionError("hostedDownloadController.invalid_request");
            }
            getAuthorizationService().applyTransientViewableFlag(fileVersion, getAuthenticatedUser());
        }
    }

    @Override
    public void validate() {
        // Note: we specifically do not want to call parent.validate()
        if (PersistableUtils.isNullOrTransient(informationResourceFile)) {
            addActionError(getText("downloadController.specify_what_to_download"));
        }
    }

    public Long getInformationResourceFileId() {
        return informationResourceFileId;
    }

    public void setInformationResourceFileId(Long informationResourceFileId) {
        this.informationResourceFileId = informationResourceFileId;
    }
}
