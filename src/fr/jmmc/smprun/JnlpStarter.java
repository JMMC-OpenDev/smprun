/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.smprun.stub.ClientStub;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import org.ivoa.util.runner.LocalLauncher;
import org.ivoa.util.runner.RootContext;
import org.ivoa.util.runner.process.ProcessContext;

/**
 * Wrapper on http://code.google.com/p/vo-urp/ task runner
 * @author Sylvain LAFRASSE
 */
public class JnlpStarter {

    /** Class logger */
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(JnlpStarter.class.getName());
    /** application identifier for LocalLauncher */
    public final static String APP_NAME = "JnlpStarter";
    /** user for LocalLauncher */
    public final static String USER_NAME = "AppLauncher";
    /** task identifier for LocalLauncher */
    public final static String TASK_NAME = "JavaWebStart";

    /** Forbidden constructor */
    private JnlpStarter() {
    }

    /**
     * Launch the Java WebStart application associated to the given client stub in another process.
     * 
     * @param client given client strub to launch the corresponding Jnlp application
     * @return the job context
     * @throws IllegalStateException if the job can not be submitted to the job queue
     */
    public static RootContext launch(final ClientStub client) throws IllegalStateException {

        final String jnlpUrl = client.getJnlpUrl();
        
        if (logger.isLoggable(Level.INFO)) {
            logger.info("launch: " + jnlpUrl);
        }

        // create the execution context without log file:
        final RootContext jobContext = LocalLauncher.prepareMainJob(APP_NAME, USER_NAME, FileUtils.getTempDir(), null);

        // command line: 'javaws -verbose <jnlpUrl>'
        LocalLauncher.prepareChildJob(jobContext, TASK_NAME, new String[]{"javaws", "-verbose", jnlpUrl});

        // puts the job in the job queue :
        // can throw IllegalStateException if job not queued :
        LocalLauncher.startJob(jobContext, client);
        
        return jobContext;
    }
}
