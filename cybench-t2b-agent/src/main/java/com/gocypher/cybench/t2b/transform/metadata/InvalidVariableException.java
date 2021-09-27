package com.gocypher.cybench.t2b.transform.metadata;

public class InvalidVariableException extends RuntimeException {
    private static final long serialVersionUID = 5798517198617338782L;

    public InvalidVariableException(String message) {
        super(message);
    }

    public InvalidVariableException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidVariableException(Throwable cause) {
        super(cause);
    }
}
