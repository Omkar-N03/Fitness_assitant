package com.example;

/**
 * Type alias / wrapper for WeightHistory to support both naming conventions.
 */
public class WeightEntry extends WeightHistory {

    public WeightEntry() {
        super();
    }

    public WeightEntry(long id, long userId, double weight, String date, String notes) {
        super(id, userId, weight, date, notes);
    }

    public WeightEntry(double weight, String date, String notes) {
        super(-1, 1, weight, date, notes);
    }
}
