
package fr.jmmc.smprun.stub.data.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 JMMC AppLauncher Stub meta data.
 *             
 * 
 * <p>Java class for SampStub complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SampStub">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="metadata" type="{}Metadata" maxOccurs="unbounded"/>
 *         &lt;element name="subscription" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SampStub", propOrder = {
    "name",
    "metadatas",
    "subscriptions"
})
@XmlRootElement(name = "SampStub")
public class SampStub {

    @XmlElement(required = true)
    protected String name;
    @XmlElement(name = "metadata", required = true)
    protected List<Metadata> metadatas;
    @XmlElement(name = "subscription", required = true)
    protected List<String> subscriptions;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    public boolean isSetName() {
        return (this.name!= null);
    }

    /**
     * Gets the value of the metadatas property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the metadatas property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMetadatas().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Metadata }
     * 
     * 
     */
    public List<Metadata> getMetadatas() {
        if (metadatas == null) {
            metadatas = new ArrayList<Metadata>();
        }
        return this.metadatas;
    }

    public boolean isSetMetadatas() {
        return ((this.metadatas!= null)&&(!this.metadatas.isEmpty()));
    }

    public void unsetMetadatas() {
        this.metadatas = null;
    }

    /**
     * Gets the value of the subscriptions property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the subscriptions property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSubscriptions().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getSubscriptions() {
        if (subscriptions == null) {
            subscriptions = new ArrayList<String>();
        }
        return this.subscriptions;
    }

    public boolean isSetSubscriptions() {
        return ((this.subscriptions!= null)&&(!this.subscriptions.isEmpty()));
    }

    public void unsetSubscriptions() {
        this.subscriptions = null;
    }

}
