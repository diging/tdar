package org.tdar.core.service.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.collection.DownloadAuthorization;
import org.tdar.core.bean.collection.WhiteLabelCollection;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.bean.resource.Document;
import org.tdar.core.bean.resource.FileType;
import org.tdar.core.bean.resource.InformationResource;
import org.tdar.core.bean.resource.InformationResourceFile;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.bean.resource.VersionType;
import org.tdar.core.bean.statistics.FileDownloadStatistic;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.dao.resource.ResourceCollectionDao;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.core.service.GenericService;
import org.tdar.core.service.PdfService;
import org.tdar.core.service.external.AuthorizationService;
import org.tdar.filestore.FileStoreFile;
import org.tdar.filestore.Filestore.ObjectType;
import org.tdar.utils.PersistableUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.opensymphony.xwork2.TextProvider;

/**
 * $Id$
 * 
 * 
 * @author Jim deVos
 * @version $Rev$
 */
@Service
public class DownloadService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final PdfService pdfService;
    private final GenericService genericService;

    private final Cache<Long, List<Integer>> downloadLock;

    private final AuthorizationService authorizationService;

    private final ResourceCollectionDao resourceCollectionDao;
    
    @Autowired
    public DownloadService(PdfService pdfService, GenericService genericService, AuthorizationService authorizationService, ResourceCollectionDao resourceCollectionDao) {
        this.pdfService = pdfService;
        this.genericService = genericService;
        this.authorizationService = authorizationService;
        this.resourceCollectionDao = resourceCollectionDao;
        this.downloadLock = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();

    }

    @Transactional(readOnly = false)
    public void registerDownload(List<FileDownloadStatistic> stats) {
        if (CollectionUtils.isNotEmpty(stats)) {
            genericService.saveOrUpdate(stats);
        }
    }

    @Transactional(readOnly = true)
    public DownloadTransferObject constructDownloadTransferObject(DownloadTransferObject dto) {
        for (InformationResourceFileVersion irFileVersion : dto.getVersionsToDownload()) {
            addFileToDownload(irFileVersion, dto);
            dto.setFileName(irFileVersion.getFilename());
            if (!irFileVersion.isDerivative()) {
                logger.debug("User {} is trying to DOWNLOAD: {} ({}: {})", dto.getAuthenticatedUser(), irFileVersion, TdarConfiguration.getInstance()
                        .getSiteAcronym(),
                        irFileVersion.getInformationResourceFile().getInformationResource().getId());
                InformationResourceFile irFile = irFileVersion.getInformationResourceFile();
                addStatistics(dto, irFile);
            }
        }
        return dto;
    }

    private void addStatistics(DownloadTransferObject dto, InformationResourceFile irFile) {
        // don't count download stats if you're downloading your own stuff
        TdarUser user = dto.getAuthenticatedUser();
        if (!PersistableUtils.isEqual(irFile.getInformationResource().getSubmitter(), user) && !authorizationService.isEditor(user)) {
            FileDownloadStatistic stat = new FileDownloadStatistic(new Date(), irFile);
            dto.getStatistics().add(stat);
        }
    }

    public void releaseDownloadLock(TdarUser authenticatedUser, List<InformationResourceFileVersion> versionsToDownload) {
        InformationResourceFileVersion[] array = new InformationResourceFileVersion[0];
        if (versionsToDownload != null) {
            array = versionsToDownload.toArray(new InformationResourceFileVersion[0]);
        }
        if (isUnauthenticatedOrThumbnail(authenticatedUser, array)) {
            return;
        }
        Long key = authenticatedUser.getId();
        List<Integer> list = downloadLock.getIfPresent(key);
        if (CollectionUtils.isNotEmpty(list)) {
            list.remove(new Integer(array.hashCode()));
        }

        if (CollectionUtils.isEmpty(list)) {
            downloadLock.invalidate(key);
        }
    }

    public void enforceDownloadLock(TdarUser authenticatedUser, List<InformationResourceFileVersion> versionsToDownload) {
        InformationResourceFileVersion[] array = new InformationResourceFileVersion[0];
        if (versionsToDownload != null) {
            array = versionsToDownload.toArray(new InformationResourceFileVersion[0]);
        }
        if (isUnauthenticatedOrThumbnail(authenticatedUser, array)) {
            return;
        }
        Long key = authenticatedUser.getId();
        List<Integer> list = downloadLock.getIfPresent(key);
        if (list == null) {
            list = new ArrayList<>();
        }
        if (list.contains(array.hashCode())) {
            if (TdarConfiguration.getInstance().shouldThrowExceptionOnConcurrentUserDownload()) {
                throw new TdarRecoverableRuntimeException("downloadService.duplicate_download");
            } else {
                logger.error("too many concurrent downloads of the same file {} by one user: {}",array, authenticatedUser);
            }
        }

        if (list.size() > 10) {
            if (TdarConfiguration.getInstance().shouldThrowExceptionOnConcurrentUserDownload()) {
                throw new TdarRecoverableRuntimeException("downloadService.too_many_concurrent_download");
            } else {
                logger.error("too many concurrent downloads ({}) by one user: {}",list.size(), authenticatedUser);
            }
        }

        list.add(new Integer(versionsToDownload.hashCode()));
        downloadLock.put(key, list);
    }

    private boolean isUnauthenticatedOrThumbnail(TdarUser authenticatedUser, InformationResourceFileVersion[] irFileVersions) {
        return PersistableUtils.isNullOrTransient(authenticatedUser) || CollectionUtils.size(irFileVersions) == 1 && irFileVersions[0].isThumbnail();
    }

    private String addFileToDownload(InformationResourceFileVersion irFileVersion, DownloadTransferObject dto) {
        File transientFile = irFileVersion.getTransientFile();
        // setting original filename on file
        String actualFilename = irFileVersion.getInformationResourceFile().getFilename();
        DownloadFile resourceFile = new DownloadFile(transientFile, actualFilename, irFileVersion);

        // If it's a PDF, add the cover page if we can, if we fail, just send the original file
        if (irFileVersion.getExtension().equalsIgnoreCase("PDF") && dto.isIncludeCoverPage()) {
            resourceFile = new DownloadPdfFile((Document) dto.getInformationResource(), irFileVersion, pdfService, dto.getAuthenticatedUser(),
                    dto.getTextProvider(), dto.getCoverPageLogo());
        }

        if (FileType.IMAGE != irFileVersion.getInformationResourceFile().getInformationResourceFileType()) {
            dto.setAttachment(true);
        }

        dto.getDownloads().add(resourceFile);
        return actualFilename;
        // downloadMap.put(resourceFile, irFileVersion.getFilename());
    }

    @Transactional(readOnly = false)
    public DownloadTransferObject validateFilterAndSetupDownload(TdarUser authenticatedUser, InformationResourceFileVersion versionToDownload,
            InformationResource resourceToDownload, boolean includeCoverPage, TextProvider textProvider, DownloadAuthorization authorization,
            boolean countDownload) {
        List<InformationResourceFileVersion> versionsToDownload = new ArrayList<>();
        if (PersistableUtils.isNotNullOrTransient(versionToDownload)) {
            versionsToDownload.add(versionToDownload);
        }

        DownloadResult issue = DownloadResult.SUCCESS;
        if (PersistableUtils.isNotNullOrTransient(resourceToDownload)) {
            for (InformationResourceFile irf : resourceToDownload.getInformationResourceFiles()) {
                if (irf.isDeleted()) {
                    continue;
                }
                versionsToDownload.add(irf.getLatestUploadedOrArchivalVersion());
                logger.trace("adding: {}", irf.getLatestUploadedVersion());
            }
        } else if (CollectionUtils.isNotEmpty(versionsToDownload)) {
            // trying to address a casting issue where we're getting a javassist version and not a tdar information resource
            resourceToDownload = versionsToDownload.get(0).getInformationResourceFile().getInformationResource();
            if (resourceToDownload.getResourceType().isDocument()) {
                resourceToDownload = genericService.find(Document.class, resourceToDownload.getId());
            }
        } else {
            issue = DownloadResult.ERROR;
        }

        File coverLogo = getCoverLogo(resourceToDownload);
        
        DownloadTransferObject dto = new DownloadTransferObject(resourceToDownload, authenticatedUser, textProvider, this, authorization);
        dto.setCoverPageLogo(coverLogo);
        dto.setIncludeCoverPage(includeCoverPage);
        if (issue != DownloadResult.SUCCESS) {
            dto.setResult(issue);
            return dto;
        }
        Iterator<InformationResourceFileVersion> iter = versionsToDownload.iterator();
        while (iter.hasNext()) {
            InformationResourceFileVersion version = iter.next();
            if (!authorizationService.canDownload(version, authenticatedUser) && authorization == null) {
                logger.warn("thumbail request: resource is confidential/embargoed: {}", version);
                dto.setResult(DownloadResult.FORBIDDEN);
                return dto;
            }

            if (version.getInformationResourceFile().isDeleted() && !authorizationService.isEditor(authenticatedUser)) {
                logger.debug("requesting deleted file");
                iter.remove();
                continue;
            }

            File resourceFile = null;
            try {
                resourceFile = TdarConfiguration.getInstance().getFilestore().retrieveFile(ObjectType.RESOURCE, version);
                version.setTransientFile(resourceFile);
            } catch (FileNotFoundException e1) {
                logNotFound(version, e1, null);
                dto.setResult(DownloadResult.NOT_FOUND);
                return dto;
            }

            if ((resourceFile == null)) {
                logNotFound(version, null, null);
                dto.setResult(DownloadResult.NOT_FOUND);
                return dto;
            }

            if (!resourceFile.exists()) {
                logNotFound(version, null, resourceFile.getAbsolutePath());
                dto.setResult(DownloadResult.NOT_FOUND);
                return dto;
            }

        }

        if (CollectionUtils.isEmpty(versionsToDownload)) {
            dto.setResult(DownloadResult.ERROR);
            return dto;
        }

        logger.info("user {} downloaded {} ({})", authenticatedUser, versionToDownload, resourceToDownload);
        dto.setVersionsToDownload(versionsToDownload);
        try {
            constructDownloadTransferObject(dto);
            if (countDownload) {
                registerDownload(dto.getStatistics());
            }
        } catch (TdarRecoverableRuntimeException tre) {
            logger.error("ERROR IN Download: {}", tre);
            dto.setResult(DownloadResult.ERROR);
            return dto;
        }
        dto.setResult(DownloadResult.SUCCESS);
        return dto;
    }

    private File getCoverLogo(InformationResource resourceToDownload) {
        WhiteLabelCollection whiteLabelCollection = resourceCollectionDao.getWhiteLabelCollectionForResource(resourceToDownload);
        if (whiteLabelCollection != null && whiteLabelCollection.isCustomDocumentLogoEnabled()) {
            VersionType small = VersionType.WEB_SMALL;
            FileStoreFile proxy = new FileStoreFile(ObjectType.COLLECTION, small, whiteLabelCollection.getId(), "logo" + small.toPath() + ".jpg");
            try {
                return  TdarConfiguration.getInstance().getFilestore().retrieveFile(ObjectType.COLLECTION, proxy);
            } catch (FileNotFoundException e) {
                logger.warn("could not get cover logo:{}", e, e);
            }
        }
        return null;
    }

    private void logNotFound(InformationResourceFileVersion version, FileNotFoundException e1, String path) {
        if (TdarConfiguration.getInstance().isProductionEnvironment()) {
            if (e1 == null) {
                logger.error("FILE NOT FOUND: {} [{}]", version, path);
            } else {
                logger.error("FILE NOT FOUND: {} [{}]", version, path, e1);
            }
        } else {
            logger.warn("FileNotFound (FilestoreConfigured?):: {}");
        }
    }

    @Transactional(readOnly = false)
    /**
     * Validate, filter, and setup download for latest uploaded version of the specified InformationResourceFile.
     */
    public DownloadTransferObject validateFilterAndSetupDownload(TdarUser authenticatedUser, InformationResourceFile fileToDownload,
            boolean includeCoverPage, TextProvider textProvider, DownloadAuthorization authorization,
            boolean countDownload) {

        InformationResourceFileVersion fileVersion = fileToDownload.getLatestUploadedVersion();

        return validateFilterAndSetupDownload(authenticatedUser, fileVersion, null, includeCoverPage, textProvider,
                authorization, countDownload);
    }

}