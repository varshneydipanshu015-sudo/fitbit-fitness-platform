package com.fitbit.service;

import com.fitbit.model.FitnessGoal;
import com.fitbit.model.FitnessLevel;
import java.util.List;

public interface RecommendationService {
    List<String> recommend(FitnessGoal goal, FitnessLevel level);
}
