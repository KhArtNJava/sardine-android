//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.4-10/27/2009 06:09 PM(mockbuild)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.12.23 at 06:27:19 PM PST 
//


package com.googlecode.sardine.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element ref="{DAV:}omit"/>
 *         &lt;element ref="{DAV:}keepalive"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "omit",
    "keepalive"
})
@XmlRootElement(name = "propertybehavior")
public class Propertybehavior {

    private Omit omit;
    private Keepalive keepalive;

    /**
     * Gets the value of the omit property.
     * 
     * @return
     *     possible object is
     *     {@link Omit }
     *     
     */
    public Omit getOmit() {
        return omit;
    }

    /**
     * Sets the value of the omit property.
     * 
     * @param value
     *     allowed object is
     *     {@link Omit }
     *     
     */
    public void setOmit(Omit value) {
        this.omit = value;
    }

    /**
     * Gets the value of the keepalive property.
     * 
     * @return
     *     possible object is
     *     {@link Keepalive }
     *     
     */
    public Keepalive getKeepalive() {
        return keepalive;
    }

    /**
     * Sets the value of the keepalive property.
     * 
     * @param value
     *     allowed object is
     *     {@link Keepalive }
     *     
     */
    public void setKeepalive(Keepalive value) {
        this.keepalive = value;
    }

}