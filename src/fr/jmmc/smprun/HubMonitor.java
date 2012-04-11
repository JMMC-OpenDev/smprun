/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.network.interop.SampManager;
import fr.jmmc.jmcs.network.interop.SampMetaData;
import fr.jmmc.smprun.stub.ClientStub;
import fr.jmmc.smprsc.data.stub.SampApplicationMetaData;
import java.util.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.astrogrid.samp.Client;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Subscriptions;
import org.astrogrid.samp.gui.SubscribedClientListModel;
import org.ivoa.util.concurrent.ThreadExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitor hub connections (register / unregister) for MTypes corresponding to all client stubs.
 * 
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public final class HubMonitor {

    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger(HubMonitor.class.getName());
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
    /** Map of sniffed real application meta-data */
    private HashMap<String, SampApplicationMetaData> _sniffedRealApplications = new HashMap<String, SampApplicationMetaData>();

    /**
     * Return the HubMonitor singleton.
     * 
     * @return HubMonitor singleton 
     */
    public static HubMonitor getInstance() {
        return INSTANCE;
    }

    /** Private constructor */
    private HubMonitor() {
        _logger.info("HubMonitor()");

        _mTypesStrings = ComputeMTypeArray();
        _capableClients = SampManager.createSubscribedClientListModel(_mTypesStrings);

        // Monitor any modification to the capable clients list
        _capableClients.addListDataListener(new ListDataListener() {

            @Override
            public void contentsChanged(final ListDataEvent e) {
                _logger.trace("ListDataListener", "contentsChanged");
                handleHubEvent();
            }

            @Override
            public void intervalAdded(final ListDataEvent e) {
                _logger.trace("ListDataListener", "intervalAdded");
                handleHubEvent();
            }

            @Override
            public void intervalRemoved(final ListDataEvent e) {
                _logger.trace("ListDataListener", "intervalRemoved");
                // note: this event is never invoked by JSamp code (1.3) !
                handleHubEvent();
            }
        });

        // Create deidcated thread executor:
        _executor = ThreadExecutors.getSingleExecutor(getClass().getSimpleName() + "ThreadPool");

        // Analize already registered samp clients
        handleHubEvent();
    }

    /**
     * Compute MType array to listen to.
     * 
     * @return A String array containing all listened mTypes.
     */
    private static String[] ComputeMTypeArray() {
        _logger.info("ComputeMTypeArray()");

        final HashSet<String> mTypesSet = new HashSet<String>();

        for (SampCapability capability : SampCapability.values()) {
            if (!capability.isFlagged()) {
                mTypesSet.add(capability.mType());
            }
        }

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
        _logger.info("loopOverHubClients() invoked by thread [" + Thread.currentThread() + "]");

        for (ClientStub stub : HubPopulator.getInstance().getClients()) {

            String stubName = stub.getApplicationName();
            boolean recipientFound = false;

            // Check each registered clients for the sought recipient name
            for (Client client : clients) {

                Metadata md = client.getMetadata();
                String clientName = md.getName();

                if (clientName.matches(stubName)) {
                    recipientFound = true;

                    String recipientId = client.getId();

                    // If current client is one of our STUB
                    Object clientIsAStubFlag = md.get(SampMetaData.getStubMetaDataId(clientName));
                    if (SampMetaData.STUB_TOKEN.equals(clientIsAStubFlag)) {
                        _logger.info("Found STUB recipient '" + clientName + "' [" + recipientId + "] : leaving it alone.");
                    } else {

                        if (stub.isConnected()) {
                            _logger.info("Found REAL recipient '" + clientName + "' [" + recipientId + "] : running STUB trickery !");

                            // Retrieve real application metadata for sniffing purpose
                            retrieveRealRecipientMetadata(client);

                            // Perform callback on client stub in background
                            handleNewRealRecipientDetection(stub, recipientId);
                        } else {
                            _logger.info("Found REAL recipient '" + clientName + "' [" + recipientId + "] : but the STUB is already disconnected.");
                        }
                    }

                    // Do not exit from loop as we can have two SAMP clients having the same application name: real and stub for example.
                }
            }

            // If no real nor stub recipient found for application name
            if (!recipientFound) {

                if (stub.isConnected()) {
                    // Could not append !!! If the stub is already connected, a client was necesserarly found.
                    _logger.info("Found NO recipient at all for '" + stubName + "' : but the STUB is already connected.");
                } else {
                    _logger.info("Found NO recipient at all for '" + stubName + "' : scheduling corresponding STUB startup.");

                    // Schedule stub for startup (by adding it to the unique set of client stubs to start asap)
                    _clientStubsToStart.add(stub);
                }
            }
        }

        if (!_clientStubsToStart.isEmpty()) {
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

        // Check each registered clients for unknown applications
        for (Client client : clients) {

            Metadata md = client.getMetadata();
            String clientName = md.getName();

            ClientStub unknownApplicationStub = HubPopulator.getInstance().getClientStub(clientName);
            if (unknownApplicationStub == null) {

                _logger.info("Detected an unknown application '" + clientName + "'.");
                retrieveRealRecipientMetadata(client);
            }
        }
    }

    /**
     * Process application registration in background using the generic thread executor
     * 
     * @param stub client stub to invoke
     * @param recipientId recipient identifier of the real application 
     */
    private void handleNewRealRecipientDetection(final ClientStub stub, final String recipientId) {

        ThreadExecutors.getGenericExecutor().submit(new Runnable() {

            /**
             * Process application registration using dedicated thread (may sleep for few seconds ...)
             */
            @Override
            public void run() {
                stub.forwardMessagesToRealRecipient(recipientId);
            }
        });
    }

    /**
     * Collect all real application meta-data.
     * 
     * @param client the real application
     */
    private void retrieveRealRecipientMetadata(final Client client) {
        final Metadata md = client.getMetadata();
        final String name = md.getName();
        final Subscriptions subscriptions = client.getSubscriptions();

        // TODO : store previously dismissed apps in preference

        if (!_sniffedRealApplications.containsKey(name)) {
            _logger.info("Sniffed new real application ''{0}'' : backed up its metadata and subscriptions.", name);
            SampApplicationMetaData stubMetaData = new SampApplicationMetaData(md, subscriptions);
            _sniffedRealApplications.put(name, stubMetaData);
            stubMetaData.reportToCentralRepository();
        }
    }
}
