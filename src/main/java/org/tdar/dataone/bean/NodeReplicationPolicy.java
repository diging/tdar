//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.04.15 at 02:23:18 PM MST 
//


package org.tdar.dataone.bean;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * The overall replication policy for the node that
 *       expresses constraints on object size, total objects, source nodes, and
 *       object format types. A node may choose to restrict replication from only
 *       certain peer nodes, may have file size limits, total allocated size
 *       limits, or may want to focus on being a :term:`replication target` for
 *       domain-specific object formats.
 * 
 * <p>Java class for NodeReplicationPolicy complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="NodeReplicationPolicy">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="maxObjectSize" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" minOccurs="0"/>
 *         &lt;element name="spaceAllocated" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" minOccurs="0"/>
 *         &lt;element name="allowedNode" type="{http://ns.dataone.org/service/types/v1}NodeReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="allowedObjectFormat" type="{http://ns.dataone.org/service/types/v1}ObjectFormatIdentifier" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NodeReplicationPolicy", propOrder = {
    "maxObjectSize",
    "spaceAllocated",
    "allowedNode",
    "allowedObjectFormat"
})
public class NodeReplicationPolicy {

    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger maxObjectSize;
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger spaceAllocated;
    protected List<NodeReference> allowedNode;
    protected List<String> allowedObjectFormat;

    /**
     * Gets the value of the maxObjectSize property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMaxObjectSize() {
        return maxObjectSize;
    }

    /**
     * Sets the value of the maxObjectSize property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMaxObjectSize(BigInteger value) {
        this.maxObjectSize = value;
    }

    /**
     * Gets the value of the spaceAllocated property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSpaceAllocated() {
        return spaceAllocated;
    }

    /**
     * Sets the value of the spaceAllocated property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSpaceAllocated(BigInteger value) {
        this.spaceAllocated = value;
    }

    /**
     * Gets the value of the allowedNode property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the allowedNode property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAllowedNode().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link NodeReference }
     * 
     * 
     */
    public List<NodeReference> getAllowedNode() {
        if (allowedNode == null) {
            allowedNode = new ArrayList<NodeReference>();
        }
        return this.allowedNode;
    }

    /**
     * Gets the value of the allowedObjectFormat property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the allowedObjectFormat property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAllowedObjectFormat().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getAllowedObjectFormat() {
        if (allowedObjectFormat == null) {
            allowedObjectFormat = new ArrayList<String>();
        }
        return this.allowedObjectFormat;
    }

}
