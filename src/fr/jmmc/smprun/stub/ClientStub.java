/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub;

import fr.jmmc.jmcs.gui.StatusBar;
import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.network.interop.SampManager;
import fr.jmmc.jmcs.network.interop.SampMetaData;

import fr.jmmc.smprsc.StubRegistry;
import fr.jmmc.smprun.DockWindow;
import fr.jmmc.smprun.JnlpStarter;
import fr.jmmc.smprun.stub.data.model.SampStub;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Queue;
import java.util.logging.Level;
import javax.swing.ImageIcon;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Subscriptions;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.SampException;
import org.ivoa.util.concurrent.ThreadExecutors;
import org.ivoa.util.runner.JobListener;
import org.ivoa.util.runner.LocalLauncher;
import org.ivoa.util.runner.RootContext;
import org.ivoa.util.runner.RunContext;
import org.ivoa.util.runner.RunState;
import org.ivoa.util.runner.process.ProcessContext;

/**
 * Registers a fake application to the hub, and later dispatch any received message to the freshly started recipient.
 *
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public final class ClientStub extends Observable implements JobListener {

    /** Class logger */
    private static final java.util.logging.Logger _logger = java.util.logging.Logger.getLogger(ClientStub.class.getName());
    /* members : app meta data object */
    /** Store desired stub application meta data */
    private final Metadata _description;
    /** Convenient proxy to meta data dedicated field) */
    private final String _applicationName;
    /** Store desired stub SAMP capabilities */
    private final SampCapability[] _mTypes;
    /** Store desired JNLP URL */
    private final String _jnlpUrl;
    /** sleep delay in milliseconds before sending the samp message (application startup workaround) */
    private final long _sleepDelayBeforeNotify;
    /** log prefix */
    private final String _logPrefix;
    /* state objects */
    /** internal lock object for synchronization */
    private final Object _lock = new Object();
    /** client stub state */
    private ClientStubState _status;
    /** job context identifier representing the executed application to be able to kill / cancel its execution */
    private volatile Long _jobContextId = null;
    /** Messages queued, to forward once recipient appeared */
    private volatile Queue<Message> _messages = new LinkedList<Message>();
    /* SAMP objects */
    /** Hub connector */
    private final HubConnector _connector;
    /** Potential message handler */
    private AbstractMessageHandler[] _mHandlers = null;

    /**
     * Constructor.
     *
     * @param data XML values
     * @param sleepDelayBeforeNotify sleep delay in milliseconds before sending the samp message
     */
    public ClientStub(final SampStub data) {

        // Retrieve each serialized SAMP meta data
        _description = new Metadata();
        for (fr.jmmc.smprun.stub.data.model.Metadata metadata : data.getMetadatas()) {
            _description.put(metadata.getKey(), metadata.getValue());
        }

        // Retrieve real application name, JNLP URL and startup delay
        _applicationName = _description.getName();
        _logPrefix = "Stub['" + _applicationName + "'] : ";
        _jnlpUrl = _description.getString(SampMetaData.JNLP_URL.id());
        _sleepDelayBeforeNotify = data.getLag().intValue();

        // Add a custom flag to all our created STUB for later skipping while looking for real recipients
        _description.put(SampMetaData.getStubMetaDataId(_applicationName), SampMetaData.STUB_TOKEN);

        // Retrieve and filter each serialized SAMP mType
        List<SampCapability> capabilityList = new ArrayList<SampCapability>();
        for (String capability : data.getSubscriptions()) {

            // Validate read mType against our known mTypes
            SampCapability sampCapability = SampCapability.fromMType(capability);

            // Do not register SAMP internal or likely broadcasted capabilities
            if (sampCapability.isFlagged()) {
                _logger.fine(_logPrefix + "Looks like '" + capability + "' SAMP capability is either broadcastable or of internal use ... skipping.");
                continue;
            }
            // Do not register UNKNOWN capabilities
            if (sampCapability == SampCapability.UNKNOWN) {
                _logger.warning(_logPrefix + "Could not retrieve '" + capability + "' unknown SAMP capability ... skipping.");
                continue;
            }

            _logger.info(_logPrefix + "Found '" + capability + "' SAMP capability.");
            capabilityList.add(sampCapability);
        }
        _mTypes = capabilityList.toArray(new SampCapability[0]);

        setState(ClientStubState.UNDEFINED);

        // Get a hook on the SAMP machinery
        final ClientProfile profile = DefaultClientProfile.getProfile();
        _connector = new HubConnector(profile);
    }

    /**
     * Return the name of the emulated application
     * @return the name of the emulated application
     */
    public String getApplicationName() {
        return _applicationName;
    }

    /**
     * Return the name of the emulated application
     * @return the name of the emulated application
     */
    @Override
    public String toString() {
        return getApplicationName();
    }

    /**
     * @return the URL of the icon if any (null otherwise)
     */
    public ImageIcon getApplicationIcon() {

        // @TODO : Use a generic app icon as placeholder when none available... BUT AppLauncherTester is kept invisible because of this...

        // Try to load embedded one in smprrsc
        return StubRegistry.getEmbeddedApplicationIcon(_applicationName);
    }

    /**
     * @return the JNLP URL
     */
    public String getJnlpUrl() {
        return _jnlpUrl;
    }

    /**
     * @return the SAMP capabilities
     */
    public SampCapability[] getSampCapabilities() {
        return _mTypes;
    }

    /**
     * Define the job context identifier
     * @param jobContextId job context identifier to set
     */
    private void setJobContextId(final Long jobContextId) {
        _jobContextId = jobContextId;
    }

    /**
     * Reset job context...
     */
    private void resetMessageQueue() {
        _messages.removeAll(_messages);
    }

    /**
     * Update stub internal state progression and notifies observers
     * 
     * @param status the current state
     */
    private void setState(final ClientStubState status) {
        // update status
        _status = status;

        setChanged();
        notifyObservers(_status);
    }

    /**
     * Perform initialization (connect to hub, register MTypes ...)
     */
    public void connect() {
        _logger.info(_logPrefix + "connect() invoked by thread [" + Thread.currentThread() + "]");

        // Reentrance / concurrency checks
        synchronized (_lock) {
            if (_status == ClientStubState.UNDEFINED || _status == ClientStubState.DIYING) {
                setState(ClientStubState.INITIALIZING);

                // Try to connect
                if (!connectToHub()) {
                    disconnect();
                }
            }
        }
    }

    /**
     * @return true only if this client stub is really connected to the hub, false otherwise 
     */
    public boolean isConnected() {
        boolean connected = false;

        // Reentrance / concurrency checks
        synchronized (_lock) {
            if (_status.after(ClientStubState.INITIALIZING) && _status.before(ClientStubState.DISCONNECTING)) {
                connected = _connector.isConnected();
            }
        }
        return connected;
    }

    /** 
     * Disconnect from hub 
     */
    public void disconnect() {
        _logger.info(_logPrefix + "disconnect() invoked by thread [" + Thread.currentThread() + "]");

        // Reentrance / concurrency checks
        synchronized (_lock) {
            if (_status.after(ClientStubState.INITIALIZING) && _status.before(ClientStubState.DISCONNECTING)) {

                // Kill the stub client
                setState(ClientStubState.DISCONNECTING);

                _logger.info(_logPrefix + "Disconnecting from hub ...");

                // Disconnect from hub
                _connector.setActive(false);

                _logger.info(_logPrefix + "Dying ...");
                setState(ClientStubState.DIYING);

                resetMessageQueue();
                setJobContextId(null);

                _logger.info(_logPrefix + "Disconnected");
            }
        }
    }

    /**
     * Launch the real application
     */
    public void launchRealApplication() {
        _logger.info(_logPrefix + "launchRealApplication() invoked by thread [" + Thread.currentThread() + "]");

        // Reentrance / concurrency checks
        synchronized (_lock) {
            // Note: when the javaws does not start correctly the application => it will never connect to SAMP; let the user retry ...

            StatusBar.show("Starting '" + getApplicationName() + "' recipient ...");

            DockWindow.getInstance().defineButtonEnabled(this, false);

            if (isConnected()) {
                // only change state if this stub is running:
                setState(ClientStubState.LAUNCHING);
            }

            _logger.info(_logPrefix + "Launching JNLP '" + _jnlpUrl + "' ...");

            // get the process context to be able to kill it later ...
            setJobContextId(JnlpStarter.launch(this));
        }
    }

    /**
     * Cancel or kill the launch of the real application 
     * 
     * TODO: DO not work (javaws can be killed but it will not kill sub processes like java ...)
     */
    public void killRealApplication() {
        _logger.info(_logPrefix + "killRealApplication() invoked by thread [" + Thread.currentThread() + "]");

        // Reentrance / concurrency checks
        synchronized (_lock) {

            if (_jobContextId != null) {

                /*
                 * Note: the cancel does not work on unix system:
                 * javaws is the parent command that launches another command java ...
                 * 
                 * Process.destroy does not kill sub processes: we could use ps -ef ... | kill 
                 * but it tricky again
                 */

                LocalLauncher.cancelOrKillJob(_jobContextId);
                setJobContextId(null);

                // Anyway: revert state like process failure

                // Report failure
                setState(ClientStubState.FAILING);

                // Handle error
                if (!_messages.isEmpty()) {
                    _logger.severe(_logPrefix + "Unable to deliver '" + _messages.size() + "' message(s) :");
                    for (Message msg : _messages) {
                        _logger.severe("\t- '" + msg + "';");
                    }

                    // @TODO : MessagePane ... => State = FAILED => Window (hide)
                }

                // Reset state
                setJobContextId(null);
                resetMessageQueue();

                setState(ClientStubState.LISTENING);

                // Update GUI
                StatusBar.show("Failed to start " + getApplicationName() + ".");
                DockWindow.getInstance().defineButtonEnabled(this, true);
            }
        }
    }

    /** 
     * Connect stub to the hub.
     * @return true if successfully connected to hub, false otherwise.
     */
    private boolean connectToHub() {

        setState(ClientStubState.CONNECTING);

        _logger.info(_logPrefix + "connecting to hub ...");

        // Set connector up
        _connector.declareMetadata(_description);

        // Try to connect
        _connector.setActive(true);

        if (!_connector.isConnected()) {
            _logger.info(_logPrefix + "could not connect to an existing hub.");
            return false;
        }

        // Keep a look out for hubs if initial one shuts down
        _connector.setAutoconnect(5);

        registerStubCapabilities();

        _logger.info(_logPrefix + "connected.");

        return true;
    }

    /**
     * Declare STUB capabilities to the hub.
     */
    private void registerStubCapabilities() {

        setState(ClientStubState.REGISTERING);

        // Lazy initialisation
        if (_mHandlers == null) {

            _mHandlers = new AbstractMessageHandler[_mTypes.length];

            int i = 0;
            for (final SampCapability mType : _mTypes) {

                // Create handler for each stub capability
                _mHandlers[i] = new AbstractMessageHandler(mType.mType()) {

                    /**
                     * Implements message processing
                     *
                     * @param senderId public ID of sender client
                     * @param message message with MType this handler is subscribed to
                     * @throws SampException if any error occurred while message processing
                     */
                    @Override
                    public final Map<?, ?> processCall(final HubConnection connection, final String senderId, final Message message) throws SampException {

                        _logger.info("processCall() invoked by thread [" + Thread.currentThread() + "]");

                        // Backup message and pending queue for later delivery
                        _messages.add(message);

                        _logger.info(_logPrefix + "received '" + mType.mType() + "' message from '" + senderId + "' : '" + message + "'.");

                        // Start application in background:
                        launchRealApplication();

                        // Once the application will have finish started and been fully registered to the hub,
                        // HubMonitor will detect it and ask the stub to forward any pending message to the real application.

                        return null;
                    }
                };

                i++;
            }

            // Declare each message handlers to SAMP hub
            for (final AbstractMessageHandler handler : _mHandlers) {
                _connector.addMessageHandler(handler);
            }
        }

        // This step is required to update message handlers into the hub:
        _connector.declareSubscriptions(_connector.computeSubscriptions());

        _logger.info(_logPrefix + " has declared subscriptions.");

        setState(ClientStubState.LISTENING);
    }

    /** 
     * Implements callback from HubMonitor when the real application is detected...
     * Note: this method is called using dedicated thread (may sleep for few seconds ...)
     * 
     * @param recipientId recipient identifier of the real application.
     */
    public void forwardMessagesToRealRecipient(final String recipientId) {
        _logger.info(_logPrefix + "forwardMessageToRealRecipient() invoked by thread [" + Thread.currentThread() + "]");

        // Reentrance check
        synchronized (_lock) {
            if (_status.after(ClientStubState.REGISTERING) && _status.before(ClientStubState.DISCONNECTING)) {
                _logger.info(_logPrefix + "Forwarding message(s) to real recipient connected with id '" + recipientId + "'.");

                // Forward all received message to recipient (if any)
                if (!_messages.isEmpty()) {
                    setState(ClientStubState.SEEKING);

                    if (_sleepDelayBeforeNotify > 0L) {
                        _logger.info(_logPrefix + "Waiting " + _sleepDelayBeforeNotify + " milliseconds to let real recipient finish its startup ...");

                        // Wait a while for application startup to finish...
                        ThreadExecutors.sleep(_sleepDelayBeforeNotify);
                    }

                    // Check real recipient availability
                    final Subscriptions subscriptions = SampManager.getSubscriptions(recipientId);
                    if (subscriptions != null) {
                        // Try tyo forward each waiting message
                        int messageIndex = 1;
                        final int nbOfMessages = _messages.size();
                        for (Message msg : _messages) {

                            // Check that the current message really match one of the real recipient SAMP capability
                            boolean subscriptionFound = false;
                            for (Object mType : subscriptions.keySet()) {
                                if (mType.toString().equalsIgnoreCase(msg.getMType())) {
                                    subscriptionFound = true;
                                    break;
                                }
                            }
                            if (subscriptionFound) {
                                // Forward the message
                                setState(ClientStubState.FORWARDING);
                                try {
                                    _connector.getConnection().notify(recipientId, msg);
                                } catch (SampException se) {
                                    _logger.log(Level.SEVERE, "Samp notication exception", se);
                                }
                                _logger.info(_logPrefix + "Forwarded message (" + messageIndex + "/" + nbOfMessages + ").");
                            } else {
                                _logger.info(_logPrefix + "Could not find '" + msg.getMType() + "' mType ... skipping.");
                            }
                            messageIndex++;
                        }

                        resetMessageQueue();
                    }
                } else {
                    _logger.info(_logPrefix + "No message to forward.");
                }

                // Kill the stub client
                disconnect();
            }
        }
    }

    /**
     * Perform the event from the given root context.
     * 
     * @see JobListener
     * 
     * @param jobContext root context.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public void performJobEvent(final RootContext jobContext) {
        _logger.info(_logPrefix + "performJobEvent() invoked by thread [" + Thread.currentThread() + "]");

        ProcessContext pCtx;

        switch (jobContext.getState()) {
            case STATE_FINISHED_OK:

                // JNLP process done
                _logger.info(_logPrefix + "JNLP execution status: " + jobContext.getState()
                        + "\n" + jobContext.getRing().getContent("Ring buffer:\n"));

                pCtx = (ProcessContext) jobContext.getChildContexts().get(0);

                _logger.info(_logPrefix + "DONE (with status '" + pCtx.getExitCode() + "').");

                // Reset job context
                setJobContextId(null);

                // Update GUI
                StatusBar.show("Started " + getApplicationName() + ".");

                DockWindow.getInstance().defineButtonEnabled(this, true);
                break;

            case STATE_FINISHED_ERROR:
                // JNLP process failed
                _logger.info(_logPrefix + "JNLP execution status : " + jobContext.getState()
                        + "\n" + jobContext.getRing().getContent("Ring buffer:\n"));


                pCtx = (ProcessContext) jobContext.getChildContexts().get(0);

                _logger.info(_logPrefix + "DONE (with status '" + pCtx.getExitCode() + "').");

            case STATE_CANCELED:
            case STATE_INTERRUPTED:
            case STATE_KILLED:
                // JNLP process failed: clean up:

                // Reentrance check
                synchronized (_lock) {

                    // Report failure
                    setState(ClientStubState.FAILING);

                    // Handle error
                    if (!_messages.isEmpty()) {
                        _logger.severe(_logPrefix + "Unable to deliver '" + _messages.size() + "' message(s) :");
                        for (Message msg : _messages) {
                            _logger.severe("\t- '" + msg + "';");
                        }

                        // MessagePane ... => State= FAILED => Window (hide)
                    }

                    // Reset state
                    setJobContextId(null);
                    resetMessageQueue();

                    setState(ClientStubState.LISTENING);
                }

                // Update GUI
                StatusBar.show("Failed to start '" + getApplicationName() + "' recipient.");
                DockWindow.getInstance().defineButtonEnabled(this, true);

                break;

            default: // Otherwise do nothing
        }
    }

    /**
     * Perform the event from the given run context.
     * 
     * @see JobListener
     * 
     * @param jobContext root context.
     * @param runCtx current run context.
     */
    @Override
    public void performTaskEvent(final RootContext jobContext, final RunContext runCtx) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine(_logPrefix + "job : " + runCtx);
        }
    }

    /**
     * Perform the event from the given run context.
     * 
     * @see JobListener
     * 
     * @param jobContext root context.
     * @param runCtx current run context.
     *
     * @return boolean: true of the processing should continue, false if the job should be terminated.
     */
    @Override
    public boolean performTaskDone(final RootContext jobContext, final RunContext runCtx) {
        return runCtx.getState() == RunState.STATE_FINISHED_OK;
    }
}
