package com.example.userssdk.dto;

import com.example.userssdk.entities.UserCustomField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldDTO {
    private String fieldName;
    private String fieldValue;

    public CustomFieldDTO(UserCustomField field) {
        this.fieldName = field.getFieldName();
        this.fieldValue = field.getFieldValue();
    }
}