package com.fitbit.service;

/** Separates password storage policy from the authentication service. */
public interface PasswordHasher {
    String hash(String password);

    boolean matches(String password, String passwordHash);
}
