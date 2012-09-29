package org.tdar.core.bean.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.tdar.core.bean.entity.Person;
import org.tdar.utils.resource.PartitionedResourceResult;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * s * $Id$
 * 
 * @author <a href='Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Revision$
 */

@Entity
@Table(name = "project")
@Indexed
@XStreamAlias("project")
public class Project extends Resource implements Comparable<Project> {

    private static final long serialVersionUID = -3339534452963234622L;

    // FIXME: remove redundant fields, perhaps implement jsonmodel in these other classes (keywords, coveragedate, etc..)
    private static final String[] JSON_PROPERTIES = {
            // keyword properties
            "label", "approved", "id",

            // resource properties
            "title", "description",
            "cultureKeywords", "materialKeywords", "geographicKeywords", "siteNameKeywords",
            "siteTypeKeywords", "temporalKeywords", "calendarDate", "radiocarbonDate",
            "firstLatitudeLongitudeBox", "otherKeywords", "investigationTypes", "resourceType",

            // derived properties
            "approvedCultureKeywords", "approvedSiteTypeKeywords",
            "uncontrolledCultureKeywords", "uncontrolledSiteTypeKeywords",

            // CoverageDate properties
            "startDate", "endDate",

            // latlongbox properties
            "minObfuscatedLongitude", "maxObfuscatedLongitude",
            "minObfuscatedLatitude", "maxObfuscatedLatitude"
    };

    public static final Project NULL = new Project() {
        private static final long serialVersionUID = -8849690416412685818L;
        // FIXME: get rid of this if not needed.
        private transient ThreadLocal<Person> personThreadLocal = new ThreadLocal<Person>();

        @Override
        public Person getSubmitter() {
            return personThreadLocal.get();
        }

        @Override
        public void setSubmitter(Person person) {
            personThreadLocal.set(person);
        }

        @Override
        public Long getId() {
            return -1L;
        }

        @Override
        public String getTitle() {
            return "No Associated Project";
        }
    };

    @Deprecated
    // used only by hibernate to instantiate a sparsely managed Project Title&Id for freemarker
    public Project(Long id, String title) {
        setId(id);
        setTitle(title);
        setResourceType(ResourceType.PROJECT);
    }

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "project", fetch = FetchType.LAZY)
    @IndexedEmbedded
    @XStreamOmitField
    private Set<InformationResource> informationResources = new HashSet<InformationResource>();

    // XXX: currently used to more easily split up the generic InformationResources into categories.
    @Transient
    @XStreamOmitField
    private List<Document> documents;
    @Transient
    @XStreamOmitField
    private List<Dataset> datasets;
    @Transient
    @XStreamOmitField
    private List<CodingSheet> codingSheets;
    @Transient
    @XStreamOmitField
    private List<Ontology> ontologies;
    @Transient
    @XStreamOmitField
    private List<Image> images;
    
    @Transient
    @XStreamOmitField
    private List<SensoryData> sensoryDataDocuments;
    

    public Project() {
        setResourceType(ResourceType.PROJECT);
    }

    private synchronized void partitionInformationResources() {
        PartitionedResourceResult p = new PartitionedResourceResult(getInformationResources());
        codingSheets = p.getResourcesOfType(CodingSheet.class);
        datasets = p.getResourcesOfType(Dataset.class);
        documents = p.getResourcesOfType(Document.class);
        ontologies = p.getResourcesOfType(Ontology.class);
        images = p.getResourcesOfType(Image.class);
        sensoryDataDocuments = p.getResourcesOfType(SensoryData.class);
        

        Collections.sort(codingSheets);
        Collections.sort(datasets);
        Collections.sort(documents);
        Collections.sort(ontologies);
        Collections.sort(images);
        Collections.sort(sensoryDataDocuments);
    }

    // FIXME: this would be better done in HQL instead via ProjectService or the like..
    public synchronized List<CodingSheet> getCodingSheets() {
        if (codingSheets == null) {
            partitionInformationResources();
        }
        return codingSheets;
    }

    public synchronized List<Dataset> getDatasets() {
        if (datasets == null) {
            partitionInformationResources();
        }
        return datasets;
    }

    public synchronized List<Document> getDocuments() {
        if (documents == null) {
            partitionInformationResources();
        }
        return documents;
    }

    public synchronized List<Ontology> getOntologies() {
        if (ontologies == null) {
            partitionInformationResources();
        }
        return ontologies;
    }

    public synchronized List<Image> getImages() {
        if (images == null) {
            partitionInformationResources();
        }
        return images;
    }
    
    public synchronized List<SensoryData> getSensoryDataDocuments() {
        if (sensoryDataDocuments == null) {
            partitionInformationResources();
        }
        return sensoryDataDocuments;
    }
    

    public Set<InformationResource> getInformationResources() {
        return informationResources;
    }

    @Transient
    public SortedSet<InformationResource> getSortedInformationResources() {
        return getSortedInformationResources(new Comparator<InformationResource>() {
            @Override
            public int compare(InformationResource a, InformationResource b) {
                int comparison = a.getTitle().compareTo(b.getTitle());
                return (comparison == 0) ? a.getId().compareTo(b.getId()) : comparison;
            }
        });
    }

    @Transient
    public synchronized SortedSet<InformationResource> getSortedInformationResources(Comparator<InformationResource> comparator) {
        TreeSet<InformationResource> sortedDatasets = new TreeSet<InformationResource>(comparator);
        sortedDatasets.addAll(getInformationResources());
        return sortedDatasets;
    }

    public synchronized void setInformationResources(Set<InformationResource> informationResources) {
        this.informationResources = informationResources;
    }

    @Override
    public int compareTo(Project project) {
        return getTitle().compareTo(project.getTitle());
    }

    @Override
    protected String[] getIncludedJsonProperties() {
        List<String> list = new ArrayList<String>(Arrays.asList(super.getIncludedJsonProperties()));
        list.addAll(Arrays.asList(JSON_PROPERTIES));
        return list.toArray(new String[list.size()]);
    }

}
