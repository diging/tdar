/**
 * 
 */
package org.tdar.fileprocessing.workflows;

import java.io.File;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;
import org.tdar.configuration.TdarConfiguration;
import org.tdar.db.ImportDatabase;
import org.tdar.db.conversion.converters.DatasetConverter;
import org.tdar.db.datatable.TDataTable;
import org.tdar.db.datatable.TDataTableRelationship;
import org.tdar.exception.ExceptionWrapper;
import org.tdar.exception.NonFatalWorkflowException;
import org.tdar.fileprocessing.tasks.Task;
import org.tdar.filestore.FileStoreFile;
import org.tdar.filestore.Filestore;

/**
 * <p>
 * A work in progress.
 * <p>
 * The Workflow context is flattened to XML (hence the Serializable) and passed to the messaging service that will then reflate it and act on its contents.
 * Hence nothing that is not cleanly serializable should be added to the context (no dao's etc). Once the messaging service is finished it will flatten the
 * context back to XML and then return that structure to the application. In this way the workflow tasks are decoupled from the application, I assume with the
 * eventual goal of allowing long running tasks to be run in the background without impacting the user.
 * 
 * @see MessageService#sendFileProcessingRequest(Workflow, FileStoreFile...)
 * @author Adam Brin
 */
@XmlRootElement
public final class WorkflowContext implements Serializable {

    private static final long serialVersionUID = -1020989469518487007L;

    private String baseUrl;

    private List<FileStoreFile> versions = new ArrayList<>();
    private FileStoreFile originalFile;
    private File workingDirectory = null;
    private int numPages = -1;
    private transient Filestore filestore;
    private String primaryExtension;
    private boolean tdarFile;
    private boolean processedSuccessfully = false;
    private boolean hasDimensions;
    private boolean dataTableSupported;
    private Class<? extends Workflow> workflowClass;
    private List<String> dataTablesToCleanup = new ArrayList<>();
    private transient List<TDataTable> dataTables = new ArrayList<>();
    private transient List<TDataTableRelationship> relationships = new ArrayList<>();
    private boolean okToStoreInFilestore = true;
    private boolean codingSheet;
    // I would be autowired, but going across the message service and serializing/deserializing, better to just "inject"
    private transient ImportDatabase targetDatabase;

    private List<ExceptionWrapper> exceptions = new ArrayList<>();

    private boolean isErrorFatal;

    private Class<? extends DatasetConverter> datasetConverter;
    private Set<String> archivePartExtensions = new HashSet<>(Arrays.asList("asc","db","e57","jpg","mtl","obj","pc","pdf","pod","pts","py","txt","xlsx","xls","ds_store"));

    public WorkflowContext() {
    }

    public WorkflowContext(Filestore store) {
        this.filestore = store;
    }

    /**
     * <b>Don't use</b>: currently not yet implemented!
     */
    public void logTask(Task t, StringBuilder message) {
        // TODO!
    }

    /*
     * All of the derivative versions of the file
     */
    @XmlElementWrapper(name = "versions")
    @XmlElement(name = "versionFile")
    public List<FileStoreFile> getVersions() {
        if (versions == null) {
            versions = new ArrayList<>();
        }
        return versions;
    }

    public void addVersion(FileStoreFile version) {
        if (this.versions == null) {
            this.versions = new ArrayList<>();
        }
        this.versions.add(version);
    }

    @XmlElement(name = "originalFile")
    public FileStoreFile getOriginalFile() {
        return originalFile;
    }

    /*
     * Get the Original File
     */
    public void setOriginalFile(FileStoreFile originalFile) {
        this.originalFile = originalFile;
    }

    /*
     * temp directory
     */
    public File getWorkingDirectory() {
        if (workingDirectory == null) {
            workingDirectory = TdarConfiguration.getInstance().getTempDirectory();
            workingDirectory = new File(workingDirectory, "workflow");
            if (!workingDirectory.exists()) {
                workingDirectory.mkdir();
            }
            workingDirectory = new File(workingDirectory, Thread.currentThread().getName() + "-" + System.currentTimeMillis());
            workingDirectory.mkdirs();
        }
        return workingDirectory;
    }

    public String toXML() throws Exception {
        StringWriter sw = new StringWriter();
        JAXBContext jc = JAXBContext.newInstance(WorkflowContext.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, getPairedSchemaUrl());
        // marshaller.setProperty(Marshaller.JAXB_, urlService.getSchemaUrl());
//        logger.trace("converting: {}", this);
        marshaller.marshal(this, sw);
        return sw.toString();
    }

    public void setNumPages(int numPages) {
        this.numPages = numPages;
    }

    public int getNumPages() {
        return numPages;
    }
    
    public String getPairedSchemaUrl() {
        return String.format("%s/schema/current schema.xsd", getBaseUrl());
    }

    /*
     * Get the tDAR Base URL
     */
    public String getBaseUrl() {
        if (baseUrl == null) {
            baseUrl = StringUtils.stripEnd(TdarConfiguration.getInstance().getBaseSecureUrl().trim(), "/");
        }
        return baseUrl;
    }


    /**
     * @param filestore
     *            the filestore to set
     */
    public void setFilestore(Filestore filestore) {
        this.filestore = filestore;
    }

    /**
     * @return the filestore
     */
    @XmlTransient
    public Filestore getFilestore() {
        return filestore;
    }

    public boolean isProcessedSuccessfully() {
        return processedSuccessfully;
    }

