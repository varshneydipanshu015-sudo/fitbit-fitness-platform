package com.fitbit.dao;

import com.fitbit.model.Exercise;
import com.fitbit.model.FitnessGoal;
import com.fitbit.model.FitnessLevel;
import com.fitbit.model.PlanExercise;
import com.fitbit.model.WorkoutPlan;
import com.fitbit.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Creates plans atomically and reads each plan with its ordered exercise details. */
public class WorkoutPlanDAO {
    private static final int MAX_EXERCISES_PER_PLAN = 30;
    private static final String INSERT_PLAN_SQL = """
            INSERT INTO workout_plans (trainer_id, plan_name, description, fitness_goal)
            VALUES (?, ?, ?, ?)
            """;
    private static final String INSERT_PLAN_EXERCISE_SQL = """
            INSERT INTO workout_plan_exercises
                (plan_id, exercise_id, exercise_order, sets_count, reps_count, rest_seconds)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND_PLANS_SQL = """
            SELECT p.plan_id, p.trainer_id, p.plan_name, p.description, p.fitness_goal,
                   u.full_name AS trainer_name,
                   e.exercise_id, e.exercise_name, e.description AS exercise_description,
                   e.target_muscle, e.difficulty, e.equipment,
                   pe.exercise_order, pe.sets_count, pe.reps_count, pe.rest_seconds
            FROM workout_plans p
            JOIN users u ON u.user_id = p.trainer_id
            JOIN workout_plan_exercises pe ON pe.plan_id = p.plan_id
            JOIN exercises e ON e.exercise_id = pe.exercise_id
            %s
            ORDER BY p.plan_id, pe.exercise_order
            """;
    private static final String FIND_BY_TRAINER_SQL =
            FIND_PLANS_SQL.formatted("WHERE p.trainer_id = ?");
    private static final String FIND_ALL_SQL = FIND_PLANS_SQL.formatted("");

    public int create(WorkoutPlan plan) throws SQLException {
        validatePlan(plan);
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Save the plan and all of its exercises as one transaction.
                int planId;
                try (PreparedStatement statement = connection.prepareStatement(
                        INSERT_PLAN_SQL,
                        Statement.RETURN_GENERATED_KEYS
                )) {
                    statement.setInt(1, plan.getTrainerId());
                    statement.setString(2, plan.getPlanName().trim());
                    statement.setString(3, plan.getDescription());
                    statement.setString(4, plan.getFitnessGoal().name());
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (!generatedKeys.next()) {
                            throw new SQLException("Creating the plan did not return an ID.");
                        }
                        planId = generatedKeys.getInt(1);
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(INSERT_PLAN_EXERCISE_SQL)) {
                    for (PlanExercise planExercise : plan.getExercises()) {
                        statement.setInt(1, planId);
                        statement.setInt(2, planExercise.getExercise().getExerciseId());
                        statement.setInt(3, planExercise.getExerciseOrder());
                        statement.setInt(4, planExercise.getSetsCount());
                        statement.setInt(5, planExercise.getRepsCount());
                        statement.setInt(6, planExercise.getRestSeconds());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
                return planId;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<WorkoutPlan> findByTrainerId(int trainerId) throws SQLException {
        if (trainerId < 1) {
            throw new IllegalArgumentException("Trainer ID must be at least 1.");
        }

        return findPlans(FIND_BY_TRAINER_SQL, trainerId);
    }

    public List<WorkoutPlan> findAll() throws SQLException {
        return findPlans(FIND_ALL_SQL, null);
    }

    private List<WorkoutPlan> findPlans(String sql, Integer trainerId) throws SQLException {
        // Group joined exercise rows under their parent plan while preserving SQL ordering.
        Map<Integer, WorkoutPlan> plans = new LinkedHashMap<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (trainerId != null) {
                statement.setInt(1, trainerId);
            }
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    int planId = results.getInt("plan_id");
                    WorkoutPlan plan = plans.get(planId);
                    if (plan == null) {
                        plan = new WorkoutPlan(
                                planId,
                                results.getInt("trainer_id"),
                                results.getString("trainer_name"),
                                results.getString("plan_name"),
                                results.getString("description"),
                                FitnessGoal.valueOf(results.getString("fitness_goal"))
                        );
                        plans.put(planId, plan);
                    }
                    plan.addExercise(new PlanExercise(
                            new Exercise(
                                    results.getInt("exercise_id"),
                                    results.getString("exercise_name"),
                                    results.getString("exercise_description"),
                                    results.getString("target_muscle"),
                                    FitnessLevel.valueOf(results.getString("difficulty")),
                                    results.getString("equipment")
                            ),
                            results.getInt("exercise_order"),
                            results.getInt("sets_count"),
                            results.getInt("reps_count"),
                            results.getInt("rest_seconds")
                    ));
                }
            }
        }
        return new ArrayList<>(plans.values());
    }

    private void validatePlan(WorkoutPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("Workout plan is required.");
        }
        if (plan.getTrainerId() < 1) {
            throw new IllegalArgumentException("Trainer ID must be at least 1.");
        }
        if (plan.getPlanName() == null || plan.getPlanName().isBlank()) {
            throw new IllegalArgumentException("Plan name is required.");
        }
        if (plan.getPlanName().trim().length() > 120) {
            throw new IllegalArgumentException("Plan name must be 120 characters or fewer.");
        }
        if (plan.getDescription() != null && plan.getDescription().length() > 5000) {
            throw new IllegalArgumentException("Plan description must be 5000 characters or fewer.");
        }
        if (plan.getFitnessGoal() == null) {
            throw new IllegalArgumentException("Fitness goal is required.");
        }
        List<PlanExercise> exercises = plan.getExercises();
        if (exercises.isEmpty() || exercises.size() > MAX_EXERCISES_PER_PLAN) {
            throw new IllegalArgumentException(
                    "A plan must contain between 1 and 30 exercises."
            );
        }
        Set<Integer> exerciseIds = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        for (PlanExercise exercise : exercises) {
            if (exercise.getExercise().getExerciseId() < 1
                    || !exerciseIds.add(exercise.getExercise().getExerciseId())) {
                throw new IllegalArgumentException(
                        "Each plan exercise must reference a unique saved exercise."
                );
            }
            if (!orders.add(exercise.getExerciseOrder())) {
                throw new IllegalArgumentException("Exercise order values must be unique.");
            }
            if (exercise.getSetsCount() > 255 || exercise.getRepsCount() > 65535
                    || exercise.getRestSeconds() > 65535) {
                throw new IllegalArgumentException("Exercise set, rep, or rest values are too large.");
            }
        }
    }

}
