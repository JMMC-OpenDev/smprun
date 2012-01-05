/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub.data;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import fr.jmmc.jmcs.jaxb.JAXBFactory;
import fr.jmmc.jmcs.jaxb.XmlBindException;
import fr.jmmc.smprun.stub.data.model.SampStub;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Subscriptions;

/**
 * Real SAMP application meta data older, that can report to JMMC central repository if not referenced yet.
 *
 * @author Sylvain LAFRASSE
 */
public class StubMetaData {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(StubMetaData.class.getName());
    /** package name for JAXB generated code */
    private final static String STUB_DATA_MODEL_JAXB_PATH = "fr.jmmc.smprun.stub.data.model";
    // TODO : find a home for JMMC AppLauncher Stub meta data repository, typically "http://jmmc.fr/~smprun/stubs/"
    private static final String REPOSITORY_URL = "http://jmmc.fr/~lafrasse/stubs/";
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
    public StubMetaData(Metadata metadata, Subscriptions subscriptions) {

        _name = metadata.getName();

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

        marshall();
    }

    private void marshall() throws XmlBindException {
        // Start JAXB
        final JAXBFactory jf = JAXBFactory.getInstance(STUB_DATA_MODEL_JAXB_PATH);
        final Marshaller m = jf.createMarshaller();
        final StringWriter sw = new StringWriter();
        try {
            m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            m.marshal(_data, sw);
        } catch (JAXBException ex) {
            Logger.getLogger(StubMetaData.class.getName()).log(Level.SEVERE, null, ex);
        }
        _xml = sw.toString();
        System.out.println("XML:\n" + _xml);
    }

    /**
     * Upload application complete description to JMMC central repository (only if not known yet).
     */
    public void reportToCentralRepository() {

        // TODO : make all the JERSEY network stuff running in the background.
        if (isNotKnownYet()) {
            _logger.warning("Real SAMP application '" + _name + "' unknown repositroy-wise, reporting it.");
            // TODO : forge an XML stub meta data file using JAXB and send it to the central repository if the user gives its approval.
            return;
        }

        _logger.info("Real SAMP application '" + _name + "' already known repositroy-wise.");
        return;
    }

    /**
     * @param name
     * @return true if the 'name' application is unknown, false otherwise.
     */
    private boolean isNotKnownYet() {

        Client c = Client.create();
        c.setFollowRedirects(false);

        // TODO : What the fuck with proxies ???
        WebResource r = c.resource(REPOSITORY_URL + _name);

        ClientResponse response = r.accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        _logger.fine("JERSEY response = " + response);
        String content = response.getEntity(String.class);
        _logger.finer("JERSEY content = " + content);

        return (response.getClientResponseStatus() != ClientResponse.Status.OK);
    }
}
