package com.fitbit.service;

import com.fitbit.model.FitnessGoal;
import com.fitbit.model.FitnessLevel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BasicRecommendationService implements RecommendationService {
    private static final Map<FitnessGoal, List<String>> EXERCISES_BY_GOAL =
            createExercisesByGoal();
    private static final Map<FitnessLevel, Integer> EXERCISE_COUNT_BY_LEVEL =
            createExerciseCountByLevel();

    @Override
    public List<String> recommend(FitnessGoal goal, FitnessLevel level) {
        if (goal == null) {
            throw new IllegalArgumentException("Fitness goal is required.");
        }
        if (level == null) {
            throw new IllegalArgumentException("Fitness level is required.");
        }

        List<String> suitableExercises = EXERCISES_BY_GOAL.get(goal);
        int exerciseCount = EXERCISE_COUNT_BY_LEVEL.get(level);
        return List.copyOf(suitableExercises.subList(0, exerciseCount));
    }

    private static Map<FitnessGoal, List<String>> createExercisesByGoal() {
        Map<FitnessGoal, List<String>> exercises = new EnumMap<>(FitnessGoal.class);
        exercises.put(
                FitnessGoal.WEIGHT_LOSS,
                List.of(
                        "Brisk walking",
                        "Bodyweight squats",
                        "Step-ups",
                        "Mountain climbers",
                        "Jumping jacks"
                )
        );
        exercises.put(
                FitnessGoal.MUSCLE_GAIN,
                List.of(
                        "Push-ups",
                        "Bodyweight squats",
                        "Dumbbell rows",
                        "Lunges",
                        "Glute bridges"
                )
        );
        exercises.put(
                FitnessGoal.GENERAL_FITNESS,
                List.of(
                        "Brisk walking",
                        "Bodyweight squats",
                        "Push-ups",
                        "Plank",
                        "Step-ups"
                )
        );
        exercises.put(
                FitnessGoal.STRENGTH,
                List.of(
                        "Bodyweight squats",
                        "Push-ups",
                        "Dumbbell rows",
                        "Glute bridges",
                        "Reverse lunges"
                )
        );
        exercises.put(
                FitnessGoal.ENDURANCE,
                List.of(
                        "Brisk walking",
                        "Step-ups",
                        "Cycling",
                        "Jumping jacks",
                        "Mountain climbers"
                )
        );
        return Map.copyOf(exercises);
    }

    private static Map<FitnessLevel, Integer> createExerciseCountByLevel() {
        Map<FitnessLevel, Integer> exerciseCounts = new EnumMap<>(FitnessLevel.class);
        exerciseCounts.put(FitnessLevel.BEGINNER, 3);
        exerciseCounts.put(FitnessLevel.INTERMEDIATE, 4);
        exerciseCounts.put(FitnessLevel.ADVANCED, 5);
        return Map.copyOf(exerciseCounts);
    }
}
