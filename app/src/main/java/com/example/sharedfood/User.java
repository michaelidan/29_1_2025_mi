package com.example.sharedfood;

public class User {
    private String email;
    private boolean isBanned;
    private Long tempBanTime;

    // Constructor with three parameters
    public User(String email, boolean isBanned, Long tempBanTime) {
        this.email = email;
        this.isBanned = isBanned;
        this.tempBanTime = tempBanTime;
    }

    // Constructor with two parameters
    public User(String email, boolean isBanned) {
        this.email = email;
        this.isBanned = isBanned;
        this.tempBanTime = null; // Default to null
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }

    public Long getTempBanTime() {
        return tempBanTime;
    }

    public void setTempBanTime(Long tempBanTime) {
        this.tempBanTime = tempBanTime;
    }
}
