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
 * Monitor hub connections (register / unregister) for MTypes corresponding to all client stubs
 * @author lafrasse
 */
public final class HubMonitor {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(HubMonitor.class.getName());
    /** HubMonitor singleton */
    private static final HubMonitor INSTANCE = new HubMonitor();
    /* members  */
    /** mType array containing all unique MTypes handled by all applications */
    private final String[] _mTypesStrings;
    /** registered samp recipients corresponding to mType array */
    private final SubscribedClientListModel _capableClients;
    /** dedicated thread executor */
    private final ThreadExecutors _executor;
    /** list of unique client stubs needed to be started asap */
    private Set<ClientStub> _clientStubsToStart = new LinkedHashSet<ClientStub>();

    /**
     * Return the HubMonitor singleton 
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

        this._mTypesStrings = ComputeMTypeArray();
        this._capableClients = SampManager.createSubscribedClientListModel(_mTypesStrings);

        // Monitor any modification to the capable clients list
        this._capableClients.addListDataListener(new ListDataListener() {

            @Override
            public void contentsChanged(final ListDataEvent e) {
                _logger.entering("ListDataListener", "contentsChanged");
                submitHubEvent();
            }

            @Override
            public void intervalAdded(final ListDataEvent e) {
                _logger.entering("ListDataListener", "intervalAdded");
                submitHubEvent();
            }

            @Override
            public void intervalRemoved(final ListDataEvent e) {
                _logger.entering("ListDataListener", "intervalRemoved");
                // note: this event is never invoked by JSamp code (1.3) !
                submitHubEvent();
            }
        });

        // Create deidcated thread executor:
        this._executor = ThreadExecutors.getSingleExecutor(getClass().getSimpleName());

        // Analize already registered samp clients:
        submitHubEvent();

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
     * Compute MType array to listen to
     * @return MType[]
     */
    private static String[] ComputeMTypeArray() {
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
    private void submitHubEvent() {

        // First copy the content of the list model to avoid concurrency issues:

        final int size = _capableClients.getSize();

        final Client[] clients = new Client[size];

        for (int i = 0; i < size; i++) {
            clients[i] = (Client) _capableClients.getElementAt(i);
        }

        this._executor.submit(new Runnable() {

            /**
             * Process hub information about registered client 
             */
            @Override
            public void run() {
                processHubClients(clients);
            }
        });
    }

    /**
     * Handle changes on registered samp recipients: TODO: javadoc
     * 
     * @param clients current hub registered clients
     */
    private void processHubClients(final Client[] clients) {
        _logger.info("processHubClients() invoked by thread [" + Thread.currentThread() + "]");

        Metadata md;
        String applicationName, clientName, recipientId;
        Object clientStubFlag;
        boolean found;

        for (ClientStub stub : HubPopulator.getInstance().getClients()) {

            applicationName = stub.getApplicationName();
            found = false;

            // Check each registered clients for the sought recipient name
            for (Client client : clients) {

                md = client.getMetadata();
                clientName = md.getName();

                if (clientName.matches(applicationName)) {
                    found = true;

                    recipientId = client.getId();

                    // If current client is one of our STUB
                    clientStubFlag = md.get(ClientStubUtils.getClientStubKey(clientName));

                    if (ClientStubUtils.TOKEN_STUB.equals(clientStubFlag)) {
                        _logger.info("\t found recipient '" + clientName + "' [" + recipientId + "] : client stub.");

                    } else {
                        _logger.info("\t found recipient '" + clientName + "' [" + recipientId + "] : real application.");

                        // perform callback on client stub:
                        stub.performRegistration(recipientId);
                    }

                    // do not exit from loop as we can have two samp clients having the same application name ??
                    /* break; */
                }
            }

            if (!found) {
                _logger.info("\t add missing client ['" + applicationName + "'].");

                // add this stub to the unique set of client stubs to start soon:
                _clientStubsToStart.add(stub);
            }
        }

        _logger.info("client stubs needed to start: " + _clientStubsToStart);

        // Do launch one client stub at once:
        // the hub will send then one registration event that will cause this method to be invoked soon

        final Iterator<ClientStub> it = _clientStubsToStart.iterator();
        if (it.hasNext()) {
            final ClientStub first = it.next();

            _logger.info("starting client stub: " + first);
            first.connect();

            // remove this one
            it.remove();
        }
    }
}
