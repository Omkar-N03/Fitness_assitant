package com.example;

import java.io.Serializable;

/**
 * Model representing an executed workout entry associated with an exercise split and session.
 */
public class WorkoutLog implements Serializable {
    private long id;
    private long userId;
    private long exerciseId;
    private String exerciseTitle;
    private String date; // YYYY-MM-DD
    private int sets;
    private int reps;
    private double weight; // kg
    private String notes;
    private String splitName;

    public WorkoutLog() {
    }

    public WorkoutLog(long id, long userId, long exerciseId, String exerciseTitle, String date, int sets, int reps, double weight, String notes) {
        this(id, userId, exerciseId, exerciseTitle, date, sets, reps, weight, notes, "Full Body");
    }

    public WorkoutLog(long id, long userId, long exerciseId, String exerciseTitle, String date, int sets, int reps, double weight, String notes, String splitName) {
        this.id = id;
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.exerciseTitle = exerciseTitle;
        this.date = date;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
        this.notes = notes;
        this.splitName = splitName;
    }

    public WorkoutLog(long id, long userId, long exerciseId, String exerciseTitle, String bodyPart, String date, int sets, int reps, double weight, String notes) {
        this(id, userId, exerciseId, exerciseTitle, date, sets, reps, weight, notes);
    }

    public WorkoutLog(long exerciseId, String date, int sets, int reps, double weight, String notes) {
        this(-1, 1, exerciseId, "", date, sets, reps, weight, notes);
    }

    public WorkoutLog(long exerciseId, String exerciseTitle, String splitName, String date, String notes) {
        this(-1, 1, exerciseId, exerciseTitle, date, 3, 10, 0.0, notes, splitName);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getExerciseTitle() {
        return exerciseTitle;
    }

    public void setExerciseTitle(String exerciseTitle) {
        this.exerciseTitle = exerciseTitle;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSplitName() {
        return splitName != null && !splitName.isEmpty() ? splitName : "Workout Split";
    }

    public void setSplitName(String splitName) {
        this.splitName = splitName;
    }

    public double getVolume() {
        return sets * reps * weight;
    }
}
