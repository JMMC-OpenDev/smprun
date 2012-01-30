/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.preference;

import fr.jmmc.jmcs.data.preference.PreferencesException;
import java.util.logging.Logger;

/**
 * Manage AppLauncher user's default values.
 * @author Sylvain LAFRASSE
 */
public class Preferences extends fr.jmmc.jmcs.data.preference.Preferences {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(Preferences.class.getName());
    /** Singleton instance */
    private static Preferences _instance = null;

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
        setDefaultPreference(PreferenceKey.FIRST_START_FLAG, "true");
    }
}
