package com.fitbit.service;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptPasswordHasher implements PasswordHasher {
    private static final int LOG_ROUNDS = 12;

    @Override
    public String hash(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(LOG_ROUNDS));
    }

    @Override
    public boolean matches(String password, String passwordHash) {
        if (password == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
