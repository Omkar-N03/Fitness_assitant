package com.example;

import androidx.annotation.NonNull;

/**
 * Model representing a registered member account.
 * Supports authentication with personal Gmail, custom email, or mobile number.
 */
public class UserAccount {
    private long id;
    private String name;
    private String identifier; // Gmail, custom email, or mobile number
    private String password;
    private String createdAt;

    public UserAccount(long id, @NonNull String name, @NonNull String identifier, @NonNull String password, @NonNull String createdAt) {
        this.id = id;
        this.name = name;
        this.identifier = identifier;
        this.password = password;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(@NonNull String identifier) {
        this.identifier = identifier;
    }

    @NonNull
    public String getPassword() {
        return password;
    }

    public void setPassword(@NonNull String password) {
        this.password = password;
    }

    @NonNull
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull String createdAt) {
        this.createdAt = createdAt;
    }
}
