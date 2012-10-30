package org.tdar.core.bean;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.tdar.core.bean.entity.Person;
import org.tdar.filestore.personal.PersonalFileType;

/**
 * $Id$
 * 
 * This allows for asynchronous uploads by creating a ticket that tracks the filestore (where things are stored temporarily)
 * and the submitter. The ticket gets created at the beginning of the first upload, and is then kept open and available until
 * the user completes the resource submission process.
 * 
 * @author Jim DeVos
 * @version $Rev$
 */
@Entity
@Table(name = "personal_filestore_ticket")
public class PersonalFilestoreTicket extends Persistable.Base {

    private static final long serialVersionUID = 3712388159075958666L;
    private final static String[] JSON_PROPERTIES = { "id", "dateGenerated", "submitter" };

    @Column(nullable = false, name = "date_generated")
    private Date dateGenerated = new Date();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "personal_file_type")
    private PersonalFileType personalFileType;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false, name = "submitter_id")
    private Person submitter;

    @Column(length = 500)
    private String description;

    public Person getSubmitter() {
        return submitter;
    }

    public void setSubmitter(Person submitter) {
        this.submitter = submitter;
    }

    public PersonalFileType getPersonalFileType() {
        return personalFileType;
    }

    public void setPersonalFileType(PersonalFileType personalFileType) {
        this.personalFileType = personalFileType;
    }

    public Date getDateGenerated() {
        return dateGenerated;
    }

    public void setDateGenerated(Date dateGenerated) {
        this.dateGenerated = dateGenerated;
    }

    @Override
    protected String[] getIncludedJsonProperties() {
        return JSON_PROPERTIES;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

}
