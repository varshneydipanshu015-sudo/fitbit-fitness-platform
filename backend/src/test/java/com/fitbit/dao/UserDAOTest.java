package com.fitbit.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fitbit.model.FitnessGoal;
import com.fitbit.model.FitnessLevel;
import com.fitbit.model.Trainer;
import org.junit.jupiter.api.Test;

class UserDAOTest {
    private final UserDAO userDAO = new UserDAO();

    @Test
    void rejectsInvalidUserIdBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDAO.findFitnessUserById(0)
        );

        assertEquals("User ID must be at least 1.", exception.getMessage());
    }

    @Test
    void rejectsInvalidUserIdBeforeUpdatingFitnessPreferences() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDAO.updateFitnessPreferences(
                        0,
                        FitnessGoal.GENERAL_FITNESS,
                        FitnessLevel.BEGINNER
                )
        );

        assertEquals("User ID must be at least 1.", exception.getMessage());
    }

    @Test
    void rejectsMissingFitnessPreferencesBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDAO.updateFitnessPreferences(1, null, FitnessLevel.BEGINNER)
        );

        assertEquals("Fitness goal and level are required.", exception.getMessage());
    }

    @Test
    void rejectsMissingTrainerBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDAO.createTrainer(null)
        );

        assertEquals("Trainer is required.", exception.getMessage());
    }

    @Test
    void rejectsTrainerBioOverLimitBeforeOpeningDatabaseConnection() {
        Trainer trainer = new Trainer(
                0,
                "Trainer",
                "trainer@example.com",
                "password-hash",
                "a".repeat(5001),
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDAO.createTrainer(trainer)
        );

        assertEquals("Trainer bio is too long.", exception.getMessage());
    }
}
