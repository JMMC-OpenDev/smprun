/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.mcs.gui.App;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JProgressBar;

/**
 *
 * @author Sylvain LAFRASSE
 */
public class StubMonitor implements Observer {

    MonitorWindow _window;

    public StubMonitor() {
        super();

        _window = new MonitorWindow();
        _window.setVisible(false);
        _window.pack();
    }

    @Override
    public void update(Observable o, Object arg) {
        ClientStubState state = ((ClientStubState) arg);

        System.out.println("monitor = '" + state.message() + "'.");
        if (state.step() > 0) {

            _window.setVisible(true);

            App.setFrame(_window);

            JProgressBar bar = _window.getProgressBar();
            bar.setMinimum(0);
            bar.setMaximum(ClientStubState.values().length);
            bar.setValue(state.step());
            bar.setString(state.message());
        } else {
            _window.setVisible(false);
        }
    }
}
