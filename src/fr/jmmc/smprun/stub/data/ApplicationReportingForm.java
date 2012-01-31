/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub.data;

import fr.jmmc.jmcs.data.preference.CommonPreferences;
import fr.jmmc.jmcs.gui.MainMenuBar;
import fr.jmmc.jmcs.gui.WindowCenterer;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
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
    private JEditorPane _mainExplanationLabel;
    /** 'JNLP URL:' label */
    private JLabel _jnlpUrlLabel;
    /** JNLP URL Field */
    private JTextField _jnlpUrlField;
    /** 'Contact eMail:' label */
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

        _mainExplanationLabel = new JEditorPane();
        _mainExplanationLabel.setEditable(false);
        _mainExplanationLabel.setOpaque(false); // To keep default background color instead of the default white
        _mainExplanationLabel.setContentType(new HTMLEditorKit().getContentType());
        _mainExplanationLabel.setCaretPosition(0); // Show first line of editor pane, and not its last line as by default !
        // Add a CSS rule to force body tags to use the default label font instead of the value in javax.swing.text.html.default.csss
        Font font = UIManager.getFont("Label.font");
        String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument)_mainExplanationLabel.getDocument()).getStyleSheet().addRule(bodyRule);
        String message = "<html><head></head><body>"
                + "AppLauncher discovered the '" + _applicationName + "' application it did not know yet.<br>"
                + "Do you wish to contribute making AppLauncher better, and send '" + _applicationName + "' application<br>"
                + "description to the JMMC ?<br><br>"
                + "<i>No other personnal information than those optionaly provided below will be sent along.</i>"
                + "</body></html>";
        _mainExplanationLabel.setText(message);

        _jnlpUrlLabel = new JLabel("JNLP URL:");
        _panel.add(_jnlpUrlLabel);
        _jnlpUrlField = new JTextField();
        _panel.add(_jnlpUrlField);

        _contactEmailLabel = new JLabel("Contact eMail:");
        _panel.add(_contactEmailLabel);
        _contactEmailField = new JTextField();
        _panel.add(_contactEmailField);
        // Automatically fulfill the email field with default shared user email (as in FeedbackReport), if any
        String storedEmail = CommonPreferences.getInstance().getPreference(CommonPreferences.FEEDBACK_REPORT_USER_EMAIL);
        _contactEmailField.setText(storedEmail);

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

        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(_mainExplanationLabel).addGroup(
                layout.createSequentialGroup().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(_jnlpUrlLabel).addComponent(_contactEmailLabel)).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(_jnlpUrlField).addComponent(_contactEmailField)).addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING).addComponent(_cancelButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(_submitButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))));

        layout.setVerticalGroup(
                layout.createSequentialGroup().addComponent(_mainExplanationLabel).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(_jnlpUrlLabel).addComponent(_jnlpUrlField).addComponent(_cancelButton)).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(_contactEmailLabel).addComponent(_contactEmailField).addComponent(_submitButton)));
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
