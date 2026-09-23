package com.example;

import java.io.Serializable;

/**
 * Model representing a gym exercise parsed from CSV or loaded from SQLite.
 * Includes complete video tutorial metadata and video match indicators.
 */
public class Exercise implements Serializable {
    private long id;
    private String title;
    private String bodyPart;
    private String equipment;
    private String difficulty;
    private String instructions;
    private String youtubeUrl;
    private String exerciseType;
    private double rating = 4.8;
    private String videoTitle;
    private String youtubeChannel;
    private String videoMatch = "Related";
    private boolean isFavorite = false;

    public Exercise() {
    }

    public Exercise(long id, String title, String bodyPart, String equipment, String difficulty, String instructions, String youtubeUrl) {
        this.id = id;
        this.title = title;
        this.bodyPart = bodyPart;
        this.equipment = equipment;
        this.difficulty = difficulty;
        this.instructions = instructions;
        this.youtubeUrl = youtubeUrl;
    }

    public Exercise(long id, String title, String instructions, String exerciseType, String bodyPart,
                    String equipment, String difficulty, double rating, String videoTitle,
                    String youtubeChannel, String youtubeUrl, String videoMatch) {
        this.id = id;
        this.title = title;
        this.instructions = instructions;
        this.exerciseType = exerciseType;
        this.bodyPart = bodyPart;
        this.equipment = equipment;
        this.difficulty = difficulty;
        this.rating = rating;
        this.videoTitle = videoTitle;
        this.youtubeChannel = youtubeChannel;
        this.youtubeUrl = youtubeUrl;
        this.videoMatch = videoMatch != null ? videoMatch : "Related";
    }

    public Exercise(String title, String bodyPart, String equipment, String difficulty, String instructions, String youtubeUrl) {
        this(-1, title, bodyPart, equipment, difficulty, instructions, youtubeUrl);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBodyPart() {
        return bodyPart;
    }

    public void setBodyPart(String bodyPart) {
        this.bodyPart = bodyPart;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getLevel() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public void setLevel(String level) {
        this.difficulty = level;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getDescription() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void setDescription(String description) {
        this.instructions = description;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public String getYoutubeLink() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }

    public String getExerciseType() {
        return exerciseType != null ? exerciseType : "Strength";
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public double getRating() {
        return rating > 0 ? rating : 4.8;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getVideoTitle() {
        if (videoTitle != null && !videoTitle.trim().isEmpty()) {
            return videoTitle;
        }
        return title != null ? title + " Form Tutorial" : "Exercise Demonstration";
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public String getYoutubeChannel() {
        if (youtubeChannel != null && !youtubeChannel.trim().isEmpty()) {
            return youtubeChannel;
        }
        return "Fitness Coach Tutorial";
    }

    public void setYoutubeChannel(String youtubeChannel) {
        this.youtubeChannel = youtubeChannel;
    }

    public String getVideoMatch() {
        return videoMatch != null ? videoMatch : "Related";
    }

    public void setVideoMatch(String videoMatch) {
        this.videoMatch = videoMatch;
    }

    public boolean isExactMatch() {
        return "Exact".equalsIgnoreCase(videoMatch);
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
    }
}
