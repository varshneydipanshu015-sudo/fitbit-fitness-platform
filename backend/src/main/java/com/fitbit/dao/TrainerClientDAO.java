package com.fitbit.dao;

import com.fitbit.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrainerClientDAO {
    private static final String ASSIGN_CLIENT_SQL = """
            INSERT INTO trainer_clients (trainer_id, client_id)
            SELECT tp.trainer_id, u.user_id
            FROM trainer_profiles tp
            JOIN users u ON u.email = ? AND u.role = 'USER'
            WHERE tp.trainer_id = ?
            """;
    private static final String FIND_CLIENTS_SQL = """
            SELECT u.user_id, u.full_name, u.email, tc.assigned_at
            FROM trainer_clients tc
            JOIN users u ON u.user_id = tc.client_id AND u.role = 'USER'
            WHERE tc.trainer_id = ?
            ORDER BY u.full_name, u.user_id
            """;

    public boolean assignClient(int trainerId, String clientEmail) throws SQLException {
        validateTrainerId(trainerId);
        if (clientEmail == null || clientEmail.isBlank()) {
            throw new IllegalArgumentException("Client email is required.");
        }
        String normalizedEmail = clientEmail.trim().toLowerCase(Locale.ROOT);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(ASSIGN_CLIENT_SQL)) {
            statement.setString(1, normalizedEmail);
            statement.setInt(2, trainerId);
            return statement.executeUpdate() == 1;
        }
    }

    public List<Client> findClientsByTrainerId(int trainerId) throws SQLException {
        validateTrainerId(trainerId);
        List<Client> clients = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_CLIENTS_SQL)) {
            statement.setInt(1, trainerId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    clients.add(new Client(
                            results.getInt("user_id"),
                            results.getString("full_name"),
                            results.getString("email"),
                            results.getTimestamp("assigned_at").toLocalDateTime().toString()
                    ));
                }
            }
        }
        return clients;
    }

    private void validateTrainerId(int trainerId) {
        if (trainerId < 1) {
            throw new IllegalArgumentException("Trainer ID must be at least 1.");
        }
    }

    public record Client(int userId, String fullName, String email, String assignedAt) {
    }
}
