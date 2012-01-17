/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub.data;

import fr.jmmc.jmcs.gui.MessagePane;
import fr.jmmc.jmcs.gui.SwingUtils;
import fr.jmmc.jmcs.jaxb.JAXBFactory;
import fr.jmmc.jmcs.jaxb.XmlBindException;
import fr.jmmc.jmcs.network.Http;
import fr.jmmc.smprun.stub.data.model.SampStub;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Subscriptions;
import org.ivoa.util.concurrent.ThreadExecutors;

/**
 * Real SAMP application meta data older, that can report to JMMC central registry if not referenced yet.
 *
 * @author Sylvain LAFRASSE
 */
public class SampApplicationMetaData {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(SampApplicationMetaData.class.getName());
    /** Package name for JAXB generated code */
    private final static String STUB_DATA_MODEL_JAXB_PATH = "fr.jmmc.smprun.stub.data.model";
    /** URL of the JMMC SAMP application meta data repository */
    //private static final String REPOSITORY_URL = "http://jmmc.fr/~lafrasse/stubs/";
    private static final String REPOSITORY_URL = "http://jmmc.fr/~smprun/stubs/";
    /** File extension of the JMMC SAMP application meta data file format */
    private static final String FILE_EXTENSION = ".xml";
    /** Submission form name */
    private static final String SUBMISSION_FORM = "push.php";
    /** SAMP application meta data container */
    private SampStub _data = new SampStub();
    /** Real application exact name */
    private String _name;

    /**
     * Constructor.
     *
     * @param metadata SAMP Meta data
     * @param subscriptions SAMP mTypes
     */
    public SampApplicationMetaData(Metadata metadata, Subscriptions subscriptions) {

        _logger.fine("Serializing SAMP application meta data.");

        _name = metadata.getName();
        _data.setUid(_name);

        // Serialize all SAMP meta data
        for (Object key : metadata.keySet()) {
            fr.jmmc.smprun.stub.data.model.Metadata tmp = new fr.jmmc.smprun.stub.data.model.Metadata();
            tmp.setKey(key.toString());
            tmp.setValue(metadata.get(key).toString());
            _data.getMetadatas().add(tmp);
        }

        // Serialize all SAMP mTypes
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

                    // TODO : Use dismissable message pane to always skip report ?

                    // Ask user if it is ok to phone home
                    SwingUtils.invokeAndWaitEDT(new Runnable() {

                        /**
                         * Synchronized by EDT
                         */
                        @Override
                        public void run() {
                            shouldPhoneHome.set(MessagePane.showConfirmMessage("AppLauncher discovered the '" + _name + "' application it did not know yet.\n"
                                    + "Do you wish to contribute making AppLauncher better, and send its\ndescription to the JMMC ?\n\n"
                                    + "No personnal information will be sent along."));
                        }
                    });

                    // If the user agreed to report unknown app
                    if (shouldPhoneHome.get()) {
                        postXMLToRegistry(marshallApplicationDescription());
                    }
                    return;
                }
            }
        });
    }

    /**
     * @param name
     * @return true if the 'name' application is unknown, false otherwise.
     */
    private boolean isNotKnownYet() {

        boolean unknownAppFlag = false;

        try {
            _logger.info("Querying JMMC SAMP application registry for '" + _name + "' ...");

            URI uri = new URI(REPOSITORY_URL + _name + FILE_EXTENSION);
            String response = Http.download(uri, false);

            _logger.fine("HTTP response = " + response);

            unknownAppFlag = (response == null) || (response.length() == 0);

            _logger.info("SAMP application '" + _name + "'" + (unknownAppFlag ? " not " : " ") + "found in JMMC registry.");

        } catch (URISyntaxException ex) {
            _logger.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            _logger.log(Level.SEVERE, null, ex);
        }

        return unknownAppFlag;
    }

    private String marshallApplicationDescription() throws XmlBindException {

        // Start JAXB
        final JAXBFactory jaxbFactory = JAXBFactory.getInstance(STUB_DATA_MODEL_JAXB_PATH);
        final Marshaller marshaller = jaxbFactory.createMarshaller();
        final StringWriter stringWriter = new StringWriter();

        // Serialize applicatio description in XML
        try {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.marshal(_data, stringWriter);
        } catch (JAXBException ex) {
            _logger.log(Level.SEVERE, null, ex);
            return null;
        }

        String xml = stringWriter.toString();
        _logger.fine("Generated SAMP application '" + _name + "' XML description:\n" + xml);
        return xml;
    }

    private void postXMLToRegistry(String xml) {

        final HttpClient httpClient = Http.getHttpClient();

        // Composing form values
        final PostMethod postMethod = new PostMethod(REPOSITORY_URL + SUBMISSION_FORM);
        postMethod.addParameter("uid", _name);
        postMethod.addParameter("xmlSampStub", xml);

        // Send SAMP application meta data to JMMC registry
        try {
            _logger.info("Sending JMMC SAMP application '" + _name + "' XML description to JMMC registry ...");

            // TODO : handle status code
            final int statusCode = httpClient.executeMethod(postMethod);

            _logger.info("Sent SAMP application '" + _name + "' XML description to JMMC regitry.");

            // Get PHP script result (either SUCCESS or FAILURE)
            final String response = postMethod.getResponseBodyAsString();
            _logger.fine("HTTP response = " + response);

            // Parse result for failure
            final boolean statusFlag = (statusCode == 200) && (postMethod.isRequestSent());
            _logger.warning("SAMP application meta data sent : " + ((statusFlag) ? "YES" : "NO"));

        } catch (IOException ioe) {
            _logger.severe("Cannot send SAMP application meta data: " + ioe);
        } finally {
            // Release the connection.
            postMethod.releaseConnection();
        }
    }
}
