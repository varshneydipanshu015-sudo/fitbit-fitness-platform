package com.fitbit.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProgressDAOTest {
    private final ProgressDAO progressDAO = new ProgressDAO();

    @Test
    void rejectsInvalidUserBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> progressDAO.findByUserId(0)
        );

        assertEquals("User ID must be at least 1.", exception.getMessage());
    }

    @Test
    void rejectsMissingWeightBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> progressDAO.create(1, null, null)
        );

        assertEquals("Weight must be greater than 0 and at most 999.99 kg.",
                exception.getMessage());
    }

    @Test
    void rejectsWeightWithTooManyDecimalPlacesBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> progressDAO.create(1, new BigDecimal("70.123"), null)
        );

        assertEquals("Weight must have no more than 2 decimal places.",
                exception.getMessage());
    }

    @Test
    void rejectsNotesLongerThanTheDatabaseColumnBeforeOpeningConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> progressDAO.create(1, new BigDecimal("70.25"), "x".repeat(256))
        );

        assertEquals("Notes must be 255 characters or fewer.", exception.getMessage());
    }
}
