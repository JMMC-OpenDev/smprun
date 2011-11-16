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
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.Timer;

/**
 * Monitor Window controller.
 * 
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public class StubMonitor implements Observer {

    /** Class logger */
    private static final java.util.logging.Logger _logger = java.util.logging.Logger.getLogger(StubMonitor.class.getName());
    /** auto hide delay in milliseconds */
    public final static int AUTO_HIDE_DELAY = 3000;
    /* members */
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
        final ClientStub client = (ClientStub) obj;
        final String applicationName = client.getApplicationName();

        final ClientStubState state = (ClientStubState) arg;
        final String message = state.message();
        final int step = state.step();

        final int minStep = ClientStubState.LISTENING.step();
        final int maxStep = ClientStubState.DIYING.step();

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("StubMonitor['" + applicationName + "'] : '" + state.message() + "' (" + step + " / " + maxStep + ").");
        }

        // Do not display initialization statuses:
        if (step > minStep) {

            SwingUtils.invokeEDT(new Runnable() {

                /**
                 * Synchronized by EDT
                 */
                @Override
                public void run() {

                    // Add cancel button action:
                    final JButton cancelButton = _window.getButtonCancel();

                    final boolean isLaunching = (step == ClientStubState.LAUNCHING.step());

                    if (isLaunching && cancelButton.getActionListeners().length == 0) {
                        cancelButton.addActionListener(new ActionListener() {

                            /**
                             * Kill the application if the button is clicked
                             */
                            @Override
                            public void actionPerformed(final ActionEvent e) {
                                client.killRealApplication();

                                // disable cancel button:
                                cancelButton.setEnabled(false);
                            }
                        });
                    }

                    cancelButton.setEnabled(isLaunching);

                    // bring this application to front :
                    App.showFrameToFront();

                    _window.getLabelMessage().setText("Redirecting to " + applicationName + ":");

                    final JProgressBar bar = _window.getProgressBar();

                    bar.setMinimum(0);
                    bar.setMaximum(maxStep);
                    bar.setValue(state.step());

                    if (message.length() == 0) {
                        bar.setStringPainted(false);
                        bar.setString(null);
                    } else {
                        bar.setStringPainted(true);
                        bar.setString(message + " ...");
                    }

                    if (!_window.isVisible() && step < ClientStubState.DISCONNECTING.step()) {
                        _window.setVisible(true);
                    }
                }
            });

            // Should the window be hidden (DYING or FAILING states) ?
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
                final Timer hideTaskTimer = new Timer(AUTO_HIDE_DELAY, hideTask);
                hideTaskTimer.setRepeats(false);
                hideTaskTimer.start();
            }
        }
    }
}
