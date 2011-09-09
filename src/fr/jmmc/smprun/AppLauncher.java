/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.gui.SwingSettings;
import java.util.logging.Logger;

/**
 * AppLauncher main class.
 * 
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
        //DockWindow window = new DockWindow();
        //App.setFrame(window);

        // @TODO : Handle JMMC app mimetypes to open our apps !!!
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
