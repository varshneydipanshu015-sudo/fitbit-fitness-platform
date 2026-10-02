package com.fitbit.model;

/** Connects a saved exercise to a plan with its order and prescription. */
public class PlanExercise {
    private final Exercise exercise;
    private final int exerciseOrder;
    private final int setsCount;
    private final int repsCount;
    private final int restSeconds;

    public PlanExercise(
            Exercise exercise,
            int exerciseOrder,
            int setsCount,
            int repsCount,
            int restSeconds
    ) {
        // Validate here as well as at the API boundary so all model callers stay safe.
        if (exercise == null) {
            throw new IllegalArgumentException("Exercise is required.");
        }
        if (exerciseOrder < 1) {
            throw new IllegalArgumentException("Exercise order must be at least 1.");
        }
        if (setsCount < 1) {
            throw new IllegalArgumentException("Sets must be at least 1.");
        }
        if (repsCount < 1) {
            throw new IllegalArgumentException("Reps must be at least 1.");
        }
        if (restSeconds < 0) {
            throw new IllegalArgumentException("Rest seconds cannot be negative.");
        }

        this.exercise = exercise;
        this.exerciseOrder = exerciseOrder;
        this.setsCount = setsCount;
        this.repsCount = repsCount;
        this.restSeconds = restSeconds;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public int getExerciseOrder() {
        return exerciseOrder;
    }

    public int getSetsCount() {
        return setsCount;
    }

    public int getRepsCount() {
        return repsCount;
    }

    public int getRestSeconds() {
        return restSeconds;
    }
}
