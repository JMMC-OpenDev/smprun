/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import org.ivoa.util.runner.RootContext;
import org.ivoa.util.runner.process.ProcessContext;
import org.ivoa.util.runner.process.ProcessRunner;
import org.ivoa.util.runner.process.RingBuffer;

/**
 * Wrapper on http://code.google.com/p/vo-urp/ task runner
 * @author Sylvain LAFRASSE
 */
public class JnlpStarter {
    
    
    public static RingBuffer ringBuf = new RingBuffer(1000, null);

    /** Forbidden constructor */
    private JnlpStarter() {
    }

    /**
     * Launch a given Java WebStart application in another process.
     * 
     * @param jnlpUrl the URL of the Java WebStart application to launch.
     * @return the execution status code.
     */
    public static int launch(String jnlpUrl) {

        String tmpDir = System.getProperty("java.io.tmpdir");

        RootContext rCtx = new RootContext("AppLauncher", new Long(0), tmpDir);

        String cmd[] = {"javaws", "-verbose", jnlpUrl};
        ProcessContext pCtx = new ProcessContext(rCtx, "javaws", new Long(1), cmd);

        pCtx.setRing(ringBuf);
        
        // #TODO : catch ERROR in RingBuffer to detect crash at launch !!

        return ProcessRunner.execute(pCtx);
    }
}
