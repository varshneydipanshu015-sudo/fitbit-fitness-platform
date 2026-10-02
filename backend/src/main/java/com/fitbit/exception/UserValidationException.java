package com.fitbit.exception;

/** Signals invalid account data that can be safely reported to the requesting user. */
public class UserValidationException extends Exception {
    private static final long serialVersionUID = 1L;

    public UserValidationException(String message) {
        super(message);
    }
}
