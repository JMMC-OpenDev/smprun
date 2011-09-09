/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.gui.StatusBar;
import fr.jmmc.jmcs.gui.WindowCenterer;
import fr.jmmc.jmcs.gui.action.RegisteredAction;
import fr.jmmc.smprun.stub.ClientStub;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

/**
 * Main window. This class is at one central point and play the mediator role.
 */
public class DockWindow extends JFrame {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(DockWindow.class.getName());
    JButton[] labels = null;
    Dimension _windowDimension = new Dimension(640, 160);
    HubPopulator _clients = null;
    HashMap<JButton, ClientStub> _clientButton = new HashMap<JButton, ClientStub>();

    /**
     * Constructor.
     */
    public DockWindow(HubPopulator clients) {

        super("AppLauncher");

        _clients = clients;

        labels = new JButton[_clients.getClientList().size()];

        prepareFrame();
        preparePane();
        finalizeFrame();

        // Show the user the app is ready to be used
        StatusBar.show("application ready.");
    }

    private void prepareFrame() {
        setMinimumSize(_windowDimension);
        setMaximumSize(_windowDimension);

        WindowCenterer.centerOnMainScreen(this);
    }

    private void preparePane() {
        Container _mainPane = getContentPane();
        _mainPane.setLayout(new BorderLayout());

        _mainPane.add(buildScrollPane(labels), BorderLayout.CENTER);
        //_mainPane.add(dockPane, BorderLayout.CENTER);

        StatusBar _statusBar = new StatusBar();
        // Show all the GUI
        _statusBar.setVisible(true);

        // Add the Status bar
        _mainPane.add(_statusBar, BorderLayout.SOUTH);
    }

    private void finalizeFrame() {
        // Set the GUI up
        pack();
        setVisible(true);

        // previous adapter manages the windowClosing(event) :
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Properly quit the application when main window close button is clicked
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(final WindowEvent e) {
                // callback on exit :
                App.quitAction().actionPerformed(null);
            }
        });
    }

    public final JScrollPane buildScrollPane(JButton[] buttons) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        for (int i = 0; i < _clients.getClientList().size(); i++) {

            final ClientStub client = _clients.getClientList().get(i);
            // #TODO : handle NPE
            final ImageIcon clientIcon = client.getApplicationIcon();
            // #TODO : handle NPE
            final String clientName = client.getApplicationName();
            // #TODO : handle NPE

            final JButton button = new JButton(clientIcon);
            button.setText(clientName);
            // @TODO : add a 10 pixel border around each button

            _clientButton.put(button, client);

            panel.add(button);
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    StatusBar.show("Starting " + clientName + "...");

                    JButton button = (JButton) e.getSource();
                    // @TODO : handle NPE

                    final ClientStub client = _clientButton.get(button);
                    // @TODO : handle NPE

                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            // Here, we can safely update the GUI
                            // because we'll be called from the
                            // event dispatch thread
                            client.startTrueApplication();
                            StatusBar.show("Started " + clientName + ".");
                        }
                    });
                }
            });
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(_windowDimension);
        scrollPane.setMinimumSize(_windowDimension);
        scrollPane.setMaximumSize(_windowDimension);

        JViewport view = scrollPane.getViewport();
        view.add(panel);

        return scrollPane;
    }

    public static void main(String[] args) {
        new DockWindow(new HubPopulator());
    }

    /**
     * Called to show the preferences window.
     */
    protected class ShowPreferencesAction extends RegisteredAction {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        ShowPreferencesAction(String classPath, String fieldName) {
            super(classPath, fieldName);
            flagAsPreferenceAction();
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            _logger.entering("ShowPreferencesAction", "actionPerformed");

            // Show the Preferences window
            //_preferencesView.setVisible(true);
        }
    }
}
/*___oOo___*/
