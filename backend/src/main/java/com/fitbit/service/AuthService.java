package com.fitbit.service;

import com.fitbit.dao.UserDAO;
import com.fitbit.exception.UserValidationException;
import com.fitbit.model.FitnessGoal;
import com.fitbit.model.FitnessLevel;
import com.fitbit.model.FitnessUser;
import com.fitbit.model.Trainer;
import com.fitbit.model.User;
import java.sql.SQLException;
import java.util.Optional;

public class AuthService {
    private final UserDAO userDAO;
    private final PasswordHasher passwordHasher;

    public AuthService() {
        this(new UserDAO(), new BCryptPasswordHasher());
    }

    AuthService(UserDAO userDAO, PasswordHasher passwordHasher) {
        this.userDAO = userDAO;
        this.passwordHasher = passwordHasher;
    }

    public FitnessUser register(
            String fullName,
            String email,
            String password,
            FitnessGoal fitnessGoal,
            FitnessLevel fitnessLevel
    ) throws UserValidationException, SQLException {
        UserValidator.validateRegistration(fullName, email, password);
        if (fitnessGoal == null) {
            throw new UserValidationException("Fitness goal is required.");
        }
        if (fitnessLevel == null) {
            throw new UserValidationException("Fitness level is required.");
        }

        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        String passwordHash = passwordHasher.hash(password);
        FitnessUser user = new FitnessUser(
                0,
                fullName.trim(),
                normalizedEmail,
                passwordHash,
                fitnessGoal,
                fitnessLevel
        );
        int userId = userDAO.createFitnessUser(user);
        return new FitnessUser(
                userId,
                user.getFullName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFitnessGoal(),
                user.getFitnessLevel()
        );
    }

    public Trainer registerTrainer(
            String fullName,
            String email,
            String password,
            String bio,
            String specialization
    ) throws UserValidationException, SQLException {
        UserValidator.validateRegistration(fullName, email, password);
        if (bio != null && bio.length() > 5000) {
            throw new UserValidationException("Trainer bio must be 5000 characters or fewer.");
        }
        if (specialization != null && specialization.length() > 120) {
            throw new UserValidationException(
                    "Trainer specialization must be 120 characters or fewer."
            );
        }

        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        String passwordHash = passwordHasher.hash(password);
        Trainer trainer = new Trainer(
                0,
                fullName.trim(),
                normalizedEmail,
                passwordHash,
                bio == null || bio.isBlank() ? null : bio.trim(),
                specialization == null || specialization.isBlank()
                        ? null
                        : specialization.trim()
        );
        int userId = userDAO.createTrainer(trainer);
        return new Trainer(
                userId,
                trainer.getFullName(),
                trainer.getEmail(),
                trainer.getPasswordHash(),
                trainer.getBio(),
                trainer.getSpecialization()
        );
    }

    public Optional<User> authenticate(String email, String password)
            throws SQLException {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            return Optional.empty();
        }

        Optional<FitnessUser> matchingUser = userDAO.findFitnessUserByEmail(email);
        if (matchingUser.isPresent()
                && passwordHasher.matches(password, matchingUser.get().getPasswordHash())) {
            return Optional.of(matchingUser.get());
        }
        Optional<Trainer> matchingTrainer = userDAO.findTrainerByEmail(email);
        if (matchingTrainer.isPresent()
                && passwordHasher.matches(
                        password,
                        matchingTrainer.get().getPasswordHash()
                )) {
            return Optional.of(matchingTrainer.get());
        }
        return Optional.empty();
    }

    public Optional<FitnessUser> findFitnessUserById(int userId) throws SQLException {
        return userDAO.findFitnessUserById(userId);
    }

    public Optional<Trainer> findTrainerById(int userId) throws SQLException {
        return userDAO.findTrainerById(userId);
    }

    public boolean updateFitnessPreferences(
            int userId,
            FitnessGoal fitnessGoal,
            FitnessLevel fitnessLevel
    ) throws SQLException {
        return userDAO.updateFitnessPreferences(userId, fitnessGoal, fitnessLevel);
    }
}
