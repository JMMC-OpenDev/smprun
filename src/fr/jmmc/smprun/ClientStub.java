/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.mcs.interop.SampCapability;
import fr.jmmc.mcs.interop.SampMessageHandler;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;

/**
 * Registers a fake App to the hub, and later dispatch any received message to the freshly started recipient.
 *
 * @author Sylvain LAFRASSE
 */
public final class ClientStub {

    /** Gui hub connector */
    private GuiHubConnector _connector;

    /** Store desired stub application name */
    private String _applicationName;

    /** Store desired stub SAMP capability */
    private SampCapability _mType;

    /**
     * Constructor
     *
     * @param applicationName
     * @param mType
     */
    public ClientStub(String applicationName, final SampCapability mType) {
        // @TODO : init JSamp env.
        final ClientProfile profile = DefaultClientProfile.getProfile();

        _connector = new GuiHubConnector(profile);
        _applicationName = applicationName;
        _mType = mType;

        // Build application metadata :
        final Metadata meta = new Metadata();

        meta.setName(_applicationName);

        // @TODO : embbed in ApplicationData.xml
        // meta.setDescriptionText("Find Interferometric Calibrators for Optical Observations");

        // @TODO : embbed the real HTML doc URL in ApplicationData.xml
        // meta.setDocumentationUrl(applicationURL);

        // @TODO : embbed the icon in each application JAR file
        // meta.setIconUrl("http://apps.jmmc.fr/~sclws/SearchCal/AppIcon.png");
/*
        // Non-standard meatadata
        meta.put("affiliation.name", "JMMC (Jean-Marie MARIOTTI Center)");
        meta.put("affiliation.url", applicationDataModel.getMainWebPageURL());
        meta.put("affiliation.feedback", "http://jmmc.fr/feedback/");
        meta.put("affiliation.support", "http://www.jmmc.fr/support.htm");
        
        final String lowerCaseApplicationName = applicationName.toLowerCase();
        meta.put(lowerCaseApplicationName + ".authors", "Brought to you by the JMMC Team");
        meta.put(lowerCaseApplicationName + ".homepage", applicationDataModel.getLinkValue());
        meta.put(lowerCaseApplicationName + ".version", applicationDataModel.getProgramVersion());
        meta.put(lowerCaseApplicationName + ".news", applicationDataModel.getHotNewsRSSFeedLinkValue());
        meta.put(lowerCaseApplicationName + ".compilationdate", applicationDataModel.getCompilationDate());
        meta.put(lowerCaseApplicationName + ".compilatorversion", applicationDataModel.getCompilatorVersion());
        meta.put(lowerCaseApplicationName + ".releasenotes", applicationDataModel.getReleaseNotesLinkValue());
        meta.put(lowerCaseApplicationName + ".faq", applicationDataModel.getFaqLinkValue());
         */
        _connector.declareMetadata(meta);

        _connector.addConnectionListener(new SampConnectionChangeListener());

        // try to connect :
        _connector.setActive(true);

        if (!_connector.isConnected()) {

            // Try to start an internal SAMP hub if none available (JNLP do not support external hub) :
            try {
                Hub.runHub(HubServiceMode.CLIENT_GUI);
            } catch (IOException ioe) {
                System.out.println("Stug '" + _applicationName + "'  unable to start internal hub (probably another hub is already running):" + ioe);
            }

            // retry to connect :
            _connector.setActive(true);
        }

        // Keep a look out for hubs if initial one shuts down
        _connector.setAutoconnect(5);

        if (!_connector.isConnected()) {
            System.out.println("Stub '" + _applicationName + "' could not connect to an existing hub or start an internal SAMP hub.");
        }

        // This step required even if no message handlers added.
        _connector.declareSubscriptions(_connector.computeSubscriptions());

        // Add handler to load query params and launch calibrator search
        registerCapability(new SampMessageHandler(_mType) {

            /**
             * Implements message processing
             *
             * @param senderId public ID of sender client
             * @param message message with MType this handler is subscribed to
             * @throws SampException if any error occurred while message processing
             */
            protected void processMessage(final String senderId, final Message message) {

                System.out.println("Stub '" + _applicationName + "' received '" + this.handledMType() + "' message from '" + senderId + "' : '" + message + "'.");

                System.out.print("Stub '" + _applicationName + "' unregistering from hub ... ");
                // @TODO : Unregister stub from hub to make room for the recipient
                System.out.println("FAILED.");

                System.out.print("Stub '" + _applicationName + "' launching recipient '" + _applicationName + "' ... ");
                // @TODO : Launch recipient if missing and known (otherwise error message)
                // @TODO : Get back newly launched recipient id.
                String recipient = "c25";
                System.out.println("FAILED.");

                // Forward recevied message to recipient
                System.out.print("Stub '" + _applicationName + "' forwarding '" + mType + "' SAMP message to '" + recipient + "' client ... ");
                try {
                    _connector.getConnection().notify(recipient, message);
                } catch (SampException ex) {
                    Logger.getLogger(ClientStub.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("DONE.");
            }
        });
    }

    /**
     * Samp Hub Connection Change listener
     */
    private final class SampConnectionChangeListener implements ChangeListener {

        /**
         * Invoked when the hub connection has changed its state i.e.
         * when this connector registers or unregisters with a hub.
         *
         * @param e  a ChangeEvent object
         */
        @Override
        public void stateChanged(final ChangeEvent e) {
            System.out.println("Stub '" + _applicationName + "' SAMP Hub connection status : " + ((_connector.isConnected()) ? "registered" : "unregistered"));
        }
    }

    /**
     * Register an app-specific capability
     * @param handler message handler
     */
    public void registerCapability(final SampMessageHandler handler) {

        _connector.addMessageHandler(handler);

        System.out.println("Stub '" + _applicationName + "' registered SAMP capability for mType '" + handler.handledMType() + "'.");

        // This step required even if no custom message handlers added.
        _connector.declareSubscriptions(_connector.computeSubscriptions());
    }
}
