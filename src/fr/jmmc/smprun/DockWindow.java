/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.data.preference.MissingPreferenceException;
import fr.jmmc.jmcs.data.preference.PreferencesException;
import fr.jmmc.jmcs.gui.component.StatusBar;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.util.ImageUtils;
import fr.jmmc.smprsc.data.list.model.Category;
import fr.jmmc.smprun.preference.PreferenceKey;
import fr.jmmc.smprun.preference.Preferences;
import fr.jmmc.smprun.stub.ClientStub;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main window. This class is at one central point and play the mediator role.

 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public class DockWindow extends JFrame implements Observer {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger(DockWindow.class.getName());
    /** DockWindow singleton */
    private static DockWindow _instance = null;
    /** window dimensions */
    private static final Dimension _windowDimension = new Dimension(640, 120);
    /* members */
    /** button / client map */
    private final HashMap<JButton, ClientStub> _clientButtons = new HashMap<JButton, ClientStub>(8);
    /** client / button map */
    private final HashMap<ClientStub, JButton> _buttonClients = new HashMap<ClientStub, JButton>(8);
    /** User-chosen application name list */
    private final Preferences _preferences;
    private ArrayList<String> _selectedApplicationNameList = null;
    private static final ArrayList<String> ALL = null;

    /**
     * Return the DockWindow singleton 
     * @return DockWindow singleton
     */
    public static DockWindow getInstance() {
        if (_instance == null) {
            _instance = new DockWindow();
            _instance.init();
        }
        return _instance;
    }

    /**
     * Constructor.
     */
    private DockWindow() {

        super("AppLauncher");

        _preferences = Preferences.getInstance();

        prepareFrame();
        update(null, null);

        // Show the user the app is ready to be used
        StatusBar.show("application ready.");
    }

    public void init() {
        _preferences.addObserver(this);
    }

    @Override
    public void update(final Observable observable, Object param) {

        _selectedApplicationNameList = ALL;
        try {
            _selectedApplicationNameList = _preferences.getPreferenceAsStringList(PreferenceKey.SELECTED_APPLICATION_LIST);
        } catch (MissingPreferenceException ex) {
            _logger.error("MissingPreferenceException :", ex);
        } catch (PreferencesException ex) {
            _logger.error("PreferencesException :", ex);
        }

        // Using invokeAndWait to be in sync with this thread :
        // note: invokeAndWaitEDT throws an IllegalStateException if any exception occurs
        SwingUtils.invokeAndWaitEDT(new Runnable() {

            /**
             * Initializes the swing components with their actions in EDT
             */
            @Override
            public void run() {

                // If the selected application list changed (preference uptdate)
                if (observable == _preferences) {
                    _logger.debug("Removing all previous components on preference update.");

                    // @TODO : remove button listerner before clearing map.
                    _clientButtons.clear();
                    _buttonClients.clear();

                    // Empty the frame
                    getContentPane().removeAll();
                }

                // Fill the frame
                preparePane();
            }
        });
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
        for (Category clientFamily : Category.values()) {

            iconPane = buildScrollPane(clientFamily);
            if (iconPane == null) {
                continue;
            }

            familyLabel = new JLabel("<HTML><B>" + clientFamily.value() + "</B></HTML>");
            verticalListPane.add(familyLabel);
            iconPane.setAlignmentX(0.01f);
            verticalListPane.add(iconPane);
            verticalListPane.add(new JSeparator());
        }

        mainPane.add(verticalListPane, BorderLayout.CENTER);
        mainPane.add(new StatusBar(), BorderLayout.SOUTH);

        pack();
    }

    /**
     * Create one scroll pane per client family
     * @param family client family
     * @return built scroll pane, or null if nothing to display (e.g daemon category)
     */
    private JScrollPane buildScrollPane(final Category family) {

        final JPanel horizontalRowPane = new JPanel();
        horizontalRowPane.setLayout(new BoxLayout(horizontalRowPane, BoxLayout.X_AXIS));

        // @TODO : fixes spaces to actually work !!!
        final Component emptyRigidArea = Box.createRigidArea(new Dimension(100, 0));
        horizontalRowPane.add(emptyRigidArea);

        final List<ClientStub> clients = HubPopulator.getInstance().getClientList(family);
        if (clients == null) {
            return null;
        }

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

        boolean categoryIsEmpty = true;
        for (final ClientStub client : clients) {

            // If the current client is not in the selected application
            final String applicationName = client.getApplicationName();
            if ((_selectedApplicationNameList != ALL) && (!_selectedApplicationNameList.contains(applicationName))) {
                continue;
            }

            final JButton button = buildClientButton(client);
            // if the current stub should remain invisble
            if (button == null) {
                continue; // Skip GUI stuff creation
            }

            categoryIsEmpty = false;

            _clientButtons.put(button, client);
            _buttonClients.put(client, button);

            button.addActionListener(buttonActionListener);

            horizontalRowPane.add(button);
            horizontalRowPane.add(emptyRigidArea);
        }

        if (categoryIsEmpty) {
            return null;
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
     * @return created button, or null if not visible.
     */
    private JButton buildClientButton(final ClientStub client) {

        final String clientName = client.getApplicationName();
        ImageIcon clientIcon = client.getApplicationIcon();
        if (clientIcon == null) {
            return null;
        }

        // Resize the icon up to 64*64 pixels
        final int iconHeight = clientIcon.getIconHeight();
        final int iconWidth = clientIcon.getIconWidth();
        clientIcon = ImageUtils.getScaledImageIcon(clientIcon, 64, 64);
        final int newHeight = clientIcon.getIconHeight();
        final int newWidth = clientIcon.getIconWidth();

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
     * Callback to re-enable the button representing the client stub
     * @param client client stub to re-enable
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
