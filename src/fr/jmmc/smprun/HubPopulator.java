/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.smprun.stub.StubMonitor;
import fr.jmmc.smprun.stub.ClientStub;
import fr.jmmc.jmcs.network.interop.SampCapability;
import org.astrogrid.samp.Metadata;

/**
 * Instanciate all known stubs.
 * @author Sylvain LAFRASSE
 */
public class HubPopulator {

    /**
     * Constructor
     */
    public HubPopulator() {
        Metadata md;
        SampCapability[] capabilities;
        String jnlpUrl;

        // @TODO : Grab all this from the Web/OV

        md = new Metadata();
        md.setName("LITpro");
        capabilities = new SampCapability[]{SampCapability.LITPRO_START_SETTING};
        jnlpUrl = "http://jmmc.fr/~swmgr/LITpro/LITpro.jnlp";
        new ClientStub(md, capabilities, jnlpUrl).addObserver(new StubMonitor());

        md = new Metadata();
        md.setName("Aladin");
        capabilities = new SampCapability[]{SampCapability.LOAD_VO_TABLE};
        jnlpUrl = "http://aladin.u-strasbg.fr/java/nph-aladin.pl?frame=get&id=aladin.jnlp";
        new ClientStub(md, capabilities, jnlpUrl).addObserver(new StubMonitor());

        md = new Metadata();
        md.setName("topcat");
        capabilities = new SampCapability[]{SampCapability.LOAD_VO_TABLE};
        jnlpUrl = "http://www.star.bris.ac.uk/~mbt/topcat/topcat-full.jnlp";
        new ClientStub(md, capabilities, jnlpUrl).addObserver(new StubMonitor());
    }
}
