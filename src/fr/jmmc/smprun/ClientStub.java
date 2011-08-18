/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.mcs.interop.SampCapability;
import fr.jmmc.mcs.interop.SampMessageHandler;

import fr.jmmc.mcs.util.Urls;
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
    /** Store desired stub application metadata */
    private Metadata _description;
    /** Convinient proxy to Metadata dedicated field) */
    private String _applicationName;
    /** Store desired stub SAMP capabilities */
    private SampCapability[] _mTypes;
    /** Store desired JNLP URL */
    private String _jnlpUrl;

    /**
     * Constructor
     *
     * @param applicationName
     * @param mType
     */
    public ClientStub(final Metadata description, final SampCapability[] mTypes, final String jnlpUrl) {

        // Initialize JSamp env.
        final ClientProfile profile = DefaultClientProfile.getProfile();
        _connector = new GuiHubConnector(profile);

        _description = description;
        _applicationName = description.getName();
        _mTypes = mTypes;
        _jnlpUrl = jnlpUrl;

        connectHub();
        registerCapabilities();

    }

    private void registerCapabilities() {
        for (final SampCapability mType : _mTypes) {
            // Add handler to load query params and launch calibrator search
            registerCapability(new SampMessageHandler(mType) {

                /**
                 * Implements message processing
                 *
                 * @param senderId public ID of sender client
                 * @param message message with MType this handler is subscribed to
                 * @throws SampException if any error occurred while message processing
                 */
                protected void processMessage(final String senderId, final Message message) {

                    System.out.println("Stub '" + _applicationName + "' received '" + this.handledMType() + "' message from '" + senderId + "' : '" + message + "'.");

                    // Unregister stub from hub to make room for the recipient
                    unregisterCapability(this);

                    String jnlpUrl = "http://jmmc.fr/~swmgr/LITpro/LITpro.jnlp";
                    System.out.print("Stub '" + _applicationName + "' web-starting JNLP '" + jnlpUrl + "' ... ");
                    int status = JnlpStarter.exec(jnlpUrl);
                    System.out.println("DONE (with status '" + status + "').");

                    // @TODO : Get back newly launched recipient id.
                    // @TODO : wait (less than 30s) til recipîent registered !!!
                    String recipient = "c2";

                    // Forward recevied message to recipient
                    System.out.print("Stub '" + _applicationName + "' forwarding '" + mType + "' SAMP message to '" + recipient + "' client ... ");
                    try {
                        _connector.getConnection().notify(recipient, message);
                    } catch (SampException ex) {
                        Logger.getLogger(ClientStub.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.out.println("DONE.");

                    // Kills the stub client
                    _connector.setActive(false);
                }
            });
        }
    }

    private void connectHub() {
        // Set connector up
        _connector.declareMetadata(_description);
        _connector.addConnectionListener(new SampConnectionChangeListener());

        // try to connectHub :
        _connector.setActive(true);
        if (!_connector.isConnected()) {
            // Try to start an internal SAMP hub if none available (JNLP do not support external hub) :
            try {
                Hub.runHub(HubServiceMode.CLIENT_GUI);
            } catch (IOException ioe) {
                System.out.println("Stug '" + _applicationName + "'  unable to start internal hub (probably another hub is already running):" + ioe);
            }
            // retry to connectHub :
            _connector.setActive(true);
        }
        // Keep a look out for hubs if initial one shuts down
        _connector.setAutoconnect(5);
        if (!_connector.isConnected()) {
            System.out.println("Stub '" + _applicationName + "' could not connect to an existing hub or start an internal SAMP hub.");
        }
        // This step required even if no message handlers added.
        _connector.declareSubscriptions(_connector.computeSubscriptions());
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

    /**
     * Unregister an app-specific capability
     * @param handler message handler
     */
    public void unregisterCapability(final SampMessageHandler handler) {

        _connector.removeMessageHandler(handler);

        System.out.println("Stub '" + _applicationName + "' unregistered SAMP capability for mType '" + handler.handledMType() + "'.");

        // This step required even if no custom message handlers added.
        _connector.declareSubscriptions(_connector.computeSubscriptions());
    }
}
