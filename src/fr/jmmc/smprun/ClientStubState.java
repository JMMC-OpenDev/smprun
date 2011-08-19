/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.smprun;

/**
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
    DISCONNECTING(5, "Cleaning up");
    private final int _step;
    private final String _message;

    ClientStubState(int step, String message) {
        _step = step;
        _message = message;
    }

    public int step() {
        return _step;
    }

    public String message() {
        return _message;
    }

    public static void main(String[] args) {
        for (ClientStubState s : ClientStubState.values()) {
            System.out.println("State '" + s + "' = [" + s.step() + ", '" + s.message() + "'].");
        }
    }
}