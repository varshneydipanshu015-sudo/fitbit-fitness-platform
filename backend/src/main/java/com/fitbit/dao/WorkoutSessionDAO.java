package com.fitbit.dao;

import com.fitbit.model.WorkoutSession;
import com.fitbit.model.WorkoutStatus;
import com.fitbit.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Persists workout sessions and scopes history/completion to the owning user. */
public class WorkoutSessionDAO {
    private static final String INSERT_SQL = """
            INSERT INTO user_workouts (user_id, plan_id, notes)
            VALUES (?, ?, ?)
            """;
    private static final String COMPLETE_SQL = """
            UPDATE user_workouts
            SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP
            WHERE workout_id = ? AND user_id = ? AND status = 'IN_PROGRESS'
            """;
    private static final String FIND_BY_USER_SQL = """
            SELECT workout_id, user_id, plan_id, started_at, completed_at, status, notes
            FROM user_workouts
            WHERE user_id = ?
            ORDER BY started_at DESC, workout_id DESC
            """;

    public int startWorkout(int userId, Integer planId, String notes) throws SQLException {
        validateUserId(userId);
        if (planId != null && planId < 1) {
            throw new IllegalArgumentException("Plan ID must be at least 1.");
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            statement.setInt(1, userId);
            if (planId == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, planId);
            }
            statement.setString(3, notes);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
                throw new SQLException("Starting the workout did not return a generated ID.");
            }
        }
    }

    public boolean completeWorkout(int workoutId, int userId) throws SQLException {
        validateWorkoutId(workoutId);
        validateUserId(userId);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(COMPLETE_SQL)) {
            statement.setInt(1, workoutId);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public List<WorkoutSession> findByUserId(int userId) throws SQLException {
        validateUserId(userId);
        List<WorkoutSession> sessions = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_USER_SQL)) {
            statement.setInt(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    sessions.add(mapSession(results));
                }
            }
        }

        return sessions;
    }

    private WorkoutSession mapSession(ResultSet results) throws SQLException {
        var startedAt = results.getTimestamp("started_at").toLocalDateTime();
        var completedTimestamp = results.getTimestamp("completed_at");
        var completedAt = completedTimestamp == null
                ? null
                : completedTimestamp.toLocalDateTime();
        int rawPlanId = results.getInt("plan_id");
        // JDBC returns zero for SQL NULL when reading an int; wasNull preserves optional plans.
        Integer planId = results.wasNull() ? null : rawPlanId;

        return new WorkoutSession(
                results.getInt("workout_id"),
                results.getInt("user_id"),
                planId,
                startedAt,
                completedAt,
                WorkoutStatus.valueOf(results.getString("status")),
                results.getString("notes")
        );
    }

    private void validateUserId(int userId) {
        if (userId < 1) {
            throw new IllegalArgumentException("User ID must be at least 1.");
        }
    }

    private void validateWorkoutId(int workoutId) {
        if (workoutId < 1) {
            throw new IllegalArgumentException("Workout ID must be at least 1.");
        }
    }
}
