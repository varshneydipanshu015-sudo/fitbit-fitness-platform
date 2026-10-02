package com.fitbit.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WorkoutSessionDAOTest {
    private final WorkoutSessionDAO workoutSessionDAO = new WorkoutSessionDAO();

    @Test
    void rejectsInvalidUserBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workoutSessionDAO.findByUserId(0)
        );

        assertEquals("User ID must be at least 1.", exception.getMessage());
    }

    @Test
    void rejectsInvalidWorkoutBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workoutSessionDAO.completeWorkout(0, 1)
        );

        assertEquals("Workout ID must be at least 1.", exception.getMessage());
    }

    @Test
    void rejectsInvalidPlanBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workoutSessionDAO.startWorkout(1, 0, null)
        );

        assertEquals("Plan ID must be at least 1.", exception.getMessage());
    }
}
