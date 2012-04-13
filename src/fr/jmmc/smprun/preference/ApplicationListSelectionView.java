/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.preference;

import fr.jmmc.jmcs.data.preference.PreferencesException;
import fr.jmmc.smprsc.data.list.ApplicationListSelectionPanel;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sylvain LAFRASSE
 */
public class ApplicationListSelectionView extends ApplicationListSelectionPanel implements Observer {

    /** Logger - get from given class name */
    private static final Logger _logger = LoggerFactory.getLogger(ApplicationListSelectionPanel.class.getName());
    private final Preferences _preferences;

    public ApplicationListSelectionView() {
        super();
        _preferences = Preferences.getInstance();
    }

    public void init() {
        update(null, null);
        _preferences.addObserver(this);
    }

    @Override
    public void update(Observable observable, Object parameter) {

        List<String> selectedApplicationList = _preferences.getSelectedApplicationNames();
        _logger.debug("Preferenced list of selected applications updated : {}", selectedApplicationList);

        if (selectedApplicationList != null) {
            setCheckedApplicationNames(selectedApplicationList);
        }
    }

    @Override
    protected void checkedApplicationChanged(List<String> checkedApplicationList) {
        _logger.debug("New list of SELECTED applications received : {}", checkedApplicationList);

        saveStringListPreference(PreferenceKey.SELECTED_APPLICATION_LIST, checkedApplicationList);
    }

    @Override
    protected boolean isApplicationBetaJnlpUrlInUse(String applicationName) {
        return _preferences.isApplicationReleaseBeta(applicationName);
    }

    @Override
    protected void betaApplicationChanged(List<String> betaApplicationList) {
        _logger.debug("New list of BETA applications received : {}", betaApplicationList);

        saveStringListPreference(PreferenceKey.BETA_APPLICATION_LIST, betaApplicationList);
    }

    private void saveStringListPreference(PreferenceKey preference, List<String> stringList) {

        if (stringList == null) {
            return;
        }

        if (stringList.size() == 0) {
            return;
        }

        try {
            _preferences.setPreference(preference, stringList);
        } catch (PreferencesException ex) {
            _logger.error("PreferencesException :", ex);
        }
    }
}
