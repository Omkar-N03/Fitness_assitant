package com.example;

import java.io.Serializable;

/**
 * Model representing a logged bodyweight measurement in time.
 */
public class WeightHistory implements Serializable {
    private long id;
    private long userId;
    private double weight;
    private String date;
    private String notes;

    public WeightHistory() {
    }

    public WeightHistory(long id, long userId, double weight, String date, String notes) {
        this.id = id;
        this.userId = userId;
        this.weight = weight;
        this.date = date;
        this.notes = notes;
    }

    public WeightHistory(double weight, String date, String notes) {
        this(-1, 1, weight, date, notes);
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

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
