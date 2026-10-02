package com.fitbit.service;

import com.fitbit.model.FitnessGoal;
import com.fitbit.model.FitnessLevel;
import java.util.List;

/** Contract for selecting exercises from a user's goal and fitness level. */
public interface RecommendationService {
    List<String> recommend(FitnessGoal goal, FitnessLevel level);
}
