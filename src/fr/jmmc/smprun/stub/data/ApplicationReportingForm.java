/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub.data;

import fr.jmmc.jmcs.gui.MainMenuBar;
import fr.jmmc.jmcs.gui.WindowCenterer;
import java.awt.Color;
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
    // GUI stuff
    /** Window panel */
    private JPanel _panel;
    /** 'Find:" label */
    private JLabel _jnlpUrlLabel;
    /** JNLP URL Field */
    private JTextField _jnlpUrlField;
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
     * @param tableSorter the object to use to select found result.
     * @param calibratorsTable the data source to search in.
     */
    public ApplicationReportingForm() {
        super("Report New SAMP Application to JMMC Registry");

        setupActions();
        createWidgets();
        layoutWidgets();
//        monitorWidgets();
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

        _jnlpUrlLabel = new JLabel("Find:");
        _panel.add(_jnlpUrlLabel);

        _jnlpUrlField = new JTextField();
        _panel.add(_jnlpUrlField);

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

    /** Place graphical widgets on the 'Find' window */
    private void layoutWidgets() {
        GroupLayout layout = new GroupLayout(_panel);
        _panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(
                layout.createSequentialGroup().addComponent(_jnlpUrlLabel).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(_jnlpUrlField).addComponent(_contactEmailField)).addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING).addComponent(_cancelButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(_submitButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        layout.setVerticalGroup(
                layout.createSequentialGroup().addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(_jnlpUrlLabel).addComponent(_jnlpUrlField).addComponent(_cancelButton)).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(_contactEmailField).addComponent(_submitButton)));
    }

    /** Start SearchField listening */
/*
    private void monitorWidgets() {
        _jnlpUrlField.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                doSearch(SearchPanel.SEARCH_DIRECTION.UNDEFINED);
            }
        });
    }
*/

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

    /**
     * (Dis)enable menu actions on demand.
     *
     * @param shouldBeEnabled Enables menu if true, disables them otherwise.
     */
/*
    public void enableMenus(boolean shouldBeEnabled) {
        _submitAction.setEnabled(shouldBeEnabled);
        _cancelAction.setEnabled(shouldBeEnabled);
        _findPreviousAction.setEnabled(shouldBeEnabled);
    }
*/

    /**
     * Handle search requests.
     * @param direction Going 'NEXT' or 'PREVIOUS', or reset in 'UNDEFINED'.
     */
/*
    private void doSearch(SearchPanel.SEARCH_DIRECTION direction) {
        final String text = _searchField.getText().trim();

        final boolean isRegExp = _regexpCheckBox.isSelected();
        if (text.length() > 0) {
            if (!_searchHelper.search(text, isRegExp, direction)) {
                _searchField.setBackground(Color.red);
            } else {
                _searchField.setBackground(Color.WHITE);
            }
        }
    }
*/
    protected class SubmitAction extends AbstractAction {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        SubmitAction() {
            super();
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            // TODO : submit meta-data
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
        }
    }
}
