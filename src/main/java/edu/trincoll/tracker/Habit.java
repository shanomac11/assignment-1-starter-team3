package edu.trincoll.tracker;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain: Habit Tracker
 *
 * Represents a trackable habit with frequency, streaks, and completion metadata.
 *
 * Fields beyond id:
 *  - name (required)
 *  - description (optional)
 *  - frequency (DAILY/WEEKLY/CUSTOM)
 *  - targetPerWeek (how many times/week when frequency is WEEKLY or CUSTOM)
 *  - currentStreak (non-negative)
 *  - bestStreak (non-negative)
 *  - lastCompleted (last date the habit was completed)
 *  - createdAt (set on creation)
 *  - archived (soft-delete / hide from active list)
 */
public class Habit {

    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @NotNull(message = "Frequency is required")
    private Frequency frequency;

    /**
     * For WEEKLY/CUSTOM habits, how many completions per week are targeted.
     * For DAILY habits this can remain 7 by convention.
     */
    @Min(value = 1, message = "Target per week must be at least 1")
    @Max(value = 7, message = "Target per week cannot exceed 7")
    private int targetPerWeek;

    @Min(value = 0, message = "Current streak cannot be negative")
    private int currentStreak;

    @Min(value = 0, message = "Best streak cannot be negative")
    private int bestStreak;

    @PastOrPresent(message = "Last completed cannot be in the future")
    private LocalDate lastCompleted;

    @PastOrPresent(message = "Created time cannot be in the future")
    private LocalDateTime createdAt;

    /** Whether the habit is archived (hidden from active lists) */
    private boolean archived;

    public Habit() {
        this.createdAt = LocalDateTime.now();
        this.frequency = Frequency.DAILY;
        this.targetPerWeek = 7; // sensible default for DAILY
        this.currentStreak = 0;
        this.bestStreak = 0;
        this.archived = false;
    }

    public Habit(String name, String description, Frequency frequency, int targetPerWeek) {
        this();
        this.name = name;
        this.description = description;
        this.frequency = frequency != null ? frequency : Frequency.DAILY;
        this.targetPerWeek = targetPerWeek;
    }

    // --------- Getters & Setters ---------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    public int getTargetPerWeek() {
        return targetPerWeek;
    }

    public void setTargetPerWeek(int targetPerWeek) {
        this.targetPerWeek = targetPerWeek;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = Math.max(0, currentStreak);
    }

    public int getBestStreak() {
        return bestStreak;
    }
    private boolean completed;

    /**
     * Mark this habit as completed (true) or not completed (false).
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * Check whether the habit is currently marked as completed.
     */
    public boolean isCompleted() {
        return completed;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = Math.max(0, bestStreak);
    }

    public LocalDate getLastCompleted() {
        return lastCompleted;
    }

    public void setLastCompleted(LocalDate lastCompleted) {
        this.lastCompleted = lastCompleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    // --------- Convenience methods (useful in services/tests) ---------

    /** Mark the habit as completed "today" and update streaks. */
    public void completeToday() {
        LocalDate today = LocalDate.now();
        if (lastCompleted != null && lastCompleted.plusDays(1).equals(today)) {
            currentStreak += 1; // consecutive day
        } else if (lastCompleted == null || lastCompleted.isBefore(today)) {
            // If last completion was not yesterday, reset to 1 for today's completion
            currentStreak = 1;
        }
        bestStreak = Math.max(bestStreak, currentStreak);
        lastCompleted = today;
    }

    /** Reset the current streak (e.g., when a gap is detected by a scheduler). */
    public void resetCurrentStreak() {
        currentStreak = 0;
    }

    // --------- Equality & Hashing ---------
    // Use id when present (common for persisted entities).
    // Fallback to name+createdAt to keep deterministic behavior pre-persist.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Habit)) return false;
        Habit habit = (Habit) o;
        if (id != null && habit.id != null) {
            return Objects.equals(id, habit.id);
        }
        return Objects.equals(name, habit.name) &&
                Objects.equals(createdAt, habit.createdAt);
    }

    @Override
    public int hashCode() {
        return (id != null)
                ? Objects.hash(id)
                : Objects.hash(name, createdAt);
    }

    @Override
    public String toString() {
        return "Habit{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", frequency=" + frequency +
                ", targetPerWeek=" + targetPerWeek +
                ", currentStreak=" + currentStreak +
                ", bestStreak=" + bestStreak +
                ", lastCompleted=" + lastCompleted +
                ", createdAt=" + createdAt +
                ", archived=" + archived +
                '}';
    }

    // --------- Nested Types ---------

    public enum Frequency {
        DAILY, WEEKLY, CUSTOM
    }
}