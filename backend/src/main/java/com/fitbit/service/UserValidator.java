package com.fitbit.service;

import com.fitbit.exception.UserValidationException;
import java.nio.charset.StandardCharsets;

/** Central validation rules shared by fitness-user and trainer registration. */
public final class UserValidator {
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    private UserValidator() {
    }

    public static void validateRegistration(String fullName, String email, String password)
            throws UserValidationException {
        if (fullName == null || fullName.isBlank()) {
            throw new UserValidationException("Full name is required.");
        }
        if (fullName.trim().length() > MAX_NAME_LENGTH) {
            throw new UserValidationException("Full name must be 100 characters or fewer.");
        }
        if (email == null || email.isBlank()) {
            throw new UserValidationException("Email is required.");
        }

        String normalizedEmail = email.trim();
        if (normalizedEmail.length() > MAX_EMAIL_LENGTH
                || !normalizedEmail.matches(EMAIL_PATTERN)) {
            throw new UserValidationException("Enter a valid email address.");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new UserValidationException("Password must be at least 8 characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            // BCrypt processes at most 72 input bytes, so reject longer UTF-8 values explicitly.
            throw new UserValidationException(
                    "Password must be 72 UTF-8 bytes or fewer."
            );
        }
    }
}
