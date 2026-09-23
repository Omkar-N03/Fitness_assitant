package com.example;

import java.io.Serializable;

/**
 * Model representing aggregated workout metrics populated for workout sessions and splits.
 */
public class WorkoutSummary implements Serializable {
    private String workoutDate;
    private String splitName;
    private int exercisesCount;
    private int totalSets;
    private int totalReps;
    private double totalVolume;
    private String exerciseNames;
    private String notes;

    public WorkoutSummary() {
    }

    public WorkoutSummary(String workoutDate, int exercisesCount, int totalSets, int totalReps, double totalVolume, String exerciseNames) {
        this(workoutDate, "Workout Session", exercisesCount, totalSets, totalReps, totalVolume, exerciseNames, "");
    }

    public WorkoutSummary(String workoutDate, String splitName, int exercisesCount, int totalSets, int totalReps, double totalVolume, String exerciseNames) {
        this(workoutDate, splitName, exercisesCount, totalSets, totalReps, totalVolume, exerciseNames, "");
    }

    public WorkoutSummary(String workoutDate, String splitName, int exercisesCount, int totalSets, int totalReps, double totalVolume, String exerciseNames, String notes) {
        this.workoutDate = workoutDate;
        this.splitName = splitName;
        this.exercisesCount = exercisesCount;
        this.totalSets = totalSets;
        this.totalReps = totalReps;
        this.totalVolume = totalVolume;
        this.exerciseNames = exerciseNames;
        this.notes = notes != null ? notes : "";
    }

    public String getWorkoutDate() {
        return workoutDate;
    }

    public void setWorkoutDate(String workoutDate) {
        this.workoutDate = workoutDate;
    }

    public String getSplitName() {
        return splitName != null && !splitName.isEmpty() ? splitName : "Workout Split";
    }

    public void setSplitName(String splitName) {
        this.splitName = splitName;
    }

    public int getExercisesCount() {
        return exercisesCount;
    }

    public void setExercisesCount(int exercisesCount) {
        this.exercisesCount = exercisesCount;
    }

    public int getTotalSets() {
        return totalSets;
    }

    public void setTotalSets(int totalSets) {
        this.totalSets = totalSets;
    }

    public int getTotalReps() {
        return totalReps;
    }

    public void setTotalReps(int totalReps) {
        this.totalReps = totalReps;
    }

    public double getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(double totalVolume) {
        this.totalVolume = totalVolume;
    }

    public String getExerciseNames() {
        return exerciseNames;
    }

    public void setExerciseNames(String exerciseNames) {
        this.exerciseNames = exerciseNames;
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes : "";
    }
}
