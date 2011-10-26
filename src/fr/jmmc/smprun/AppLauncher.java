/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.gui.SwingSettings;
import fr.jmmc.jmcs.gui.SwingUtils;
import fr.jmmc.smprun.stub.ClientStub;
import java.util.logging.Level;
import org.ivoa.util.runner.LocalLauncher;

/**
 * AppLauncher main class.
 * 
 * @author Sylvain LAFRASSE
 */
public class AppLauncher extends App {

    /** Class logger */
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AppLauncher.class.getName());

    /**
     * Launch the AppLauncher application.
     *
     * Create all objects needed by AppLauncher and plug event responding
     * loop (Listener/Listenable, Observer/Observable) in.
     *
     * @param args command-line options.
     */
    public AppLauncher(final String[] args) {
        super(args);
    }

    /**
     * Initialize application objects
     * @param args ignored arguments
     *
     * @throws RuntimeException if the AppLauncher initialisation failed
     */
    @Override
    protected void init(final String[] args) {

        // Initialize job runner:
        LocalLauncher.startUp();

        // Initialize first the Client descriptions:
        HubPopulator.getInstance();

        // Using invokeAndWait to be in sync with this thread :
        // note: invokeAndWaitEDT throws an IllegalStateException if any exception occurs
        SwingUtils.invokeAndWaitEDT(new Runnable() {

            /**
             * Initializes the swing components with their actions in EDT
             */
            @Override
            public void run() {
                App.setFrame(new DockWindow());

                // @TODO : Handle JMMC app mimetypes to open our apps !!!
            }
        });
    }

    /**
     * Execute application body = make the application frame visible
     */
    @Override
    protected void execute() {
        SwingUtils.invokeLaterEDT(new Runnable() {

            /**
             * Show the application frame using EDT
             */
            @Override
            public void run() {
                logger.fine("AppLauncher.ready : handler called.");

                getFrame().setVisible(true);
            }
        });
    }

    /**
     * Hook to handle operations before closing application.
     *
     * @return should return true if the application can exit, false otherwise
     * to cancel exit.
     */
    @Override
    protected boolean finish() {

        // TODO: confirm dialog to inform the user that SAMP interoperability can fail 
        // if hub (living inside this JVM) is stopped

        return true;
    }

    /**
     * Hook to handle operations when exiting application.
     * @see App#exit(int)
     */
    @Override
    public void onFinish() {

        // Properly disconnect and dispose SAMP hub:
        for (ClientStub client: HubPopulator.getInstance().getClients()) {
            client.disconnectFromHub();
        }
        
        // Stop job runner:
        LocalLauncher.shutdown();

        super.onFinish();
    }

    /**
     * Main entry point
     *
     * @param args command line arguments (open file ...)
     */
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void main(final String[] args) {

        // init swing application for science
        SwingSettings.setup();

        final long start = System.nanoTime();
        try {
            // Start application with the command line arguments
            new AppLauncher(args);
        } finally {
            final long time = (System.nanoTime() - start);

            if (logger.isLoggable(Level.INFO)) {
                logger.info("startup : duration = " + 1e-6d * time + " ms.");
            }
        }
    }
}
/*___oOo___*/
