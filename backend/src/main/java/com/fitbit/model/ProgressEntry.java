package com.fitbit.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProgressEntry {
    private final int progressId;
    private final int userId;
    private final LocalDate recordedOn;
    private final BigDecimal weightKg;
    private final String note;

    public ProgressEntry(
            int progressId,
            int userId,
            LocalDate recordedOn,
            BigDecimal weightKg,
            String note
    ) {
        this.progressId = progressId;
        this.userId = userId;
        this.recordedOn = recordedOn;
        this.weightKg = weightKg;
        this.note = note;
    }

    public int getProgressId() {
        return progressId;
    }

    public int getUserId() {
        return userId;
    }

    public LocalDate getRecordedOn() {
        return recordedOn;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public String getNote() {
        return note;
    }
}
