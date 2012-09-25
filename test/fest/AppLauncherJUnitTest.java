/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fest;

import fest.common.JmcsApplicationSetup;
import fest.common.JmcsFestSwingJUnitTestCase;
import fr.jmmc.smprun.HubPopulator;
import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
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

        // waits for initialization to finish :
        pause(new Condition("TaskRunning") {
            /**
             * Checks if the condition has been satisfied.
             * @return <code>true</code> if the condition has been satisfied, otherwise <code>false</code>.
             */
            @Override
            public boolean test() {

                return GuiActionRunner.execute(new GuiQuery<Boolean>() {
                    @Override
                    protected Boolean executeInEDT() {
                        final boolean done = HubPopulator.isInitialized();
                        _logger.debug("checkRunningTasks : test = {}", done);
                        return done;
                    }
                });

            }
        }, LONG_TIMEOUT);

        window.button("Aspro2").click();

        _logger.debug("checkRunningTasks : exit");
    }

    /**
     * Test the application exit sequence : ALWAYS THE LAST TEST
     */
    @Test
    @GUITest
    public void shouldExit() {
        logger.severe("shouldExit test");
        
        // TODO : wait for test to finish !

        window.close();

        // TODO : confirmSampMessage
        //confirmDialogDontSave();
    }
}
