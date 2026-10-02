package com.fitbit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fitbit.exception.UserValidationException;
import org.junit.jupiter.api.Test;

class UserValidatorTest {
    @Test
    void acceptsValidRegistrationDetails() {
        assertDoesNotThrow(
                () -> UserValidator.validateRegistration(
                        "Asha Rao",
                        "asha@example.com",
                        "strong-pass"
                )
        );
    }

    @Test
    void rejectsMissingName() {
        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> UserValidator.validateRegistration(
                        "  ",
                        "asha@example.com",
                        "strong-pass"
                )
        );

        assertEquals("Full name is required.", exception.getMessage());
    }

    @Test
    void rejectsInvalidEmail() {
        assertThrows(
                UserValidationException.class,
                () -> UserValidator.validateRegistration(
                        "Asha Rao",
                        "not-an-email",
                        "strong-pass"
                )
        );
    }

    @Test
    void rejectsShortPassword() {
        assertThrows(
                UserValidationException.class,
                () -> UserValidator.validateRegistration(
                        "Asha Rao",
                        "asha@example.com",
                        "short"
                )
        );
    }

    @Test
    void rejectsPasswordLongerThanBcryptLimitInUtf8Bytes() {
        assertThrows(
                UserValidationException.class,
                () -> UserValidator.validateRegistration(
                        "Asha Rao",
                        "asha@example.com",
                        "é".repeat(37)
                )
        );
    }
}
