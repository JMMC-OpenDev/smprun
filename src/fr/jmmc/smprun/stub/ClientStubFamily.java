/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub;

/**
 * Enumeration of all the different stub families.
 * @author Sylvain LAFRASSE
 */
public enum ClientStubFamily {

    JMMC("Interferometry"),
    GENERAL("Essentials");
    
    /** the user displayable text to explain the internal family */
    private final String _family;

    /**
     * Constructor
     * @param family the user displayable text to explain the internal family
     */
    ClientStubFamily(String family) {
        _family = family;
    }

    /**
     * @return the user displayable text to explain the internal family
     */
    public String family() {
        return _family;
    }

    /**
     * For unit testing purpose only.
     * @param args
     */
    public static void main(String[] args) {
        for (ClientStubFamily f : ClientStubFamily.values()) {
            System.out.println("Family '" + f + "' => '" + f.family() + "'.");
        }
    }
}