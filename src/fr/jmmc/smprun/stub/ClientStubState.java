/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun.stub;

/**
 * Enumeration of all the internal states a stub can go through.
 * @author Sylvain LAFRASSE
 */
public enum ClientStubState {

    INITIALIZING(0, ""),
    CONNECTING(0, ""),
    REGISTERING(0, ""),
    LISTENING(0, ""),
    PROCESSING(1, "Received a new message"),
    LAUNCHING(2, "Downloading the application"),
    SEEKING(3, "Starting the application"),
    FORWARDING(4, "Forwarding the message"),
    DISCONNECTING(5, "Cleaning up"),
    DIYING(6, "Done"),
    FAILING(7, "Failed to start the application");
    
    /** the numerical order of the internal progress (steps equal to zero don't trigger GUI updates) */
    private final int _step;
    /** the user displayable text to explain the internal state */
    private final String _message;

    /**
     * Constructor
     * @param step the numerical order of the internal progress (steps equal to zero don't trigger GUI updates)
     * @param message the user displayable text to explain the internal state
     */
    ClientStubState(int step, String message) {
        _step = step;
        _message = message;
    }

    /**
     * @return the internal numerical progression.
     */
    public int step() {
        return _step;
    }
     
    /**
     * Return true if this state is before the given state
     * @param state state to compare with
     * @return true if this state is before the given state 
     */
    public boolean before(final ClientStubState state) {
        return _step < state.step();
    }

    /**
     * @return the user displayable text to explain the internal state
     */
    public String message() {
        return _message;
    }

    /**
     * For unit testing purpose only.
     * @param args
     */
    public static void main(String[] args) {
        for (ClientStubState s : ClientStubState.values()) {
            System.out.println("State '" + s + "' = [" + s.step() + ", '" + s.message() + "'].");
        }
    }
}