package org.tdar.core.bean.resource;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DynamicBoost;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.tdar.core.bean.BulkImportField;
import org.tdar.core.bean.coverage.CoverageDate;
import org.tdar.core.bean.coverage.LatitudeLongitudeBox;
import org.tdar.core.bean.entity.Institution;
import org.tdar.core.bean.keyword.CultureKeyword;
import org.tdar.core.bean.keyword.GeographicKeyword;
import org.tdar.core.bean.keyword.InvestigationType;
import org.tdar.core.bean.keyword.MaterialKeyword;
import org.tdar.core.bean.keyword.OtherKeyword;
import org.tdar.core.bean.keyword.SiteNameKeyword;
import org.tdar.core.bean.keyword.SiteTypeKeyword;
import org.tdar.core.bean.keyword.TemporalKeyword;
import org.tdar.core.bean.resource.InformationResourceFileVersion.VersionType;
import org.tdar.index.LowercaseWhiteSpaceStandardAnalyzer;
import org.tdar.search.query.boost.InformationResourceBoostStrategy;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * $Id$
 * <p>
 * Represents a Resource with a file payload and additional metadata that can be one of the following:
 * </p>
 * <ol>
 * <li>Image
 * <li>Dataset file (Access, Excel)
 * <li>Document (PDF)
 * </ol>
 * 
 * @author <a href='Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */
