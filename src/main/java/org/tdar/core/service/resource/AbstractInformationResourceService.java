package org.tdar.core.service.resource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.PersonalFilestoreTicket;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.InformationResourceFile;
import org.tdar.core.bean.resource.InformationResourceFile.FileAccessRestriction;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.bean.resource.Language;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.GenericDao;
import org.tdar.core.dao.resource.InformationResourceFileDao;
import org.tdar.core.dao.resource.ResourceDao;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.service.ServiceInterface;
import org.tdar.core.service.workflow.WorkflowResult;
import org.tdar.filestore.FileAnalyzer;
import org.tdar.filestore.Filestore;
import org.tdar.filestore.Filestore.BaseFilestore;
import org.tdar.filestore.personal.PersonalFilestore;
import org.tdar.struts.data.FileProxy;

/**
 * $Id: AbstractInformationResourceService.java 1466 2011-01-18 20:32:38Z abrin$
 * 
 * Provides basic InformationResource services including file management (via FileProxyS).
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Rev$
 */

public abstract class AbstractInformationResourceService<T extends InformationResource, R extends ResourceDao<T>> extends ServiceInterface.TypedDaoBase<T, R> {

    // FIXME: this should be injected
    private static final Filestore filestore = TdarConfiguration.getInstance().getFilestore();

    @Autowired
    private InformationResourceFileDao informationResourceFileDao;

    @Autowired
    private FileAnalyzer analyzer;
    @Autowired
    @Qualifier("genericDao")
    private GenericDao genericDao;

    @Transactional(readOnly = false)
    private void addInformationResourceFile(InformationResource resource, InformationResourceFile irFile, FileProxy proxy) throws IOException {
        // always set the download/version info and persist the relationships between the InformationResource and its IRFile.
        incrementVersionNumber(irFile);
        // genericDao.saveOrUpdate(resource);
        genericDao.saveOrUpdate(resource);
        irFile.setInformationResource(resource);
        proxy.setInformationResourceFileVersion(createVersionMetadataAndStore(irFile, proxy));
        setInformationResourceFileMetadata(irFile, proxy);
        for (FileProxy additionalVersion : proxy.getAdditionalVersions()) {
            logger.debug("Creating new version {}", additionalVersion);
            createVersionMetadataAndStore(irFile, additionalVersion);
        }
        genericDao.saveOrUpdate(irFile);
        resource.add(irFile);
        logger.debug("all versions for {}", irFile);
    }

    @Transactional(readOnly = true)
    private InformationResourceFile findInformationResourceFile(FileProxy proxy) {
        InformationResourceFile irFile = genericDao.find(InformationResourceFile.class, proxy.getFileId());
        if (irFile == null) {
            logger.error("{} had no findable InformationResourceFile.id set on it", proxy);
            // FIXME: throw an exception?
        }
        return irFile;
    }

    @Transactional
    public WorkflowResult processFileProxies(PersonalFilestore filestore, T resource, List<FileProxy> fileProxiesToProcess, Long ticketId)
            throws IOException {

        processMedataForFileProxies(resource, fileProxiesToProcess);
        fileProxiesToProcess = validateAndConsolidateProxies(fileProxiesToProcess);
        for (FileProxy proxy : fileProxiesToProcess) {
            InformationResourceFile irFile = proxy.getInformationResourceFile();
            InformationResourceFileVersion version = proxy.getInformationResourceFileVersion();
            logger.info("version: {} proxy: {} ", version, proxy);
            if (proxy.getAction().requiresWorkflowProcessing()) {
                switch (version.getFileVersionType()) {
                    case UPLOADED:
                    case UPLOADED_ARCHIVAL:
                        irFile.setInformationResourceFileType(analyzer.analyzeFile(version));
                        try {
                            analyzer.processFile(proxy.getInformationResourceFileVersion());
                        } catch (Exception e) {
                            logger.warn("caught exception {} while analyzing file {}", e, proxy.getFilename());
                        }
                        break;
                    default:
                        logger.debug("Not setting file type on irFile {} for VersionType {}", irFile, proxy.getVersionType());
                }
            }

        }
        WorkflowResult result = new WorkflowResult(fileProxiesToProcess);

        /*
         * FIXME: Should I purge regardless of errors??? Really???
         */
        if (ticketId != null) {
            filestore.purge(getDao().find(PersonalFilestoreTicket.class, ticketId));
        }
        return result;
    }

    @Transactional
    protected List<FileProxy> validateAndConsolidateProxies(List<FileProxy> fileProxiesToProcess) {
        // TODO Auto-generated method stub
        // if we're dealing with a composite type; find the proxy with the primary file; add all the rest to that and pass it in
        // also validate that the thing is "right"

        return fileProxiesToProcess;
    }

