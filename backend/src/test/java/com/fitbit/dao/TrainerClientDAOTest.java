package com.fitbit.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TrainerClientDAOTest {
    private final TrainerClientDAO trainerClientDAO = new TrainerClientDAO();

    @Test
    void rejectsInvalidTrainerIdBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> trainerClientDAO.findClientsByTrainerId(0)
        );

        assertEquals("Trainer ID must be at least 1.", exception.getMessage());
    }

    @Test
    void rejectsMissingClientEmailBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> trainerClientDAO.assignClient(1, " ")
        );

        assertEquals("Client email is required.", exception.getMessage());
    }

    @Test
    void rejectsInvalidTrainerIdBeforeAssigningClient() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> trainerClientDAO.assignClient(0, "client@example.com")
        );

        assertEquals("Trainer ID must be at least 1.", exception.getMessage());
    }
}
