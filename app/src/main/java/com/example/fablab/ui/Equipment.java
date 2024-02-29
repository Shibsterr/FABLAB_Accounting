package com.example.fablab.ui;

public class Equipment {
    private  String name;
    private String description;
    private String imageUrl;

    public Equipment() {
    }

    public Equipment(String name, String description, String imageUrl) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}