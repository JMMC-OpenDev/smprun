/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub;

import fr.jmmc.jmcs.network.interop.SampCapability;

import fr.jmmc.smprun.JnlpStarter;
import java.io.IOException;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.astrogrid.samp.Client;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.gui.SubscribedClientListModel;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;

/**
 * Registers a fake App to the hub, and later dispatch any received message to the freshly started recipient.
 *
 * @author Sylvain LAFRASSE
 */
public final class ClientStub extends Observable {

    private boolean _shouldLog = true;
    /** GUI hub connector */
    private GuiHubConnector _connector;
    /** Store desired stub application metadata */
    private Metadata _description;
    /** Convenient proxy to Metadata dedicated field) */
    private String _applicationName;
    /** Store desired stub SAMP capabilities */
    private SampCapability[] _mTypes;
    /** Store desired JNLP URL */
    private String _jnlpUrl;
    /** Potential recipients */
    private SubscribedClientListModel _capableClients;
    /** Potential recipient id */
    private String _recipientId;
    /** Message to forward once recipient appeared */
    private Message _message;
    /** Message to forward once recipient appeared */
    private ClientStubState _status;

    /**
     * Constructor.
     *
     * @param description metadata about the stub app
     * @param mTypes handled mtypes
     * @param jnlpUrl URL of Java WebStart recipient
     */
    public ClientStub(final Metadata description, final SampCapability[] mTypes, final String jnlpUrl) {

        setState(ClientStubState.INITIALIZING);

        // Initialize JSamp env.
        final ClientProfile profile = DefaultClientProfile.getProfile();
        _connector = new GuiHubConnector(profile);

        _description = description;
        _applicationName = description.getName();

        // Flag any created STUB for later skipping while looking for recipients
        _description.put("fr.jmmc.applauncher." + _applicationName, "STUB");

        _mTypes = mTypes;
        _jnlpUrl = jnlpUrl;

        _capableClients = null;
        _recipientId = null;
        _message = null;

        connectToHub();
        registerStubCapabilities();
        listenToRecipientConnections();
    }

    /**
     * @return the name of the emulated application
     */
    public String getApplicationName() {

        return _applicationName;
    }

    /**
     * Used to follow stub internal state progression
     * 
     * @param status the current state
     */
    private void setState(ClientStubState status) {

        _status = status;

        setChanged();
        notifyObservers(_status);
    }

    private void log(String message) {
        print("STUB['" + _applicationName + "']: " + message);
    }

    private void logLine(String message) {
        log(message + "\n");
    }

    private void logDone() {
        println("DONE.");
    }

    private void println(String str) {
        print(str + "\n");
    }

