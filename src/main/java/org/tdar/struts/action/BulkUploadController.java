package org.tdar.struts.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.PersonalFilestoreTicket;
import org.tdar.core.bean.resource.Image;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.service.AsyncUpdateReceiver;
import org.tdar.core.service.BulkUploadService;
import org.tdar.filestore.FileAnalyzer;
import org.tdar.filestore.PersonalFilestore;
import org.tdar.struts.data.FileProxy;
import org.tdar.transform.DcTransformer;
import org.tdar.transform.ModsTransformer;
import org.tdar.utils.Pair;

/**
 * $Id$
 * 
 * <p>
 * Manages requests to create/delete/edit an CodingSheet and its associated metadata.
 * </p>
 * 
 * 
 * @author <a href='mailto:adam.brin@asu.edu'>Adam Brin</a>
 * @version $Revision$
 */
@ParentPackage("secured")
@Component
@Scope("prototype")
@Namespace("/batch")
public class BulkUploadController extends AbstractInformationResourceController<Image> {

    private static final long serialVersionUID = -6419692259588266839L;

    @Autowired
    private BulkUploadService bulkUploadService;

    @Override
    protected void loadCustomMetadata() {
        loadInformationResourceProperties();
    }

    // private Class validClasses = {}

    @Autowired
    private FileAnalyzer analyzer;

    private String bulkFileName;

    private long bulkContentLength;

    private FileInputStream templateInputStream;

    private Float percentDone = 0f;

    private String phase;

    private List<Pair<Long, String>> details;

    private Object asyncErrors;

    /**
     * Save basic metadata of the registering concept.
     * 
     * @param concept
     */
    @Override
    protected String save(Image image) {
        logger.info("saving batches...");
        saveBasicResourceMetadata();
        saveInformationResourceProperties();
        File excelManifest = null;
        logger.info("{} and names {}", getUploadedFiles(), getUploadedFilesFileName());
        if (!CollectionUtils.isEmpty(getUploadedFilesFileName())) {
            PersonalFilestoreTicket ticket = filestoreService.findPersonalFilestoreTicket(getTicketId());
            PersonalFilestore personalFilestore = filestoreService.getPersonalFilestore(ticket);
            try {
                String filename = getUploadedFilesFileName().get(0);
                excelManifest = personalFilestore.store(ticket, getUploadedFiles().get(0), filename);
            } catch (Exception e) {
                addActionErrorWithException("could not store manifest file", e);
            }
        }
        logger.debug("excel manifest is: {}", excelManifest);
        handleAsyncUploads();
        Collection<FileProxy> fileProxiesToProcess = getFileProxiesToProcess();

        if (image.getProject() != null) {
            // FIXME: there should be a better way to manage this than simply having to
            // manually bring everything onto session... but the @Async call does not seem to guarantee
            // that this will work otherwise
//            getReflectionService().warmUp(image.getProject(), 4);
        }

        if (isAsync()) {
            logger.info("running asyncronously");
            bulkUploadService.saveAsync(image, getAuthenticatedUser(), getTicketId(), excelManifest, fileProxiesToProcess);
        } else {
            logger.info("running inline");
            bulkUploadService.save(image, getAuthenticatedUser(), getTicketId(), excelManifest, fileProxiesToProcess);
        }
        setResource(null);
        return SUCCESS_ASYNC;
    }

    @SkipValidation
    @Action(value = "checkstatus", results = {
            @Result(name = "wait", type = "freemarker", location = "checkstatus-wait.ftl", params = { "contentType", "application/json" }) })
    public String checkStatus() {
        AsyncUpdateReceiver reciever = bulkUploadService.checkAsyncStatus(getTicketId());
        phase = reciever.getStatus();
        percentDone = reciever.getPercentComplete();
        setAsyncErrors(reciever.getHtmlAsyncErrors());
        if (percentDone == 100f) {
            List<Pair<Long, String>> details = reciever.getDetails();
            setDetails(details);
        }
        return "wait";
    }

    int maxReferenceRow = 0;

    @SkipValidation
    @Action(value = "template", results = {
            @Result(name = SUCCESS, type = "stream",
                    params = {
                            "contentType", "application/vnd.ms-excel",
                            "inputName", "templateInputStream",
                            "contentDisposition", "attachment;filename=\"${bulkFileName}\"",
                            "contentLength", "${bulkContentLength}"
            })
    })
    public String downloadBulkTemplate() {
        // create temporary file

        HSSFWorkbook workbook = bulkUploadService.createExcelTemplate();
        setBulkFileName("tdar-bulk-upload-template.xls");
        try {
            File resultFile = File.createTempFile(getBulkFileName(), ".xls", TdarConfiguration.getInstance().getTempDirectory());
            resultFile.deleteOnExit();
            workbook.write(new FileOutputStream(resultFile));
            setBulkContentLength(resultFile.length());
            setTemplateInputStream(new FileInputStream(resultFile));
        } catch (Exception iox) {
            logger.error("an error ocurred creating the template file", iox);
            throw new TdarRecoverableRuntimeException("could not save file");
        }

        return SUCCESS;
    }

    @Override
    protected Image loadResourceFromId(Long resourceId) {
        return null;
    }

    @Override
    protected void processUploadedFile() throws IOException {
        return;
    }

    @Override
    protected Image createResource() {
        return new Image();
    }

    /**
     * Get the current concept.
     * 
     * @return
     */
    public Image getImage() {
        return resource;
    }

    public void setImage(Image image) {
        this.resource = image;
    }

    @Override
    public Collection<String> getValidFileExtensions() {
        return analyzer.getExtensionsForTypes(ResourceType.IMAGE, ResourceType.DOCUMENT);
    }

    @Override
    public ModsTransformer<Image> getModsTransformer() {
        return null;
    }

    @Override
    public DcTransformer<Image> getDcTransformer() {
        return null;
    }

    @Override
    public boolean shouldSaveResource() {
        return false;
    }

    public void setBulkContentLength(long bulkContentLength) {
        this.bulkContentLength = bulkContentLength;
    }

    public long getBulkContentLength() {
        return bulkContentLength;
    }

    public void setBulkFileName(String bulkFileName) {
        this.bulkFileName = bulkFileName;
    }

    public String getBulkFileName() {
        return bulkFileName;
    }

    public void setTemplateInputStream(FileInputStream templateInputStream) {
        this.templateInputStream = templateInputStream;
    }

    public FileInputStream getTemplateInputStream() {
        return templateInputStream;
    }

    public void setPercentDone(Float percentDone) {
        this.percentDone = percentDone;
    }

    public Float getPercentDone() {
        return percentDone;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getPhase() {
        return phase;
    }

    @Override
    public boolean isMultipleFileUploadEnabled() {
        return true;
    }

    public void setDetails(List<Pair<Long, String>> details) {
        this.details = details;
    }

    public List<Pair<Long, String>> getDetails() {
        return details;
    }

    /**
     * @param asyncErrors
     *            the asyncErrors to set
     */
    public void setAsyncErrors(Object asyncErrors) {
        this.asyncErrors = asyncErrors;
    }

    /**
     * @return the asyncErrors
     */
    public Object getAsyncErrors() {
        return asyncErrors;
    }

}
