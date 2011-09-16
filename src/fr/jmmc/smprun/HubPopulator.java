/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.smprun.stub.StubMonitor;
import fr.jmmc.smprun.stub.ClientStub;
import fr.jmmc.jmcs.network.interop.SampCapability;
import java.util.ArrayList;
import java.util.List;
import org.astrogrid.samp.Metadata;

/**
 * Instanciate all known stubs.
 * @author Sylvain LAFRASSE
 */
public class HubPopulator {

    private List<ClientStub> clients = new ArrayList<ClientStub>();

    /**
     * Constructor
     */
    public HubPopulator() {
        Metadata md;
        SampCapability[] capabilities;
        String jnlpUrl;
        ClientStub client;

        // @TODO : Grab all this from the Web/OV
        
        md = new Metadata();
        md.setName("Aspro2");
        md.setIconUrl("http://www.jmmc.fr/searchcal/images/aspro2-6464.png");
        capabilities = new SampCapability[]{SampCapability.LOAD_VO_TABLE};
        jnlpUrl = "http://apps.jmmc.fr/~swmgr/Aspro2/Aspro2.jnlp";
        client = new ClientStub(md, capabilities, jnlpUrl);
        client.addObserver(new StubMonitor());
        clients.add(client);

        md = new Metadata();
        md.setName("SearchCal");
        md.setIconUrl("http://apps.jmmc.fr/~sclws/SearchCal/AppIcon.png");
        capabilities = new SampCapability[]{SampCapability.SEARCHCAL_START_QUERY};
        jnlpUrl = "http://apps.jmmc.fr/~sclws/SearchCal/SearchCal.jnlp";
        client = new ClientStub(md, capabilities, jnlpUrl);
        client.addObserver(new StubMonitor());
        clients.add(client);

        md = new Metadata();
        md.setName("LITpro");
        md.setIconUrl("http://www.jmmc.fr/images/litpro6464ws.jpg");
        capabilities = new SampCapability[]{SampCapability.LITPRO_START_SETTING};
        jnlpUrl = "http://jmmc.fr/~swmgr/LITpro/LITpro.jnlp";
        client = new ClientStub(md, capabilities, jnlpUrl);
        client.addObserver(new StubMonitor());
        clients.add(client);

        md = new Metadata();
        md.setName("Aladin");
        md.setIconUrl("http://aladin.u-strasbg.fr/aladin_large.gif");
        capabilities = new SampCapability[]{SampCapability.LOAD_VO_TABLE};
        jnlpUrl = "http://aladin.u-strasbg.fr/java/nph-aladin.pl?frame=get&id=aladin.jnlp";
        client = new ClientStub(md, capabilities, jnlpUrl);
        client.addObserver(new StubMonitor());
        clients.add(client);

        md = new Metadata();
        md.setName("topcat");
        md.setIconUrl("http://www.star.bris.ac.uk/~mbt/topcat/tc3.gif");
        capabilities = new SampCapability[]{SampCapability.LOAD_VO_TABLE};
        jnlpUrl = "http://www.star.bris.ac.uk/~mbt/topcat/topcat-full.jnlp";
        client = new ClientStub(md, capabilities, jnlpUrl);
        client.addObserver(new StubMonitor());
        clients.add(client);
    }

    public List<ClientStub> getClientList() {
        return clients;
    }
}
