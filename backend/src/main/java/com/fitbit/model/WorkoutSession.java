package com.fitbit.model;

import java.time.LocalDateTime;

public class WorkoutSession {
    private final int workoutId;
    private final int userId;
    private final Integer planId;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final WorkoutStatus status;
    private final String notes;

    public WorkoutSession(
            int workoutId,
            int userId,
            Integer planId,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            WorkoutStatus status,
            String notes
    ) {
        this.workoutId = workoutId;
        this.userId = userId;
        this.planId = planId;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.status = status;
        this.notes = notes;
    }

    public int getWorkoutId() {
        return workoutId;
    }

    public int getUserId() {
        return userId;
    }

    public Integer getPlanId() {
        return planId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public WorkoutStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }
}
