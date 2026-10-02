package com.fitbit.dao;

import com.fitbit.model.ProgressEntry;
import com.fitbit.util.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Stores each user's dated progress measurements and returns them newest first. */
public class ProgressDAO {
    private static final String INSERT_SQL = """
            INSERT INTO progress (user_id, recorded_on, weight_kg, note)
            VALUES (?, CURRENT_DATE, ?, ?)
            """;
    private static final String FIND_BY_USER_SQL = """
            SELECT progress_id, user_id, recorded_on, weight_kg, note
            FROM progress
            WHERE user_id = ?
            ORDER BY recorded_on DESC, progress_id DESC
            """;
    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("999.99");

    public int create(int userId, BigDecimal weightKg, String note) throws SQLException {
        validateUserId(userId);
        validateWeight(weightKg);
        validateNote(note);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            statement.setInt(1, userId);
            statement.setBigDecimal(2, weightKg);
            statement.setString(3, note);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
                throw new SQLException("Creating the progress entry did not return an ID.");
            }
        }
    }

    public List<ProgressEntry> findByUserId(int userId) throws SQLException {
        validateUserId(userId);
        List<ProgressEntry> entries = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_USER_SQL)) {
            statement.setInt(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    entries.add(new ProgressEntry(
                            results.getInt("progress_id"),
                            results.getInt("user_id"),
                            results.getDate("recorded_on").toLocalDate(),
                            results.getBigDecimal("weight_kg"),
                            results.getString("note")
                    ));
                }
            }
        }

        return entries;
    }

    private void validateUserId(int userId) {
        if (userId < 1) {
            throw new IllegalArgumentException("User ID must be at least 1.");
        }
    }

    private void validateWeight(BigDecimal weightKg) {
        // Match the precision and range of the database DECIMAL(5, 2) column.
        if (weightKg == null || weightKg.signum() <= 0 || weightKg.compareTo(MAX_WEIGHT_KG) > 0) {
            throw new IllegalArgumentException("Weight must be greater than 0 and at most 999.99 kg.");
        }
        if (weightKg.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Weight must have no more than 2 decimal places.");
        }
    }

    private void validateNote(String note) {
        if (note != null && note.length() > 255) {
            throw new IllegalArgumentException("Notes must be 255 characters or fewer.");
        }
    }
}
