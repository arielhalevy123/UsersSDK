package com.example.userssdk.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_custom_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCustomField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fieldName;
    private String fieldValue;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    public UserCustomField(String fieldName, String fieldValue, User user) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.user = user;
    }
}