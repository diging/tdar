package org.tdar.core.bean.resource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.validator.constraints.Length;
import org.tdar.core.bean.BulkImportField;
import org.tdar.core.bean.FieldLength;
import org.tdar.search.index.analyzer.TdarCaseSensitiveStandardAnalyzer;

/**
 * Represents a Document information resource.
 * 
 * The design decision was made to have null fields instead of overloading fields to mean different things for different
 * document types, e.g., a journal article has journal volume, journal name, and journal number instead of series name, series number,
 * and volume / # of volumes
 * 
 * NOTE: uses Resource.dateCreated as year published field
 * 
 * 
 * @author <a href='mailto:Allen.Lee@asu.edu'>Allen Lee</a>
 * @version $Rev$
 */
@Entity
@Indexed
@Table(name = "document", indexes = {
        @Index(name = "document_type_index", columnList = "document_type")
})
@XmlRootElement(name = "document")
public class Document extends InformationResource {

    private static final long serialVersionUID = 7895887664126751989L;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = FieldLength.FIELD_LENGTH_255)
    @Field(norms = Norms.NO, store = Store.YES, analyzer = @Analyzer(impl = TdarCaseSensitiveStandardAnalyzer.class))
    @BulkImportField(key ="DOCUMENT_TYPE")
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "degree", length = FieldLength.FIELD_LENGTH_50)
    @Field(norms = Norms.NO, store = Store.YES, analyzer = @Analyzer(impl = TdarCaseSensitiveStandardAnalyzer.class))
    @BulkImportField(key="DEGREE")
    private DegreeType degree;

    @BulkImportField(key="SERIES_NAME")
    @Column(name = "series_name")
    @Field
    @Length(max = FieldLength.FIELD_LENGTH_255)
    private String seriesName;

    @BulkImportField(key="SERIES_NUMBER")
    @Column(name = "series_number")
    @Length(max = FieldLength.FIELD_LENGTH_255)
    private String seriesNumber;

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @BulkImportField(key = "EDITION")
    @Length(max = FieldLength.FIELD_LENGTH_255)
    private String edition;

    @BulkImportField(key = "ISBN")
    @Field
    @Length(max = FieldLength.FIELD_LENGTH_255)
    @Analyzer(impl = KeywordAnalyzer.class)
    private String isbn;

    @BulkImportField(key = "BOOK_TITLE")
    @Length(max = FieldLength.FIELD_LENGTH_255)
    @Column(name = "book_title")
    @Field
    // @Boost(1.5f)
    private String bookTitle;

    @BulkImportField(key = "ISSN")
    @Field
    @Length(max = FieldLength.FIELD_LENGTH_255)
    @Analyzer(impl = KeywordAnalyzer.class)
    private String issn;

    @BulkImportField(key = "START_PAGE", order = 10)
    @Column(name = "start_page")
    @Length(max = 10)
    private String startPage;

    @BulkImportField(key = "END_PAGE", order = 11)
    @Column(name = "end_page")
    @Length(max = 10)
    private String endPage;

    @BulkImportField(key = "JOURNAL_NAME")
    @Column(name = "journal_name")
    @Field
    @Length(max = FieldLength.FIELD_LENGTH_255)
    private String journalName;

    @BulkImportField(key = "VOLUME")
    @Length(max = FieldLength.FIELD_LENGTH_255)
    private String volume;

    @BulkImportField(key = "NUM_VOLUMES")
    @Column(name = "number_of_volumes")
    private Integer numberOfVolumes;

    @BulkImportField(key = "JOURNAL_NUMBER")
    @Column(name = "journal_number")
    @Length(max = FieldLength.FIELD_LENGTH_255)
    private String journalNumber;

    public Document() {
        setResourceType(ResourceType.DOCUMENT);
        setDocumentType(DocumentType.BOOK);
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String type) {
        setDocumentType(DocumentType.valueOf(type));
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public String getSeriesNumber() {
        return seriesNumber;
    }

    public void setSeriesNumber(String seriesNumber) {
        this.seriesNumber = seriesNumber;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Integer getNumberOfVolumes() {
        return numberOfVolumes;
    }

    public void setNumberOfVolumes(Integer numberOfVolumes) {
        this.numberOfVolumes = numberOfVolumes;
    }

    @Deprecated
    public Integer getNumberOfPages() {
        return numberOfPages;
    }

    public Integer getTotalNumberOfPages() {
        Integer count = 0;
        if (CollectionUtils.isNotEmpty(getInformationResourceFiles())) {
            for (InformationResourceFile file : getInformationResourceFiles()) {
                if (!file.isDeleted() && (file.getNumberOfParts() != null)) {
                    count += file.getNumberOfParts();
                }
            }
            if (count > 0) {
                return count;
            }
        }
        return numberOfPages;
    }

    public void setNumberOfPages(Integer numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public String getStartPage() {
        return startPage;
    }

    public void setStartPage(String startPage) {
        this.startPage = startPage;
    }

    public String getEndPage() {
        return endPage;
    }

    public void setEndPage(String endPage) {
        this.endPage = endPage;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getIssn() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn = issn;
    }

    public String getJournalName() {
        return journalName;
    }

    public void setJournalName(String journalName) {
        this.journalName = journalName;
    }

    public String getJournalNumber() {
        return journalNumber;
    }

    public void setJournalNumber(String journalNumber) {
        this.journalNumber = journalNumber;
    }

    @Override
    public String getFormattedTitleInfo() {
        StringBuilder sb = new StringBuilder();
        appendIfNotBlank(sb, getTitle(), "", "");
        appendIfNotBlank(sb, getEdition(), ",", "");
        return sb.toString();
    }

    // FIXME: ADD IS?N

    @Override
    // TODO: refactor using MessageFormat or with a freemarker template
    public String getFormattedSourceInformation() {
        StringBuilder sb = new StringBuilder();
        switch (getDocumentType()) {
            case BOOK:
                appendIfNotBlank(sb, getPublisherLocation(), "", "");
                appendIfNotBlank(sb, getPublisherName(), ":", "");
                break;
            case BOOK_SECTION:
                appendIfNotBlank(sb, getBookTitle(), "", "In ");
                appendIfNotBlank(sb, getPageRange(), ".", "Pp. ");
                appendIfNotBlank(sb, getPublisherLocation(), ".", "");
                appendIfNotBlank(sb, getPublisherName(), ":", "");
                break;
            case CONFERENCE_PRESENTATION:
                appendIfNotBlank(sb, getPublisherName(), "", "Presented at ");
                appendIfNotBlank(sb, getPublisherLocation(), ",", "");
                break;
            case JOURNAL_ARTICLE:
                appendIfNotBlank(sb, getJournalName(), "", "");
                if (StringUtils.isNotBlank(getJournalNumber()) || StringUtils.isNotBlank(getVolume())) {
                    sb.append(".");
                }
                appendIfNotBlank(sb, getVolume(), "", "");
                if (StringUtils.isNotBlank(getJournalNumber())) {
                    appendIfNotBlank(sb, "(" + getJournalNumber() + ")", "", "");
                }
                appendIfNotBlank(sb, getPageRange(), ":", "");
                break;
            case OTHER:
                break;
            case THESIS:
                String degreetext = "";
                if (getDegree() != null) {
                    degreetext = getDegree().getLabel();
                }
                appendIfNotBlank(sb, degreetext + ".", "", "");
                appendIfNotBlank(sb, getPublisherName(), "", "");
                appendIfNotBlank(sb, getPublisherLocation(), ",", "");
                break;
        }
        if ((getDate() != null) && (getDate() != -1)) {
            appendIfNotBlank(sb, getDate().toString(), ".", "");
        }
        return sb.toString();
    }

    @Override
    public String getAdditonalKeywords() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getAdditonalKeywords()).append(" ").append(getBookTitle()).append(" ").
                append(" ").append(getIssn()).append(" ").append(getIsbn()).append(" ").append(getPublisher()).append(" ").
                append(getSeriesName());
        return sb.toString();
    }

    public String getPageRange() {
        StringBuilder sb = new StringBuilder();
        appendIfNotBlank(sb, getStartPage(), "", "");
        appendIfNotBlank(sb, getEndPage(), "-", "");
        return sb.toString().replaceAll("\\s", "");
    }

    public DegreeType getDegree() {
        return degree;
    }

    public void setDegree(DegreeType degree) {
        this.degree = degree;
    }

    @Override
    @Transient
    public boolean isSupportsThumbnails() {
        return true;
    }
}
