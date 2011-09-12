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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

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

        // @TODO : Put it in System Tray ??

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
        panel.add(Box.createRigidArea(new Dimension(10, 0)));

        for (int i = 0; i < _clients.getClientList().size(); i++) {

            // #TODO : handle NPE
            final ClientStub client = _clients.getClientList().get(i);

            // #TODO : handle NPE
            final String clientName = client.getApplicationName();

            // #TODO : handle NPE
            ImageIcon clientIcon = client.getApplicationIcon();
            Image image = clientIcon.getImage();
            final int iconHeight = clientIcon.getIconHeight();
            int newHeight = Math.min(iconHeight, 64);
            System.out.println("newHeight[" + clientName + "] = " + newHeight + " (was " + iconHeight + ").");
            final int iconWidth = clientIcon.getIconWidth();
            int newWidth = Math.min(iconWidth, 64);
            System.out.println("newWidth[" + clientName + "] = " + newWidth + " (was " + iconWidth + ").");
            Image newImage = image.getScaledInstance(newHeight, newWidth, java.awt.Image.SCALE_SMOOTH);
            clientIcon = new ImageIcon(newImage);
            System.out.println("height = " + clientIcon.getIconHeight());
            System.out.println("width = " + clientIcon.getIconWidth());

            // @TODO : fill blank for icons less than 64*64
            int midHorizontalMargin = (68 - newWidth) / 2;
            System.out.println("horizontalMargin[" + clientName + "] = " + midHorizontalMargin);
            int topVerticalMargin = (68 - newHeight) / 2;
            System.out.println("verticalMargin[" + clientName + "] = " + topVerticalMargin);
            Border border = new EmptyBorder(topVerticalMargin, midHorizontalMargin, topVerticalMargin, midHorizontalMargin);
            border = new EtchedBorder();

            // Place Application name centered below its icon
            final JButton button = new JButton(clientIcon);
            button.setText(clientName);
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setBorder(border);

            // @TODO : add a 10 pixel border around each button

            _clientButton.put(button, client);

            panel.add(button);
            panel.add(Box.createRigidArea(new Dimension(10, 0)));

            // Start clein application when its icon is clicked
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    StatusBar.show("Starting " + clientName + "...");

                    // @TODO : handle NPE
                    JButton button = (JButton) e.getSource();

                    // @TODO : handle NPE
                    final ClientStub client = _clientButton.get(button);

                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            client.launchApplication();
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