@Entity
// @Indexed(index = "Resource")
@Table(name = "information_resource")
@DynamicBoost(impl = InformationResourceBoostStrategy.class)
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class InformationResource extends Resource implements Comparable<InformationResource> {

    private static final long serialVersionUID = -1534799746444826257L;
    private static final String[] JSON_PROPERTIES = { "inheritingCulturalInformation", "inheritingInvestigationInformation", "inheritingMaterialInformation",
            "inheritingOtherInformation", "inheritingSiteInformation", "inheritingSpatialInformation", "inheritingTemporalInformation",
            // FIXME: what is inheritingXInformation?
            // "inheritingXInformation"
            };

    public InformationResource() {

    }

    @Deprecated
    public InformationResource(Long id, String title) {
        setId(id);
        setTitle(title);
    }

    @Deprecated
    public InformationResource(Long id, String title, ResourceType type) {
        setId(id);
        setTitle(title);
        setResourceType(type);
    }

    @ManyToOne(optional = true)
    // @ContainedIn
    @XStreamOmitField
    private Project project;

    @Transient
    private Long projectId;

    @ManyToMany
    @JoinTable(name = "information_resource_related_citation", joinColumns = @JoinColumn(name = "information_resource_id"), inverseJoinColumns = @JoinColumn(
            name = "document_id"))
    private Set<Document> relatedCitations = new HashSet<Document>();

    @ManyToMany(cascade = CascadeType.ALL)
    @XStreamOmitField
    @JoinTable(name = "information_resource_source_citation", joinColumns = @JoinColumn(name = "information_resource_id"), inverseJoinColumns = @JoinColumn(
            name = "document_id"))
    private Set<Document> sourceCitations = new HashSet<Document>();

    @OneToMany(mappedBy = "informationResource", cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
    private Set<InformationResourceFile> informationResourceFiles = new HashSet<InformationResourceFile>();

    @BulkImportField
    @Enumerated(EnumType.STRING)
    @Column(name = "metadata_language")
    private LanguageEnum metadataLanguage;

    @BulkImportField
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_language")
    private LanguageEnum resourceLanguage;

    @Column(name = "available_to_public")
    private boolean availableToPublic;

    @Column(name = "external_reference", nullable = true)
    private boolean externalReference;

    @BulkImportField
    @Column(name = "copy_location")
    private String copyLocation;

    @Column(name = "last_uploaded")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUploaded;

    // a date in standard form that a resource will become public if availableToPublic was set to false.
    // This date may be extended by the publisher but will not extend past the publisher's death unless
    // special arrangements are made.
    @Column(name = "date_made_public")
    private Date dateMadePublic;

    // currently just a 4 digit year.
    @Column(name = "date_created")
    // @NumericField
    @Field(index = Index.UN_TOKENIZED)
    @BulkImportField
    private String dateCreated;

    // The institution providing this InformationResource
    @ManyToOne
    @JoinColumn(name = "provider_institution_id")
    @IndexedEmbedded
    private Institution resourceProviderInstitution;

    // downward inheritance sections
    @Column(name = "inheriting_investigation_information", nullable = false, columnDefinition = "boolean default FALSE")
    private boolean inheritingInvestigationInformation;
    @Column(name = "inheriting_site_information", nullable = false, columnDefinition = "boolean default FALSE")
    private boolean inheritingSiteInformation;
    @Column(name = "inheriting_material_information", nullable = false, columnDefinition = "boolean default FALSE")
    private boolean inheritingMaterialInformation;
    @Column(name = "inheriting_other_information", nullable = false, columnDefinition = "boolean default FALSE")
    private boolean inheritingOtherInformation;
    @Column(name = "inheriting_cultural_information", nullable = false, columnDefinition = "boolean default FALSE")
    private boolean inheritingCulturalInformation;
    @Column(name = "inheriting_spatial_information", nullable = false, columnDefinition = "boolean default FALSE")
    private boolean inheritingSpatialInformation;
    @Column(name = "inheriting_temporal_information", nullable = false, columnDefinition = "boolean default FALSE")
    private boolean inheritingTemporalInformation;

    /**
     * Returns the result of a String comparison between this
     * InformationResource's title and the given InformationResource's title. If
     * the titles are equal, uses compareTo between their Identifier, which is
     * guaranteed to be unique.
     * 
     * @see java.lang.String#compareTo(String)
     */
    public int compareTo(InformationResource informationResource) {
        int comparison = getTitle().compareTo(informationResource.getTitle());
        return (comparison == 0) ? getId().compareTo(informationResource.getId()) : comparison;
    }

    public LanguageEnum getMetadataLanguage() {
        return metadataLanguage;
    }

    public void setMetadataLanguage(LanguageEnum metadataLanguage) {
        this.metadataLanguage = metadataLanguage;
    }

    public LanguageEnum getResourceLanguage() {
        return resourceLanguage;
    }

    public void setResourceLanguage(LanguageEnum resourceLanguage) {
        this.resourceLanguage = resourceLanguage;
    }

    public boolean isAvailableToPublic() {
        return availableToPublic;
    }

    public void setAvailableToPublic(boolean availableToPublic) {
        this.availableToPublic = availableToPublic;
    }

    public Date getDateMadePublic() {
        return dateMadePublic;
    }

    public void setDateMadePublic(Date dateMadePublic) {
        this.dateMadePublic = dateMadePublic;
    }

    @Field(index = Index.UN_TOKENIZED, store = Store.YES)
    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Institution getResourceProviderInstitution() {
        return resourceProviderInstitution;
    }

    public void setResourceProviderInstitution(Institution resourceProviderInstitution) {
        this.resourceProviderInstitution = resourceProviderInstitution;
    }

    @XmlAttribute
    @XmlIDREF
    public Project getProject() {
        if (project == null) {
            return Project.NULL;
        }
        return project;
    }

    public void setProject(Project project) {
        if (project == Project.NULL) {
            this.project = null;
        } else {
            this.project = project;
        }
    }

    /**
     * Returns true if this resource is an externally referenced resource,
     * signifying that there is no uploaded file. The URL should then
     * 
     * @return
     */
    public boolean isExternalReference() {
        return externalReference;
    }

    public void setExternalReference(boolean externalReference) {
        this.externalReference = externalReference;
    }

    public Date getLastUploaded() {
        return lastUploaded;
    }

    public void setLastUploaded(Date lastUploaded) {
        this.lastUploaded = lastUploaded;
    }

    @Transient
    @Field(name = "projectId")
    @Analyzer(impl = KeywordAnalyzer.class)
    public Long getProjectId() {
        if (projectId != null)
            return projectId;
        if (project == null || project == Project.NULL)
            return null;
        projectId = project.getId();
        return projectId;
    }

    @Deprecated
    public void setProjectId(Long projectId) {
        // FIXME: jtd - added this method to assist w/ sensoryData xml creation export. In any other scenario you should probably be using setProject() to
        // implicitly set projectId.
        this.projectId = projectId;
    }

    public Set<Document> getRelatedCitations() {
        return relatedCitations;
    }

    public void setRelatedCitations(Set<Document> relatedCitations) {
        this.relatedCitations = relatedCitations;
    }

    public Set<Document> getSourceCitations() {
        return sourceCitations;
    }

    public void setSourceCitations(Set<Document> sourceCitations) {
        this.sourceCitations = sourceCitations;
    }

    public int getTotalNumberOfFiles() {
        return informationResourceFiles.size();
    }

    @XmlElementWrapper(name = "informationResourceFiles")
    @XmlElement(name = "informationResourceFile")
    public Set<InformationResourceFile> getInformationResourceFiles() {
        if (informationResourceFiles == null) {
            informationResourceFiles = new HashSet<InformationResourceFile>();
        }
        return informationResourceFiles;
    }

    public InformationResourceFile getFirstInformationResourceFile() {
        if (getInformationResourceFiles().isEmpty()) {
            return null;
        }
        return informationResourceFiles.iterator().next();
    }

    public void setInformationResourceFiles(Set<InformationResourceFile> informationResourceFiles) {
        this.informationResourceFiles = informationResourceFiles;
    }

    public void add(InformationResourceFile informationResourceFile) {
        getInformationResourceFiles().add(informationResourceFile);
        logger.debug("adding information resource file: {} ({})", informationResourceFile, informationResourceFiles.size());
    }

    public Collection<InformationResourceFileVersion> getLatestVersions() {
        ArrayList<InformationResourceFileVersion> latest = new ArrayList<InformationResourceFileVersion>();
        for (InformationResourceFile irfile : getInformationResourceFiles()) {
            latest.addAll(irfile.getLatestVersions());
        }
        return latest;
    }

    public Collection<InformationResourceFileVersion> getLatestVersions(String type) {
        return getLatestVersions(VersionType.valueOf(type));
    }

    public Collection<InformationResourceFileVersion> getLatestVersions(VersionType type) {
        ArrayList<InformationResourceFileVersion> latest = new ArrayList<InformationResourceFileVersion>();
        for (InformationResourceFile irfile : getInformationResourceFiles()) {
            InformationResourceFileVersion irfileVersion = irfile.getCurrentVersion(type);
            if (irfileVersion != null) {
                latest.add(irfileVersion);
            }
        }
        return latest;
    }

    public InformationResourceFileVersion getLatestUploadedVersion() {
        Collection<InformationResourceFileVersion> latestUploadedVersions = getLatestUploadedVersions();
        if (CollectionUtils.isEmpty(latestUploadedVersions)) {
            logger.warn("No latest uploaded version for {}", this);
            return null;
        }
        return getLatestUploadedVersions().iterator().next();
    }

    public Collection<InformationResourceFileVersion> getLatestUploadedVersions() {
        return getLatestVersions(VersionType.UPLOADED);
    }

    @Field
    @Analyzer(impl = LowercaseWhiteSpaceStandardAnalyzer.class)
    @Transient
    @Boost(0.5f)
    public String getContent() {
        if (!isAvailableToPublic()) {
            return "";
        }
        List<InputStream> streams = new ArrayList<InputStream>();
        for (InformationResourceFile irFile : getPublicFiles()) {
            try {
                InformationResourceFileVersion indexableVersion = irFile.getIndexableVersion();
                streams.add(new FileInputStream(indexableVersion.getFile()));
                logger.debug("getting indexed content for " + getId() + ": length:" + ("" + indexableVersion.getIndexableContent()).length());
            } catch (Exception e) {
                logger.trace("an exception occured while reading file: {} ", e);
            }
        }
        try {
            SequenceInputStream stream = new SequenceInputStream(Collections.enumeration(streams));
            return IOUtils.toString(stream);
        } catch (Exception e) {
            logger.warn("exception: {}", e);
        }

        return "";
    }

    public boolean isInheritingInvestigationInformation() {
        return inheritingInvestigationInformation;
    }

    public void setInheritingInvestigationInformation(boolean inheritingInvestigationInformation) {
        this.inheritingInvestigationInformation = inheritingInvestigationInformation;
    }

    public boolean isInheritingSiteInformation() {
        return inheritingSiteInformation;
    }

    public void setInheritingSiteInformation(boolean inheritingSiteInformation) {
        this.inheritingSiteInformation = inheritingSiteInformation;
    }

    public boolean isInheritingMaterialInformation() {
        return inheritingMaterialInformation;
    }

    public void setInheritingMaterialInformation(boolean inheritingMaterialInformation) {
        this.inheritingMaterialInformation = inheritingMaterialInformation;
    }

    public boolean isInheritingOtherInformation() {
        return inheritingOtherInformation;
    }

    public void setInheritingOtherInformation(boolean inheritingOtherInformation) {
        this.inheritingOtherInformation = inheritingOtherInformation;
    }

    public boolean isInheritingCulturalInformation() {
        return inheritingCulturalInformation;
    }

    public void setInheritingCulturalInformation(boolean inheritingCulturalInformation) {
        this.inheritingCulturalInformation = inheritingCulturalInformation;
    }

    public boolean isInheritingSpatialInformation() {
        return inheritingSpatialInformation;
    }

    public void setInheritingSpatialInformation(boolean inheritingSpatialInformation) {
        this.inheritingSpatialInformation = inheritingSpatialInformation;
    }

    public boolean isInheritingTemporalInformation() {
        return inheritingTemporalInformation;
    }

    public void setInheritingTemporalInformation(boolean inheritingTemporalInformation) {
        this.inheritingTemporalInformation = inheritingTemporalInformation;
    }

    public String getCopyLocation() {
        return copyLocation;
    }

    public void setCopyLocation(String copyLocation) {
        this.copyLocation = copyLocation;
    }

    @IndexedEmbedded
    @Override
    public Set<InvestigationType> getActiveInvestigationTypes() {
        return project != null && isInheritingInvestigationInformation() ? project.getInvestigationTypes() : getInvestigationTypes();
    }

    @IndexedEmbedded
    @Override
    public Set<SiteNameKeyword> getActiveSiteNameKeywords() {
        return project != null && isInheritingSiteInformation() ? project.getSiteNameKeywords() : getSiteNameKeywords();
    }

    @IndexedEmbedded
    @Override
    public Set<SiteTypeKeyword> getActiveSiteTypeKeywords() {
        return project != null && isInheritingSiteInformation() ? project.getSiteTypeKeywords() : getSiteTypeKeywords();
    }

    public Set<SiteTypeKeyword> getActiveApprovedSiteTypeKeywords() {
        return project != null && isInheritingSiteInformation() ? project.getApprovedSiteTypeKeywords() : getApprovedSiteTypeKeywords();
    }

    public Set<SiteTypeKeyword> getActiveUncontrolledSiteTypeKeywords() {
        return project != null && isInheritingSiteInformation() ? project.getUncontrolledSiteTypeKeywords() : getUncontrolledSiteTypeKeywords();
    }

    @Override
    @IndexedEmbedded
    public Set<MaterialKeyword> getActiveMaterialKeywords() {
        return project != null && isInheritingMaterialInformation() ? project.getMaterialKeywords() : getMaterialKeywords();
    }

    @Override
    @IndexedEmbedded(targetElement = OtherKeyword.class)
    public Set<OtherKeyword> getActiveOtherKeywords() {
        return project != null && isInheritingOtherInformation() ? project.getOtherKeywords() : getOtherKeywords();
    }

    @Override
    @IndexedEmbedded
    public Set<CultureKeyword> getActiveCultureKeywords() {
        return project != null && isInheritingCulturalInformation() ? project.getCultureKeywords() : getCultureKeywords();
    }

    public Set<CultureKeyword> getActiveApprovedCultureKeywords() {
        return project != null && isInheritingCulturalInformation() ? project.getApprovedCultureKeywords() : getApprovedCultureKeywords();
    }

    public Set<CultureKeyword> getActiveUncontrolledCultureKeywords() {
        return project != null && isInheritingCulturalInformation() ? project.getUncontrolledCultureKeywords() : getUncontrolledCultureKeywords();
    }

    @Override
    @IndexedEmbedded
    public Set<GeographicKeyword> getActiveGeographicKeywords() {
        return project != null && isInheritingSpatialInformation() ? project.getGeographicKeywords() : getGeographicKeywords();
    }

    @Override
    @IndexedEmbedded
    public Set<LatitudeLongitudeBox> getActiveLatitudeLongitudeBoxes() {
        return project != null && isInheritingSpatialInformation() ? project.getLatitudeLongitudeBoxes() : getLatitudeLongitudeBoxes();
    }

    @Override
    @IndexedEmbedded
    public Set<TemporalKeyword> getActiveTemporalKeywords() {
        return project != null && isInheritingTemporalInformation() ? project.getTemporalKeywords() : getTemporalKeywords();
    }

    @Override
    @IndexedEmbedded
    public Set<CoverageDate> getActiveCoverageDates() {
        return project != null && isInheritingTemporalInformation() ? project.getCoverageDates() : getCoverageDates();
    }

    @Transient
    public boolean hasFiles() {
        return getInformationResourceFiles().size() > 0;
    }

    @Override
    protected String[] getIncludedJsonProperties() {
        ArrayList<String> allProperties = new ArrayList<String>(Arrays.asList(super.getIncludedJsonProperties()));
        allProperties.addAll(Arrays.asList(JSON_PROPERTIES));
        return allProperties.toArray(new String[allProperties.size()]);
    }

    @Transient
    public boolean hasConfidentialFiles() {
        return !getConfidentialFiles().isEmpty();
    }

    @Transient
    public List<InformationResourceFile> getFilesWithRestrictions(boolean confidential) {
        List<InformationResourceFile> confidentialFiles = new ArrayList<InformationResourceFile>();
        List<InformationResourceFile> publicFiles = new ArrayList<InformationResourceFile>();
        for (InformationResourceFile irFile : getInformationResourceFiles()) {
            if (irFile.isConfidential()) {
                confidentialFiles.add(irFile);
            } else {
                publicFiles.add(irFile);
            }
        }
        if (confidential) {
            return confidentialFiles;
        } else {
            return publicFiles;
        }
    }

    @Transient
    public List<InformationResourceFile> getConfidentialFiles() {
        return getFilesWithRestrictions(true);
    }

    @Transient
    public List<InformationResourceFile> getPublicFiles() {
        return getFilesWithRestrictions(false);
    }

}
