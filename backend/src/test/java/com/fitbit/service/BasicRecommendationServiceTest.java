package com.fitbit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fitbit.model.FitnessGoal;
import com.fitbit.model.FitnessLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

class BasicRecommendationServiceTest {
    private final RecommendationService recommendationService =
            new BasicRecommendationService();

    @Test
    void returnsGoalSpecificRecommendationsForEveryGoal() {
        for (FitnessGoal goal : FitnessGoal.values()) {
            List<String> recommendations = recommendationService.recommend(
                    goal,
                    FitnessLevel.BEGINNER
            );

            assertEquals(3, recommendations.size());
            assertTrue(!recommendations.isEmpty());
        }

        assertFalse(
                recommendationService.recommend(
                        FitnessGoal.MUSCLE_GAIN,
                        FitnessLevel.BEGINNER
                ).contains("Lunges")
        );
        assertTrue(
                recommendationService.recommend(
                        FitnessGoal.MUSCLE_GAIN,
                        FitnessLevel.INTERMEDIATE
                ).contains("Lunges")
        );
    }

    @Test
    void increasesRecommendationCountWithFitnessLevel() {
        assertEquals(
                3,
                recommendationService.recommend(
                        FitnessGoal.GENERAL_FITNESS,
                        FitnessLevel.BEGINNER
                ).size()
        );
        assertEquals(
                4,
                recommendationService.recommend(
                        FitnessGoal.GENERAL_FITNESS,
                        FitnessLevel.INTERMEDIATE
                ).size()
        );
        assertEquals(
                5,
                recommendationService.recommend(
                        FitnessGoal.GENERAL_FITNESS,
                        FitnessLevel.ADVANCED
                ).size()
        );
    }

    @Test
    void returnsAnUnmodifiableRecommendationList() {
        List<String> recommendations = recommendationService.recommend(
                FitnessGoal.STRENGTH,
                FitnessLevel.BEGINNER
        );

        assertThrows(UnsupportedOperationException.class, recommendations::clear);
    }

    @Test
    void rejectsMissingGoalOrLevel() {
        assertThrows(
                IllegalArgumentException.class,
                () -> recommendationService.recommend(null, FitnessLevel.BEGINNER)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> recommendationService.recommend(FitnessGoal.STRENGTH, null)
        );
    }
}
