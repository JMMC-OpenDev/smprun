/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.gui.StatusBar;
import fr.jmmc.jmcs.gui.SwingUtils;
import fr.jmmc.jmcs.gui.action.RegisteredAction;
import fr.jmmc.jmcs.util.ImageUtils;
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
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * Main window. This class is at one central point and play the mediator role.

 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public class DockWindow extends JFrame {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Logger */
    private static final Logger _logger = Logger.getLogger(DockWindow.class.getName());
    /** DockWindow singleton */
    private static DockWindow instance = null;
    /** window dimensions */
    private static final Dimension _windowDimension = new Dimension(640, 120);
    /* members */
    /** button / client map */
    private final HashMap<JButton, ClientStub> _clientButtons = new HashMap<JButton, ClientStub>(8);
    /** client / button map */
    private final HashMap<ClientStub, JButton> _buttonClients = new HashMap<ClientStub, JButton>(8);

    /**
     * Return the DockWindow singleton 
     * @return DockWindow singleton
     */
    public static DockWindow getInstance() {
        if (instance == null) {
            instance = new DockWindow();
        }
        return instance;
    }

    /**
     * Constructor.
     */
    private DockWindow() {

        super("AppLauncher");

        prepareFrame();
        preparePane();

        // Show the user the app is ready to be used
        StatusBar.show("application ready.");
    }

    /**
     * Prepare the frame
     */
    private void prepareFrame() {
        setMinimumSize(_windowDimension);
        setMaximumSize(_windowDimension);

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

    /**
     * Prepare the content pane
     */
    private void preparePane() {
        final Container mainPane = getContentPane();
        mainPane.setLayout(new BorderLayout());

        final JPanel verticalListPane = new JPanel();
        verticalListPane.setLayout(new BoxLayout(verticalListPane, BoxLayout.Y_AXIS));

        JLabel familyLabel;
        JScrollPane iconPane;
        for (ClientStubFamily clientFamily : ClientStubFamily.values()) {
            familyLabel = new JLabel("<html><b>" + clientFamily.GetFamily() + "</b></html>");
            verticalListPane.add(familyLabel);

            iconPane = buildScrollPane(clientFamily);
            iconPane.setAlignmentX(0.01f);
            verticalListPane.add(iconPane);

            verticalListPane.add(new JSeparator());
        }

        mainPane.add(verticalListPane, BorderLayout.CENTER);
        mainPane.add(new StatusBar(), BorderLayout.SOUTH);
    }

    /**
     * Create one scroll pane per client family
     * @param family client family
     * @return scroll pane
     */
    public final JScrollPane buildScrollPane(final ClientStubFamily family) {

        final JPanel horizontalRowPane = new JPanel();
        horizontalRowPane.setLayout(new BoxLayout(horizontalRowPane, BoxLayout.X_AXIS));

        // @TODO : fixes spaces to actually work !!!
        final Component emptyRigidArea = Box.createRigidArea(new Dimension(100, 0));
        horizontalRowPane.add(emptyRigidArea);

        final List<ClientStub> clients = HubPopulator.getInstance().getClientList(family);

        // Create the button action listener once:
        final ActionListener buttonActionListener = new ActionListener() {

            /**
             * Start client application when its icon is clicked
             */
            @Override
            public void actionPerformed(final ActionEvent e) {

                if (e.getSource() instanceof JButton) {
                    final JButton button = (JButton) e.getSource();

                    final ClientStub stub = _clientButtons.get(button);

                    // Start application in background:
                    if (stub != null) {
                        stub.launchRealApplication();
                    }
                }
            }
        };

        JButton button;
        for (final ClientStub client : clients) {

            button = buildClientButton(client);
            button.addActionListener(buttonActionListener);

            _clientButtons.put(button, client);
            _buttonClients.put(client, button);

            horizontalRowPane.add(button);
            horizontalRowPane.add(emptyRigidArea);
        }

        horizontalRowPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        final JScrollPane scrollPane = new JScrollPane(horizontalRowPane);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollPane.setPreferredSize(_windowDimension);
        scrollPane.setMinimumSize(_windowDimension);
        scrollPane.setMaximumSize(_windowDimension);

        JViewport view = scrollPane.getViewport();
        view.add(horizontalRowPane);

        return scrollPane;
    }

    /**
     * Create the button representing one client stub (application)
     * @param client client stub instance
     * @return created button
     */
    private JButton buildClientButton(final ClientStub client) {

        final String clientName = client.getApplicationName();
        ImageIcon clientIcon = client.getApplicationIcon();

        // Resize the icon up to 64*64 pixels
        final int iconHeight = clientIcon.getIconHeight();
        final int newHeight = Math.min(iconHeight, 64);
        final int iconWidth = clientIcon.getIconWidth();
        final int newWidth = Math.min(iconWidth, 64);
        clientIcon = ImageUtils.getScaledImageIcon(clientIcon, 64, 64);

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

    /**
     * Called to show the preferences window.
     */
    protected class ShowPreferencesAction extends RegisteredAction {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        /**
         * Action constructor
         * @param classPath the path of the class containing the field pointing to
         * the action, in the form returned by 'getClass().getName();'.
         * @param fieldName the name of the field pointing to the action.
         */
        ShowPreferencesAction(final String classPath, final String fieldName) {
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

    /**
     * Callback to reenable the button representing the client stub
     * @param client client stub to reenable
     * @param enabled button state
     */
    public void defineButtonEnabled(final ClientStub client, final boolean enabled) {
        final JButton button = _buttonClients.get(client);
        if (button != null) {
            SwingUtils.invokeEDT(new Runnable() {

                @Override
                public void run() {
                    button.setEnabled(enabled);
                }
            });
        }
    }
}
/*___oOo___*/
