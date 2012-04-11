/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.preference;

import fr.jmmc.jmcs.data.preference.MissingPreferenceException;
import fr.jmmc.jmcs.data.preference.PreferencesException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage AppLauncher user's default values.
 * @author Sylvain LAFRASSE
 */
public class Preferences extends fr.jmmc.jmcs.data.preference.Preferences {

    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger(Preferences.class.getName());
    /** Singleton instance */
    private static Preferences _instance = null;
    /** Default selected application list */
    private static final List<String> _defaultSelectedApplicationList = Arrays.asList("Aspro2", "SearchCal", "LITpro", "topcat", "Aladin");
    /** Constant to detect that no application is deselected */
    public List<String> ALL_APPLICATIONS_SELECTED = null;

    /**
     * @return the singleton instance.
     */
    public static final synchronized Preferences getInstance() {
        // DO NOT MODIFY !!!
        if (_instance == null) {
            _instance = new Preferences();
        }

        return _instance;

        // DO NOT MODIFY !!!
    }

    @Override
    protected String getPreferenceFilename() {
        return "fr.jmmc.applauncher.properties";
    }

    @Override
    protected int getPreferencesVersionNumber() {
        return 1;
    }

    @Override
    protected void setDefaultPreferences() throws PreferencesException {
        // By default always consider it is the first time ever AppLauncher is started
        setDefaultPreference(PreferenceKey.FIRST_START_FLAG, true);
        // By default always show dock window on startup
        setDefaultPreference(PreferenceKey.SHOW_DOCK_WINDOW, true);
        // By default always start all stubs
        setDefaultPreference(PreferenceKey.START_SELECTED_STUBS, false);
        // By default always show exit warning
        setDefaultPreference(PreferenceKey.SHOW_EXIT_WARNING, true);
        // By default always show JMMC and ESSENTIALS applications
        setDefaultPreference(PreferenceKey.SELECTED_APPLICATION_LIST, _defaultSelectedApplicationList);
    }

    public List<String> getSelectedApplicationNames() {

        List<String> selectedApplicationList = ALL_APPLICATIONS_SELECTED;

        try {
            selectedApplicationList = getPreferenceAsStringList(PreferenceKey.SELECTED_APPLICATION_LIST);
        } catch (MissingPreferenceException ex) {
            _logger.error("MissingPreferenceException :", ex);
        } catch (PreferencesException ex) {
            _logger.error("PreferencesException :", ex);
        }

        return selectedApplicationList;
    }

    public boolean isApplicationNameSelected(String applicationName) {
        List<String> selectedApplicationNameList = getSelectedApplicationNames();
        if ((selectedApplicationNameList == ALL_APPLICATIONS_SELECTED) || (selectedApplicationNameList.contains(applicationName))) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) {

        final Preferences prefs = Preferences.getInstance();

        String currentPrefs = prefs.dumpCurrentProperties();
        System.out.println("---------------\n" + "Current Preferences Dump :\n" + currentPrefs + "\n---------------");

        try {
            List<String> list = prefs.getPreferenceAsStringList(PreferenceKey.SELECTED_APPLICATION_LIST);
            System.out.println("Selected Application List : " + list + "\n---------------");
        } catch (MissingPreferenceException ex) {
            System.out.println("MissingPreferenceException = " + ex);
        } catch (PreferencesException ex) {
            System.out.println("PreferencesException = " + ex);
        }
    }
}
