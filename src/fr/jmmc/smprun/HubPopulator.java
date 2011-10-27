/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.smprun.stub.StubMonitor;
import fr.jmmc.smprun.stub.ClientStub;
import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.smprun.stub.ClientStubFamily;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.astrogrid.samp.Metadata;

/**
 * Instanciate all known stubs.
 * @author Sylvain LAFRASSE
 */
public class HubPopulator {

    /** Class logger */
    private static final java.util.logging.Logger _logger = java.util.logging.Logger.getLogger(HubPopulator.class.getName());
    /** HubPopulator singleton */
    private static final HubPopulator INSTANCE = new HubPopulator();
    /* members */
    /** Client family  / client stub mapping */
    private EnumMap<ClientStubFamily, List<ClientStub>> _familyLists = new EnumMap<ClientStubFamily, List<ClientStub>>(ClientStubFamily.class);
    /** all client stubs */
    private final List<ClientStub> _clients = new ArrayList<ClientStub>(5);
    private HashMap<String, ClientStub> _stubsMap = new HashMap<String, ClientStub>();
    private HashSet<SampCapability> _mTypeSet = new HashSet<SampCapability>();

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
        Metadata md;
        String name;
        SampCapability[] capabilities;
        String jnlpUrl;
        ClientStub client;
        List<ClientStub> clients;

        // @TODO : Grab all this from the Web/OV

        // Note: Use Icon URL pointing to files extracted from Jar file (see resource package)

        clients = new ArrayList<ClientStub>(3);

        // --- ASPRO2 ---
        md = new Metadata();
        name = "Aspro2";
        md.setName(name);
        md.setIconUrl(extractResource("aspro2-6464.png")); // http://www.jmmc.fr/searchcal/images/aspro2-6464.png
        jnlpUrl = "http://apps.jmmc.fr/~swmgr/Aspro2/Aspro2.jnlp";
        capabilities = new SampCapability[]{SampCapability.LOAD_VO_TABLE};
        _mTypeSet.addAll(Arrays.asList(capabilities));
        client = new ClientStub(md, jnlpUrl, capabilities);
        client.addObserver(new StubMonitor());
        clients.add(client);
        _stubsMap.put(name, client);

        // --- SEARCHCAL ---
        md = new Metadata();
        name = "SearchCal";
        md.setName(name);
        md.setIconUrl(extractResource("searchcal-6464.png")); // http://apps.jmmc.fr/~sclws/SearchCal/AppIcon.png
        jnlpUrl = "http://apps.jmmc.fr/~sclws/SearchCal/SearchCal.jnlp";
        capabilities = new SampCapability[]{SampCapability.SEARCHCAL_START_QUERY};
        _mTypeSet.addAll(Arrays.asList(capabilities));
        client = new ClientStub(md, jnlpUrl, capabilities);
        client.addObserver(new StubMonitor());
        clients.add(client);
        _stubsMap.put(name, client);

        // --- LITPRO ---
        md = new Metadata();
        name = "LITpro";
        md.setName(name);
        md.setIconUrl(extractResource("litpro-6464.png")); // http://www.jmmc.fr/images/litpro6464ws.jpg
        jnlpUrl = "http://jmmc.fr/~swmgr/LITpro/LITpro.jnlp";
        capabilities = new SampCapability[]{SampCapability.LITPRO_START_SETTING};
        _mTypeSet.addAll(Arrays.asList(capabilities));
        client = new ClientStub(md, jnlpUrl, capabilities);
        client.addObserver(new StubMonitor());
        clients.add(client);
        _stubsMap.put(name, client);

        _familyLists.put(ClientStubFamily.JMMC, clients);
        _clients.addAll(clients);

        clients = new ArrayList<ClientStub>(2);

        /*
         * Note: Following SAMP messages must not trigger application startup:
         * - SampCapability.POINT_COORDINATES
         * - SampCapability.HIGHLIGHT_ROW
         * - SampCapability.SELECT_LIST
         */

        // --- ALADIN ---
        md = new Metadata();
        name = "Aladin";
        md.setName(name);
        md.setIconUrl(extractResource("aladin-6464.png")); // http://aladin.u-strasbg.fr/aladin_large.gif
        jnlpUrl = "http://aladin.u-strasbg.fr/java/nph-aladin.pl?frame=get&id=aladin.jnlp";
        capabilities = new SampCapability[]{SampCapability.LOAD_VO_TABLE,
            /* SampCapability.POINT_COORDINATES, */
            SampCapability.LOAD_FITS_IMAGE,
            /* SampCapability.HIGHLIGHT_ROW, */
            SampCapability.LOAD_FITS_TABLE, /* SampCapability.SELECT_LIST */};
        _mTypeSet.addAll(Arrays.asList(capabilities));
        client = new ClientStub(md, jnlpUrl, capabilities);
        client.addObserver(new StubMonitor());
        clients.add(client);
        _stubsMap.put(name, client);

        // --- TOPCAT ---
        md = new Metadata();
        name = "topcat";
        md.setName(name);
        md.setIconUrl(extractResource("topcat-6464.png")); // "http://www.star.bris.ac.uk/~mbt/topcat/tc3.gif"
        jnlpUrl = "http://www.star.bris.ac.uk/~mbt/topcat/topcat-full.jnlp";
        capabilities = new SampCapability[]{
            SampCapability.LOAD_VO_TABLE,
            /* SampCapability.POINT_COORDINATES, */
            /* SampCapability.HIGHLIGHT_ROW, */
            SampCapability.LOAD_FITS_TABLE, /* SampCapability.SELECT_LIST */};
        _mTypeSet.addAll(Arrays.asList(capabilities));
        client = new ClientStub(md, jnlpUrl, capabilities);
        client.addObserver(new StubMonitor());
        clients.add(client);
        _stubsMap.put(name, client);

        _familyLists.put(ClientStubFamily.GENERAL, clients);
        _clients.addAll(clients);

        _logger.info("configuration: " + _familyLists);
        _logger.info("clients:       " + _clients);
    }

    public HashSet<SampCapability> getMTypeSet() {
        return _mTypeSet;
    }

    public ClientStub getClientStub(String name) {
        return _stubsMap.get(name);
    }

    public HashMap<String, ClientStub> getStubsMap() {
        return _stubsMap;
    }

    /**
     * Return the client stubs per client family
     * @param family client family
     * @return client stubs
     */
    public List<ClientStub> getClientList(final ClientStubFamily family) {
        return _familyLists.get(family);
    }

    /**
     * Return all client stubs
     * @return client stubs
     */
    public List<ClientStub> getClients() {
        return _clients;
    }

    /**
     * TODO: move that code into FileUtils
     * Extract the given resource given its file name in the Jar archive at /fr/jmmc/smprun/resource/
     * And save it as one temporary file
     * @param resourceFile resource name to extract
     * @return file URL
     * @throws IllegalStateException if the given resource does not exist
     */
    private static String extractResource(final String resourceFile) throws IllegalStateException {

        // use the class loader resource resolver
        final URL url = FileUtils.getResource("fr/jmmc/smprun/resource/" + resourceFile);

        final File tmpFile = FileUtils.getTempFile(resourceFile);

        try {
            FileUtils.saveStream(url.openStream(), tmpFile);

            return tmpFile.toURI().toString();

        } catch (IOException ioe) {
            throw new IllegalStateException("unable to save file: " + tmpFile + " for url: " + url, ioe);
        }
    }
}
