/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import org.ivoa.util.runner.RootContext;
import org.ivoa.util.runner.RunContext;
import org.ivoa.util.runner.process.ProcessContext;
import org.ivoa.util.runner.process.ProcessRunner;
import org.ivoa.util.runner.process.RingBuffer;

/**
 *
 * @author Sylvain LAFRASSE
 */
public class JnlpStarter {

    /** Forbidden constructor */
    private JnlpStarter() {
    }

    public static int exec(String jnlpUrl) {

        String tmpDir = System.getProperty("java.io.tmpdir");

        RootContext rCtx = new RootContext("AppLauncher", new Long(0), tmpDir);

        String cmd[] = {"javaws", jnlpUrl};
        ProcessContext pCtx = new ProcessContext(rCtx, "uname", new Long(1), cmd);
        RingBuffer ringBuf = new RingBuffer(1000, null);
        pCtx.setRing(ringBuf);

        return ProcessRunner.execute(pCtx);
    }
}
