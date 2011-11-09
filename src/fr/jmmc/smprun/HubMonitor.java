/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.network.interop.SampManager;
import fr.jmmc.smprun.stub.ClientStub;
import fr.jmmc.smprun.stub.ClientStubUtils;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.astrogrid.samp.Client;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.gui.SubscribedClientListModel;
import org.ivoa.util.concurrent.ThreadExecutors;

/**
 * Monitor hub connections (register / unregister) for MTypes corresponding to all client stubs.
 * 
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public final class HubMonitor {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(HubMonitor.class.getName());
    /** HubMonitor singleton */
    private static final HubMonitor INSTANCE = new HubMonitor();
    /* members  */
    /** mType array containing all unique MTypes handled by all applications */
    private final String[] _mTypesStrings;
    /** Registered SAMP recipients corresponding to mType array */
    private final SubscribedClientListModel _capableClients;
    /** Dedicated thread executor */
    private final ThreadExecutors _executor;
    /** List of unique client stubs needed to be started ASAP */
    private Set<ClientStub> _clientStubsToStart = new LinkedHashSet<ClientStub>();

    /**
     * Return the HubMonitor singleton.
     * 
     * @return HubMonitor singleton 
     */
    public static HubMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private HubMonitor() {
        _logger.info("HubMonitor()");

        _mTypesStrings = ComputeMTypeArray();
        _capableClients = SampManager.createSubscribedClientListModel(_mTypesStrings);

        // Monitor any modification to the capable clients list
        _capableClients.addListDataListener(new ListDataListener() {

            @Override
            public void contentsChanged(final ListDataEvent e) {
                _logger.entering("ListDataListener", "contentsChanged");
                handleHubEvent();
            }

            @Override
            public void intervalAdded(final ListDataEvent e) {
                _logger.entering("ListDataListener", "intervalAdded");
                handleHubEvent();
            }

            @Override
            public void intervalRemoved(final ListDataEvent e) {
                _logger.entering("ListDataListener", "intervalRemoved");
                // note: this event is never invoked by JSamp code (1.3) !
                handleHubEvent();
            }
        });

        // Create deidcated thread executor:
        _executor = ThreadExecutors.getSingleExecutor(getClass().getSimpleName() + "ThreadPool");

        // Analize already registered samp clients
        handleHubEvent();

        // TODO: monitor hub shutdown too ?
        // TODO: implement Samp sniffer ASAP

        /*
        String[] mTypes = {"samp.hub.event.shutdown", "samp.hub.event.register", "samp.hub.event.metadata", "samp.hub.event.subscriptions", "samp.hub.event.unregister", "samp.hub.disconnect"};
        SampMessageHandler smh;
        for (String mType : mTypes) {
        smh = new SampMessageHandler(mType) {
        
        @Override
        public void processMessage(String senderId, Message msg) {
        // do stuff
        System.out.println("Received '" + _mType + "' message from '" + senderId + "' : '" + msg + "'.");
        return;
        }
        };
        SampManager.registerCapability(smh);
        }
         */
    }

    /**
     * Compute MType array to listen to.
     * 
     * @return A String array containing all listened mTypes.
     */
    private static String[] ComputeMTypeArray() {
        _logger.info("ComputeMTypeArray()");

        final Set<SampCapability> sampCapabilitySet = HubPopulator.getInstance().getSampCapabilitySet();
        final HashSet<String> mTypesSet = new HashSet<String>(sampCapabilitySet.size());

        for (SampCapability capability : sampCapabilitySet) {
            mTypesSet.add(capability.mType());
        }

        _logger.info("monitoring capabilities = " + sampCapabilitySet);
        _logger.info("monitoring MTypes       = " + mTypesSet);

        // Get a dynamic list of SAMP clients able to respond to the specified capability.
        final String[] mTypesStrings = new String[mTypesSet.size()];
        mTypesSet.toArray(mTypesStrings);

        return mTypesStrings;
    }

    /**
     * Process hub clients in background using the dedicated thread executor
     */
    private void handleHubEvent() {

        // First copy the content of the list model to avoid concurrency issues
        final int size = _capableClients.getSize();
        final Client[] clients = new Client[size];
        for (int i = 0; i < size; i++) {
            clients[i] = (Client) _capableClients.getElementAt(i);
        }

        _executor.submit(new Runnable() {

            /**
             * Process hub information about registered client 
             */
            @Override
            public void run() {
                loopOverHubClients(clients);
            }
        });
    }

    /**
     * Handle changes on registered SAMP recipients:
     * 
     * @param clients current hub registered clients
     */
    private void loopOverHubClients(final Client[] clients) {
        _logger.info("processHubClients() invoked by thread [" + Thread.currentThread() + "]");


        for (ClientStub stub : HubPopulator.getInstance().getClients()) {

            String applicationName = stub.getApplicationName();
            boolean recipientFound = false;

            // Check each registered clients for the sought recipient name
            for (Client client : clients) {

                Metadata md = client.getMetadata();
                String clientName = md.getName();

                if (clientName.matches(applicationName)) {
                    recipientFound = true;

                    String recipientId = client.getId();

                    // If current client is one of our STUB
                    Object clientStubFlag = md.get(ClientStubUtils.getClientStubKey(clientName));
                    if (ClientStubUtils.STUB_TOKEN.equals(clientStubFlag)) {
                        _logger.info("Found STUB recipient '" + clientName + "' [" + recipientId + "] : leaving it alone.");
                    } else {
                        _logger.info("Found REAL recipient '" + clientName + "' [" + recipientId + "] : running STUB trickery.");

                        // Perform callback on client stub
                        handleRealRecipientRegistration(stub, recipientId);
                    }

                    // Do not exit from loop yet, as we can have two SAMP clients having the same application name ??
                    /* break; */
                }
            }

            // If no real nor stub recipient found for application name
            if (!recipientFound) {
                _logger.info("Found NO recipient at all for '" + applicationName + "' : scheduling corresponding STUB startup.");

                // Schedule stub for startup (by adding it to the unique set of client stubs to start asap)
                _clientStubsToStart.add(stub);
            }
        }

        _logger.info("Stub recipients waiting to start : " + _clientStubsToStart);

        // Do launch one client stub at a time (hub will then send one registration event that will cause this method to be invoked again soon for those left)
        final Iterator<ClientStub> it = _clientStubsToStart.iterator();
        if (it.hasNext()) {
            final ClientStub first = it.next();

            _logger.info("Starting STUB recipient '" + first + "'.");
            first.connect();

            // Remove this one from the waiting queue
            it.remove();
        }
    }

    /**
     * Process application registration in background using the generic thread executor
     * 
     * @param stub client stub to invoke
     * @param recipientId recipient identifier of the real application 
     */
    private void handleRealRecipientRegistration(final ClientStub stub, final String recipientId) {

        ThreadExecutors.getGenericExecutor().submit(new Runnable() {

            /**
             * Process application registration using dedicated thread (may sleep for few seconds ...)
             */
            @Override
            public void run() {
                stub.forwardMessage(recipientId);
            }
        });
    }
}
