/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub.data;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import fr.jmmc.smprun.stub.data.model.SampStub;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
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
    // TODO : find a home for JMMC AppLauncher Stub meta data repository, typically "http://jmmc.fr/~smprun/stubs/"
    private static final String REPOSITORY_URL = "http://jmmc.fr/~lafrasse/stubs/";
    /** SAMP application meta data container */
    private SampStub _data = new SampStub();

    public StubMetaData(String name, Metadata metadata, Subscriptions subscriptions) {

        _data.setName(name);

        for (Object subscription : subscriptions.keySet()) {
            _data.getSubscriptions().add(subscription.toString());
        }

        for (Object key : metadata.keySet()) {
            fr.jmmc.smprun.stub.data.model.Metadata tmp = new fr.jmmc.smprun.stub.data.model.Metadata();
            tmp.setKey(key.toString());
            tmp.setValue(metadata.get(key).toString());
            _data.getMetadatas().add(tmp);
        }
    }

    public void reportToCentralRepository() {

        final String name = _data.getName();

        // TODO : make all the JERSEY network stuff running in the background.
        if (isNotKnownYet(name)) {
            _logger.warning("Real SAMP application '" + name + "' unknown repositroy-wise, reporting it.");
            // TODO : forge an XML stub meta data file using JAXB and send it to the central repository if the user gives its approval.
            return;
        }

        _logger.info("Real SAMP application '" + name + "' already known repositroy-wise.");
        return;
    }

    /**
     * @param name
     * @return true if the 'name' application is unknown, false otherwise.
     */
    private boolean isNotKnownYet(String name) {

        Client c = Client.create();
        c.setFollowRedirects(false);

        WebResource r = c.resource(REPOSITORY_URL + name);

        ClientResponse response = r.accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        _logger.fine("JERSEY response = " + response);
        String content = response.getEntity(String.class);
        _logger.finer("JERSEY content = " + content);

        return (response.getClientResponseStatus() != ClientResponse.Status.OK);
    }
}
