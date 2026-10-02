package com.fitbit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class WorkoutPlanModelTest {
    @Test
    void storesOrderedTypedExercisesWithPlanSpecificDetails() {
        Exercise squat = new Exercise(
                1,
                "Bodyweight Squat",
                "Squat using body weight.",
                "Legs",
                FitnessLevel.BEGINNER,
                "None"
        );
        Exercise pushUp = new Exercise(
                2,
                "Push-up",
                "Push up from the floor.",
                "Chest",
                FitnessLevel.BEGINNER,
                "None"
        );
        WorkoutPlan plan = new WorkoutPlan(
                10,
                4,
                "Beginner Strength",
                "A simple full-body plan.",
                FitnessGoal.STRENGTH
        );

        plan.addExercise(new PlanExercise(squat, 1, 3, 10, 60));
        plan.addExercise(new PlanExercise(pushUp, 2, 3, 8, 45));

        List<PlanExercise> planExercises = plan.getExercises();
        assertEquals(2, planExercises.size());
        assertEquals("Bodyweight Squat", planExercises.get(0).getExercise().getExerciseName());
        assertEquals(3, planExercises.get(0).getSetsCount());
        assertEquals("Push-up", planExercises.get(1).getExercise().getExerciseName());
        assertEquals(2, planExercises.get(1).getExerciseOrder());
    }

    @Test
    void doesNotAllowCallersToChangeThePlansExerciseList() {
        WorkoutPlan plan = new WorkoutPlan(
                10,
                4,
                "Beginner Strength",
                null,
                FitnessGoal.STRENGTH
        );

        assertThrows(UnsupportedOperationException.class, () -> plan.getExercises().clear());
    }

    @Test
    void rejectsInvalidPlanExerciseDetails() {
        Exercise squat = new Exercise(
                1,
                "Bodyweight Squat",
                null,
                "Legs",
                FitnessLevel.BEGINNER,
                "None"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PlanExercise(squat, 0, 3, 10, 60)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlanExercise(squat, 1, 0, 10, 60)
        );
    }
}
