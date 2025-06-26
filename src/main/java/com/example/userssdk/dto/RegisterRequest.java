package com.example.userssdk.dto;

import lombok.Data;
import com.example.userssdk.entities.Role;
import com.example.userssdk.dto.CustomFieldDTO;
import java.util.List;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private Role role;
    private Long adminId; // אם נרשמים כמשתמש רגיל, נציין את המנהל שלו
    private List<CustomFieldDTO> customFields;
}