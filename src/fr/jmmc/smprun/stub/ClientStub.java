/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub;

import fr.jmmc.jmcs.gui.StatusBar;
import fr.jmmc.jmcs.network.interop.SampCapability;

import fr.jmmc.smprun.DockWindow;
import fr.jmmc.smprun.JnlpStarter;
import java.net.URL;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Level;
import javax.swing.ImageIcon;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.ivoa.util.concurrent.ThreadExecutors;
import org.ivoa.util.runner.JobListener;
import org.ivoa.util.runner.LocalLauncher;
import org.ivoa.util.runner.RootContext;
import org.ivoa.util.runner.RunContext;
import org.ivoa.util.runner.RunState;
import org.ivoa.util.runner.process.ProcessContext;

/**
 * Registers a fake App to the hub, and later dispatch any received message to the freshly started recipient.
 *
 * @author Sylvain LAFRASSE
 */
public final class ClientStub extends Observable implements JobListener {

    /** Class logger */
    private static final java.util.logging.Logger _logger = java.util.logging.Logger.getLogger(ClientStub.class.getName());
    /* members : app meta data object */
    /** Store desired stub application metadata */
    private final Metadata _description;
    /** Convenient proxy to Metadata dedicated field) */
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
    private final Object lock = new Object();
    /** client stub state */
    private ClientStubState _status;
    /** job context identifier representing the executed application to be able to kill / cancel its execution */
    private volatile Long _jobContextId = null;
    /** Message to forward once recipient appeared */
    private volatile Message _message = null;
    /* SAMP objects */
    /** Hub connector */
    private final HubConnector _connector;
    /** Potential message handler */
    private AbstractMessageHandler[] _mHandlers = null;

