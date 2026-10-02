package com.fitbit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UserModelTest {
    @Test
    void fitnessUserHasUserRoleAndFitnessDetails() {
        FitnessUser fitnessUser = new FitnessUser(
                1,
                "Asha Rao",
                "asha@example.com",
                "test-hash",
                FitnessGoal.MUSCLE_GAIN,
                FitnessLevel.BEGINNER
        );
        User user = fitnessUser;

        assertEquals("USER", user.getRole());
        assertEquals("Asha Rao", user.getFullName());
        assertEquals(FitnessGoal.MUSCLE_GAIN, fitnessUser.getFitnessGoal());
        assertEquals(FitnessLevel.BEGINNER, fitnessUser.getFitnessLevel());
    }

    @Test
    void trainerHasTrainerRoleAndProfileDetails() {
        Trainer trainer = new Trainer(
                2,
                "Dev Mehta",
                "dev@example.com",
                "test-hash",
                "Strength coach",
                "Strength training"
        );
        User user = trainer;

        assertEquals("TRAINER", user.getRole());
        assertEquals("Strength training", trainer.getSpecialization());
    }
}
