package org.tdar.core.bean.entity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Check;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.validator.constraints.Length;
import org.tdar.core.bean.BulkImportField;
import org.tdar.core.bean.FieldLength;
import org.tdar.core.bean.Obfuscatable;
import org.tdar.core.bean.Validatable;
import org.tdar.search.index.analyzer.AutocompleteAnalyzer;
import org.tdar.search.index.analyzer.NonTokenizingLowercaseKeywordAnalyzer;
import org.tdar.search.index.analyzer.TdarCaseSensitiveStandardAnalyzer;
import org.tdar.search.query.QueryFieldNames;
import org.tdar.utils.PersistableUtils;
import org.tdar.utils.json.JsonAdminLookupFilter;
import org.tdar.utils.json.JsonIdNameFilter;
import org.tdar.utils.json.JsonLookupFilter;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * $Id$
 * 
 * Basic person class within tDAR. Can represent a registered user or a person
 * (with identifying email) entered as a contact or attributed reference, etc.
 * 
 * @author Allen Lee
 * @version $Revision$
 */
@Entity
@Table(name = "person", indexes = { @Index(name = "person_instid", columnList = "institution_id, id") })
@Indexed(index = "Person")
@XmlRootElement(name = "person")
@Check(constraints = "email <> ''")
public class Person extends Creator<Person> implements Comparable<Person>, Dedupable<Person>, Validatable {

    private static final long serialVersionUID = -3863573773250268081L;

    @Transient
    private transient String wildcardName;

    @JsonView(JsonLookupFilter.class)
    @Column(nullable = false, name = "last_name")
    @BulkImportField(key = "CREATOR_LNAME", order = 2)
    @Fields({ @Field(name = QueryFieldNames.LAST_NAME, analyzer = @Analyzer(impl = NonTokenizingLowercaseKeywordAnalyzer.class)),
            @Field(name = QueryFieldNames.LAST_NAME_AUTO, norms = Norms.NO, store = Store.YES, analyzer = @Analyzer(impl = AutocompleteAnalyzer.class)),
            @Field(name = QueryFieldNames.LAST_NAME_SORT, norms = Norms.NO, store = Store.YES) })
    @Length(max = FieldLength.FIELD_LENGTH_255)
    private String lastName;

    @Column(nullable = false, name = "first_name")
    @BulkImportField(key = "CREATOR_FNAME", order = 1)
    @Fields({ @Field(name = QueryFieldNames.FIRST_NAME, analyzer = @Analyzer(impl = NonTokenizingLowercaseKeywordAnalyzer.class)),
            @Field(name = QueryFieldNames.FIRST_NAME_AUTO, norms = Norms.NO, store = Store.YES, analyzer = @Analyzer(impl = AutocompleteAnalyzer.class)),
            @Field(name = QueryFieldNames.FIRST_NAME_SORT, norms = Norms.NO, store = Store.YES) })
    @Length(max = FieldLength.FIELD_LENGTH_255)
    @JsonView(JsonLookupFilter.class)
    private String firstName;

    // http://support.orcid.org/knowledgebase/articles/116780-structure-of-the-orcid-identifier
    @Column(name = "orcid_id")
    private String orcidId;

    @Column(unique = true, nullable = true)
    @Field(name = "email", analyzer = @Analyzer(impl = NonTokenizingLowercaseKeywordAnalyzer.class))
    @BulkImportField(key = "EMAIL", order = 3)
    @Length(min = 1, max = FieldLength.FIELD_LENGTH_255)
    @JsonView(JsonLookupFilter.class)
    private String email;

    @Column(nullable = false, name = "email_public", columnDefinition = "boolean default FALSE")
    private Boolean emailPublic = Boolean.FALSE;

