package com.example.userssdk.model;


import com.google.gson.annotations.SerializedName;


public class CustomFieldDTO {
    @SerializedName("fieldName")
    private String fieldName;

    @SerializedName("fieldValue")
    private String fieldValue;

    public CustomFieldDTO() {}

    public CustomFieldDTO(String fieldName, String fieldValue) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getFieldValue() { return fieldValue; }
    public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }
}