package com.fitbit.service;

public interface PasswordHasher {
    String hash(String password);

    boolean matches(String password, String passwordHash);
}
