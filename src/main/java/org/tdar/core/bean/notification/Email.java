package org.tdar.core.bean.notification;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.tdar.core.bean.FieldLength;
import org.tdar.core.bean.Persistable;

@Entity
@Table(name = "email_queue")
public class Email extends Persistable.Base {

    private static final String SEPARATOR_CHARS = ";";
    private static final long serialVersionUID = -5791173542997998092L;

    public enum Status {
        IN_REVIEW,
        QUEUED,
        ERROR,
        SENT;

        public boolean isInReview() {
            return this == IN_REVIEW;
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = FieldLength.FIELD_LENGTH_25)
    private Status status = Status.QUEUED;

    @Column(name = "user_generated", nullable = false, columnDefinition = "boolean default TRUE")
    private boolean userGenerated = true;

    @Lob
    @Type(type = "org.hibernate.type.StringClobType")
    @Column(name = "message")
    private String message;

    @Column(name = "subject", length = FieldLength.FIELD_LENGTH_1024)
    private String subject;

    @Column(name = "from_address", length = FieldLength.FIELD_LENGTH_255)
    private String from;

    @Column(name = "to_address", length = FieldLength.FIELD_LENGTH_1024)
    private String to;

    @Column(name = "date_created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date date = new Date();

    @Column(name = "date_sent")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateSent;

    @Column(name = "number_of_tries")
    private Integer numberOfTries = 5;

    @Column(name = "error_message", length = FieldLength.FIELD_LENGTH_2048)
    private String errorMessage;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Integer getNumberOfTries() {
        return numberOfTries;
    }

    public void setNumberOfTries(Integer numberOfTries) {
        this.numberOfTries = numberOfTries;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void addToAddress(String email) {
        if (StringUtils.isNotBlank(email)) {
            if (StringUtils.isNotBlank(to)) {
                this.to += SEPARATOR_CHARS;
                this.to += email;
            } else {
                this.to = email;
            }

        }
    }

    public String[] getToAsArray() {
        return StringUtils.split(to, SEPARATOR_CHARS);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDateSent() {
        return dateSent;
    }

    public void setDateSent(Date dateSent) {
        this.dateSent = dateSent;
    }

    @Override
    public String toString() {
        String fmt = "[id:%-5d from:%-20s to:%-20s sub:%-20s tries:%-3d status:%-6s]";
        String msg = String.format(fmt, getId(), from, to, StringUtils.left(subject, 20), numberOfTries, status);
        return msg;
    }

    public boolean isUserGenerated() {
        return userGenerated;
    }

    public void setUserGenerated(boolean userGenerated) {
        this.userGenerated = userGenerated;
    }
}
