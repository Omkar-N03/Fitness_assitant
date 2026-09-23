package com.example;

import java.io.Serializable;
import java.util.Locale;

/**
 * Model representing a gym member user profile and physiological metrics.
 */
public class UserProfile implements Serializable {
    private long id;
    private String name;
    private double height; // in cm
    private double currentWeight; // in kg
    private double targetWeight; // in kg
    private String createdAt;
    private String updatedAt;
    private String profileImagePath;

    public UserProfile() {
    }

    public UserProfile(long id, String name, double height, double currentWeight, double targetWeight, String createdAt, String updatedAt) {
        this(id, name, height, currentWeight, targetWeight, createdAt, updatedAt, null);
    }

    public UserProfile(long id, String name, double height, double currentWeight, double targetWeight, String createdAt, String updatedAt, String profileImagePath) {
        this.id = id;
        this.name = name;
        this.height = height;
        this.currentWeight = currentWeight;
        this.targetWeight = targetWeight;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.profileImagePath = profileImagePath;
    }

    public String getProfileImagePath() {
        return profileImagePath;
    }

    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return (name == null || name.trim().isEmpty()) ? "Athlete" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getHeight() {
        return height;
    }

    public double getHeightCm() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public void setHeightCm(double height) {
        this.height = height;
    }

    public double getCurrentWeight() {
        return currentWeight;
    }

    public double getCurrentWeightKg() {
        return currentWeight;
    }

    public void setCurrentWeight(double currentWeight) {
        this.currentWeight = currentWeight;
    }

    public void setCurrentWeightKg(double weight) {
        this.currentWeight = weight;
    }

    public double getTargetWeight() {
        return targetWeight;
    }

    public double getTargetWeightKg() {
        return targetWeight;
    }

    public void setTargetWeight(double targetWeight) {
        this.targetWeight = targetWeight;
    }

    public void setTargetWeightKg(double targetWeight) {
        this.targetWeight = targetWeight;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Calculates Body Mass Index: weight (kg) / (height (m) ^ 2)
     */
    public double calculateBmi() {
        if (height <= 0 || currentWeight <= 0) {
            return 0.0;
        }
        double heightInMeters = height / 100.0;
        double bmi = currentWeight / (heightInMeters * heightInMeters);
        return Math.round(bmi * 10.0) / 10.0;
    }

    /**
     * Returns standard WHO BMI classification.
     */
    public String getBmiCategory() {
        double bmi = calculateBmi();
        if (bmi <= 0) return "Not Set";
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal";
        if (bmi < 30.0) return "Overweight";
        return "Obese";
    }

    /**
     * Returns 2-letter uppercase initials for profile avatar.
     */
    public String getInitials() {
        if (name == null || name.trim().isEmpty()) return "AR";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
        } else if (parts.length == 1 && !parts[0].isEmpty()) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.US);
        }
        return "AR";
    }
}
