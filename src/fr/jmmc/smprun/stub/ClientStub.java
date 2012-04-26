/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub;

import fr.jmmc.jmcs.gui.component.StatusBar;
import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.network.interop.SampManager;
import fr.jmmc.jmcs.network.interop.SampMetaData;

import fr.jmmc.smprsc.data.stub.StubMetaData;
import fr.jmmc.smprun.DockWindow;
import fr.jmmc.jmcs.util.JnlpStarter;
import fr.jmmc.smprsc.data.stub.model.SampStub;
import fr.jmmc.smprun.preference.Preferences;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Queue;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers a fake application to the hub, and later dispatch any received message to the freshly started recipient.
 *
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public final class ClientStub extends Observable implements JobListener {

    /** Class logger */
    private static final Logger _logger = LoggerFactory.getLogger(ClientStub.class.getName());
    /* members : app meta data object */
    /** Store desired stub application meta data */
    private final Metadata _description;
    /** Convenient proxy to meta data dedicated field) */
    private final String _applicationName;
    /** Store desired stub SAMP capabilities */
    private final SampCapability[] _mTypes;
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
     */
    public ClientStub(final SampStub data) {

        // Retrieve each serialized SAMP meta data
        _description = new Metadata();
        for (fr.jmmc.smprsc.data.stub.model.Metadata metadata : data.getMetadatas()) {
            _description.put(metadata.getKey(), metadata.getValue());
        }

        // Retrieve real application name, JNLP URL and startup delay
        _applicationName = _description.getName();
        _logPrefix = "Stub['" + _applicationName + "'] ";
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
                _logger.debug("{}Looks like '{}' SAMP capability is either broadcastable or of internal use ... skipping.", _logPrefix, capability);
                continue;
            }
            // Do not register UNKNOWN capabilities
            if (sampCapability == SampCapability.UNKNOWN) {
                _logger.warn("{}Could not retrieve '{}' unknown SAMP capability ... skipping.", _logPrefix, capability);
                continue;
            }

            _logger.info("{}Found '{}' SAMP capability.", _logPrefix, capability);

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
        return StubMetaData.getEmbeddedApplicationIcon(_applicationName);
    }

    /**
     * @return the good JNLP URL, whether production or beta one according to user preferences current state.
     */
    public String getFinalJnlpUrl() {

        final List<String> betaApplicationNames = Preferences.getInstance().getBetaApplicationNames();

        SampMetaData sampFinalJnlpId = SampMetaData.JNLP_URL;
        if (betaApplicationNames.contains(_applicationName)) {
            sampFinalJnlpId = SampMetaData.JNLP_BETA_URL;
        }

        return _description.getString(sampFinalJnlpId.id());
    }

    /**
     * @return the SAMP capabilities
     */
    public SampCapability[] getSampCapabilities() {
        return _mTypes;
    }

    /**
     * Change the application button state (enabled or disabled) on the dock window
     * @param state true to enable the application button
     */
    private void setClientButtonEnabled(final boolean state) {
        // Only if DockWindow is visible
        final DockWindow dockWindow = DockWindow.getInstance();
        if (dockWindow != null) {
            dockWindow.setClientButtonEnabled(this, state);
        }
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
        _messages.clear();
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
        _logger.info("{}connect()", _logPrefix);

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
        _logger.info("{}disconnect()", _logPrefix);

        // Reentrance / concurrency checks
        synchronized (_lock) {
            if (_status.after(ClientStubState.INITIALIZING) && _status.before(ClientStubState.DISCONNECTING)) {

                // Kill the stub client
                setState(ClientStubState.DISCONNECTING);

                _logger.info("{}Disconnecting from hub ...", _logPrefix);

                // Disconnect from hub
                _connector.setActive(false);

                _logger.info("{}Dying ...", _logPrefix);

                setState(ClientStubState.DIYING);

                // openJDK issue: detach from started javaws process:
                cleanup(true);

                _logger.info("{}Disconnected.", _logPrefix);
            }
        }
    }

    /**
     * Launch the real application
     */
    public void launchRealApplication() {
        _logger.info("{}launchRealApplication()", _logPrefix);

        // Reentrance / concurrency checks
        synchronized (_lock) {
            // Note: when the javaws does not start correctly the application => it will never connect to SAMP; let the user retry ...

            StatusBar.show("starting '" + getApplicationName() + "' recipient...");

            _logger.info("{}Launching JNLP '{}' ...", _logPrefix, getFinalJnlpUrl());

            // stub is connected i.e. monitoring samp messages ...
            if (isConnected()) {
                // only change state if this stub is running:
                setState(ClientStubState.LAUNCHING);

                setClientButtonEnabled(false);

                // get the process context to be able to kill it later ...
                setJobContextId(JnlpStarter.launch(getFinalJnlpUrl(), this));
            } else {
                // just start application without callbacks:
                JnlpStarter.launch(getFinalJnlpUrl());
            }
        }
    }

    /**
     * Cancel or kill the launch of the real application 
     */
    public void killRealApplication() {
        _logger.info("{}killRealApplication()", _logPrefix);

        cleanup(false);
    }

    /**
     * Handle cleanup (process, button state and other internal state)
     * @param success true for success
     */
    private void cleanup(final boolean success) {
        _logger.info("{}cleanup()", _logPrefix);

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

                if (_logger.isDebugEnabled()) {
                    _logger.debug("{}cleanup(): cancelOrKillJob = {}", _logPrefix, _jobContextId);
                }

                LocalLauncher.cancelOrKillJob(_jobContextId);
                setJobContextId(null);
            }

            // check current state to avoid reporting failure twice or incorrect state:
            final boolean doFail = !success && (_status.after(ClientStubState.PROCESSING) && _status.before(ClientStubState.DISCONNECTING));

            if (_logger.isDebugEnabled()) {
                _logger.debug("{}cleanup(): doFail = {}", _logPrefix, doFail);
            }

            if (doFail) {
                // Report failure
                setState(ClientStubState.FAILING);
            }

            // Handle error
            if (!_messages.isEmpty()) {
                _logger.error("{}Unable to deliver '{}' message(s) :", _logPrefix, _messages.size());
                for (Message msg : _messages) {
                    _logger.error("\t- '{}'", msg);
                }

                // @TODO : MessagePane ... => State = FAILED => Window (hide)
            }

            // Reset state
            setJobContextId(null);
            resetMessageQueue();

            if (doFail) {
                setState(ClientStubState.LISTENING);

                // Update GUI
                StatusBar.show("failed to start '" + getApplicationName() + "'.");
            } else if (success) {

                // Update GUI
                StatusBar.show("started '" + getApplicationName() + "'.");
            }
            setClientButtonEnabled(true);
        }
    }

    /** 
     * Connect stub to the hub.
     * @return true if successfully connected to hub, false otherwise.
     */
    private boolean connectToHub() {

        setState(ClientStubState.CONNECTING);

        _logger.info("{}connecting to hub ...", _logPrefix);

        // Set connector up
        _connector.declareMetadata(_description);

        // Try to connect
        _connector.setActive(true);

        if (!_connector.isConnected()) {
            _logger.info("{}could not connect to an existing hub.", _logPrefix);
            return false;
        }

        // Keep a look out for hubs if initial one shuts down
        _connector.setAutoconnect(5);

        registerStubCapabilities();

        _logger.info("{}connected.", _logPrefix);

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

                        _logger.info("{}processCall()", _logPrefix);

                        // Backup message and pending queue for later delivery
                        _messages.add(message);

                        _logger.info("{}received '{}' message from '{}' : '{}'.", new Object[]{_logPrefix, mType.mType(), senderId, message});

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

        _logger.info("{} has declared subscriptions.", _logPrefix);

        setState(ClientStubState.LISTENING);
    }

    /** 
     * Implements callback from HubMonitor when the real application is detected...
     * Note: this method is called using dedicated thread (may sleep for few seconds ...)
     * 
     * @param recipientId recipient identifier of the real application.
     */
    public void forwardMessagesToRealRecipient(final String recipientId) {
        _logger.info("{}forwardMessageToRealRecipient()", _logPrefix);

        // Reentrance check
        synchronized (_lock) {
            if (_status.after(ClientStubState.REGISTERING) && _status.before(ClientStubState.DISCONNECTING)) {
                _logger.info("{}Forwarding message(s) to real recipient connected with id '{}'.", _logPrefix, recipientId);

                // Forward all received message to recipient (if any)
                if (!_messages.isEmpty()) {
                    setState(ClientStubState.SEEKING);

                    if (_sleepDelayBeforeNotify > 0L) {
                        _logger.info("{}Waiting {} milliseconds to let real recipient finish its startup ...", _logPrefix, _sleepDelayBeforeNotify);

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
                                    _logger.error("{}Samp notication exception:", _logPrefix, se);
                                }
                                _logger.info("{}Forwarded message ({} / {}).", new Object[]{_logPrefix, messageIndex, nbOfMessages});
                            } else {
                                _logger.info("{}Could not find '{}' mType ... skipping.", _logPrefix, msg.getMType());
                            }
                            messageIndex++;
                        }

                        resetMessageQueue();
                    }
                } else {
                    _logger.info("{}No message to forward.", _logPrefix);
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
        _logger.debug("{}performJobEvent()", _logPrefix);

        ProcessContext pCtx;

        switch (jobContext.getState()) {
            case STATE_FINISHED_OK:

                // JNLP process done
                _logger.info("{}JNLP execution status: {}\n{}",
                        new Object[]{_logPrefix, jobContext.getState(), jobContext.getRing().getContent("Ring buffer:\n")});

                pCtx = (ProcessContext) jobContext.getChildContexts().get(0);

                _logger.info("{}DONE (with status '{}').", _logPrefix, pCtx.getExitCode());

                // Reset job context
                setJobContextId(null);
                break;

            case STATE_FINISHED_ERROR:
                // JNLP process failed
                _logger.info("{}JNLP execution status: {}\n{}",
                        new Object[]{_logPrefix, jobContext.getState(), jobContext.getRing().getContent("Ring buffer:\n")});

                pCtx = (ProcessContext) jobContext.getChildContexts().get(0);

                _logger.info("{}DONE (with status '{}').", _logPrefix, pCtx.getExitCode());

                // JNLP process failed: clean up:
                cleanup(false);
                break;

            case STATE_CANCELED:
            case STATE_INTERRUPTED:
            case STATE_KILLED:
                // JNLP process failed
                _logger.info("{}JNLP execution status: {}\n{}",
                        new Object[]{_logPrefix, jobContext.getState(), jobContext.getRing().getContent("Ring buffer:\n")});

                // JNLP process failed: clean up:
                cleanup(false);
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
        _logger.debug("{}job : {}", _logPrefix, runCtx);
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
