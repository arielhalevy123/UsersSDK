package com.example.userssdk.dto;

import com.example.userssdk.entities.Role;
import com.example.userssdk.entities.User;
import com.example.userssdk.entities.UserCustomField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String name;
    private Role role;
    private List<CustomFieldDTO> customFields;

    public UserDTO(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.role = user.getRole();
        if (user.getCustomFields() != null) {
            this.customFields = user.getCustomFields().stream()
                    .map(CustomFieldDTO::new)
                    .collect(Collectors.toList());
        }
    }
}