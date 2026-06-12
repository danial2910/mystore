package com.store.model;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ItemDTO {

    private String name;

    private String brand;

    @NotEmpty(message = "Category must not be empty")
    private String category;

    @Min(value = 0, message = "Quantity must be 0 or greater")
    private int quantity;

    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    private MultipartFile imageFile;

    @NotNull(message = "Storage start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime storageStartTime;

    @NotNull(message = "Storage end time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime storageEndTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }

    public LocalDateTime getStorageStartTime() { return storageStartTime; }
    public void setStorageStartTime(LocalDateTime storageStartTime) { this.storageStartTime = storageStartTime; }

    public LocalDateTime getStorageEndTime() { return storageEndTime; }
    public void setStorageEndTime(LocalDateTime storageEndTime) { this.storageEndTime = storageEndTime; }
}
