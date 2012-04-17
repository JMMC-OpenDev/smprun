/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.smprun.stub.ClientStub;
import org.ivoa.util.runner.LocalLauncher;
import org.ivoa.util.runner.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper on http://code.google.com/p/vo-urp/ task runner.
 * 
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public class JnlpStarter {

    /** Class logger */
    private static final Logger _logger = LoggerFactory.getLogger(JnlpStarter.class.getName());
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
     * @param client given client stub to launch the corresponding Jnlp application
     * @return the job context identifier
     * @throws IllegalStateException if the job can not be submitted to the job queue
     */
    public static Long launch(final ClientStub client) throws IllegalStateException {

        final String jnlpUrl = client.getFinalJnlpUrl();

        _logger.info("launch: {}", jnlpUrl);

        // create the execution context without log file:
        final RootContext jobContext = LocalLauncher.prepareMainJob(APP_NAME, USER_NAME, FileUtils.getTempDirPath(), null);

        // command line: 'javaws -Xnosplash <jnlpUrl>'
        LocalLauncher.prepareChildJob(jobContext, TASK_NAME, new String[]{"javaws", "-verbose", "-Xnosplash", jnlpUrl});

        // puts the job in the job queue :
        // can throw IllegalStateException if job not queued :
        LocalLauncher.startJob(jobContext, client);

        return jobContext.getId();
    }
}
