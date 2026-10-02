package com.fitbit.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fitbit.model.Exercise;
import com.fitbit.model.FitnessLevel;
import org.junit.jupiter.api.Test;

class ExerciseDAOTest {
    private final ExerciseDAO exerciseDAO = new ExerciseDAO();

    @Test
    void rejectsMissingExerciseBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> exerciseDAO.create(null)
        );

        assertEquals("Exercise is required.", exception.getMessage());
    }

    @Test
    void rejectsInvalidExerciseIdBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> exerciseDAO.findById(0)
        );

        assertEquals("Exercise ID must be at least 1.", exception.getMessage());
    }

    @Test
    void rejectsExerciseWithoutANameBeforeOpeningDatabaseConnection() {
        Exercise exercise = new Exercise(
                0,
                "  ",
                null,
                null,
                FitnessLevel.BEGINNER,
                "None"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> exerciseDAO.create(exercise)
        );

        assertEquals("Exercise name is required.", exception.getMessage());
    }
}
