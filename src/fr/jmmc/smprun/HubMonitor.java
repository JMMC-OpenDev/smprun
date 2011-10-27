/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.network.interop.SampManager;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.astrogrid.samp.Client;
import org.astrogrid.samp.gui.SubscribedClientListModel;

/**
 *
 * @author lafrasse
 */
public class HubMonitor {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(HubMonitor.class.getName());
    private final String[] _mTypesStrings;
    private final SubscribedClientListModel _capableClients;

    public static HubMonitor getInstance() {
        return HubMonitorHolder.INSTANCE;
    }

    private static class HubMonitorHolder {

        private static final HubMonitor INSTANCE = new HubMonitor();
    }

    private HubMonitor() {
        System.out.println("HubMonitor()");
        _mTypesStrings = ComputeMTypeArray();
        _capableClients = SampManager.createSubscribedClientListModel(_mTypesStrings);

        // Monitor any modification to the capable clients list
        _capableClients.addListDataListener(new ListDataListener() {

            @Override
            public void contentsChanged(final ListDataEvent e) {
                _logger.entering("ListDataListener", "contentsChanged");
                processHubActivity();
            }

            @Override
            public void intervalAdded(final ListDataEvent e) {
                _logger.entering("ListDataListener", "intervalAdded");
                processHubActivity();
            }

            @Override
            public void intervalRemoved(final ListDataEvent e) {
                _logger.entering("ListDataListener", "intervalRemoved");
                processHubActivity();
            }
        });

        // but do one first test if one registered app already handle such capability
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                processHubActivity();
            }
        });

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

    private String[] ComputeMTypeArray() {
        HashSet<String> mTypesSet = new HashSet<String>();
        for (SampCapability capapbility : HubPopulator.getInstance().getMTypeSet()) {
            mTypesSet.add(capapbility.mType());
            System.out.println("\t monitoring capapbility = " + capapbility.mType());
        }

        // Get a dynamic list of SAMP clients able to respond to the specified capability.
        String[] mTypesStrings = new String[mTypesSet.size()];
        mTypesSet.toArray(mTypesStrings);
        return mTypesStrings;
    }

    public final void processHubActivity() {
        System.out.println("processHubActivity()");

        new Thread(new Runnable() {

            @Override
            public void run() {

                Set<String> knownClients = HubPopulator.getInstance().getStubsMap().keySet();

                // Check each registered clients for the sought recipient name
                for (int i = 0; i < _capableClients.getSize(); i++) {

                    final Client client = (Client) _capableClients.getElementAt(i);
                    String recipientId = client.getId();
                    String clientName = client.getMetadata().getName();
                    System.out.println("\t found recipient '" + clientName + "' with id '" + recipientId + "' ... ");

                    // If current client name does not matches anyone we know
                    if (!knownClients.contains(clientName)) {
                        System.out.println("\t skipping unknown '" + clientName + "' client.");
                        break;
                    }

                    // If current client is one of our STUB
                    Object clientStubFlag = client.getMetadata().get("fr.jmmc.applauncher." + clientName);
                    if (clientStubFlag != null) {
                        // Skip STUBS
                        System.out.println("\t skipping stub '" + clientName + "' client.");
                        break;
                    } else {
                        System.out.println("\t killing stub '" + clientName + "' client.");
                        break;
                        //return;
                    }
                }

                // TODO :  how to start missing stubs ???
                for (String stubName : knownClients) {

                    boolean found = false;

                    // Check each registered clients for the sought recipient name
                    for (int i = 0; i < _capableClients.getSize(); i++) {

                        final Client client = (Client) _capableClients.getElementAt(i);
                        String recipientId = client.getId();
                        String clientName = client.getMetadata().getName();
                        System.out.println("\t found recipient '" + clientName + "' with id '" + recipientId + "' ... ");
                        if (clientName.matches(stubName)) {
                            found = true;
                            break;
                        }
                    }
 
                    if (! found) {
                        System.out.println("\t starting missing '" + stubName + "' stub.");
                        break;
                        //return;
                    }
                }
            }
        }).start();
    }
}
