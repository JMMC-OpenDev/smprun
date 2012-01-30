/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.smprun.stub.ClientStub;
import fr.jmmc.smprun.stub.ClientStubFamily;
import fr.jmmc.smprun.stub.StubMonitor;
import java.util.*;
import org.astrogrid.samp.Metadata;
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
    /** no sleeping delay before sending the samp message */
    public final static long WAIT_NO = -1L;
    /** 3 second sleeping delay before sending the samp message */
    public final static long WAIT_BEFORE_SEND = 3000L;
    /* members */
    /** all client stubs */
    private final List<ClientStub> _clients = new ArrayList<ClientStub>();
    /** Client family  / client stub mapping */
    private EnumMap<ClientStubFamily, List<ClientStub>> _familyLists = new EnumMap<ClientStubFamily, List<ClientStub>>(ClientStubFamily.class);
    /** Client stub map keyed by application name */
    private HashMap<String, ClientStub> _clientStubMap = new HashMap<String, ClientStub>();
    /** SampCapability set */
    private Set<SampCapability> _sampCapabilitySet = new HashSet<SampCapability>();

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

        // @TODO : Grab all this from the Web/OV

        // Note: Use Icon URL pointing to files extracted from Jar file (see resource package)

        Metadata md;

        // JMMC client list:
        final List<ClientStub> jmmcClients = new ArrayList<ClientStub>(3);

        // --- AppLauncherTester ---
        md = new Metadata();
        md.setName("AppLauncherTester");
        // Sets no icon to keep AppLauncherTester invisible
        jmmcClients.add(createClientStub(md,
                "http://apps.jmmc.fr/~smprun/AppLauncherTester/AppLauncherTester.jnlp",
                new SampCapability[]{SampCapability.APPLAUNCHERTESTER_TRY_LAUNCH},
                WAIT_NO));

        // --- ASPRO2 ---
        md = new Metadata();
        md.setName("Aspro2");
        md.setIconUrl(FileUtils.extractResource(RESOURCE_PATH_PREFIX + "aspro2-6464.png")); // http://www.jmmc.fr/searchcal/images/aspro2-6464.png
        jmmcClients.add(createClientStub(md,
                "http://apps.jmmc.fr/~swmgr/Aspro2/Aspro2.jnlp",
                new SampCapability[]{SampCapability.LOAD_VO_TABLE},
                WAIT_NO));

        // --- SEARCHCAL ---
        md = new Metadata();
        md.setName("SearchCal");
        md.setIconUrl(FileUtils.extractResource(RESOURCE_PATH_PREFIX + "searchcal-6464.png")); // http://apps.jmmc.fr/~sclws/SearchCal/AppIcon.png
        jmmcClients.add(createClientStub(md,
                "http://apps.jmmc.fr/~sclws/SearchCal/SearchCal.jnlp",
                new SampCapability[]{SampCapability.SEARCHCAL_START_QUERY},
                WAIT_NO));

        // --- LITPRO ---
        md = new Metadata();
        md.setName("LITpro");
        md.setIconUrl(FileUtils.extractResource(RESOURCE_PATH_PREFIX + "litpro-6464.png")); // http://www.jmmc.fr/images/litpro6464ws.jpg
        jmmcClients.add(createClientStub(md,
                "http://jmmc.fr/~swmgr/LITpro/LITpro.jnlp",
                new SampCapability[]{SampCapability.LITPRO_START_SETTING},
                WAIT_NO));

        // Update JMMC ClientStubFamily:
        _familyLists.put(ClientStubFamily.JMMC, jmmcClients);

        // Generic client list:
        final List<ClientStub> generalClients = new ArrayList<ClientStub>(3);

        /*
         * Note: Following SAMP messages must not trigger application startup:
         * - SampCapability.POINT_COORDINATES
         * - SampCapability.HIGHLIGHT_ROW
         * - SampCapability.SELECT_LIST
         */

        // --- ALADIN ---
        md = new Metadata();
        md.setName("Aladin");
        md.setIconUrl(FileUtils.extractResource(RESOURCE_PATH_PREFIX + "aladin-6464.png")); // http://aladin.u-strasbg.fr/aladin_large.gif
        // Use redirected url instead of the one copied from the web pages (/java/nph-aladin.pl?frame=get&id=aladin.jnlp)
        generalClients.add(createClientStub(md,
                "http://aladin.u-strasbg.fr/java/aladin.jnlp",
                new SampCapability[]{SampCapability.LOAD_VO_TABLE,
                    /* SampCapability.POINT_COORDINATES, */
                    SampCapability.LOAD_FITS_IMAGE,
                    /* SampCapability.HIGHLIGHT_ROW, */
                    SampCapability.LOAD_FITS_TABLE
                /* SampCapability.SELECT_LIST */
                },
                WAIT_BEFORE_SEND));

        // --- TOPCAT ---
        md = new Metadata();
        md.setName("topcat");
        md.setIconUrl(FileUtils.extractResource(RESOURCE_PATH_PREFIX + "topcat-6464.png")); // "http://www.star.bris.ac.uk/~mbt/topcat/tc3.gif"
        // TODO: Provide our own topcat JNLP (http://jmmc.fr/~bourgesl/topcat/topcat-full-appLauncher.jnlp) or convince Mark to change its JNLP to include JVM settings (memory or cmd ling args)
        generalClients.add(createClientStub(md,
                "http://www.star.bris.ac.uk/~mbt/topcat/topcat-full.jnlp",
                new SampCapability[]{SampCapability.LOAD_VO_TABLE,
                    /* SampCapability.POINT_COORDINATES, */
                    /* SampCapability.HIGHLIGHT_ROW, */
                    SampCapability.LOAD_FITS_TABLE
                /* SampCapability.SELECT_LIST */
                },
                WAIT_NO));

        // Update GENERAL ClientStubFamily:
        _familyLists.put(ClientStubFamily.GENERAL, generalClients);

        _logger.info("configuration: " + _familyLists);
        _logger.info("clients:       " + _clients);
    }

    /**
     * Create a new Client Stub using given arguments and store it in collections
     * 
     * @param md SAMP meta data
     * @param jnlpUrl JNLP URL
     * @param capabilities samp capabilities
     * @param sleepDelayBeforeNotify sleep delay in milliseconds before sending the samp message
     * @return client stub 
     */
    private ClientStub createClientStub(final Metadata md,
            final String jnlpUrl,
            final SampCapability[] capabilities,
            final long sleepDelayBeforeNotify) {

        final ClientStub client = new ClientStub(md, jnlpUrl, capabilities, sleepDelayBeforeNotify);
        client.addObserver(new StubMonitor());

        _clients.add(client);
        _clientStubMap.put(client.getApplicationName(), client);
        _sampCapabilitySet.addAll(Arrays.asList(capabilities));

        return client;
    }

    /**
     * Return the SampCapability set managed by client stubs
     * @return SampCapability set
     */
    public Set<SampCapability> getSampCapabilitySet() {
        return _sampCapabilitySet;
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
}
