package com.fitbit.model;

public class Trainer extends User {
    private String bio;
    private String specialization;

    public Trainer(
            int userId,
            String fullName,
            String email,
            String passwordHash,
            String bio,
            String specialization
    ) {
        super(userId, fullName, email, passwordHash);
        this.bio = bio;
        this.specialization = specialization;
    }

    @Override
    public String getRole() {
        return "TRAINER";
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }
}
