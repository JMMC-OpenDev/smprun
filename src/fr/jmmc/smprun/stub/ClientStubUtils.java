/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub;

/**
 * Stupid utility class to manage special Samp meta data key 
 * 'fr.jmmc.applauncher.<clientName>' in order to distinguish client stub and real application
 * @author bourgesl
 */
public final class ClientStubUtils {

    /** app launcher prefix for custom samp metadata */
    public final static String APP_LAUNCHER_PREFIX = "fr.jmmc.applauncher.";
    /** special value for our client stubs */
    public final static String TOKEN_STUB = "STUB";
    
    /**
     * Forbidden constructor
     */
    private ClientStubUtils() {
        super();
    }
    
    /**
     * Return the Samp meta data key 'fr.jmmc.applauncher.<clientName>'
     * @param name client name
     * @return 'fr.jmmc.applauncher.<clientName>'
     */
    public static String getClientStubKey(final String name) {
        return APP_LAUNCHER_PREFIX + name;
    }
}
