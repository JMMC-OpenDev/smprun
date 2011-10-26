/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub;

import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.gui.SwingUtils;
import fr.jmmc.jmcs.gui.WindowCenterer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import javax.swing.JProgressBar;
import javax.swing.Timer;

/**
 * Monitor Window controller.
 * @author Sylvain LAFRASSE
 */
public class StubMonitor implements Observer {

    /** Class logger */
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(StubMonitor.class.getName());

    /** Monitor GUI */
    private MonitorWindow _window;

    /**
     * Set up the GUI
     */
    public StubMonitor() {
        super();

        SwingUtils.invokeEDT(new Runnable() {

            /**
             * Synchronized by EDT
             */
            @Override
            public void run() {
                _window = new MonitorWindow();
                _window.setTitle("JMMC AppLauncher");
                _window.setVisible(false);
                _window.pack();
                WindowCenterer.centerOnMainScreen(_window);
            }
        });
    }

    /**
     * Handle the observable event
     * @see java.util.Observer
     * @param obj ClientStub instance
     * @param arg ClientStubState instance
     */
    @Override
    public void update(final Observable obj, final Object arg) {

        final String applicationName = ((ClientStub) obj).toString();

        final ClientStubState state = (ClientStubState) arg;
        final String message = state.message();
        final int step = state.step();
        final int maxStep = ClientStubState.DIYING.step();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("StubMonitor['" + applicationName + "'] : '" + state.message() + "' (" + state.step() + "/" + maxStep + ").");
        }

        SwingUtils.invokeEDT(new Runnable() {

            /**
             * Synchronized by EDT
             */
            @Override
            public void run() {
                
                // bring this application to front :
                App.showFrameToFront();

                if (step > 0) {

                    _window.getLabel().setText("Redirecting to " + applicationName + ":");

                    final JProgressBar bar = _window.getProgressBar();

                    bar.setMinimum(0);
                    bar.setMaximum(maxStep);
                    bar.setValue(state.step());

                    if (message.isEmpty()) {
                        bar.setStringPainted(false);
                        bar.setString(null);
                    } else {
                        bar.setStringPainted(true);
                        bar.setString(message + "...");
                    }

                    if (!_window.isVisible() && step < ClientStubState.DISCONNECTING.step()) {
                        _window.setVisible(true);
                    }
                }
            }
        });

        // Should the window be hidden ?
        if (step >= maxStep) {

            // Postpone hiding to let the user see the last message
            final ActionListener hideTask = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (_window.isVisible()) {
                        _window.setVisible(false);
                    }
                }
            };
            
            // Fire after 1.5 second
            final Timer hideTaskTimer = new Timer(1500, hideTask); 
            hideTaskTimer.setRepeats(false);
            hideTaskTimer.start();
        }
    }
}
