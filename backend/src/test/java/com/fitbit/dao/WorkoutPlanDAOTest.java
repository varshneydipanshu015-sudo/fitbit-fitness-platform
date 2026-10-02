package com.fitbit.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fitbit.model.FitnessGoal;
import com.fitbit.model.WorkoutPlan;
import org.junit.jupiter.api.Test;

class WorkoutPlanDAOTest {
    private final WorkoutPlanDAO workoutPlanDAO = new WorkoutPlanDAO();

    @Test
    void rejectsInvalidTrainerIdBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workoutPlanDAO.findByTrainerId(0)
        );

        assertEquals("Trainer ID must be at least 1.", exception.getMessage());
    }

    @Test
    void rejectsMissingWorkoutPlanBeforeOpeningDatabaseConnection() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workoutPlanDAO.create(null)
        );

        assertEquals("Workout plan is required.", exception.getMessage());
    }

    @Test
    void requiresAtLeastOneExerciseBeforeOpeningDatabaseConnection() {
        WorkoutPlan plan = new WorkoutPlan(
                0,
                1,
                "Starter",
                null,
                FitnessGoal.GENERAL_FITNESS
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workoutPlanDAO.create(plan)
        );

        assertEquals("A plan must contain between 1 and 30 exercises.", exception.getMessage());
    }
}
