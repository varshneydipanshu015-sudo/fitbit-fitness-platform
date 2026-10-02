package com.fitbit.model;

import java.util.ArrayList;
import java.util.List;

/** A trainer-authored routine containing an ordered set of exercise prescriptions. */
public class WorkoutPlan {
    private final int planId;
    private final int trainerId;
    private final String trainerName;
    private final String planName;
    private final String description;
    private final FitnessGoal fitnessGoal;
    private final List<PlanExercise> exercises;

    public WorkoutPlan(
            int planId,
            int trainerId,
            String planName,
            String description,
            FitnessGoal fitnessGoal
    ) {
        this(planId, trainerId, null, planName, description, fitnessGoal);
    }

    public WorkoutPlan(
            int planId,
            int trainerId,
            String trainerName,
            String planName,
            String description,
            FitnessGoal fitnessGoal
    ) {
        this.planId = planId;
        this.trainerId = trainerId;
        this.trainerName = trainerName;
        this.planName = planName;
        this.description = description;
        this.fitnessGoal = fitnessGoal;
        this.exercises = new ArrayList<>();
    }

    public void addExercise(PlanExercise exercise) {
        if (exercise == null) {
            throw new IllegalArgumentException("Plan exercise is required.");
        }
        exercises.add(exercise);
    }

    public int getPlanId() {
        return planId;
    }

    public int getTrainerId() {
        return trainerId;
    }

    public String getTrainerName() {
        return trainerName;
    }

    public String getPlanName() {
        return planName;
    }

    public String getDescription() {
        return description;
    }

    public FitnessGoal getFitnessGoal() {
        return fitnessGoal;
    }

    public List<PlanExercise> getExercises() {
        return List.copyOf(exercises);
    }
}
