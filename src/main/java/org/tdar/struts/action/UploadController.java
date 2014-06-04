package org.tdar.struts.action;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.PersonalFilestoreTicket;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.service.PersonalFilestoreService;
import org.tdar.core.service.XmlService;
import org.tdar.filestore.personal.PersonalFilestore;
import org.tdar.filestore.personal.PersonalFilestoreFile;
import org.tdar.utils.json.JsonLookupFilter;

@SuppressWarnings("serial")
@Namespace("/upload")
@Component
@Scope("prototype")
@ParentPackage("secured")
@Results({
        @Result(name = "exception", type = "httpheader", params = { "error", "500" }),
        @Result(name = "input", type = "httpheader", params = { "error", "500" })
})
public class UploadController extends AuthenticationAware.Base {

    @Autowired
    private transient PersonalFilestoreService filestoreService;

    @Autowired
    private transient XmlService xmlService;

    private List<File> uploadFile = new ArrayList<File>();
    private List<String> uploadFileContentType = new ArrayList<String>();
    private List<String> uploadFileFileName = new ArrayList<String>();
    private PersonalFilestoreTicket personalFilestoreTicket;
    private String callback;
    private Long informationResourceId;
    private InputStream jsonInputStream;
    private int jsonContentLength;

    // this is the groupId that comes back to us from the the various upload requests
    private Long ticketId;

    // indicates that client is not sending a ticket because the server should create a new ticket for this upload
    private boolean ticketRequested = false;

    @Action(value = "index", results = { @Result(name = "success", location = "index.ftl") })
    public String index() {

        // get a claimcheck that all uploads will use
        // personalFilestoreTicket = filestoreService.createPersonalFilestoreTicket(getAuthenticatedUser());
        return SUCCESS;
    }

    @Action(value = "upload",
            results = { @Result(name = SUCCESS, type = JSONRESULT, params = { "stream", "jsonInputStream" }),
                    @Result(name = ERROR, type = JSONRESULT, params = { "stream", "jsonInputStream" })
            })
    public String upload() {
        PersonalFilestoreTicket ticket = null;
        getLogger().info("UPLOAD CONTROLLER: called with " + uploadFile.size() + " tkt:" + ticketId);
        if (ticketRequested) {
            grabTicket();
            ticketId = personalFilestoreTicket.getId();
            ticket = personalFilestoreTicket;
            getLogger().debug("UPLOAD CONTROLLER: on-demand ticket requested: {}", ticket);
        } else {
            ticket = getGenericService().find(PersonalFilestoreTicket.class, ticketId);
            getLogger().debug("UPLOAD CONTROLLER: upload request with ticket included: {}", ticket);
            if (ticket == null) {
                addActionError(getText("uploadController.require_valid_ticket"));
            }
        }

        if (CollectionUtils.isEmpty(uploadFile)) {
            addActionError(getText("uploadController.no_files"));
        }
        List<String> hashCodes = new ArrayList<>();
        if (CollectionUtils.isEmpty(getActionErrors())) {
            TdarUser submitter = getAuthenticatedUser();
            for (int i = 0; i < uploadFile.size(); i++) {
                File file = uploadFile.get(i);
                String fileName = uploadFileFileName.get(i);
                // put upload in holding area to be retrieved later (maybe) by the informationResourceController
                if ((file != null) && file.exists()) {
                    String contentType = "";
                    try {
                        contentType = uploadFileContentType.get(i);
                    } catch (Exception e) { /* OK, JUST USED FOR DEBUG */
                    }
                    Object[] out = { fileName, file.length(), contentType, ticketId };
                    getLogger().debug("UPLOAD CONTROLLER: processing file: {} ({}) , contentType: {} , tkt: {}", out);
                    PersonalFilestore filestore = filestoreService.getPersonalFilestore(submitter);
                    try {
                        PersonalFilestoreFile store = filestore.store(ticket, file, fileName);
                        hashCodes.add(store.getMd5());
                    } catch (Exception e) {
                        addActionErrorWithException(getText("uploadController.could_not_store"), e);
                    }
                }
            }
        }
        if (CollectionUtils.isEmpty(getActionErrors())) {
            Map<String, Object> result = new HashMap<>();
            ArrayList<HashMap<String, Object>> files = new ArrayList<HashMap<String, Object>>();
            result.put("files", files);
            for (int i = 0; i < uploadFileFileName.size(); i++) {
                HashMap<String, Object> file = new HashMap<>();
                files.add(file);
                file.put("name", uploadFileFileName.get(i));
                if (CollectionUtils.isNotEmpty(hashCodes)) {
                    file.put("hashCode", hashCodes.get(i));
                }
                if (CollectionUtils.isNotEmpty(getUploadFileSize())) {
                    file.put("size", getUploadFileSize().get(i));
                }
                if (CollectionUtils.isNotEmpty(getUploadFileContentType())) {
                    file.put("type", getUploadFileContentType().get(i));
                }
                file.put("delete_type", "DELETE");
            }
            result.put("ticket", ticket);
            setJsonInputStream(new ByteArrayInputStream(xmlService.convertFilteredJsonForStream(result, JsonLookupFilter.class, getCallback()).getBytes()));

            return SUCCESS;
        } else {
            getServletResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
            buildJsonError();
            return ERROR;
        }
    }

