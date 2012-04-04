/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.preference;

import fr.jmmc.jmcs.data.preference.MissingPreferenceException;
import fr.jmmc.jmcs.data.preference.PreferencesException;
import fr.jmmc.smprsc.data.list.ApplicationListSelectionPanel;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import org.ivoa.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lafrasse
 */
public class ApplicationListSelectionView extends ApplicationListSelectionPanel implements Observer {

    /** Logger - get from given class name */
    private static final Logger _logger = LoggerFactory.getLogger(ApplicationListSelectionPanel.class.getName());
    private final Preferences preferences;

    public ApplicationListSelectionView() {
        super();
        preferences = Preferences.getInstance();
        update(null, null);
    }

    @Override
    public void update(Observable o, Object o1) {

        System.out.println("ApplicationListSelectionView::Reading selected applications.");

        try {
            setCheckedApplicationNames(preferences.getPreferenceAsStringList(PreferenceKey.SELECTED_APPLICATION_LIST));
        } catch (MissingPreferenceException ex) {
            _logger.error("MissingPreferenceException :", ex);
        } catch (PreferencesException ex) {
            _logger.error("PreferencesException :", ex);
        }
    }

    @Override
    protected void checkedApplicationChanged(List<String> checkedApplicationList) {

        if (checkedApplicationList == null){
            return;
        }

        System.out.println("ApplicationListSelectionView::Writing selected applications : " + CollectionUtils.toString(checkedApplicationList, ", ", "{", "}") + ".");

        try {
            preferences.setPreference(PreferenceKey.SELECTED_APPLICATION_LIST, checkedApplicationList);
        } catch (PreferencesException ex) {
            _logger.error("PreferencesException :", ex);
        }
    }
}
