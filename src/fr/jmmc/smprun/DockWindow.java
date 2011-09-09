/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.gui.StatusBar;
import fr.jmmc.jmcs.gui.action.RegisteredAction;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Main window. This class is at one central point and play the mediator role.
 */
public class DockWindow extends JFrame {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(DockWindow.class.getName());
    JLabel[] labels = null;
        Dimension _dimension = new Dimension(640, 160);

    /**
     * Constructor.
     */
    public DockWindow(int numOfLabels) {

        super(numOfLabels + " labels");

        labels = new JLabel[numOfLabels];

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initLabels();

        getContentPane().add(buildPanelOfLabels(labels), "Center");

        setSize(_dimension);

        //setLocation(100, 300);

        pack();

        setVisible(true);

        /*
        super("AppLauncher Dock");
        
        JPanel dockPane = new JPanel();
        dockPane.setLayout(new BoxLayout(dockPane, BoxLayout.X_AXIS));
        
        for (int i = 0; i < 10; i++) {
        dockPane.add(new JLabel("toto " + i));
        }
        
        // Size management
        Dimension dimension = new Dimension(640, 160);
        //setMinimumSize(dimension);
        
        // Place tables into scrollPane
        JScrollPane scrollPane = new JScrollPane();
        //scrollPane.setMinimumSize(dimension);
        //scrollPane.setPreferredSize(dimension);
        scrollPane.add(dockPane);
        scrollPane.setVisible(true);
        
        try {
        setTitle("Dock");
        
        Container _mainPane = getContentPane();
        _mainPane.setLayout(new BorderLayout());
        
        _mainPane.add(scrollPane, BorderLayout.CENTER);
        //_mainPane.add(dockPane, BorderLayout.CENTER);
        
        StatusBar _statusBar = new StatusBar();
        // Show all the GUI
        _statusBar.setVisible(true);
        
        // Add the Status bar
        _mainPane.add(_statusBar, BorderLayout.SOUTH);
        
        // Set the GUI up
        pack();
        setVisible(true);
        
        // Show the user the app is ready to be used
        StatusBar.show("application ready.");
        } catch (Exception e) {
        _logger.log(Level.SEVERE, "Main window failure : ", e);
        }
        
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
         * 
         */
    }

    private void initLabels() {

        for (int i = 0; i < labels.length; i++) {

            labels[i] = new JLabel("Label Test " + i);

        }
    }

    public final JScrollPane buildPanelOfLabels(JLabel[] labels) {

        JPanel panel = new JPanel();

        panel.setPreferredSize(_dimension);

        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        //panel.setLayout(new GridLayout(0,1));

        JScrollPane scrlP = new JScrollPane(panel);

        scrlP.getViewport().add(panel);

        for (int i = 0; i < labels.length; i++) {

            panel.add(labels[i]);

        }

        return scrlP;

    }

    public static void main(String[] args) {

        new DockWindow(30);

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
