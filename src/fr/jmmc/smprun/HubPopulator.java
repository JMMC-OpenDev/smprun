/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.smprsc.StubRegistry;
import fr.jmmc.smprsc.data.list.model.Category;
import fr.jmmc.smprun.stub.ClientStub;
import fr.jmmc.smprun.stub.StubMonitor;
import fr.jmmc.smprsc.data.stub.SampApplicationMetaData;
import fr.jmmc.smprsc.data.stub.model.SampStub;
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
    private static final HubPopulator INSTANCE = new HubPopulator();
    /* members */
    /** Client family  / client stub mapping */
    private EnumMap<Category, List<ClientStub>> _familyLists = new EnumMap<Category, List<ClientStub>>(Category.class);
    /** Client stub map keyed by application name */
    private HashMap<String, ClientStub> _clientStubMap = new HashMap<String, ClientStub>();

    /**
     * Return the HubPopulator singleton
     * @return HubPopulator singleton
     */
    public static HubPopulator getInstance() {
        return INSTANCE;
    }

    /**
     * Constructor: create meta data for SAMP applications
     */
    private HubPopulator() {

        for (Category category : Category.values()) {

            _logger.trace("Loading {} category appications.", category.value());

            List<ClientStub> clientList = new ArrayList<ClientStub>();

            List<String> pathes = StubRegistry.getCategoryApplicationResourcePaths(category);
            for (String path : pathes) {

                _logger.trace("Loading {} appications.", path);

                clientList.add(createClientStub(path));
            }

            _familyLists.put(category, clientList);
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
    public Map<String, ClientStub> getClientStubMap() {
        return _clientStubMap;
    }

    /**
     * Return the client stub given its name
     * @param name application name to match
     * @return client stub or null if not found
     */
    public ClientStub getClientStub(final String name) {
        return _clientStubMap.get(name);
    }

    /**
     * Properly disconnect connected clients
     */
    public void disconnectAllStubs() {
        for (ClientStub client : _clientStubMap.values()) {
            client.disconnect();
        }
    }
}
