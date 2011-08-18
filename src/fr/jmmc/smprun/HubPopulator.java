/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import com.sun.jdi.connect.spi.TransportService.Capabilities;
import fr.jmmc.mcs.interop.SampCapability;
import org.astrogrid.samp.Metadata;

/**
 *
 * @author lafrasse
 */
class HubPopulator {

    public HubPopulator() {
        Metadata md;
        SampCapability[] capabilities;
        String jnlpUrl;

        md = new Metadata();
        md.setName("toto");
        capabilities = new SampCapability[] {SampCapability.LOAD_VO_TABLE};
        jnlpUrl = "http://jmmc.fr/~swmgr/LITpro/LITpro.jnlp";
        new ClientStub(md, capabilities, jnlpUrl);

        md = new Metadata();
        md.setName("titi");
        capabilities = new SampCapability[] {SampCapability.LOAD_VO_TABLE};
        ClientStub clientStub = new ClientStub(md, capabilities, "toto");
    }
}
