/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.gui.StatusBar;
import fr.jmmc.jmcs.gui.WindowCenterer;
import fr.jmmc.jmcs.gui.action.RegisteredAction;
import fr.jmmc.smprun.stub.ClientStub;
import fr.jmmc.smprun.stub.ClientStubFamily;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * Main window. This class is at one central point and play the mediator role.
 */
public class DockWindow extends JFrame {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(DockWindow.class.getName());
    Dimension _windowDimension = new Dimension(640, 110);
    HashMap<JButton, ClientStub> _clientButton = new HashMap<JButton, ClientStub>();

    /**
     * Constructor.
     */
    public DockWindow() {

        super("AppLauncher");

        prepareFrame();
        preparePane();
        finalizeFrame();

        // Show the user the app is ready to be used
        StatusBar.show("application ready.");
    }

    private void prepareFrame() {
        setMinimumSize(_windowDimension);
        setMaximumSize(_windowDimension);
    }

    private void preparePane() {
        Container _mainPane = getContentPane();
        _mainPane.setLayout(new BorderLayout());

        final JPanel verticalListPane = new JPanel();
        verticalListPane.setLayout(new BoxLayout(verticalListPane, BoxLayout.Y_AXIS));

        for (ClientStubFamily clientFamily : ClientStubFamily.values()) {
            JLabel familyLabel = new JLabel("<html><b>" + clientFamily.family() + "</b></html>");
            verticalListPane.add(familyLabel);

            JScrollPane iconPane = buildScrollPane(clientFamily);
            iconPane.setAlignmentX(0.01f);
            verticalListPane.add(iconPane);

            JSeparator separator = new JSeparator();
            verticalListPane.add(separator);
        }

        _mainPane.add(verticalListPane, BorderLayout.CENTER);

        StatusBar _statusBar = new StatusBar();
        _statusBar.setVisible(true);
        _mainPane.add(_statusBar, BorderLayout.SOUTH);
    }

    private void finalizeFrame() {
        // Set the GUI up
        pack();
        setVisible(true);

        WindowCenterer.centerOnMainScreen(this);

        // @TODO : Put it in System Tray ??

        // Previous adapter manages the windowClosing(event) :
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Properly quit the application when main window close button is clicked
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(final WindowEvent e) {
                // Callback on exit :
                App.quitAction().actionPerformed(null);
            }
        });
    }

    public final JScrollPane buildScrollPane(ClientStubFamily family) {

        final JPanel horizontalRowPane = new JPanel();
        horizontalRowPane.setLayout(new BoxLayout(horizontalRowPane, BoxLayout.X_AXIS));

        // @TODO : fixes spaces to actually work !!!
        final Component emptyRigidArea = Box.createRigidArea(new Dimension(100, 0));
        horizontalRowPane.add(emptyRigidArea);

        List<ClientStub> clients = HubPopulator.getInstance().getClientList(family);
        for (final ClientStub client : clients) {

            JButton button = buildClientButton(client);
            _clientButton.put(button, client);

            horizontalRowPane.add(button);
            horizontalRowPane.add(emptyRigidArea);

            // Start client application when its icon is clicked
            button.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    StatusBar.show("Starting " + client + "...");

                    // @TODO : handle NPE
                    final JButton button = (JButton) e.getSource();

                    // @TODO : handle NPE
                    final ClientStub client = _clientButton.get(button);

                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            client.launchApplication();
                            StatusBar.show("Started " + client + ".");
                        }
                    });
                }
            });
        }

        horizontalRowPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(horizontalRowPane);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollPane.setPreferredSize(_windowDimension);
        scrollPane.setMinimumSize(_windowDimension);
        scrollPane.setMaximumSize(_windowDimension);

        JViewport view = scrollPane.getViewport();
        view.add(horizontalRowPane);

        return scrollPane;
    }

    private JButton buildClientButton(final ClientStub client) {
        // #TODO : handle NPE
        final String clientName = client.toString();

        // #TODO : handle NPE
        ImageIcon clientIcon = client.getApplicationIcon();

        // Resize the icon up to 64*64 pixels
        final Image image = clientIcon.getImage();
        final int iconHeight = clientIcon.getIconHeight();
        final int newHeight = Math.min(iconHeight, 64);
        final int iconWidth = clientIcon.getIconWidth();
        final int newWidth = Math.min(iconWidth, 64);
        final Image newImage = image.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
        clientIcon = new ImageIcon(newImage);
        // Horizontally center the icon, and bottom-aligned them all vertically
        final int squareSize = 68;
        final int borderSize = 2;
        final int midHorizontalMargin = (squareSize - newWidth) / 2;
        final int topVerticalMargin = squareSize - borderSize - newHeight; // Space to fill above if the icon is smaller than 64 pixels
        final Border border = new EmptyBorder(topVerticalMargin, midHorizontalMargin, borderSize, midHorizontalMargin);
        // Horizontally center application name below its icon
        final JButton button = new JButton(clientIcon);
        button.setText(clientName);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setBorder(border);

        return button;
    }

    public static void main(String[] args) {
        new DockWindow();
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
