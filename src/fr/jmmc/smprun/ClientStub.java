/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.mcs.interop.SampCapability;
import fr.jmmc.mcs.interop.SampMessageHandler;

import fr.jmmc.mcs.util.Urls;
import java.io.IOException;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.astrogrid.samp.Client;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.DefaultClientProfile;
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
     * Constructor
     *
     * @param applicationName
     * @param mType
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

    public String getApplicationName() {

        return _applicationName;
    }

    private void setState(ClientStubState status) {

        _status = status;

        setChanged();
        notifyObservers(_status);
    }

    private void connectToHub() {

        setState(ClientStubState.CONNECTING);

        System.out.print("Stub '" + _applicationName + "' connecting to hub ... ");

        // Set connector up
        _connector.declareMetadata(_description);

        // Try to connect
        _connector.setActive(true);
        if (!_connector.isConnected()) {
            // Try to start an internal SAMP hub if none available (JNLP do not support external hub) :
            try {
                Hub.runHub(HubServiceMode.CLIENT_GUI);
            } catch (IOException ioe) {
                System.out.println("Stub '" + _applicationName + "'  unable to start internal hub (probably another hub is already running):" + ioe);
            }
            // retry to connectToHub :
            _connector.setActive(true);
        }

        // Keep a look out for hubs if initial one shuts down
        _connector.setAutoconnect(5);
        if (!_connector.isConnected()) {
            System.out.println("Stub '" + _applicationName + "' could not connect to an existing hub or start an internal SAMP hub.");
        }

        // This step required even if no message handlers added.
        _connector.declareSubscriptions(_connector.computeSubscriptions());

        System.out.println("DONE.");
    }

    private void registerStubCapabilities() {

        setState(ClientStubState.REGISTERING);

        for (final SampCapability mType : _mTypes) {

            // Add handler for each stub capability
            registerCapability(new SampMessageHandler(mType) {

                /**
                 * Implements message processing
                 *
                 * @param senderId public ID of sender client
                 * @param message message with MType this handler is subscribed to
                 * @throws SampException if any error occurred while message processing
                 */
                protected void processMessage(final String senderId, final Message message) {

                    setState(ClientStubState.PROCESSING);

                    // Backup message for later forward
                    _message = message;

                    System.out.println("Stub '" + _applicationName + "' received '" + this.handledMType() + "' message from '" + senderId + "' : '" + _message + "'.");

                    // Unregister stub from hub to make room for the recipient
                    unregisterCapability(this);

                    setState(ClientStubState.LAUNCHING);

                    System.out.print("Stub '" + _applicationName + "' web-starting JNLP '" + _jnlpUrl + "' ... ");
                    int status = JnlpStarter.exec(_jnlpUrl);
                    System.out.println("DONE (with status '" + status + "').");

                    // @TODO : show a popup window to ask user patience while webstarting JNLP
                }
            });
        }
    }

    private void listenToRecipientConnections() {

        setState(ClientStubState.LISTENING);

        System.out.println("Stub '" + _applicationName + "' starting listening to recipient connections ...");

        // Get a dynamic list of SAMP clients able to respond to the specified capability.
        String[] mTypeStrings = new String[_mTypes.length];
        for (int i = 0; i < _mTypes.length; i++) {
            mTypeStrings[i] = _mTypes[i].mType();
            System.out.println(" - listening for mType '" + mTypeStrings[i] + "'.");
        }
        _capableClients = new SubscribedClientListModel(_connector, mTypeStrings);

        // Monitor any modification to the capable clients list
        _capableClients.addListDataListener(new ListDataListener() {

            public void contentsChanged(final ListDataEvent e) {
                System.out.println("Stub '" + _applicationName + "' ListDataListener : contentsChanged.");
                lookForRecipientAvailability();
            }

            public void intervalAdded(final ListDataEvent e) {
                System.out.println("Stub '" + _applicationName + "' ListDataListener : intervalAdded.");
                lookForRecipientAvailability();
            }

            public void intervalRemoved(final ListDataEvent e) {
                System.out.println("Stub '" + _applicationName + "' ListDataListener : intervalRemoved.");
                lookForRecipientAvailability();
            }
        });

        System.out.println("DONE.");

        // but do one first test if one registered app already handle such capability
        lookForRecipientAvailability();
    }

    private void lookForRecipientAvailability() {

        System.out.println("Stub '" + _applicationName + "' looking for recipient availability ... ");

        // Check each registered clients for the seeked recipient name
        for (int i = 0; i < _capableClients.getSize(); i++) {
            final Client client = (Client) _capableClients.getElementAt(i);
            _recipientId = client.getId();

            String clientName = client.getMetadata().getName();
            if (clientName.matches(_applicationName)) {
                System.out.println(" - found candidate '" + clientName + "' recipient with id '" + _recipientId + "'.");

                Object clientStubFlag = client.getMetadata().get("fr.jmmc.applauncher." + clientName);
                if (clientStubFlag == null) {

                    setState(ClientStubState.SEEKING);

                    // Forward recevied message to recipient (if any)
                    System.out.print(" - forwarding message (if any) to '" + _recipientId + "' client ... ");
                    try {
                        if (_message != null) {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(ClientStub.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            setState(ClientStubState.FORWARDING);
                            _connector.getConnection().notify(_recipientId, _message);
                        }
                    } catch (SampException ex) {
                        Logger.getLogger(ClientStub.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.out.println("DONE.");

                    // Kills the stub client
                    setState(ClientStubState.DISCONNECTING);
                    _connector.setActive(false);
                }
            } else {
                System.out.println(" - skipping STUB '" + clientName + "' recipient with id '" + _recipientId + "'.");
            }
        }

        System.out.println("DONE.");
    }

    /**
     * Register an app-specific capability
     * @param handler message handler
     */
    private void registerCapability(final SampMessageHandler handler) {

        _connector.addMessageHandler(handler);

        System.out.println("Stub '" + _applicationName + "' registered SAMP capability for mType '" + handler.handledMType() + "'.");

        // This step required even if no custom message handlers added.
        _connector.declareSubscriptions(_connector.computeSubscriptions());
    }

    /**
     * Unregister an app-specific capability
     * @param handler message handler
     */
    private void unregisterCapability(final SampMessageHandler handler) {

        _connector.removeMessageHandler(handler);

        System.out.println("Stub '" + _applicationName + "' unregistered SAMP capability for mType '" + handler.handledMType() + "'.");

        // This step required even if no custom message handlers added.
        _connector.declareSubscriptions(_connector.computeSubscriptions());
    }
}
