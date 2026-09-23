package com.example;

import java.io.Serializable;

/**
 * Model representing a user's personal best lift, queried using an SQL subquery.
 */
public class PersonalBest implements Serializable {
    private String exerciseTitle;
    private double maxWeight;
    private int reps;
    private String date;

    public PersonalBest() {
    }

    public PersonalBest(String exerciseTitle, double maxWeight, int reps, String date) {
        this.exerciseTitle = exerciseTitle;
        this.maxWeight = maxWeight;
        this.reps = reps;
        this.date = date;
    }

    public String getExerciseTitle() {
        return exerciseTitle;
    }

    public void setExerciseTitle(String exerciseTitle) {
        this.exerciseTitle = exerciseTitle;
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(double maxWeight) {
        this.maxWeight = maxWeight;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