    /**
     * Constructor.
     *
     * @param description metadata about the stub app
     * @param jnlpUrl URL of Java WebStart recipient
     * @param mTypes handled mtypes
     * @param sleepDelayBeforeNotify sleep delay in milliseconds before sending the samp message
     */
    public ClientStub(final Metadata description, final String jnlpUrl, final SampCapability[] mTypes,
            final long sleepDelayBeforeNotify) {

        _description = description;
        _applicationName = description.getName();

        _logPrefix = "Stub['" + _applicationName + "']: ";

        // Flag any created STUB for later skipping while looking for recipients
        _description.put(ClientStubUtils.getClientStubKey(_applicationName), ClientStubUtils.TOKEN_STUB);

        _mTypes = mTypes;
        _jnlpUrl = jnlpUrl;
        _sleepDelayBeforeNotify = sleepDelayBeforeNotify;

        setState(ClientStubState.UNDEFINED);

        // @TODO : init JSamp env.
        final ClientProfile profile = DefaultClientProfile.getProfile();

        // TODO use HubConnector instead
        _connector = new GuiHubConnector(profile);
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
     * @return the URL if the icon if any (null otherwise)
     */
    public ImageIcon getApplicationIcon() {
        ImageIcon imageIcon = null; // @TODO : Use a generic app icon as placeholder when none available...
        URL iconURL = _description.getIconUrl();
        if (iconURL != null) {
            imageIcon = new ImageIcon(iconURL);

            // @TODO : handle NPE
        }
        return imageIcon;
    }

    /**
     * Return the Jnlp Url
     * @return Jnlp Url
     */
    public String getJnlpUrl() {
        return _jnlpUrl;
    }

    /**
     * Define the job context identifier
     * @param jobContextId job context identifier to set
     */
    private void setJobContextId(final Long jobContextId) {
        _jobContextId = jobContextId;
    }

    /**
     * Reset job context ...
     */
    private void resetMessage() {
        _message = null;
    }

    /**
     * Used to follow stub internal state progression
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

        // TODO: reentrance / concurrency checks
        synchronized (lock) {
            if (_status == ClientStubState.UNDEFINED || _status == ClientStubState.DIYING) {
                setState(ClientStubState.INITIALIZING);

                if (!connectToHub()) {
                    disconnect();
                }
            }
        }
    }

    /** 
     * Disconnect from hub 
     */
    public void disconnect() {
        _logger.info(_logPrefix + "disconnect() invoked by thread [" + Thread.currentThread() + "]");

        synchronized (lock) {
            if (_status.after(ClientStubState.INITIALIZING) && _status.before(ClientStubState.DISCONNECTING)) {
                _logger.info(_logPrefix + "disconnecting from hub...");

                // Kill the stub client
                setState(ClientStubState.DISCONNECTING);

                // Disconnect from hub
                _connector.setActive(false);

                _logger.info(_logPrefix + "dying ... ");
                setState(ClientStubState.DIYING);

                resetMessage();
                setJobContextId(null);

                _logger.info(_logPrefix + "disconnected");
            }
        }
    }

    /**
     * Launch the real application
     */
    public void launchApplication() {
        _logger.info(_logPrefix + "launchApplication() invoked by thread [" + Thread.currentThread() + "]");

        // TODO: reentrance / concurrency checks
        synchronized (lock) {
// Not correct: when the javaws does not start correctly the application => it will never connect to SAMP; let the user retry ...
//            if (_status == ClientStubState.LISTENING || _status == ClientStubState.PROCESSING) {

//            if (_status != ClientStubState.LAUNCHING) {

            StatusBar.show("starting " + getApplicationName() + "...");

            DockWindow.getInstance().defineButtonEnabled(this, false);

            setState(ClientStubState.LAUNCHING);

            _logger.info(_logPrefix + "starting JNLP '" + _jnlpUrl + "' ...");

            // get the process context to be able to kill it later ...
            setJobContextId(JnlpStarter.launch(this));
        }
//        }
    }

    /**
     * Cancel or kill the the launching of the real application 
     * 
     * TODO: DO not work (javaws can be killed but it will not kill sub processes like java ...)
     */
    public void killApplication() {
        _logger.info(_logPrefix + "killApplication() invoked by thread [" + Thread.currentThread() + "]");

        // TODO: reentrance / concurrency checks
        synchronized (lock) {

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

                // report failure
                setState(ClientStubState.FAILING);

                // handle error:
                if (_message != null) {
                    _logger.severe(_logPrefix + "unable to deliver message : " + _message);

                    // MessagePane ... => State= FAILED => Window (hide)
                }

                // Reset state
                setJobContextId(null);
                resetMessage();

                setState(ClientStubState.LISTENING);

                // update GUI:
                StatusBar.show("Failed to start " + getApplicationName() + ".");

                DockWindow.getInstance().defineButtonEnabled(this, true);
            }
        }
    }

    /** 
     * Set up connection to hub
     * @return true if successfully connected to hub
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

            // TODO: test case
            return false;
        }

        // Keep a look out for hubs if initial one shuts down
        _connector.setAutoconnect(5);

        _logger.info(_logPrefix + "connected.");

        registerStubCapabilities();

        return true;
    }

    /** Declare STUB capabilities to the hub */
    private void registerStubCapabilities() {

        setState(ClientStubState.REGISTERING);

        if (_mHandlers == null) {
            // lazy initialisation

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

                        // TODO: put message in one FIFO queue or simply discard evrything but the first...

                        // TODO: reentrance checks : message should be null
                        // Backup message for later forward
                        _message = message;

                        _logger.info(_logPrefix + "received '" + mType.mType() + "' message from '" + senderId + "' : '" + _message + "'.");

                        // start application in background:
                        launchApplication();

                        return null;
                    }
                };

