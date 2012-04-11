/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.smprsc.StubRegistry;
import fr.jmmc.smprsc.data.list.model.Category;
import fr.jmmc.smprun.stub.ClientStub;
import fr.jmmc.smprun.stub.StubMonitor;
import fr.jmmc.smprsc.data.stub.SampApplicationMetaData;
import fr.jmmc.smprsc.data.stub.model.SampStub;
import fr.jmmc.smprun.preference.PreferenceKey;
import fr.jmmc.smprun.preference.Preferences;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start all known stubs.
 * 
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public class HubPopulator {

    /** Class logger */
    private static final Logger _logger = LoggerFactory.getLogger(HubPopulator.class.getName());
    /** Resource path prefix */
    private static final String RESOURCE_PATH_PREFIX = "fr/jmmc/smprun/resource/";
    /** HubPopulator singleton */
    private static HubPopulator _singleton = null;
    /* members */
    /** Client family  / client stub mapping */
    private EnumMap<Category, List<ClientStub>> _familyLists = new EnumMap<Category, List<ClientStub>>(Category.class);
    /** Client stub map keyed by application name */
    private HashMap<String, ClientStub> _clientStubMap = new HashMap<String, ClientStub>();
    private static final List<String> ALL = null;

    /**
     * Return the HubPopulator singleton
     * @return HubPopulator singleton
     */
    public static HubPopulator start() {
        if (_singleton == null) {
            _singleton = new HubPopulator();
        }
        return _singleton;
    }

    /**
     * Constructor: create meta data for SAMP applications
     */
    private HubPopulator() {

        // For each known category
        for (Category currentCategory : Category.values()) {

            // Forge the list of stub for the current category
            List<ClientStub> currentCategoryClientList = new ArrayList<ClientStub>();

            // For each application name of the category
            List<String> applicationNames = StubRegistry.getCategoryApplicationNames(currentCategory);
            for (String applicationName : applicationNames) {

                // If the user asked through preferences not to start all stubs
                final Preferences preferences = Preferences.getInstance();
                if (preferences.getPreferenceAsBoolean(PreferenceKey.START_SELECTED_STUBS)) {

                    // If the current application stub should not be created (i.e was not selected by the iser)
                    if (!preferences.isApplicationNameSelected(applicationName)) {
                        System.out.println("Skipping unwanted '" + applicationName + "' application.");
                        continue; // Skip stub creation
                    }
                }

                // Forge stub XML description file resource path
                final String stubMetaDataResourcePAth = StubRegistry.forgeApplicationResourcePath(applicationName);

                _logger.debug("Loading '{}' category's stub '{}' data from resource '{}'.", new Object[]{currentCategory.value(), applicationName, stubMetaDataResourcePAth});
                final ClientStub newClientStub = createClientStub(stubMetaDataResourcePAth);
                currentCategoryClientList.add(newClientStub);
            }

            _familyLists.put(currentCategory, currentCategoryClientList);
        }

        _logger.info("configuration: " + _familyLists);
    }

    /**
     * Create a new Client Stub using given arguments and store it in collections
     * 
     * @param resourcePath SAMP application data resource path
     * @return client stub 
     */
    private ClientStub createClientStub(final String resourcePath) {

        SampStub data = SampApplicationMetaData.loadSampSubFromResourcePath(resourcePath);
        final ClientStub client = new ClientStub(data);
        client.addObserver(new StubMonitor());

        _clientStubMap.put(client.getApplicationName(), client);

        return client;
    }

    /**
     * Return the client stub map keyed by application name
     * @return client stub map keyed by application name
     */
    public static Map<String, ClientStub> getClientStubMap() {

        return start()._clientStubMap;
    }

    /**
     * Return the client stub given its name
     * @param name application name to match
     * @return client stub or null if not found
     */
    public static ClientStub retrieveClientStub(final String name) {
        return start()._clientStubMap.get(name);
    }

    /**
     * Properly disconnect connected clients
     */
    public static void disconnectAllStubs() {
        for (ClientStub client : start()._clientStubMap.values()) {
            client.disconnect();
        }
    }
}
