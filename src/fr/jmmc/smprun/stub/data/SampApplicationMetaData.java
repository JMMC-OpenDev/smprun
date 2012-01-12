/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub.data;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import fr.jmmc.jmcs.gui.MessagePane;
import fr.jmmc.jmcs.gui.SwingUtils;
import fr.jmmc.jmcs.jaxb.JAXBFactory;
import fr.jmmc.jmcs.jaxb.XmlBindException;
import fr.jmmc.smprun.stub.data.model.SampStub;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Subscriptions;
import org.ivoa.util.concurrent.ThreadExecutors;

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

        // Make all the JERSEY network stuff run in the background
        ThreadExecutors.getGenericExecutor().submit(new Runnable() {

            AtomicBoolean shouldPhoneHome = new AtomicBoolean(false);

            public void run() {

                // If the current application does not exist in the central repository
                if (isNotKnownYet()) {
                    _logger.warning("Real SAMP application '" + _name + "' unknown repository-wise, reporting it.");

                    // TODO : Use dismissable message pane to always skip report ?

                    // Ask user if he is ok to phone home
                    SwingUtils.invokeAndWaitEDT(new Runnable() {

                        /** Synchronized by EDT */
                        @Override
                        public void run() {
                            shouldPhoneHome.set(MessagePane.showConfirmMessage("AppLauncher discovered the '" + _name + "' application it did not know yet.\n"
                                    + "Do you wish to contribute making AppLauncher better and send '" + _name + "' description to JMMC ?\n\n"
                                    + "No personnal information will be sent along."));
                        }
                    });

                    // If the user agreed to report unknown app
                    if (shouldPhoneHome.get()) {
                        phoneHome();
                    }
                    return;
                }

                _logger.info("Real SAMP application '" + _name + "' already known repository-wise.");
            }
        });
    }

    /**
     * @param name
     * @return true if the 'name' application is unknown, false otherwise.
     */
    private boolean isNotKnownYet() {

        // TODO : what about exceptions on failure ???

        WebResource webResource = _jerseyClient.resource(REPOSITORY_URL + _name + FILE_EXTENSION);

        _logger.fine("JERSEY webResource = " + webResource);
        ClientResponse response = webResource.accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
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
        _logger.fine("Application XML description:\n" + _xml);
    }

    private void postXML() {

        // TODO : what about exceptions on failure ???

        _logger.info("Sending XML to central repository ...");

        WebResource webResource = _jerseyClient.resource(REPOSITORY_URL + SUBMISSION_FORM);
        _logger.fine("JERSEY webResource = " + webResource);

        // Prepare form values
        MultivaluedMap formData = new MultivaluedMapImpl();
        formData.add("uid", _name);
        formData.add("xmlSampStub", _xml);

        ClientResponse response = webResource.type("application/x-www-form-urlencoded").post(ClientResponse.class, formData);
        _logger.fine("JERSEY response = " + response);
    }
}
