package com.example.userssdk.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UserDTO {
    @SerializedName("id")
    private long id;
    @SerializedName("email")
    private String email;
    @SerializedName("name")
    private String name;
    @SerializedName("role")
    private String role;
    @SerializedName("adminId")
    private Long adminId;
    @SerializedName("customFields")
    private List<CustomFieldDTO> customFields;

    public UserDTO() {}

    public UserDTO(long id, String email, String name, String role,Long adminId, List<CustomFieldDTO> customFields) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.adminId = adminId;
        this.customFields = customFields;
    }

    // Getters & Setters...
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public List<CustomFieldDTO> getCustomFields() { return customFields; }
    public void setCustomFields(List<CustomFieldDTO> customFields) { this.customFields = customFields; }
}