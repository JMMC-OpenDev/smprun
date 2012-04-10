/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.preference;

/**
 * Enumeration of all AppLauncher preference keys.
 * @author Sylvain LAFRASSE
 */
public enum PreferenceKey {

        FIRST_START_FLAG("first.start.flag"),
        SHOW_DOCK_WINDOW("show.dock.window"),
        START_ALL_STUBS("start.all.stubs"),
        SHOW_EXIT_WARNING("show.exit.warning"),
        SELECTED_APPLICATION_LIST("selected.application.list");
    
    /** the preferenced value identifying token */
    private final String _key;

    /**
     * Constructor
     * @param key the preferenced value identifying token
     */
    PreferenceKey(String key) {
        _key = key;
    }

    /**
     * @return the preferenced value identifying token
     */
    @Override
    public String toString() {
        return _key;
    }

    /**
     * For unit testing purpose only.
     * @param args
     */
    public static void main(String[] args) {
        for (PreferenceKey k : PreferenceKey.values()) {
            System.out.println("Key '" + k.name() + "' = ['" + k + "'].");
        }
    }
}
