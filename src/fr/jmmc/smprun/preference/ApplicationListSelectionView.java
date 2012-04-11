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
        update(null, null);
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

        _logger.debug("New list of selected applications received : {}", checkedApplicationList);

        if (checkedApplicationList == null) {
            return;
        }

        if (checkedApplicationList.size() == 0) {
            return;
        }

        try {
            _preferences.setPreference(PreferenceKey.SELECTED_APPLICATION_LIST, checkedApplicationList);
        } catch (PreferencesException ex) {
            _logger.error("PreferencesException :", ex);
        }
    }
}