    /**
     * Do not call this! it is used by the Workflow instance when processing tasks, and any setting made by the tasks will be overwritten.
     * 
     * @see Workflow#run(WorkflowContext)
     */
    public void setProcessedSuccessfully(boolean processed) {
        this.processedSuccessfully = processed;
    }

    public List<String> getDataTablesToCleanup() {
        return dataTablesToCleanup;
    }

    public void setDataTablesToCleanup(List<String> dataTablesToCleanup) {
        this.dataTablesToCleanup = dataTablesToCleanup;
    }

    public void setTargetDatabase(ImportDatabase tdarDataImportDatabase) {
        this.targetDatabase = tdarDataImportDatabase;
    }

    @XmlTransient
    public ImportDatabase getTargetDatabase() {
        return this.targetDatabase;
    }

    /**
     * Keeps a history of the exceptions that are thrown by the task run method if it exits abnormally.
     * If you have an exception you want recorded during that run, that isn't thrown out of the run, then add it using this method!
     * That sure beats calling getExceptions().add(...), and makes for a consistent interface.
     * 
     * @see Workflow#run(WorkflowContext)
     * @see Task#run()
     * @param e
     *            The exception that has brought the Task#run to an untimely demise..
     */
    public void addException(Throwable e) {
        int maxDepth = 4;
        Throwable thrw = e;
        StringBuilder sb = new StringBuilder();

        sb.append(e.getMessage());
        while ((thrw.getCause() != null) && (maxDepth > -1)) {
            thrw = thrw.getCause();
            if (StringUtils.isNotBlank(thrw.getMessage())) {
                sb.append(": ").append(thrw.getMessage());
            }
            maxDepth--;
        }

        ExceptionWrapper exceptionWrapper = new ExceptionWrapper(sb.toString(), e);
        if (e instanceof NonFatalWorkflowException || thrw instanceof NonFatalWorkflowException) {
            exceptionWrapper.setFatal(false);
        }
        this.getExceptions().add(exceptionWrapper);
    }

    /**
     * If you find yourself calling this to add an exception, <b>first ask: why aren't I using addException()?</b>
     * 
     * @see #addException(Throwable)
     * @return the exceptions recorded during the executions of the tasks.
     */
    @XmlElementWrapper(name = "exceptions")
    @XmlElement(name = "exception")
    public List<ExceptionWrapper> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<ExceptionWrapper> exceptions) {
        this.exceptions = exceptions;
    }

    public Class<? extends Workflow> getWorkflowClass() {
        return workflowClass;
    }

    public void setWorkflowClass(Class<? extends Workflow> workflowClass) {
        this.workflowClass = workflowClass;
    }

    @XmlTransient
    public String getExceptionAsString() {
        String exceptions = StringUtils.join(getExceptions(), "\n");
        if (StringUtils.isNotBlank(exceptions)) {
            exceptions = StringUtils.replace(exceptions, TdarConfiguration.getInstance().getFileStoreLocation(), "");
        }
        return exceptions;
    }

    public boolean isErrorFatal() {
        return isErrorFatal;
    }

    /**
     * A subtle one. Your task might have thrown an exception, but was it fatal? ie: was it an error or a warning? If it was an error best set this to true,
     * otherwise don't bother.
     * 
     * @see WorkflowContextService#processContext(WorkflowContext)
     * @param isErrorFatal
     *            If true, then there was an error that should be reported as an error, not a warning...
     */
    public void setErrorFatal(boolean isErrorFatal) {
        this.isErrorFatal = isErrorFatal;
    }

    public void clear() {
        getDataTables().clear();
        getRelationships().clear();
        versions = null;
        originalFile = null;

        // TODO Auto-generated method stub

    }

    public boolean isOkToStoreInFilestore() {
        return okToStoreInFilestore;
    }

    public void setOkToStoreInFilestore(boolean okToStoreInFilestore) {
        this.okToStoreInFilestore = okToStoreInFilestore;
    }

    public List<TDataTable> getDataTables() {
        return dataTables;
    }

    public void setDataTables(List<TDataTable> dataTables) {
        this.dataTables = dataTables;
    }

    public List<TDataTableRelationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<TDataTableRelationship> relationships) {
        this.relationships = relationships;
    }

    public Class<? extends DatasetConverter> getDatasetConverter() {
        return datasetConverter;
    }

    public void setDatasetConverter(Class<? extends DatasetConverter> class1) {
        this.datasetConverter = class1;
    }

    public boolean isHasDimensions() {
        return hasDimensions;
    }

    public void setHasDimensions(boolean hasDimensions) {
        this.hasDimensions = hasDimensions;
    }

    public boolean isDataTableSupported() {
        return dataTableSupported;
    }

    public void setDataTableSupported(boolean dataTableSupported) {
        this.dataTableSupported = dataTableSupported;
    }

    public String getPrimaryExtension() {
        return primaryExtension;
    }

    public void setPrimaryExtension(String primaryExtension) {
        this.primaryExtension = primaryExtension;
    }

    public boolean isCodingSheet() {
        return codingSheet;
    }

    public void setCodingSheet(boolean codingSheet) {
        this.codingSheet = codingSheet;
    }

    public boolean isTdarFile() {
        return tdarFile;
    }

    public void setTdarFile(boolean tdarFile) {
        this.tdarFile = tdarFile;
    }
    
    public Set<String> getArchivePartExtensions() {
        return this.archivePartExtensions;
    }

    public void setArchivePartExtensions(Set<String> archivePartExtensions) {
        this.archivePartExtensions = archivePartExtensions;
    }

}
