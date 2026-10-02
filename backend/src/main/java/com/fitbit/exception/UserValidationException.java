package com.fitbit.exception;

public class UserValidationException extends Exception {
    private static final long serialVersionUID = 1L;

    public UserValidationException(String message) {
        super(message);
    }
}
