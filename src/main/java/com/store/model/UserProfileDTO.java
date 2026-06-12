package com.store.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.multipart.MultipartFile;

public class UserProfileDTO {

    @NotEmpty(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "^[0-9\\-]*$", message = "IC number must contain only digits and dashes")
    private String icNumber;

    @Pattern(regexp = "^[0-9\\-+() ]*$", message = "Phone number must contain only digits and valid characters")
    private String phoneNumber;

    private String address;

    private MultipartFile profileImageFile;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIcNumber() {
        return icNumber;
    }

    public void setIcNumber(String icNumber) {
        this.icNumber = icNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public MultipartFile getProfileImageFile() {
        return profileImageFile;
    }

    public void setProfileImageFile(MultipartFile profileImageFile) {
        this.profileImageFile = profileImageFile;
    }
}
