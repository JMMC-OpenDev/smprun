/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.mcs.gui.App;
import fr.jmmc.mcs.gui.SwingSettings;
import fr.jmmc.mcs.interop.SampCapability;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

/**
 * AppLauncher main
 * @author Sylvain LAFRASSE
 */
public class AppLauncher extends App {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(
            "fr.jmmc.smprun.AppLauncher");

    /**
     * Launch the AppLauncher application.
     *
     * Create all objects needed by AppLauncher and plug event responding
     * loop (Listener/Listenable, Observer/Observable) in.
     *
     * @param args command-line options.
     */
    public AppLauncher(String[] args) {
        super(args);
    }

    /** Initialize application objects */
    @Override
    protected void init(String[] args) {

        try {
            // Using invokeAndWait to be in sync with the main thread :
            SwingUtilities.invokeAndWait(new Runnable() {

                /**
                 * Initializes the swing components with their actions in EDT
                 */
                @Override
                public void run() {

                    // Insert your code here...

                    // Build the main window
                    MainWindow window = new MainWindow();
                    window.pack();
                    window.setVisible(true);

                    App.setFrame(window);
                }
            });
        } catch (InterruptedException ie) {
            // Propagate the exception :
            throw new IllegalStateException("AppLauncher.init : interrupted", ie);
        } catch (InvocationTargetException ite) {
            // Propagate the internal exception :
            throw new IllegalStateException("AppLauncher.init : exception", ite.getCause());
        }
    }

    /** Execute application body */
    @Override
    protected void execute() {
        new HubPopulator();
    }

    /** Handle operations before closing application */
    @Override
    protected boolean finish() {
        // @TODO : Properly disconnect and dispose SAMP hub ?
        return true;
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

        new AppLauncher(args);
    }
}
/*___oOo___*/
