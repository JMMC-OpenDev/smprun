/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.preference;

import fr.jmmc.jmcs.data.preference.PreferencesException;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sylvain LAFRASSE
 */
public class GeneralSettingsView extends JPanel implements Observer, ChangeListener {

    /** Logger - get from given class name */
    private static final Logger _logger = LoggerFactory.getLogger(GeneralSettingsView.class.getName());
    private final Preferences _preferences;
    private final Map<PreferenceKey, JCheckBox> _booleanPreferencesHashMap;
    private boolean _programaticUpdateUnderway = false;

    public GeneralSettingsView() {

        super();

        _preferences = Preferences.getInstance();

        _booleanPreferencesHashMap = new LinkedHashMap<PreferenceKey, JCheckBox>();
        _booleanPreferencesHashMap.put(PreferenceKey.SHOW_DOCK_WINDOW, new JCheckBox("Show Dock window on startup"));
        _booleanPreferencesHashMap.put(PreferenceKey.SHOW_EXIT_WARNING, new JCheckBox("Show warning before shuting down SAMP hub while quitting"));
        _booleanPreferencesHashMap.put(PreferenceKey.START_ALL_STUBS, new JCheckBox("Only provide selected application SAMP handling on startup"));
    }

    public void init() {

        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);

        // Layout management
        topPanel.setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 0;

        // Initialize all checkboxes
        for (JCheckBox checkBox : _booleanPreferencesHashMap.values()) {
            topPanel.add(checkBox, gridBagConstraints);
            checkBox.addChangeListener(this);
            gridBagConstraints.gridy++;
        }
        add(topPanel);

        update(null, null);
    }

    @Override
    public void update(Observable observable, Object parameter) {

        _programaticUpdateUnderway = true;

        for (Map.Entry<PreferenceKey, JCheckBox> entry : _booleanPreferencesHashMap.entrySet()) {

            final JCheckBox currentCheckBox = entry.getValue();
            final String currentCheckBoxName = currentCheckBox.getText();
            final boolean currentCheckBoxState = currentCheckBox.isSelected();
            final PreferenceKey currentPreferenceKey = entry.getKey();
            final boolean currentPreferenceState = _preferences.getPreferenceAsBoolean(currentPreferenceKey);

            _logger.debug("Set checkbox '" + currentCheckBoxName + "' to '" + currentPreferenceState + "' (was '" + currentCheckBoxState + "').");
            currentCheckBox.setSelected(currentPreferenceState);
        }

        _programaticUpdateUnderway = false;
    }

    /**
     * Update preferences according buttons change
     * @param ev 
     */
    @Override
    public void stateChanged(ChangeEvent ev) {

        JCheckBox clickedCheckBox = (JCheckBox) ev.getSource();
        if (clickedCheckBox == null) {
            _logger.error("Could not retrieve event source : " + ev);
            return;
        }

        final String clickedCheckBoxName = clickedCheckBox.getText();
        _logger.debug("Checkbox '" + clickedCheckBoxName + "' state changed:");

        if (_programaticUpdateUnderway) {
            _logger.trace("Programatic update underway, SKIPPING.");
            return;
        }

        for (Map.Entry<PreferenceKey, JCheckBox> entry : _booleanPreferencesHashMap.entrySet()) {

            final JCheckBox currentCheckBox = entry.getValue();
            if (!clickedCheckBox.equals(currentCheckBox)) {
                continue;
            }

            final PreferenceKey currentPreferenceKey = entry.getKey();
            final boolean currentPreferenceState = _preferences.getPreferenceAsBoolean(currentPreferenceKey);
            final boolean clickedCheckBoxState = currentCheckBox.isSelected();

            if (clickedCheckBoxState == currentPreferenceState) {
                _logger.trace("State did not trully changed (" + clickedCheckBoxState + " == " + currentPreferenceState + "), SKIPPING.");
                return;
            }

            try {
                _logger.debug("State did changed (" + currentPreferenceState + " -> " + clickedCheckBoxState + "), WRITING.");
                _preferences.setPreference(currentPreferenceKey, clickedCheckBoxState);
            } catch (PreferencesException ex) {
                _logger.warn("Could not set preference : " + ex);
            }

            return;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        final GeneralSettingsView generalSettingsView = new GeneralSettingsView();
        generalSettingsView.init();
        frame.add(generalSettingsView);
        frame.pack();
        frame.setVisible(true);
    }
}