                i++;
            }

            // declare message handlers:
            for (final AbstractMessageHandler handler : _mHandlers) {
                _connector.addMessageHandler(handler);
            }
        }

        // This step required to update message handlers into the hub:
        _connector.declareSubscriptions(_connector.computeSubscriptions());

        setState(ClientStubState.LISTENING);
    }

    /** 
     * Implements callback from HubMonitor when the real application is detected ...
     * @param recipientId recipient identifier of the real application 
     */
    public void performRegistration(final String recipientId) {
        _logger.info(_logPrefix + "performRegistration() invoked by thread [" + Thread.currentThread() + "]");

        // reentrance check
        synchronized (lock) {
            if (_status.after(ClientStubState.REGISTERING) && _status.before(ClientStubState.DISCONNECTING)) {
                _logger.info(_logPrefix + "performRegistration: recipient connect with id = " + recipientId);

                // Forward any received message to recipient (if any)
                if (_message != null) {
                    setState(ClientStubState.SEEKING);

                    if (_sleepDelayBeforeNotify > 0l) {
                        _logger.info(_logPrefix + "waiting " + _sleepDelayBeforeNotify + " millis before forwarding the SAMP message ...");

                        // Wait a while for application startup to finish...

                        // TODO: use dedicated thread queue to delay the delivery
                        ThreadExecutors.sleep(_sleepDelayBeforeNotify);
                    }

                    if (_message != null) {
                        // Forward the message
                        setState(ClientStubState.FORWARDING);
                        try {
                            _connector.getConnection().notify(recipientId, _message);
                        } catch (SampException se) {
                            _logger.log(Level.SEVERE, "Samp notication exception", se);
                        }
                        _logger.info(_logPrefix + "FORWARDED MESSAGE.");
                    }

                    resetMessage();

                } else {
                    _logger.info(_logPrefix + "NOTHING TO FORWARD.");
                }

                // Kill the stub client
                disconnect();
            }
        }
    }

    /**
     * Perform the event from the given root context
     * 
     * @see JobListener
     * 
     * @param jobContext root context
     */
    @Override
    @SuppressWarnings("fallthrough")
    public void performJobEvent(final RootContext jobContext) {
        _logger.info(_logPrefix + "performJobEvent() invoked by thread [" + Thread.currentThread() + "]");

        ProcessContext pCtx;

        switch (jobContext.getState()) {
            case STATE_FINISHED_OK:

                // Jnlp process done
                _logger.info(_logPrefix + "Jnlp execution status: " + jobContext.getState()
                        + "\n" + jobContext.getRing().getContent("Ring buffer:\n"));

                pCtx = (ProcessContext) jobContext.getChildContexts().get(0);

                _logger.info(_logPrefix + "DONE (with status '" + pCtx.getExitCode() + "').");

                // Reset job context
                setJobContextId(null);

                // update GUI:
                StatusBar.show("Started " + getApplicationName() + ".");

                DockWindow.getInstance().defineButtonEnabled(this, true);
                break;

            case STATE_FINISHED_ERROR:
                // Jnlp process failed
                _logger.info(_logPrefix + "Jnlp execution status: " + jobContext.getState()
                        + "\n" + jobContext.getRing().getContent("Ring buffer:\n"));


                pCtx = (ProcessContext) jobContext.getChildContexts().get(0);

                _logger.info(_logPrefix + "DONE (with status '" + pCtx.getExitCode() + "').");

            case STATE_CANCELLED:
            case STATE_INTERRUPTED:
            case STATE_KILLED:
                // Jnlp process failed: clean up:

                // reentrance check
                synchronized (lock) {

                    // report failure
                    setState(ClientStubState.FAILING);

                    // handle error:
                    if (_message != null) {
                        _logger.severe(_logPrefix + "unable to deliver message : " + _message);

                        // MessagePane ... => State= FAILED => Window (hide)
                    }

                    // Reset state
                    setJobContextId(null);
                    resetMessage();

                    setState(ClientStubState.LISTENING);
                }

                // update GUI:
                StatusBar.show("Failed to start " + getApplicationName() + ".");

                DockWindow.getInstance().defineButtonEnabled(this, true);

                break;
            default:
        }
    }

    /**
     * Perform the event from the given run context
     * 
     * @see JobListener
     * 
     * @param jobContext root context
     * @param runCtx current run context
     */
    @Override
    public void performTaskEvent(final RootContext jobContext, final RunContext runCtx) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine(_logPrefix + "job : " + runCtx);
        }
    }

    /**
     * Perform the event from the given run context
     * 
     * @see JobListener
     * 
     * @param jobContext root context
     * @param runCtx current run context
     * @return boolean: true of the processing should continue, false if the job
     *         should be terminated
     */
    @Override
    public boolean performTaskDone(final RootContext jobContext, final RunContext runCtx) {
        return runCtx.getState() == RunState.STATE_FINISHED_OK;
    }
}
