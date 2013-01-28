/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fest;

import fest.common.JmcsApplicationSetup;
import fest.common.JmcsFestSwingJUnitTestCase;
import fr.jmmc.jmcs.Bootstrapper;
import fr.jmmc.smprun.HubMonitor;
import fr.jmmc.smprun.HubPopulator;
import fr.jmmc.smprun.preference.Preferences;
import org.fest.swing.annotation.GUITest;
import org.fest.swing.core.matcher.JButtonMatcher;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.DialogFixture;
import org.fest.swing.timing.Condition;
import static org.fest.swing.timing.Pause.*;
import org.fest.swing.timing.Timeout;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AppLauncher FEST test
 * @author lafrasse
 */
public class AppLauncherJUnitTest extends JmcsFestSwingJUnitTestCase {

    /** Class logger */
    private static final Logger _logger = LoggerFactory.getLogger(JmcsFestSwingJUnitTestCase.class.getName());
    /** 5s timeout */
    private static final Timeout LONG_TIMEOUT = Timeout.timeout(10000l);

    /**
     * Define the application
     */
    static {
        // disable dev LAF menu :
        System.setProperty("jmcs.laf.menu", "false");

        // reset Preferences:
        Preferences.getInstance().resetToDefaultPreferences();

        JmcsApplicationSetup.define(fr.jmmc.smprun.AppLauncher.class);

        // define robot delays :
        defineRobotDelayBetweenEvents(SHORT_DELAY);

        // define delay before taking screenshot :
        defineScreenshotDelay(SHORT_DELAY);

        // disable tooltips :
        enableTooltips(false);
    }

    /**
     * Test if the application started correctly
     */
    @Test
    @GUITest
    public void shouldStart() {

        // Grab Welcome message
        final DialogFixture welcome = window.dialog();//"\"Welcome to AppLauncher !!!");
        saveScreenshot(welcome, "al_ihm_welcome.png");
        welcome.button(JButtonMatcher.withText("OK")).click();

        // Waits for apploication initialization to finish
        pause(new Condition("AppInitializing") {
            /**
             * Checks if the condition has been satisfied.
             * @return <code>true</code> if the condition has been satisfied, otherwise <code>false</code>.
             */
            @Override
            public boolean test() {
                final boolean done = HubPopulator.isInitialized();
                _logger.info("AppInitializing : test = {}", done);
                return done;
            }
        }, LONG_TIMEOUT);

        // Grab Dock window
        saveScreenshot(window, "al_ihm.png");

        _logger.info("looking for JMMC AppLauncher ...");

        // Grab Monitor Window
        saveScreenshot(getFrame("JMMC AppLauncher"), "al_ihm_popup.png");

        waitHubTasks();

        _logger.info("Wait for checkJnlpSampAbilities done ...");

        // wait until checkJnlpSampAbilities done
        pause(30000l);
    }

    /**
     * Test the application exit sequence : ALWAYS THE LAST TEST
     */
    @Test
    @GUITest
    public void shouldExit() {
        logger.severe("shouldExit test");

        // TODO : handle this !
        //window.close();

        // TODO : confirmSampMessage
        //confirmDialogDontSave();

        Bootstrapper.stopApp(1);
    }

    private void waitHubTasks() {
        // Waits for hub tasks to finish
        pause(new Condition("HubTaskRunning") {
            /**
             * Checks if the condition has been satisfied.
             * @return <code>true</code> if the condition has been satisfied, otherwise <code>false</code>.
             */
            @Override
            public boolean test() {

                return GuiActionRunner.execute(new GuiQuery<Boolean>() {
                    @Override
                    protected Boolean executeInEDT() {
                        final boolean idle = HubMonitor.getInstance().isIdle();
                        _logger.debug("HubTaskRunning : test = {}", !idle);
                        return idle;
                    }
                });

            }
        }, LONG_TIMEOUT);
    }
}