    private void print(String str) {
        if (_shouldLog) {
            System.out.print(str);
        }
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientStub.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** Set up connection to hub (or a start its own hub if none available) */
    private void connectToHub() {

        setState(ClientStubState.CONNECTING);

        log("connecting to hub ... ");

        // Set connector up
        _connector.declareMetadata(_description);

        // Try to connect
        _connector.setActive(true);
        if (!_connector.isConnected()) {
            // Try to start an internal SAMP hub if none available (JNLP do not support external hub) :
            try {
                Hub.runHub(HubServiceMode.CLIENT_GUI);
            } catch (IOException ioe) {
                logLine("unable to start internal hub (probably another hub is already running):" + ioe);
            }
            // retry to connectToHub :
            _connector.setActive(true);
        }

        // Keep a look out for hubs if initial one shuts down
        _connector.setAutoconnect(5);
        if (!_connector.isConnected()) {
            logLine("could not connect to an existing hub or start an internal SAMP hub.");
        }

        // This step required even if no message handlers added.
        _connector.declareSubscriptions(_connector.computeSubscriptions());

        logDone();
    }

    /** Declare STUB capabilities to the hub */
    private void registerStubCapabilities() {

        setState(ClientStubState.REGISTERING);

        for (final SampCapability mType : _mTypes) {

            // Add handler for each stub capability
            registerCapability(new AbstractMessageHandler(mType.mType()) {

                /**
                 * Implements message processing
                 *
                 * @param senderId public ID of sender client
                 * @param message message with MType this handler is subscribed to
                 * @throws SampException if any error occurred while message processing
                 */
                //protected void processMessage(final String senderId, final Message message) {
                @Override
                public final Map<?, ?> processCall(final HubConnection connection, final String senderId, final Message message) throws SampException {

                    setState(ClientStubState.PROCESSING);

                    // Backup message for later forward
                    _message = message;

                    logLine("received '" + mType.mType() + "' message from '" + senderId + "' : '" + _message + "'.");

                    // Unregister stub from hub to make room for the recipient
                    unregisterCapability(this);
                    logLine("unregistered SAMP capability for mType '" + mType.mType() + "'.");

                    setState(ClientStubState.LAUNCHING);

                    log("web-starting JNLP '" + _jnlpUrl + "' ... ");
                    int status = JnlpStarter.launch(_jnlpUrl);
                    println("DONE (with status '" + status + "').");
                    return null;
                }
            });

            logLine("registered SAMP capability for mType '" + mType.mType() + "'.");
        }
    }

    /** Set up monitoring of new connection to the hub to detect true applications */
    private void listenToRecipientConnections() {

        setState(ClientStubState.LISTENING);

        // Get a dynamic list of SAMP clients able to respond to the specified capability.
        String[] mTypeStrings = new String[_mTypes.length];
        for (int i = 0; i < _mTypes.length; i++) {
            mTypeStrings[i] = _mTypes[i].mType();
            logLine("listening for mType '" + mTypeStrings[i] + "'.");
        }
        _capableClients = new SubscribedClientListModel(_connector, mTypeStrings);

        // Monitor any modification to the capable clients list
        _capableClients.addListDataListener(new ListDataListener() {

            @Override
            public void contentsChanged(final ListDataEvent e) {
                lookForRecipientAvailability();
            }

            @Override
            public void intervalAdded(final ListDataEvent e) {
                lookForRecipientAvailability();
            }

            @Override
            public void intervalRemoved(final ListDataEvent e) {
                lookForRecipientAvailability();
            }
        });

        // but do one first test if one registered app already handle such capability
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                lookForRecipientAvailability();
            }
        });
    }

    /**
     * Decipher whether currently connected applications match STUB personality.
     * If, so forward any waiting message to the true application.
     */
    private void lookForRecipientAvailability() {

        new Thread(new Runnable() {

            @Override
            public void run() {

                // Check each registered clients for the sought recipient name
                for (int i = 0; i < _capableClients.getSize(); i++) {
                    final Client client = (Client) _capableClients.getElementAt(i);
                    _recipientId = client.getId();

                    // If current client name matches sought one
                    String clientName = client.getMetadata().getName();
                    if (clientName.matches(_applicationName)) {

                        log("found recipient '" + clientName + "' with id '" + _recipientId + "' ... ");

                        // If current client is one of our STUB
                        Object clientStubFlag = client.getMetadata().get("fr.jmmc.applauncher." + clientName);
                        if (clientStubFlag != null) {
                            // Skip STUBS
                            println("SKIPPED STUB.");
                            return;
                        }

                        // Forward anyreceived message to recipient (if any)
                        if (_message != null) {
                            setState(ClientStubState.SEEKING);

                            // Wait a while for application startup to finish...
                            sleep(1000);

                            // Forward the message
                            setState(ClientStubState.FORWARDING);
                            try {
                                _connector.getConnection().notify(_recipientId, _message);
                            } catch (SampException ex) {
                                Logger.getLogger(ClientStub.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            println("FORWARDED MESSAGE.");

                            setState(ClientStubState.DISCONNECTING);
                        } else {
                            println("NOTHING TO FORWARD.");
                        }

                        // Kill the stub client
                        _connector.setActive(false);
                        log("dying ... ");
                        setState(ClientStubState.DIYING);
                        logDone();
                    }
                }
                // @TODO : restart the STUB if the recipient disapear ?
            }
        }).start();
    }

    /**
     * Register an app-specific capability
     * @param handler message handler
     */
    private void registerCapability(final AbstractMessageHandler handler) {

        _connector.addMessageHandler(handler);

        // This step required even if no custom message handlers added.
        _connector.declareSubscriptions(_connector.computeSubscriptions());
    }

    /**
     * Unregister an app-specific capability
     * @param handler message handler
     */
    private void unregisterCapability(final AbstractMessageHandler handler) {

        _connector.removeMessageHandler(handler);

        // This step required even if no custom message handlers added.
        _connector.declareSubscriptions(_connector.computeSubscriptions());
    }
}
