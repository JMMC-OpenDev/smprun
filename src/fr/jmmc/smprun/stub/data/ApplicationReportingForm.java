/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub.data;

import fr.jmmc.jmcs.gui.MainMenuBar;
import fr.jmmc.jmcs.gui.WindowCenterer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search Panel
 *
 * @author Sylvain LAFRASSE.
 */
public class ApplicationReportingForm extends JFrame {

    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger(ApplicationReportingForm.class.getName());
    /** Hold the name of the application to report */
    private final String _applicationName;
    // GUI stuff
    /** Window panel */
    private JPanel _panel;
    /** main explanation label */
    private JLabel _mainExplanationLabel;
    /** 'JNLP URL:' label */
    private JLabel _jnlpUrlLabel;
    /** JNLP URL Field */
    private JTextField _jnlpUrlField;
    /** 'Contact E-Mail:' label */
    private JLabel _contactEmailLabel;
    /** Contact email Field */
    private JTextField _contactEmailField;
    /** Button to submit application meta-data to JMMC registry */
    private JButton _submitButton;
    /** Button to cancel application reporting */
    private JButton _cancelButton;
    // Action stuff
    /** Submit action */
    private SubmitAction _submitAction;
    /** Find Next action */
    private CancelAction _cancelAction;

    /**
     * Constructor
     */
    public ApplicationReportingForm(String applicationName) {

        super("Report New SAMP Application to JMMC Registry");

        _applicationName = applicationName;

        setupActions();
        createWidgets();
        layoutWidgets();
        prepareFrame();
    }

    /** Create required actions */
    private void setupActions() {
        _submitAction = new SubmitAction();
        _cancelAction = new CancelAction();
    }

    /** Create graphical widgets */
    private void createWidgets() {

        _panel = new JPanel();

        String message = "AppLauncher discovered the '" + _applicationName + "' application it did not know yet.\n"
                + "Do you wish to contribute making AppLauncher better, and send\n"
                + "'" + _applicationName + "' application description to the JMMC ?\n\n"
                + "No other personnal information than those optionaly provided below will be sent along.";

        _mainExplanationLabel = new JLabel(message);

        _jnlpUrlLabel = new JLabel("JNLP URL:");
        _panel.add(_jnlpUrlLabel);

        _jnlpUrlField = new JTextField();
        _panel.add(_jnlpUrlField);

        _contactEmailLabel = new JLabel("Contact E-Mail:");
        _panel.add(_contactEmailLabel);

        // TODO : fill the eamil field with defaut shared email as in FeedbackReport
        _contactEmailField = new JTextField();
        _panel.add(_contactEmailField);

        _submitButton = new JButton(_submitAction);
        _submitButton.setText("Submit");
        getRootPane().setDefaultButton(_submitButton);
        _panel.add(_submitButton);

        _cancelButton = new JButton(_cancelAction);
        _cancelButton.setText("Cancel");
        _panel.add(_cancelButton);
    }

    /** Place graphical widgets on the window */
    private void layoutWidgets() {
        GroupLayout layout = new GroupLayout(_panel);
        _panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(
                layout.createSequentialGroup().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(_jnlpUrlLabel).addComponent(_contactEmailLabel)).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(_jnlpUrlField).addComponent(_contactEmailField)).addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING).addComponent(_cancelButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(_submitButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        layout.setVerticalGroup(
                layout.createSequentialGroup().addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(_jnlpUrlLabel).addComponent(_jnlpUrlField).addComponent(_cancelButton)).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(_contactEmailLabel).addComponent(_contactEmailField).addComponent(_submitButton)));
    }

    /** Finish window setup */
    private void prepareFrame() {
        getContentPane().add(_panel);
        pack();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        WindowCenterer.centerOnMainScreen(this);

        // Trap Escape key
        KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        // Trap command-W key
        KeyStroke metaWStroke = KeyStroke.getKeyStroke(MainMenuBar.getSystemCommandKey() + "W");

        // Close window on either strike
        ActionListener actionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                _logger.debug("Hiding about box on keyboard shortcut.");
                setVisible(false);
            }
        };
        getRootPane().registerKeyboardAction(actionListener, escapeStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(actionListener, metaWStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    protected class SubmitAction extends AbstractAction {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        SubmitAction() {
            super();
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            // TODO : submit meta-data
            _logger.info("Reported SAMP applicatin meta-data to JMMC registry.");
        }
    }

    protected class CancelAction extends AbstractAction {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        CancelAction() {
            super();
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            // TODO : cancel meta-data submition
            _logger.info("Cancelled SAMP applicatin meta-data reporting.");
        }
    }

    public static void main(String[] args) {
        JFrame frame = new ApplicationReportingForm("Toto");
        WindowCenterer.centerOnMainScreen(frame);
        frame.setVisible(true);
    }
}
