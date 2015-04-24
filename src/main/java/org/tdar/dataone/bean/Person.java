//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.04.15 at 02:23:18 PM MST 
//


package org.tdar.dataone.bean;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * *Person* represents metadata about a :term:`Principal`
 *       that is a person and that can be used by clients and nodes for
 *       :class:`Types.AccessPolicy` information. The mutable properties of a
 *       *Person* instance can only be changed by itself (i.e., the Subject
 *       identifying the Person instance) and by the Coordinating Node identity,
 *       but can be read by any identity in the DataONE system.
 *       
 * 
 * <p>Java class for Person complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Person">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="subject" type="{http://ns.dataone.org/service/types/v1}Subject"/>
 *         &lt;element name="givenName" type="{http://ns.dataone.org/service/types/v1}NonEmptyString" maxOccurs="unbounded"/>
 *         &lt;element name="familyName" type="{http://ns.dataone.org/service/types/v1}NonEmptyString"/>
 *         &lt;element name="email" type="{http://ns.dataone.org/service/types/v1}NonEmptyString" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="isMemberOf" type="{http://ns.dataone.org/service/types/v1}Subject" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="equivalentIdentity" type="{http://ns.dataone.org/service/types/v1}Subject" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="verified" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Person", propOrder = {
    "subject",
    "givenName",
    "familyName",
    "email",
    "isMemberOf",
    "equivalentIdentity",
    "verified"
})
public class Person {

    @XmlElement(required = true)
    protected Subject subject;
    @XmlElement(required = true)
    protected List<String> givenName;
    @XmlElement(required = true)
    protected String familyName;
    protected List<String> email;
    protected List<Subject> isMemberOf;
    protected List<Subject> equivalentIdentity;
    protected Boolean verified;

    /**
     * Gets the value of the subject property.
     * 
     * @return
     *     possible object is
     *     {@link Subject }
     *     
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * Sets the value of the subject property.
     * 
     * @param value
     *     allowed object is
     *     {@link Subject }
     *     
     */
    public void setSubject(Subject value) {
        this.subject = value;
    }

    /**
     * Gets the value of the givenName property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the givenName property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGivenName().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getGivenName() {
        if (givenName == null) {
            givenName = new ArrayList<String>();
        }
        return this.givenName;
    }

    /**
     * Gets the value of the familyName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFamilyName() {
        return familyName;
    }

    /**
     * Sets the value of the familyName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFamilyName(String value) {
        this.familyName = value;
    }

    /**
     * Gets the value of the email property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the email property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEmail().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getEmail() {
        if (email == null) {
            email = new ArrayList<String>();
        }
        return this.email;
    }

    /**
     * Gets the value of the isMemberOf property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the isMemberOf property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIsMemberOf().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Subject }
     * 
     * 
     */
    public List<Subject> getIsMemberOf() {
        if (isMemberOf == null) {
            isMemberOf = new ArrayList<Subject>();
        }
        return this.isMemberOf;
    }

    /**
     * Gets the value of the equivalentIdentity property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the equivalentIdentity property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEquivalentIdentity().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Subject }
     * 
     * 
     */
    public List<Subject> getEquivalentIdentity() {
        if (equivalentIdentity == null) {
            equivalentIdentity = new ArrayList<Subject>();
        }
        return this.equivalentIdentity;
    }

    /**
     * Gets the value of the verified property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isVerified() {
        return verified;
    }

    /**
     * Sets the value of the verified property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setVerified(Boolean value) {
        this.verified = value;
    }

}
