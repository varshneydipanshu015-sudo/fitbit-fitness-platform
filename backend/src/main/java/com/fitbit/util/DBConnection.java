package com.fitbit.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Opens MySQL connections using credentials supplied by the Tomcat environment. */
public final class DBConnection {
    private static final String URL_VARIABLE = "FITBIT_DB_URL";
    private static final String USER_VARIABLE = "FITBIT_DB_USER";
    private static final String PASSWORD_VARIABLE = "FITBIT_DB_PASSWORD";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = requireEnvironmentVariable(URL_VARIABLE);
        String user = requireEnvironmentVariable(USER_VARIABLE);
        String password = requireEnvironmentVariable(PASSWORD_VARIABLE);

        try {
            // Explicit loading supports the Tomcat deployment's JDBC driver discovery.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("MySQL JDBC driver is not available.", "08001", exception);
        }

        return DriverManager.getConnection(url, user, password);
    }

    private static String requireEnvironmentVariable(String name) throws SQLException {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new SQLException(
                    "Required database environment variable is not set: " + name,
                    "08001"
            );
        }
        return value;
    }
}
