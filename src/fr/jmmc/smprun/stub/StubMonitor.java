/**
 * *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 *****************************************************************************
 */
package fr.jmmc.smprun.stub;

import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.gui.util.WindowUtils;
import fr.jmmc.smprun.AppLauncher;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitor Window controller.
 *
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public class StubMonitor implements Observer {

    /**
     * Class logger
     */
    private static final Logger _logger = LoggerFactory.getLogger(StubMonitor.class.getName());
    /**
     * auto hide delay in milliseconds
     */
    public final static int AUTO_HIDE_DELAY = 5 * 1000;
    /**
     * cancel launching delay in milliseconds
     */
    public final static int CANCEL_TIMEOUT = 5 * 60 * 1000;
    /* members */
    /**
     * Monitor GUI
     */
    private MonitorWindow _window;
    /**
     * Cancel launching timer (timeout)
     */
    private Timer _cancelTimer = null;

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
                WindowUtils.centerOnMainScreen(_window);
            }
        });
    }

    /**
     * Handle the observable event
     *
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

        if (_logger.isDebugEnabled()) {
            _logger.debug("StubMonitor['{}'] : '{}' ({} / {}).", applicationName, state.message(), step, maxStep);
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

                    if (isLaunching) {
                        // avoid reentrant launching:
                        if (cancelButton.getActionListeners().length == 0) {

                            // Cancel task used by user or after tiemout:
                            final ActionListener cancelTask = new ActionListener() {
                                /**
                                 * Kill (or detach) the javaws process if the button is clicked
                                 */
                                @Override
                                public void actionPerformed(final ActionEvent e) {
                                    client.killRealApplication();

                                    // disable cancel button:
                                    cancelButton.setEnabled(false);
                                }
                            };

                            cancelButton.addActionListener(cancelTask);

                            // add cancel timer:
                            _cancelTimer = new Timer(CANCEL_TIMEOUT, cancelTask);
                            _cancelTimer.setRepeats(false);
                        }
                    } else {
                        for (ActionListener listener : cancelButton.getActionListeners()) {
                            cancelButton.removeActionListener(listener);
                        }
                    }

                    cancelButton.setEnabled(isLaunching);
                    enableCancelTimer(isLaunching);

                    // Bring this application to front
                    AppLauncher.showFrameToFront();

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

                // anyway: cancel timer:
                enableCancelTimer(false);

                // Postpone hiding to let the user see the last message
                final ActionListener hideTask = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (_window.isVisible()) {
                            _window.setVisible(false);
                        }
                    }
                };

                // Fire after 3 second
                final Timer hideTaskTimer = new Timer(AUTO_HIDE_DELAY, hideTask);
                hideTaskTimer.setRepeats(false);
                hideTaskTimer.start();
            }
        }
    }

    /**
     * Start/Stop the internal cancel timer
     *
     * @param enable true to enable it, false otherwise
     */
    private void enableCancelTimer(final boolean enable) {

        if (_cancelTimer == null) {
            return;
        }

        if (enable) {
            if (!_cancelTimer.isRunning()) {
                _logger.info("Starting timer: {}", _cancelTimer);
                _cancelTimer.start();
            }
        } else {
            if (_cancelTimer.isRunning()) {
                _logger.info("Stopping timer: {}", _cancelTimer);
                _cancelTimer.stop();
            }
            _cancelTimer = null;
        }
    }
}
