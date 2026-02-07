package com.example.userssdk.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RegisterRequest {
    @SerializedName("name")
    private String name;
    @SerializedName("email")
    private String email;
    @SerializedName("password")
    private String password;
    @SerializedName("role")
    private String role; // "ADMIN" | "USER"
    @SerializedName("adminId")
    private Long adminId; // nullable
    @SerializedName("customFields")
    private List<CustomFieldDTO> customFields;

    public RegisterRequest() {}

    public RegisterRequest(String name, String email, String password, String role, Long adminId, List<CustomFieldDTO> customFields) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.adminId = adminId;
        this.customFields = customFields;
    }

    // Getters & Setters...
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public List<CustomFieldDTO> getCustomFields() { return customFields; }
    public void setCustomFields(List<CustomFieldDTO> customFields) { this.customFields = customFields; }
}