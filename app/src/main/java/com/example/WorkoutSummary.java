package com.example;

import java.io.Serializable;

/**
 * Model representing aggregated workout metrics populated by the SQLite workout_summary VIEW.
 */
public class WorkoutSummary implements Serializable {
    private String workoutDate;
    private int exercisesCount;
    private int totalSets;
    private int totalReps;
    private double totalVolume;
    private String exerciseNames;

    public WorkoutSummary() {
    }

    public WorkoutSummary(String workoutDate, int exercisesCount, int totalSets, int totalReps, double totalVolume, String exerciseNames) {
        this.workoutDate = workoutDate;
        this.exercisesCount = exercisesCount;
        this.totalSets = totalSets;
        this.totalReps = totalReps;
        this.totalVolume = totalVolume;
        this.exerciseNames = exerciseNames;
    }

    public String getWorkoutDate() {
        return workoutDate;
    }

    public void setWorkoutDate(String workoutDate) {
        this.workoutDate = workoutDate;
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
}