    @IndexedEmbedded(depth = 1)
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE, CascadeType.DETACH }, optional = true)
    @BulkImportField(key = "CREATOR_PERSON_INSTITUTION", order = 50)
    @JsonView(JsonLookupFilter.class)
    private Institution institution;

    // rpanet.org "number"
    @Column(name = "rpa_number")
    @Length(max = FieldLength.FIELD_LENGTH_255)
    private String rpaNumber;

    @Length(max = FieldLength.FIELD_LENGTH_255)
    private String phone;

    @Column(nullable = false, name = "phone_public", columnDefinition = "boolean default FALSE")
    private Boolean phonePublic = Boolean.FALSE;

    public Person() {
    }

    public Person(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    /**
     * Returns the person's name in [last name, first name] format.
     * 
     * @return formatted String name
     */
    @Override
    @Transient
    @Fields({
            @Field(name = QueryFieldNames.NAME_TOKEN),
            @Field(name = QueryFieldNames.NAME_PHRASE, norms = Norms.NO, store = Store.NO,
                    analyzer = @Analyzer(impl = TdarCaseSensitiveStandardAnalyzer.class))
    })
    @JsonView(JsonLookupFilter.class)
    public String getName() {
        return lastName + ", " + firstName;
    }

    @Override
    @JsonView(JsonIdNameFilter.class)
    public String getProperName() {
        return firstName + " " + lastName;
    }

    /**
     * set the user firstname, lastname from string in "last first" format. anything other than simple
     * two word string is ignored.
     * 
     * @param properName
     */
    public void setName(String name) {
        String[] names = Person.split(name);
        if (names.length == 2) {
            setLastName(names[0]);
            setFirstName(names[1]);
        }
    }

    /**
     * FIXME: Only handles names in the form "FirstName LastName". So a name
     * like "Colin McGregor McCoy" would have "Colin" as the first name and
     * "McGregor McCoy" as the last name
     * 
     * @param name
     *            a full name in 'FirstName LastName' or 'LastName, FirstName'
     *            format.
     * @return String array of length 2 - [LastName, FirstName]
     */
    public static String[] split(String name) {
        String firstName = "";
        String lastName = "";
        int splitIndex = name.indexOf(',');
        if (splitIndex == -1) {
            splitIndex = name.indexOf(' ');
            if (splitIndex == -1) {
                // give up, warn?
                return new String[0];
            }
            firstName = name.substring(0, splitIndex);
            lastName = name.substring(splitIndex + 1, name.length());
        } else {
            lastName = name.substring(0, splitIndex);
            firstName = name.substring(splitIndex + 1, name.length());
        }
        return new String[] { lastName, firstName };
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        if (lastName == null) {
            return;
        }
        this.lastName = lastName.trim();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        if (firstName == null) {
            return;
        }
        this.firstName = firstName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (StringUtils.isBlank(email)) {
            this.email = null;
        } else {
            this.email = email.toLowerCase();
        }
    }

    public Boolean getEmailPublic() {
        return emailPublic;

    }

    public void setEmailPublic(Boolean toggle) {
        this.emailPublic = toggle;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    @Override
    public String toString() {
        Institution i = getInstitution();
        String institutionName = (i != null && StringUtils.isNotBlank(i.toString())) ? i.toString() : "No institution specified.";
        return String.format("%s [%s | %s | %s]", getName(), getId(), email, institutionName);
    }

    /**
     * Compares by last name, first name, and then finally email.
     */
    @Override
    public int compareTo(Person otherPerson) {
        if (this == otherPerson) {
            return 0;
        }
        int comparison = lastName.compareTo(otherPerson.lastName);
        if (comparison == 0) {
            comparison = firstName.compareTo(otherPerson.firstName);
            if (comparison == 0) {
                // last straw is email
                comparison = email.compareTo(otherPerson.email);
            }
        }
        return comparison;
    }

    public String getRpaNumber() {
        return rpaNumber;
    }

    public void setRpaNumber(String rpaNumber) {
        this.rpaNumber = rpaNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getPhonePublic() {
        return phonePublic;
    }

    public void setPhonePublic(Boolean phonePublic) {
        this.phonePublic = phonePublic;
    }

    @Override
    public CreatorType getCreatorType() {
        return CreatorType.PERSON;
    }

    @Override
    @Transient
    public String getInstitutionName() {
        String name = null;
        if (institution != null) {
            name = institution.getName();
        }
        return name;
    }

    @Override
    public boolean isDedupable() {
        return true;
    }

    @Override
    public Set<Obfuscatable> obfuscate() {
        setObfuscated(true);
        setObfuscatedObjectDifferent(false);
        // check if email and phone are actually confidential
        Set<Obfuscatable> set = new HashSet<>();
        if (!getEmailPublic()) {
            setEmail(null);
            setObfuscatedObjectDifferent(true);
        }
        if (!getPhonePublic()) {
            setObfuscatedObjectDifferent(true);
            setPhone(null);
        }
        setRpaNumber(null);
        set.add(getInstitution());
        return set;
    }

    @Override
    public boolean isValidForController() {
        return StringUtils.isNotBlank(firstName) && StringUtils.isNotBlank(lastName);
    }

    @Transient
    @Override
    public boolean hasNoPersistableValues() {
        Institution i = getInstitution();
        return StringUtils.isBlank(email)
                && ((i == null) || StringUtils.isBlank(i.getName()))
                && StringUtils.isBlank(lastName)
                && StringUtils.isBlank(firstName)
                && PersistableUtils.isNullOrTransient(getId());
    }

    @Override
    public boolean isValid() {
        return isValidForController() && (getId() != null);
    }

    @XmlTransient
    public String getWildcardName() {
        return wildcardName;
    }

    public void setWildcardName(String wildcardName) {
        this.wildcardName = wildcardName;
    }

    @Override
    @Field(norms = Norms.NO, store = Store.YES)
    @DateBridge(resolution = Resolution.MILLISECOND)
    public Date getDateUpdated() {
        return super.getDateUpdated();
    }

    public String getOrcidId() {
        return orcidId;
    }

    public void setOrcidId(String orcidId) {
        this.orcidId = orcidId;
    }

    @JsonView(JsonAdminLookupFilter.class)
    public boolean isRegistered() {
        return false;
    }

    public static Person fromName(String properName) {
        String[] split = split(properName);
        if (split.length > 1) {
            return new Person(split[0], split[1], null);
        } else {
            return new Person("", properName, null);
        }
    }

}
