/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import com.jidesoft.plaf.LookAndFeelFactory;
import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.data.preference.PreferencesException;
import fr.jmmc.jmcs.gui.FeedbackReport;
import fr.jmmc.jmcs.gui.PreferencesView;
import fr.jmmc.jmcs.gui.util.SwingSettings;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.gui.util.WindowUtils;
import fr.jmmc.jmcs.gui.action.RegisteredAction;
import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.network.interop.SampManager;
import fr.jmmc.smprun.preference.ApplicationListSelectionView;
import fr.jmmc.jmcs.gui.component.BooleanPreferencesView;
import fr.jmmc.jmcs.gui.component.ResizableTextViewFactory;
import fr.jmmc.smprun.preference.PreferenceKey;
import fr.jmmc.smprun.preference.Preferences;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.astrogrid.samp.client.SampException;
import org.ivoa.util.runner.LocalLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AppLauncher main class.
 * 
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public class AppLauncher extends App {

    private final static String WELCOME_MESSAGE = "<HTML><HEAD></HEAD><BODY>"
            + "<CENTER><H2>Welcome to AppLauncher !!!</H2></CENTER>"
            + "<BR/>"
            + "And thank you for your confidence in the JMMC automatic SAMP application launcher.<BR/>"
            + "<BR/>"
            + "- First, an auto-test procedure will now proceed to confirm everything is fine for AppLauncher to work well;<BR/>"
            + "- You can customize (among other things) which applications are shown in the Dock using the preferences window;<BR/>"
            + "- Further documentation is available directly from the Help menu, so don't hesitate to have a look;<BR/>"
            + "- You can easily provide (greatly appreciated) feedback and bug reports to us from the dedicated entry in the Help menu.<BR/>"
            + "<BR/>"
            + "<B>We hope you will appreciate using AppLauncher as much as we had fun making it !</B>";
    /** Logger */
    protected static final Logger _logger = LoggerFactory.getLogger(AppLauncher.class.getName());
    /** Export to SAMP action */
    public LaunchJnlpSampAutoTestAction _launchJnlpSampAutoTestAction;
    private static DockWindow _dockWindow = null;
    private Preferences _preferences;

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
        // For debugging purpose only, to dismiss splashscreen
        //super(args, false, true, false);
    }

    /**
     * Initialize application objects
     * @param args ignored arguments
     *
     * @throws RuntimeException if the AppLauncher initialization failed
     */
    @Override
    protected void init(final String[] args) {

        _preferences = Preferences.getInstance();

        _launchJnlpSampAutoTestAction = new LaunchJnlpSampAutoTestAction(getClass().getName(), "_launchJnlpSampAutoTestAction");

        // Start first the SampManager (connect to an existing hub or start a new one)
        // and check if it is connected to one Hub:
        if (!SampManager.isConnected()) {
            throw new IllegalStateException("Unable to connect to an existing hub or start an internal SAMP hub !");
        }

        // Initialize job runner:
        LocalLauncher.startUp();

        // First initialize the Client descriptions:
        HubPopulator.start();

        // Using invokeAndWait to be in sync with this thread :
        // note: invokeAndWaitEDT throws an IllegalStateException if any exception occurs
        SwingUtils.invokeAndWaitEDT(new Runnable() {

            /**
             * Initializes the swing components with their actions in EDT
             */
            @Override
            public void run() {

                // Prepare dock window
                _dockWindow = DockWindow.getInstance();
                App.setFrame(_dockWindow);
                WindowUtils.centerOnMainScreen(_dockWindow);

                preparePreferencesWindow();

                // @TODO : Handle JMMC app mimetypes to open our apps !!!
            }

            private void preparePreferencesWindow() {

                // Retrieve application preference panes and attach them to their view
                LinkedHashMap<String, JPanel> panels = new LinkedHashMap<String, JPanel>();

                // Create application selection pane
                ApplicationListSelectionView applicationListSelectionView = new ApplicationListSelectionView();
                applicationListSelectionView.init();
                panels.put("Application Selection", applicationListSelectionView);

                // Create general settings pane
                LinkedHashMap<Object, String> generalSettingsMap = new LinkedHashMap<Object, String>();
                generalSettingsMap.put(PreferenceKey.SHOW_DOCK_WINDOW, "Show Dock window on startup");
                generalSettingsMap.put(PreferenceKey.START_SELECTED_STUBS, "Restrict SAMP support to your selected applications on startup");
                generalSettingsMap.put(PreferenceKey.SHOW_EXIT_WARNING, "Show warning before shuting down SAMP hub while quitting");
                BooleanPreferencesView generalSettingsView = new BooleanPreferencesView(_preferences, generalSettingsMap, BooleanPreferencesView.SAVE_AND_RESTART_MESSAGE);
                generalSettingsView.init();
                panels.put("General Settings", generalSettingsView);

                // Finalize prefence window
                PreferencesView preferencesView = new PreferencesView(_preferences, panels);
                preferencesView.init();
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

        // Show Welcome pane and perform JNLP/SAMP auto-test on first AppLauncher start
        performFirstRunTasks();

        // If JNLP/SAMP startup test went fine
        SwingUtils.invokeLaterEDT(new Runnable() {

            /**
             * Show the application frame using EDT
             */
            @Override
            public void run() {
                _logger.debug("Setting AppLauncher GUI up.");

                // Show Dock window if not hidden
                if (_dockWindow != null) {
                    final JFrame frame = getFrame();
                    frame.setVisible(true);
                }
            }
        });
    }

    @Override
    public boolean shouldSilentlyKillSampHubOnQuit() {

        final boolean shouldSilentlyKillSampHubOnQuit = !_preferences.getPreferenceAsBoolean(PreferenceKey.SHOW_EXIT_WARNING);
        return shouldSilentlyKillSampHubOnQuit;
    }

    /**
     * Hook to handle operations when exiting application.
     * @see App#exit(int)
     */
    @Override
    public void cleanup() {
        // Properly disconnect connected clients:
        HubPopulator.disconnectAllStubs();

        // Stop job runner:
        LocalLauncher.shutdown();

        super.cleanup();
    }

    /**
     * Show the application frame and bring it to front only if the DockWindow is visible
     */
    public static void showFrameToFront() {
        if (_dockWindow != null) {
            App.showFrameToFront();
        }
    }

    /**
     * @return true if the test went fine, false otherwise
     */
    private void performFirstRunTasks() {

        // If it is the first time ever AppLauncher is started
        final boolean isFirstRun = _preferences.getPreferenceAsBoolean(PreferenceKey.FIRST_START_FLAG);
        if (!isFirstRun) {
            return; // Skip first run tasks
        }

        _logger.info("First time AppLauncher is starting (no preference file found).");

        // Show a Welcome pane
        ResizableTextViewFactory.createHtmlWindow(WELCOME_MESSAGE, "Welcome to AppLauncher !!!");

        // Run JNLP/SAMP abailities test
        // TODO : Do not work anymore ?!?
        if (!checkJnlpSampAbilities()) {
            _logger.error("Could not succesfully perform JNLP/SAMP auto-test, aborting.");
            return;
        }

        _logger.info("Succesfully performed first run tasks.");

        // Create preference file to skip this test for future starts
        try {
            _preferences.setPreference(PreferenceKey.FIRST_START_FLAG, false);
            _preferences.saveToFile();
        } catch (PreferencesException ex) {
            _logger.warn("Could not write to preference file :", ex);
        }
    }

    /**
     * @return true if the test went fine, false otherwise
     */
    private boolean checkJnlpSampAbilities() {

        // Try to send a SampCapability.APPLAUNCHERTESTER_TRY_LAUNCH to AppLauncherTester stub to test our whole machinery
        List<String> clientIds = SampManager.getClientIdsForName("AppLauncherTester");
        if (!clientIds.isEmpty()) {

            // TODO : Should only send this message to our own stub
            String appLauncherTesterClientId = clientIds.get(0);

            // try to send the dedicated test message to our stub
            try {
                final String appLauncherTesterMType = SampCapability.APPLAUNCHERTESTER_TRY_LAUNCH.mType();
                SampManager.sendMessageTo(appLauncherTesterMType, appLauncherTesterClientId, null);
            } catch (SampException ex) {
                FeedbackReport.openDialog(ex);
                return false;
            }
        }

        return true;
    }

    protected class LaunchJnlpSampAutoTestAction extends RegisteredAction {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        public LaunchJnlpSampAutoTestAction(String classPath, String fieldName) {
            super(classPath, fieldName);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            checkJnlpSampAbilities();
        }
    }

    /**
     * Main entry point
     *
     * @param args command line arguments (open file ...)
     */
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void main(final String[] args) {

        // To ensure the use of TriStateCheckBoxes in the Jide CheckBoxTree
        LookAndFeelFactory.installDefaultLookAndFeelAndExtension();

        // init swing application for science
        SwingSettings.setup();

        final long start = System.nanoTime();
        try {
            // Start application with the command line arguments
            new AppLauncher(args);
        } finally {
            final long time = (System.nanoTime() - start);
            _logger.debug("Startup duration = {} ms.", 1e-6d * time);
        }
    }
}
/*___oOo___*/
