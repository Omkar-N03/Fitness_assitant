package com.example;

import java.io.Serializable;

/**
 * Model representing a gym exercise parsed from CSV or loaded from SQLite.
 */
public class Exercise implements Serializable {
    private long id;
    private String title;
    private String bodyPart;
    private String equipment;
    private String difficulty;
    private String instructions;
    private String youtubeUrl;

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

    public String getInstructions() {
        return instructions;
    }

    public String getDescription() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public String getYoutubeLink() {
        return youtubeUrl;
    }

    public String getVideoTitle() {
        return title != null ? title + " Form & Execution" : "Exercise Demonstration";
    }

    public String getYoutubeChannel() {
        return "Fitness Coach Demonstration";
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }
}
