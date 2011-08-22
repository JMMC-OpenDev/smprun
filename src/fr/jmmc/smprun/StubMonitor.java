/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.mcs.gui.App;
import fr.jmmc.mcs.gui.WindowCenterer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author Sylvain LAFRASSE
 */
public class StubMonitor implements Observer {

    MonitorWindow _window;

    public StubMonitor() {
        super();

        SwingUtilities.invokeLater(new Runnable() {

            /**
             * Synchronized by EDT
             */
            public void run() {
                _window = new MonitorWindow();
                _window.setTitle("JMMC AppLauncher");
                _window.setVisible(false);
                _window.pack();
                WindowCenterer.centerOnMainScreen(_window);
            }
        });
    }

    @Override
    public void update(Observable o, Object arg) {

        final String applicationName = ((ClientStub) o).getApplicationName();

        final ClientStubState state = ((ClientStubState) arg);
        final String message = state.message();
        final int step = state.step();
        final int maxStep = ClientStubState.DIYING.step();

        // System.out.println("monitor['" + applicationName + "'] : '" + state.message() + "' (" + state.step() + "/" + maxStep + ").");

        SwingUtilities.invokeLater(new Runnable() {

            /**
             * Synchronized by EDT
             */
            public void run() {
                // bring this application to front :
                App.showFrameToFront();

                if (step > 0) {

                    JLabel label = _window.getLabel();
                    label.setText("Redirecting to " + applicationName + ":");

                    JProgressBar bar = _window.getProgressBar();

                    bar.setMinimum(0);
                    bar.setMaximum(maxStep);
                    bar.setValue(state.step());

                    bar.setStringPainted(true);
                    if (!message.isEmpty()) {
                        bar.setString(message + "...");
                    }

                    if (step < maxStep) {
                        _window.setVisible(true);
                    }
                }
            }
        });

        // Should the window close ?
        if (step == maxStep) {

            // Postpone closing to let the user see the last message
            ActionListener task = new ActionListener() {

                boolean alreadyDisposed = false;

                public void actionPerformed(ActionEvent e) {
                    if (_window.isDisplayable()) {
                        alreadyDisposed = true;
                        _window.dispose();
                    }
                }
            };
            Timer timer = new Timer(1500, task); // Fire after 1.5 second
            timer.setRepeats(false);
            timer.start();
        }
    }
}
