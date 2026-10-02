package com.fitbit.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {
    private final BCryptPasswordHasher passwordHasher = new BCryptPasswordHasher();

    @Test
    void hashesPasswordsAndVerifiesOnlyTheMatchingPassword() {
        String password = "sample-password";
        String hash = passwordHasher.hash(password);

        assertNotEquals(password, hash);
        assertTrue(passwordHasher.matches(password, hash));
        assertFalse(passwordHasher.matches("different-password", hash));
    }

    @Test
    void usesAUniqueSaltForEachHash() {
        String firstHash = passwordHasher.hash("sample-password");
        String secondHash = passwordHasher.hash("sample-password");

        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordHasher.matches("sample-password", firstHash));
        assertTrue(passwordHasher.matches("sample-password", secondHash));
    }

    @Test
    void rejectsMissingPasswordWhenHashing() {
        assertThrows(IllegalArgumentException.class, () -> passwordHasher.hash(" "));
        assertFalse(passwordHasher.matches(null, "stored-hash"));
        assertFalse(passwordHasher.matches("sample-password", null));
    }
}