    @Transactional
    public void processFileProxyMetadata(InformationResource informationResource, FileProxy proxy) throws IOException {
        logger.debug("applying {} to {}", proxy, informationResource);
        // will be reassigned in a REPLACE or ADD_DERIVATIVE
        InformationResourceFile irFile = new InformationResourceFile();
        if (proxy.getAction().requiresExistingIrFile()) {
            irFile = findInformationResourceFile(proxy);
            if (irFile == null) {
                logger.error("FileProxy {} {} had no InformationResourceFile.id ({}) set on it", proxy.getFilename(), proxy.getAction(), proxy.getFileId());
                return;
            }
        }
        switch (proxy.getAction()) {
            case MODIFY_METADATA:
                // set sequence number and confidentiality
                setInformationResourceFileMetadata(irFile, proxy);
                genericDao.update(irFile);
                break;
            case REPLACE:
                // explicit fall through to ADD after loading the existing irFile to be replaced.
            case ADD:
                addInformationResourceFile(informationResource, irFile, proxy);
                break;
            case ADD_DERIVATIVE:
                createVersionMetadataAndStore(irFile, proxy);
                break;
            case DELETE:
                irFile.markAsDeleted();
                genericDao.update(irFile);
                break;
            case NONE:
                logger.debug("Taking no action on {} with proxy {}", informationResource, proxy);
                break;
            default:
                break;
        }
        proxy.setInformationResourceFile(irFile);
    }

    @Transactional
    public void processMedataForFileProxies(InformationResource informationResource, List<FileProxy> proxies) throws IOException {
        for (FileProxy proxy : proxies) {
            processFileProxyMetadata(informationResource, proxy);
        }
    }

    private void setInformationResourceFileMetadata(InformationResourceFile irFile, FileProxy fileProxy) {
        irFile.setRestriction(fileProxy.getRestriction());
        Integer sequenceNumber = fileProxy.getSequenceNumber();
        if (fileProxy.getRestriction() == FileAccessRestriction.EMBARGOED) {
            if (irFile.getDateMadePublic() == null) {
                Calendar calendar = Calendar.getInstance();
                // set date made public to 5 years now.
                calendar.add(Calendar.YEAR, TdarConfiguration.getInstance().getEmbargoPeriod());
                irFile.setDateMadePublic(calendar.getTime());
            }
        } else {
            irFile.setDateMadePublic(null);
        }
        if (sequenceNumber == null) {
            logger.warn("No sequence number set on file proxy {}, existing sequence number was {}", fileProxy, irFile.getSequenceNumber());
        }
        else {
            irFile.setSequenceNumber(sequenceNumber);
        }
    }

    private void incrementVersionNumber(InformationResourceFile irFile) {
        irFile.incrementVersionNumber();
        irFile.clearStatus();
        logger.info("incremented version number and reset download and status for irfile: {}", irFile, irFile.getLatestVersion());
    }

    @Transactional(readOnly = false)
    public void reprocessInformationResourceFiles(Collection<InformationResourceFile> informationResourceFiles) {
        Iterator<InformationResourceFile> fileIterator = informationResourceFiles.iterator();
        while (fileIterator.hasNext()) {
            InformationResourceFile irFile = fileIterator.next();
            InformationResourceFileVersion original = irFile.getLatestUploadedVersion();
            Iterator<InformationResourceFileVersion> iterator = irFile.getInformationResourceFileVersions().iterator();
            // List<InformationResourceFileVersion> toDelete = new ArrayList<InformationResourceFileVersion>();
            while (iterator.hasNext()) {
                InformationResourceFileVersion version = iterator.next();
                if (!version.equals(original) && !version.isUploaded() && !version.isArchival()) {
                    iterator.remove();
                    informationResourceFileDao.delete(version);
                }
            }
            // this is a known case where we need to purge the session
            genericDao.synchronize();
            try {
                analyzer.processFile(original);
            } catch (Exception e) {
                logger.warn("caught exception {} while analyzing file {}", e, original.getFilename());
            }
        }

    }

    @Transactional(readOnly = false)
    private InformationResourceFileVersion createVersionMetadataAndStore(InformationResourceFile irFile, FileProxy fileProxy) throws IOException {
        String filename = BaseFilestore.sanitizeFilename(fileProxy.getFilename());
        if (fileProxy.getFile() == null || !fileProxy.getFile().exists()) {
            throw new TdarRecoverableRuntimeException("something went wrong, file " + fileProxy.getFilename() + " does not exist");
        }
        InformationResourceFileVersion version = new InformationResourceFileVersion(fileProxy.getVersionType(), filename, irFile);
        if (irFile.isTransient()) {
            genericDao.saveOrUpdate(irFile);
        }

        irFile.addFileVersion(version);
        filestore.store(fileProxy.getFile(), version);
        genericDao.save(version);
        genericDao.saveOrUpdate(irFile);
        return version;
    }

    public List<Language> findAllLanguages() {
        return Arrays.asList(Language.values());
    }

}