    @Action(value = "grab-ticket", results = { @Result(name = SUCCESS, type = JSONRESULT, params = { "stream", "jsonInputStream" })
    })
    public String grabTicket() {
        personalFilestoreTicket = filestoreService.createPersonalFilestoreTicket(getAuthenticatedUser());
        setJsonInputStream(new ByteArrayInputStream(xmlService.convertFilteredJsonForStream(personalFilestoreTicket, JsonLookupFilter.class, getCallback())
                .getBytes()));

        return SUCCESS;
    }

    // construct a json result expected by js client (currently dictated by jquery-blueimp-fileupload)
    private void buildJsonError() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("ticket", ticketId);
        result.put("errors", getActionErrors());
        getLogger().warn("upload request encountered actionErrors: {}", getActionErrors());
        setJsonInputStream(new ByteArrayInputStream(xmlService.convertFilteredJsonForStream(result, null, getCallback()).getBytes()));
    }

    public List<File> getUploadFile() {
        return uploadFile;
    }

    public void setUploadFile(List<File> uploadFile) {
        this.uploadFile = uploadFile;
    }

    public List<String> getUploadFileContentType() {
        return uploadFileContentType;
    }

    public void setUploadFileContentType(List<String> uploadFileContentType) {
        this.uploadFileContentType = uploadFileContentType;
    }

    public List<String> getUploadFileFileName() {
        return uploadFileFileName;
    }

    public void setUploadFileFileName(List<String> uploadFileFileName) {
        this.uploadFileFileName = uploadFileFileName;
    }

    public PersonalFilestoreTicket getPersonalFilestoreTicket() {
        return personalFilestoreTicket;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    /**
     * @return list of file sizes. Struts uses this naming convention for contentType and fileName lists, so we do the same here
     */
    public List<Long> getUploadFileSize() {
        List<Long> sizes = new ArrayList<Long>();
        for (File file : uploadFile) {
            sizes.add(file.length());
        }
        return sizes;
    }

    public Long getInformationResourceId() {
        return informationResourceId;
    }

    public void setInformationResourceId(Long informationResourceId) {
        this.informationResourceId = informationResourceId;
    }

    public InputStream getJsonInputStream() {
        return jsonInputStream;
    }

    public void setJsonInputStream(InputStream jsonInputStream) {
        this.jsonInputStream = jsonInputStream;
    }

    public int getJsonContentLength() {
        return jsonContentLength;
    }

    public boolean isTicketRequested() {
        return ticketRequested;
    }

    public void setTicketRequested(boolean ticketRequested) {
        this.ticketRequested = ticketRequested;
    }

}