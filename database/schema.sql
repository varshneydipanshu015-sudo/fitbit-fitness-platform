CREATE DATABASE IF NOT EXISTS fitbit_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE fitbit_db;

-- Shared identity table; role-specific profile data lives in related tables.
CREATE TABLE IF NOT EXISTS users (
    user_id INT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('USER', 'TRAINER') NOT NULL DEFAULT 'USER',
    fitness_goal ENUM(
        'WEIGHT_LOSS',
        'MUSCLE_GAIN',
        'GENERAL_FITNESS',
        'STRENGTH',
        'ENDURANCE'
    ) NULL,
    fitness_level ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS trainer_profiles (
    trainer_id INT NOT NULL,
    bio TEXT NULL,
    specialization VARCHAR(120) NULL,
    PRIMARY KEY (trainer_id),
    CONSTRAINT fk_trainer_profiles_user
        FOREIGN KEY (trainer_id) REFERENCES users (user_id)
        ON DELETE CASCADE
) ENGINE = InnoDB;

-- Reusable movement catalog referenced by workout plans.
CREATE TABLE IF NOT EXISTS exercises (
    exercise_id INT NOT NULL AUTO_INCREMENT,
    exercise_name VARCHAR(120) NOT NULL,
    description TEXT NULL,
    target_muscle VARCHAR(100) NULL,
    difficulty ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') NOT NULL DEFAULT 'BEGINNER',
    equipment VARCHAR(120) NOT NULL DEFAULT 'None',
    PRIMARY KEY (exercise_id),
    UNIQUE KEY uq_exercises_name (exercise_name)
) ENGINE = InnoDB;

-- Trainers own plans; ordered exercise prescriptions are stored separately.
CREATE TABLE IF NOT EXISTS workout_plans (
    plan_id INT NOT NULL AUTO_INCREMENT,
    trainer_id INT NOT NULL,
    plan_name VARCHAR(120) NOT NULL,
    description TEXT NULL,
    fitness_goal ENUM(
        'WEIGHT_LOSS',
        'MUSCLE_GAIN',
        'GENERAL_FITNESS',
        'STRENGTH',
        'ENDURANCE'
    ) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (plan_id),
    KEY idx_workout_plans_trainer (trainer_id),
    CONSTRAINT fk_workout_plans_trainer
        FOREIGN KEY (trainer_id) REFERENCES trainer_profiles (trainer_id)
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS workout_plan_exercises (
    plan_id INT NOT NULL,
    exercise_id INT NOT NULL,
    exercise_order INT NOT NULL,
    sets_count TINYINT UNSIGNED NOT NULL,
    reps_count SMALLINT UNSIGNED NOT NULL,
    rest_seconds SMALLINT UNSIGNED NOT NULL DEFAULT 60,
    PRIMARY KEY (plan_id, exercise_id),
    UNIQUE KEY uq_plan_exercise_order (plan_id, exercise_order),
    CONSTRAINT fk_plan_exercises_plan
        FOREIGN KEY (plan_id) REFERENCES workout_plans (plan_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_plan_exercises_exercise
        FOREIGN KEY (exercise_id) REFERENCES exercises (exercise_id)
        ON DELETE RESTRICT
) ENGINE = InnoDB;

-- Trainer-client links are restricted to valid trainer profiles and user IDs.
CREATE TABLE IF NOT EXISTS trainer_clients (
    trainer_id INT NOT NULL,
    client_id INT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (trainer_id, client_id),
    CONSTRAINT fk_trainer_clients_trainer
        FOREIGN KEY (trainer_id) REFERENCES trainer_profiles (trainer_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_trainer_clients_client
        FOREIGN KEY (client_id) REFERENCES users (user_id)
        ON DELETE CASCADE
) ENGINE = InnoDB;

-- Personal activity and progress are always associated with the owning user.
CREATE TABLE IF NOT EXISTS user_workouts (
    workout_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    plan_id INT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    status ENUM('IN_PROGRESS', 'COMPLETED') NOT NULL DEFAULT 'IN_PROGRESS',
    notes TEXT NULL,
    PRIMARY KEY (workout_id),
    KEY idx_user_workouts_user_started (user_id, started_at),
    KEY idx_user_workouts_plan (plan_id),
    CONSTRAINT fk_user_workouts_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_workouts_plan
        FOREIGN KEY (plan_id) REFERENCES workout_plans (plan_id)
        ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS progress (
    progress_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    recorded_on DATE NOT NULL,
    weight_kg DECIMAL(5, 2) NULL,
    note VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (progress_id),
    KEY idx_progress_user_recorded (user_id, recorded_on),
    CONSTRAINT fk_progress_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE CASCADE
) ENGINE = InnoDB;