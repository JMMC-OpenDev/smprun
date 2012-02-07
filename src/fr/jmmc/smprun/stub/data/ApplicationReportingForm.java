/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub.data;

import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.data.preference.CommonPreferences;
import fr.jmmc.jmcs.gui.MainMenuBar;
import fr.jmmc.jmcs.gui.WindowCenterer;
import java.awt.Font;
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
public class ApplicationReportingForm extends JDialog {

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
    // Data stuff
    /** User answer (true for submit, false for cancel) */
    private boolean _shouldSubmit;
    /** Hold user email address */
    private String _userEmail;
    /** Hold JNLP URL address */
    private String _jnlpURL;

    /**
     * Constructor
     */
    public ApplicationReportingForm(String applicationName) {

        super(App.getFrame(), "Report New SAMP Application to JMMC Registry ?", true);

        _applicationName = applicationName;

        initFields();
        setupActions();
        createWidgets();
        layoutWidgets();
        prepareFrame();
    }

    private void initFields() {
        _shouldSubmit = false;
        _userEmail = null;
        _jnlpURL = null;
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
        ((HTMLDocument) _mainExplanationLabel.getDocument()).getStyleSheet().addRule(bodyRule);
        String message = "<html><head></head><body>"
                + "<center>AppLauncher discovered the '<b>" + _applicationName + "</b>' application it did not know yet !</center><br/>"
                + "Do you wish to contribute making AppLauncher better, and send '" + _applicationName + "' application<br/>"
                + "description to the JMMC ?<br/><br/>"
                + "<small><i>No other personnal information than those optionaly provided below will be sent along.</i></small><br/>"
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
        _userEmail = CommonPreferences.getInstance().getPreference(CommonPreferences.FEEDBACK_REPORT_USER_EMAIL);
        _contactEmailField.setText(_userEmail);

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
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(_mainExplanationLabel)
                                                                         .addGroup(layout.createParallelGroup().addGroup(layout.createSequentialGroup()
                                                                                                                               .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING).addComponent(_jnlpUrlLabel).addComponent(_contactEmailLabel))
                                                                                                                               .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(_jnlpUrlField).addComponent(_contactEmailField)))
                                                                                                               .addGroup(layout.createSequentialGroup().addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(_cancelButton).addComponent(_submitButton))));

        layout.setVerticalGroup(
                layout.createSequentialGroup().addComponent(_mainExplanationLabel)
                                              .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(_jnlpUrlLabel).addComponent(_jnlpUrlField))
                                              .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(_contactEmailLabel).addComponent(_contactEmailField))
                                              .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING).addComponent(_cancelButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(_submitButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
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

        // Show the dialog and wait for user inputs
        setVisible(true);
    }

    /**
     * @return true if the user choose to submit, false otherwise (cancel)
     */
    public boolean shouldSubmit() {
        return _shouldSubmit;
    }

    /**
     * @return the user email address if any, null otherwise.
     */
    public String getUserEmail() {
        return _userEmail;
    }

    /**
     * @return the JNLP URL address if any, null otherwise.
     */
    public String getJnlpURL() {
        return _jnlpURL;
    }

    protected class SubmitAction extends AbstractAction {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        SubmitAction() {
            super();
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            _logger.info("Reported SAMP application meta-data to JMMC registry.");

            _shouldSubmit = true;
            _userEmail = _contactEmailField.getText();
            _jnlpURL = _jnlpUrlField.getText();

            _logger.debug("Hiding about box on Submit button.");
            setVisible(false);
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
            _logger.info("Cancelled SAMP application meta-data reporting.");

            _shouldSubmit = false;
            _userEmail = null;
            _jnlpURL = null;

            _logger.debug("Hiding about box on Cancel button.");
            setVisible(false);
        }
    }

    public static void main(String[] args) {

        // Create dialog and wait for user response
        ApplicationReportingForm form = new ApplicationReportingForm("Toto BlahBlah v33");

        // Output user values
        System.out.println("User answered:");
        System.out.println(" _shouldSubmit = '" + form.shouldSubmit() + "'.");
        System.out.println(" _userEmail    = '" + form.getUserEmail() + "'.");
        System.out.println(" _jnlpURL      = '" + form.getJnlpURL() + "'.");

        System.exit(0);
    }
}
