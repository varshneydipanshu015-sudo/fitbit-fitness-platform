package com.fitbit.dao;

import com.fitbit.model.FitnessGoal;
import com.fitbit.model.FitnessLevel;
import com.fitbit.model.FitnessUser;
import com.fitbit.model.Trainer;
import com.fitbit.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Optional;

/** Persists fitness-user and trainer accounts and their role-specific profiles. */
public class UserDAO {
    private static final String INSERT_USER_SQL = """
            INSERT INTO users (full_name, email, password_hash, role, fitness_goal, fitness_level)
            VALUES (?, ?, ?, 'USER', ?, ?)
            """;
    private static final String INSERT_TRAINER_SQL = """
            INSERT INTO users (full_name, email, password_hash, role)
            VALUES (?, ?, ?, 'TRAINER')
            """;
    private static final String INSERT_TRAINER_PROFILE_SQL = """
            INSERT INTO trainer_profiles (trainer_id, bio, specialization)
            VALUES (?, ?, ?)
            """;
    private static final String FIND_USER_BY_EMAIL_SQL = """
            SELECT user_id, full_name, email, password_hash, fitness_goal, fitness_level
            FROM users
            WHERE email = ? AND role = 'USER'
            """;
    private static final String FIND_USER_BY_ID_SQL = """
            SELECT user_id, full_name, email, password_hash, fitness_goal, fitness_level
            FROM users
            WHERE user_id = ? AND role = 'USER'
            """;
    private static final String FIND_TRAINER_BY_EMAIL_SQL = """
            SELECT u.user_id, u.full_name, u.email, u.password_hash,
                   tp.bio, tp.specialization
            FROM users u
            JOIN trainer_profiles tp ON tp.trainer_id = u.user_id
            WHERE u.email = ? AND u.role = 'TRAINER'
            """;
    private static final String FIND_TRAINER_BY_ID_SQL = """
            SELECT u.user_id, u.full_name, u.email, u.password_hash,
                   tp.bio, tp.specialization
            FROM users u
            JOIN trainer_profiles tp ON tp.trainer_id = u.user_id
            WHERE u.user_id = ? AND u.role = 'TRAINER'
            """;
    private static final String UPDATE_FITNESS_PREFERENCES_SQL = """
            UPDATE users
            SET fitness_goal = ?, fitness_level = ?
            WHERE user_id = ? AND role = 'USER'
            """;

    public int createFitnessUser(FitnessUser user) throws SQLException {
        if (user == null) {
            throw new IllegalArgumentException("Fitness user is required.");
        }
        if (user.getFitnessGoal() == null || user.getFitnessLevel() == null) {
            throw new IllegalArgumentException("Fitness goal and level are required.");
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_USER_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            statement.setString(1, user.getFullName().trim());
            statement.setString(2, normalizeEmail(user.getEmail()));
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getFitnessGoal().name());
            statement.setString(5, user.getFitnessLevel().name());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating the user did not return a generated ID.");
                }
                return generatedKeys.getInt(1);
            }
        }
    }

    public int createTrainer(Trainer trainer) throws SQLException {
        if (trainer == null) {
            throw new IllegalArgumentException("Trainer is required.");
        }
        validateTrainerFields(trainer);

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Both rows must exist together because trainer_profiles shares the user ID.
                int trainerId;
                try (PreparedStatement statement = connection.prepareStatement(
                        INSERT_TRAINER_SQL,
                        Statement.RETURN_GENERATED_KEYS
                )) {
                    statement.setString(1, trainer.getFullName().trim());
                    statement.setString(2, normalizeEmail(trainer.getEmail()));
                    statement.setString(3, trainer.getPasswordHash());
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (!generatedKeys.next()) {
                            throw new SQLException("Creating the trainer did not return an ID.");
                        }
                        trainerId = generatedKeys.getInt(1);
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(INSERT_TRAINER_PROFILE_SQL)) {
                    statement.setInt(1, trainerId);
                    statement.setString(2, trainer.getBio());
                    statement.setString(3, trainer.getSpecialization());
                    statement.executeUpdate();
                }
                connection.commit();
                return trainerId;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public Optional<FitnessUser> findFitnessUserByEmail(String email) throws SQLException {
        String normalizedEmail = normalizeEmail(email);
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_USER_BY_EMAIL_SQL)) {
            statement.setString(1, normalizedEmail);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(mapFitnessUser(results)) : Optional.empty();
            }
        }
    }

    public Optional<Trainer> findTrainerByEmail(String email) throws SQLException {
        String normalizedEmail = normalizeEmail(email);
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(FIND_TRAINER_BY_EMAIL_SQL)) {
            statement.setString(1, normalizedEmail);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(mapTrainer(results)) : Optional.empty();
            }
        }
    }

    public Optional<FitnessUser> findFitnessUserById(int userId) throws SQLException {
        if (userId < 1) {
            throw new IllegalArgumentException("User ID must be at least 1.");
        }
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_USER_BY_ID_SQL)) {
            statement.setInt(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(mapFitnessUser(results)) : Optional.empty();
            }
        }
    }

    public Optional<Trainer> findTrainerById(int userId) throws SQLException {
        if (userId < 1) {
            throw new IllegalArgumentException("User ID must be at least 1.");
        }
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_TRAINER_BY_ID_SQL)) {
            statement.setInt(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(mapTrainer(results)) : Optional.empty();
            }
        }
    }

    public boolean updateFitnessPreferences(
            int userId,
            FitnessGoal fitnessGoal,
            FitnessLevel fitnessLevel
    ) throws SQLException {
        if (userId < 1) {
            throw new IllegalArgumentException("User ID must be at least 1.");
        }
        if (fitnessGoal == null || fitnessLevel == null) {
            throw new IllegalArgumentException("Fitness goal and level are required.");
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(UPDATE_FITNESS_PREFERENCES_SQL)) {
            statement.setString(1, fitnessGoal.name());
            statement.setString(2, fitnessLevel.name());
            statement.setInt(3, userId);
            int updatedRows = statement.executeUpdate();
            return updatedRows == 1 || findFitnessUserById(userId).isPresent();
        }
    }

    private FitnessUser mapFitnessUser(ResultSet results) throws SQLException {
        return new FitnessUser(
                results.getInt("user_id"),
                results.getString("full_name"),
                results.getString("email"),
                results.getString("password_hash"),
                FitnessGoal.valueOf(results.getString("fitness_goal")),
                FitnessLevel.valueOf(results.getString("fitness_level"))
        );
    }

    private Trainer mapTrainer(ResultSet results) throws SQLException {
        return new Trainer(
                results.getInt("user_id"),
                results.getString("full_name"),
                results.getString("email"),
                results.getString("password_hash"),
                results.getString("bio"),
                results.getString("specialization")
        );
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateTrainerFields(Trainer trainer) {
        if (trainer.getBio() != null && trainer.getBio().length() > 5000) {
            throw new IllegalArgumentException("Trainer bio is too long.");
        }
        if (trainer.getSpecialization() != null
                && trainer.getSpecialization().length() > 120) {
            throw new IllegalArgumentException(
                    "Trainer specialization must be 120 characters or fewer."
            );
        }
    }
}
