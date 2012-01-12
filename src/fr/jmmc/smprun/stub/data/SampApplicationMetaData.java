/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub.data;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import fr.jmmc.jmcs.jaxb.JAXBFactory;
import fr.jmmc.jmcs.jaxb.XmlBindException;
import fr.jmmc.smprun.stub.data.model.SampStub;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Subscriptions;

/**
 * Real SAMP application meta data older, that can report to JMMC central repository if not referenced yet.
 *
 * @author Sylvain LAFRASSE
 */
public class SampApplicationMetaData {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(SampApplicationMetaData.class.getName());
    /** package name for JAXB generated code */
    private final static String STUB_DATA_MODEL_JAXB_PATH = "fr.jmmc.smprun.stub.data.model";
    // TODO : find a home for JMMC AppLauncher Stub meta data repository, typically "http://jmmc.fr/~smprun/stubs/"
    /** URL of the JMMC SAMP application meta data repository */
    //private static final String REPOSITORY_URL = "http://jmmc.fr/~smprun/stubs/";
    private static final String REPOSITORY_URL = "http://jmmc.fr/~lafrasse/stubs/";
    /** File extension of the JMMC SAMP application meta data file format */
    private static final String FILE_EXTENSION = ".xml";
    /** Submission form name */
    private static final String SUBMISSION_FORM = "push.php";
    /** Jersey client */
    Client _jerseyClient;
    /** SAMP application meta data container */
    private SampStub _data = new SampStub();
    /** Real application exact name */
    private String _name;
    /** Application description in XML*/
    private String _xml;

    /**
     * Constructor.
     *
     * @param metadata SAMP Meta data
     * @param subscriptions SAMP mTypes
     */
    public SampApplicationMetaData(Metadata metadata, Subscriptions subscriptions) {

        // Jersey setup
        _jerseyClient = Client.create();
        _jerseyClient.setFollowRedirects(false);
        // TODO : What the fuck with proxies ???

        _name = metadata.getName();
        _data.setUid(_name);

        // Serialze SAMP meta data
        for (Object key : metadata.keySet()) {
            fr.jmmc.smprun.stub.data.model.Metadata tmp = new fr.jmmc.smprun.stub.data.model.Metadata();
            tmp.setKey(key.toString());
            tmp.setValue(metadata.get(key).toString());
            _data.getMetadatas().add(tmp);
        }

        // Serialze SAMP mTypes
        for (Object subscription : subscriptions.keySet()) {
            _data.getSubscriptions().add(subscription.toString());
        }
    }

    /**
     * Upload application complete description to JMMC central repository (only if not known yet).
     */
    public void reportToCentralRepository() {

        // TODO : make all the JERSEY network stuff running in the background.

        if (isNotKnownYet()) {
            _logger.warning("Real SAMP application '" + _name + "' unknown repositroy-wise, reporting it.");

            phoneHome();
            return;
        }

        _logger.info("Real SAMP application '" + _name + "' already known repositroy-wise.");
    }

    /**
     * @param name
     * @return true if the 'name' application is unknown, false otherwise.
     */
    private boolean isNotKnownYet() {

        WebResource r = _jerseyClient.resource(REPOSITORY_URL + _name + FILE_EXTENSION);

        ClientResponse response = r.accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        _logger.fine("JERSEY response = " + response);
        String content = response.getEntity(String.class);
        _logger.finer("JERSEY content = " + content);

        return (response.getClientResponseStatus() != ClientResponse.Status.OK);
    }

    private void phoneHome() throws XmlBindException {
        
        marshall();
        postXML();
    }

    private void marshall() throws XmlBindException {

        // Start JAXB
        final JAXBFactory jaxbFactory = JAXBFactory.getInstance(STUB_DATA_MODEL_JAXB_PATH);
        final Marshaller marshaller = jaxbFactory.createMarshaller();
        final StringWriter stringWriter = new StringWriter();

        try {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.marshal(_data, stringWriter);
        } catch (JAXBException ex) {
            Logger.getLogger(SampApplicationMetaData.class.getName()).log(Level.SEVERE, null, ex);
        }

        _xml = stringWriter.toString();
        System.out.println("XML:\n" + _xml);
    }

    private void postXML() {

        System.out.println("Sending XML to central repository ...");

        WebResource r = _jerseyClient.resource(REPOSITORY_URL + SUBMISSION_FORM);
        MultivaluedMap formData = new MultivaluedMapImpl();
        formData.add("uid", _name);
        formData.add("xmlSampStub", _xml);
        ClientResponse response = r.type("application/x-www-form-urlencoded").post(ClientResponse.class, formData);
    }
}
