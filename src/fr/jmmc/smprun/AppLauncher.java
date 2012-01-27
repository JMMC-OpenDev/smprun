/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.gui.FeedbackReport;
import fr.jmmc.jmcs.gui.SwingSettings;
import fr.jmmc.jmcs.gui.SwingUtils;
import fr.jmmc.jmcs.gui.WindowCenterer;
import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.network.interop.SampManager;
import fr.jmmc.smprun.stub.ClientStub;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.astrogrid.samp.client.SampException;
import org.ivoa.util.runner.LocalLauncher;

/**
 * AppLauncher main class.
 * 
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public class AppLauncher extends App {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(AppLauncher.class.getName());

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

        // Start first the SampManager (connect to an existing hub or start a new one)
        // and check if it is connected to one Hub:
        if (!SampManager.isConnected()) {
            throw new IllegalStateException("Unable to connect to an existing hub or start an internal SAMP hub !");
        }

        // Initialize job runner:
        LocalLauncher.startUp();

        // First initialize the Client descriptions:
        HubPopulator.getInstance();

        // Using invokeAndWait to be in sync with this thread :
        // note: invokeAndWaitEDT throws an IllegalStateException if any exception occurs
        SwingUtils.invokeAndWaitEDT(new Runnable() {

            /**
             * Initializes the swing components with their actions in EDT
             */
            @Override
            public void run() {
                App.setFrame(DockWindow.getInstance());

                // @TODO : Handle JMMC app mimetypes to open our apps !!!
            }
        });
    }

    /**
     * Create SAMP Message handlers
     */
    @Override
    protected void declareInteroperability() {
        // Initialize the Hub monitor which starts client stubs if necessary
        HubMonitor.getInstance();
    }

    /**
     * Execute application body = make the application frame visible
     */
    @Override
    protected void execute() {

        // Wait 3 seconds in order to fulfill AppLauncherTester stub registration
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(AppLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Try to send a SampCapability.APPLAUNCHERTESTER_TRY_LAUNCH to AppLauncherTester to test the whole machinery
        // TODO : Do this only on first start
        List<String> clientIds = SampManager.getClientIdsForName("AppLauncherTester");
        if (!clientIds.isEmpty()) {

            // TODO : Should only send this message to our own stub
            String appLauncherTesterClientId = clientIds.get(0);

            try {
                SampManager.sendMessageTo(SampCapability.APPLAUNCHERTESTER_TRY_LAUNCH.mType(), appLauncherTesterClientId, null);
            } catch (SampException ex) {
                FeedbackReport.openDialog(ex);
                return;
            }
        }

        // If JNLP startup test went fine
        SwingUtils.invokeLaterEDT(new Runnable() {

            /**
             * Show the application frame using EDT
             */
            @Override
            public void run() {
                _logger.fine("AppLauncher.ready : handler called.");

                final JFrame frame = getFrame();
                WindowCenterer.centerOnMainScreen(frame);
                frame.setVisible(true);
            }
        });
    }

    /**
     * Hook to handle operations when exiting application.
     * @see App#exit(int)
     */
    @Override
    public void onFinish() {

        // Properly disconnect connected clients:
        for (ClientStub client : HubPopulator.getInstance().getClients()) {
            client.disconnect();
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

            if (_logger.isLoggable(Level.INFO)) {
                _logger.info("startup : duration = " + 1e-6d * time + " ms.");
            }
        }
    }
}
/*___oOo___*/
