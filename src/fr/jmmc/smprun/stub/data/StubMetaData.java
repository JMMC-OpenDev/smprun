/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub.data;

import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.smprun.stub.data.model.SampStub;
import org.astrogrid.samp.Metadata;

/**
 * Stub XML data model (un)marshaller.
 * @author lafrasse
 */
public class StubMetaData {

    public void StubMetaData(String name, Metadata metadata, SampCapability[] mTypes) {

        SampStub data = new SampStub();

        data.setName(name);

        for (SampCapability sampCapability : mTypes) {
            data.getSubscriptions().add(sampCapability.mType());
        }

        for (Object key : metadata.keySet()) {
            fr.jmmc.smprun.stub.data.model.Metadata tmp = new fr.jmmc.smprun.stub.data.model.Metadata();
            tmp.setKey(key.toString());
            tmp.setValue(metadata.get(key).toString());
            data.getMetadatas().add(tmp);
        }
    }
}
