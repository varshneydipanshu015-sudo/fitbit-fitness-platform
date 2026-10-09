# Fitbit Fitness Platform — Architecture

## 1. Overview
A web-based fitness platform that connects workout logging, exercise discovery, trainer-created plans, and progress tracking.

## 2. System Architecture
- Frontend: Next.js 16 and React 19
- Backend: Java 21, Jakarta Servlets, JDBC, and Apache Tomcat 10.1
- Database: MySQL 8

Request flow: Browser → Frontend → Java backend/API → MySQL database → Response.

## 3. Main Modules
1. Authentication
2. Exercise Library
3. Workout Plans
4. Workout Tracking
5. Progress Tracking
6. Recommendations
7. Trainer Clients
8. Dashboard

## 4. Database Design
The documented entities include users, trainer_profiles, exercises, workout_plans, workout_plan_exercises, trainer_clients, user_workouts, and progress.

## 5. Security
The project presentation describes BCrypt password hashing, server-side HTTP sessions, JDBC PreparedStatements, and role-based access for USER and TRAINER accounts.

## 6. User Workflows
Fitness users browse exercises and plans, record workouts, and track progress. Trainers create workout plans and manage their client rosters.

## 7. Scope
This document summarizes the architecture presented for the local-development project. Implementation details should be verified against the source code.
