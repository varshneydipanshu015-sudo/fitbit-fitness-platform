package com.fitbit.model;

public class Exercise {
    private int exerciseId;
    private String exerciseName;
    private String description;
    private String targetMuscle;
    private FitnessLevel difficulty;
    private String equipment;

    public Exercise(
            int exerciseId,
            String exerciseName,
            String description,
            String targetMuscle,
            FitnessLevel difficulty,
            String equipment
    ) {
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.description = description;
        this.targetMuscle = targetMuscle;
        this.difficulty = difficulty;
        this.equipment = equipment;
    }

    public int getExerciseId() {
        return exerciseId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public String getDescription() {
        return description;
    }

    public String getTargetMuscle() {
        return targetMuscle;
    }

    public FitnessLevel getDifficulty() {
        return difficulty;
    }

    public String getEquipment() {
        return equipment;
    }
}
