package com.fitbit.model;

public class FitnessUser extends User {
    private FitnessGoal fitnessGoal;
    private FitnessLevel fitnessLevel;

    public FitnessUser(
            int userId,
            String fullName,
            String email,
            String passwordHash,
            FitnessGoal fitnessGoal,
            FitnessLevel fitnessLevel
    ) {
        super(userId, fullName, email, passwordHash);
        this.fitnessGoal = fitnessGoal;
        this.fitnessLevel = fitnessLevel;
    }

    @Override
    public String getRole() {
        return "USER";
    }

    public FitnessGoal getFitnessGoal() {
        return fitnessGoal;
    }

    public void setFitnessGoal(FitnessGoal fitnessGoal) {
        this.fitnessGoal = fitnessGoal;
    }

    public FitnessLevel getFitnessLevel() {
        return fitnessLevel;
    }

    public void setFitnessLevel(FitnessLevel fitnessLevel) {
        this.fitnessLevel = fitnessLevel;
    }
}
