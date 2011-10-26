/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub;

import fr.jmmc.jmcs.gui.StatusBar;
import fr.jmmc.jmcs.gui.SwingUtils;
import fr.jmmc.jmcs.network.interop.SampCapability;

import fr.jmmc.smprun.DockWindow;
import fr.jmmc.smprun.JnlpStarter;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;

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
import org.ivoa.util.runner.JobListener;
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
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ClientStub.class.getName());
    /** home made logs enabled ? (TODO: KILL, use slf4j / JUL at least) */
    private boolean _shouldLog = true;
    /* members : app meta data object */
    /** Store desired stub application metadata */
    private final Metadata _description;
    /** Convenient proxy to Metadata dedicated field) */
    private final String _applicationName;
    /** Store desired stub SAMP capabilities */
    private final SampCapability[] _mTypes;
    /** Store desired JNLP URL */
    private final String _jnlpUrl;
    /* state objects */
    /** Message to forward once recipient appeared */
    private ClientStubState _status;
    /** job context representing the executed application to be able to kill / cancel its execution */
    private RootContext _jobContext;
    /* SAMP objects */
    /** GUI hub connector: TODO HubConnector instead */
    private GuiHubConnector _connector;
    /** Potential message handler */
    private AbstractMessageHandler[] _mHandlers;
    /** Potential recipients */
    private SubscribedClientListModel _capableClients;
    /** Potential recipient id */
    private String _recipientId;
    /** Message to forward once recipient appeared */
    private Message _message;

    /**
     * Constructor.
     *
     * @param description metadata about the stub app
     * @param jnlpUrl URL of Java WebStart recipient
     * @param mTypes handled mtypes
     */
    public ClientStub(final Metadata description, final String jnlpUrl, final SampCapability[] mTypes) {

        _description = description;
        _applicationName = description.getName();

        // Flag any created STUB for later skipping while looking for recipients
        _description.put("fr.jmmc.applauncher." + _applicationName, "STUB");

        _mTypes = mTypes;
        _jnlpUrl = jnlpUrl;

        _mHandlers = null;
        _capableClients = null;

        reset();

        setState(ClientStubState.INITIALIZING);

        // define JSamp log verbosity to warning level (avoid debug messages) :
        Logger.getLogger("org.astrogrid.samp").setLevel(Level.WARNING);

        // @TODO : init JSamp env.
        final ClientProfile profile = DefaultClientProfile.getProfile();

        _connector = new GuiHubConnector(profile);

        connectToHub();
        registerStubCapabilities();
        listenToRecipientConnections();
    }

    /**
     * @return the name of the emulated application
     */
    @Override
    public String toString() {
        return _applicationName;
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

        logLine("connecting to hub ... ");

        // Set connector up
        _connector.declareMetadata(_description);

        // Try to connect
        _connector.setActive(true);

        if (!_connector.isConnected()) {

            // TODO: move that into HubMonitor and start it before any Client Stub

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

    /** Disconnect from hub */
    public void disconnectFromHub() {

        // TODO: reentrance / concurrency checks
        synchronized (this) {
            if (_status.before(ClientStubState.DISCONNECTING)) {

                // check that the client is really connected to the hub:
                if (_connector.isConnected()) {
                    logLine("disconnecting from hub...");

                    // Kill the stub client
                    setState(ClientStubState.DISCONNECTING);

                    // Disconnect from hub
                    _connector.setActive(false);

                    logLine("dying ... ");
                    setState(ClientStubState.DIYING);

                    logDone();

                    reset();
                }
            }
        }
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
                    //protected void processMessage(final String senderId, final Message message) {
                    @Override
                    public final Map<?, ?> processCall(final HubConnection connection, final String senderId, final Message message) throws SampException {

                        // TODO: put message in one FIFO queue or simply discard evrything but the first...

                        // Backup message for later forward
                        _message = message;

                        logLine("received '" + mType.mType() + "' message from '" + senderId + "' : '" + _message + "'.");

                        // Unregister stub from hub to make room for the recipient
//                        _connector.removeMessageHandler(this);
//                        unregisterCapability(this);
//                        logLine("unregistered SAMP capability for mType '" + mType.mType() + "'.");

                        // start application in background:
                        launchApplication();

                        return null;
                    }
                };

                i++;
            }

            // register all message handlers:
            for (final AbstractMessageHandler handler : _mHandlers) {
                _connector.addMessageHandler(handler);
            }

            // This step required to update new message handlers:
            _connector.declareSubscriptions(_connector.computeSubscriptions());
        }
    }

    public void launchApplication() {

        // TODO: reentrance / concurrency checks
        synchronized (this) {
            if (_status == ClientStubState.LISTENING || _status == ClientStubState.PROCESSING) {

                DockWindow.getInstance().defineButtonEnabled(this, false);
                
                setState(ClientStubState.LAUNCHING);

                logLine("web-starting JNLP '" + _jnlpUrl + "' ... ");

                // get the process context to be able to kill it later ...
                _jobContext = JnlpStarter.launch(this);
            }
        }
    }

    /** Set up monitoring of new connection to the hub to detect true applications */
    private void listenToRecipientConnections() {

        setState(ClientStubState.LISTENING);

        if (_capableClients == null) {
            // lazy initialisation:

            // Get a dynamic list of SAMP clients able to respond to the specified capability.
            final String[] mTypeStrings = new String[_mTypes.length];
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
        }

        // but do one first test if one registered app already handle such capability
        SwingUtils.invokeLaterEDT(new Runnable() {

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
        
        final ClientStub thisClient = this;

        // TODO: concurrency issues on state ?? (samp thread, EDT, Job thread) ...

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

                        logLine("found recipient '" + clientName + "' with id '" + _recipientId + "' ... ");

                        // If current client is one of our STUB
                        Object clientStubFlag = client.getMetadata().get("fr.jmmc.applauncher." + clientName);
                        if (clientStubFlag != null) {
                            // Skip STUBS
                            println("SKIPPED STUB.");
                            return;
                        }

                        // reentrance check
                        synchronized (thisClient) {

                            // Forward any received message to recipient (if any)
                            if (_message != null) {
                                setState(ClientStubState.SEEKING);

                                // Wait a while for application startup to finish...
                                sleep(1000);

                                if (_message != null) {
                                    // Forward the message
                                    setState(ClientStubState.FORWARDING);
                                    try {
                                        _connector.getConnection().notify(_recipientId, _message);
                                    } catch (SampException ex) {
                                        Logger.getLogger(ClientStub.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    println("FORWARDED MESSAGE.");
                                }

                                reset();

                            } else {
                                println("NOTHING TO FORWARD.");
                            }

                            // Kill the stub client
                            disconnectFromHub();
                        }

                        // exit from loop:
                        break;
                    }
                }
                // @TODO : restart the STUB if the recipient disapear ?
            }
        }).start();
    }

    /**
     * Perform the event from the given root context
     * 
     * @see JobListener
     * 
     * @param jobContext root context
     */
    @Override
    public void performJobEvent(final RootContext jobContext) {
        ProcessContext pCtx;

        switch (jobContext.getState()) {
            case STATE_FINISHED_OK:

                // Jnlp process done
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("ClientStub.performJobEvent : Jnlp execution status: " + jobContext.getState());
                    logger.info(jobContext.getRing().getContent("Ring buffer:\n"));
                }

                pCtx = (ProcessContext) jobContext.getChildContexts().get(0);

                println("DONE (with status '" + pCtx.getExitCode() + "').");

                StatusBar.show("Started " + toString() + ".");

                DockWindow.getInstance().defineButtonEnabled(this, true);
                
                // reset job context:
                _jobContext = null;
                break;

            case STATE_FINISHED_ERROR:
                // Jnlp process failed
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("ClientStub.performJobEvent : Jnlp execution status: " + jobContext.getState());
                    logger.info(jobContext.getRing().getContent("Ring buffer:\n"));
                }

                pCtx = (ProcessContext) jobContext.getChildContexts().get(0);

                println("DONE (with status '" + pCtx.getExitCode() + "').");

            case STATE_CANCELLED:
            case STATE_INTERRUPTED:
            case STATE_KILLED:
                // Jnlp process failed: clean up:
                // TODO: restore client state ...

                StatusBar.show("Failed to start " + toString() + ".");

                DockWindow.getInstance().defineButtonEnabled(this, true);
                
                // report failure
                setState(ClientStubState.FAILING);

                // handle error:
                if (_message != null) {
                    logger.severe("ClientStub.performJobEvent : unable to deliver message : " + _message);

                    // MessagePane ... => State= FAILED => Window (hide)

                }

                setState(ClientStubState.LISTENING);

                reset();
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
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JobServlet.performTaskEvent : job : " + runCtx);
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

    /**
     * Return the Jnlp Url
     * @return Jnlp Url
     */
    public String getJnlpUrl() {
        return _jnlpUrl;
    }

    /**
     * Reset internal state (message, job context) ...
     */
    private void reset() {

        _recipientId = null;
        _message = null;

        _jobContext = null;

    }
}
