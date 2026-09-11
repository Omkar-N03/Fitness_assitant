package com.example;

import java.io.Serializable;

/**
 * Model representing a physique progress photo captured by the user.
 */
public class ProgressPhoto implements Serializable {
    private long id;
    private long userId;
    private String imagePath;
    private String date;
    private String notes;
    private double weight;

    public ProgressPhoto() {
    }

    public ProgressPhoto(long id, long userId, String imagePath, String date, String notes, double weight) {
        this.id = id;
        this.userId = userId;
        this.imagePath = imagePath;
        this.date = date;
        this.notes = notes;
        this.weight = weight;
    }

    public ProgressPhoto(long id, long userId, String imagePath, String date, double weight, String notes) {
        this.id = id;
        this.userId = userId;
        this.imagePath = imagePath;
        this.date = date;
        this.notes = notes;
        this.weight = weight;
    }

    public ProgressPhoto(String imagePath, String date, String notes, double weight)
    {
        this(-1, 1, imagePath, date, notes, weight);
    }

    public ProgressPhoto(String imagePath, String date, double weight, String notes) {
        this(-1, 1, imagePath, date, notes, weight);
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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
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

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
