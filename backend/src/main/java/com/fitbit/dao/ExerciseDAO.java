package com.fitbit.dao;

import com.fitbit.model.Exercise;
import com.fitbit.model.FitnessLevel;
import com.fitbit.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExerciseDAO {
    private static final String INSERT_SQL = """
            INSERT INTO exercises (exercise_name, description, target_muscle, difficulty, equipment)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String FIND_ALL_SQL = """
            SELECT exercise_id, exercise_name, description, target_muscle, difficulty, equipment
            FROM exercises
            ORDER BY exercise_id
            """;
    private static final String FIND_BY_ID_SQL = """
            SELECT exercise_id, exercise_name, description, target_muscle, difficulty, equipment
            FROM exercises
            WHERE exercise_id = ?
            """;
    private static final String UPDATE_SQL = """
            UPDATE exercises
            SET exercise_name = ?, description = ?, target_muscle = ?, difficulty = ?, equipment = ?
            WHERE exercise_id = ?
            """;
    private static final String DELETE_SQL = """
            DELETE FROM exercises
            WHERE exercise_id = ?
            """;

    public int create(Exercise exercise) throws SQLException {
        validateExercise(exercise);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            setExerciseFields(statement, exercise);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
                throw new SQLException("Creating the exercise did not return a generated ID.");
            }
        }
    }

    public List<Exercise> findAll() throws SQLException {
        List<Exercise> exercises = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                exercises.add(mapExercise(results));
            }
        }

        return exercises;
    }

    public Optional<Exercise> findById(int exerciseId) throws SQLException {
        validateId(exerciseId);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, exerciseId);

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return Optional.of(mapExercise(results));
                }
                return Optional.empty();
            }
        }
    }

    public boolean update(int exerciseId, Exercise exercise) throws SQLException {
        validateId(exerciseId);
        validateExercise(exercise);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            setExerciseFields(statement, exercise);
            statement.setInt(6, exerciseId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(int exerciseId) throws SQLException {
        validateId(exerciseId);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, exerciseId);
            return statement.executeUpdate() == 1;
        }
    }

    private void setExerciseFields(PreparedStatement statement, Exercise exercise)
            throws SQLException {
        statement.setString(1, exercise.getExerciseName().trim());
        statement.setString(2, exercise.getDescription());
        statement.setString(3, exercise.getTargetMuscle());
        statement.setString(4, exercise.getDifficulty().name());
        statement.setString(5, exercise.getEquipment().trim());
    }

    private Exercise mapExercise(ResultSet results) throws SQLException {
        return new Exercise(
                results.getInt("exercise_id"),
                results.getString("exercise_name"),
                results.getString("description"),
                results.getString("target_muscle"),
                FitnessLevel.valueOf(results.getString("difficulty")),
                results.getString("equipment")
        );
    }

    private void validateExercise(Exercise exercise) {
        if (exercise == null) {
            throw new IllegalArgumentException("Exercise is required.");
        }
        if (exercise.getExerciseName() == null || exercise.getExerciseName().isBlank()) {
            throw new IllegalArgumentException("Exercise name is required.");
        }
        if (exercise.getExerciseName().trim().length() > 120) {
            throw new IllegalArgumentException("Exercise name must be 120 characters or fewer.");
        }
        if (exercise.getTargetMuscle() != null && exercise.getTargetMuscle().length() > 100) {
            throw new IllegalArgumentException("Target muscle must be 100 characters or fewer.");
        }
        if (exercise.getDifficulty() == null) {
            throw new IllegalArgumentException("Exercise difficulty is required.");
        }
        if (exercise.getEquipment() == null || exercise.getEquipment().isBlank()) {
            throw new IllegalArgumentException("Exercise equipment is required.");
        }
        if (exercise.getEquipment().trim().length() > 120) {
            throw new IllegalArgumentException("Equipment must be 120 characters or fewer.");
        }
    }

    private void validateId(int exerciseId) {
        if (exerciseId < 1) {
            throw new IllegalArgumentException("Exercise ID must be at least 1.");
        }
    }
}
