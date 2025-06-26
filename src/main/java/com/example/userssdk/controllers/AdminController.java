package com.example.userssdk.controllers;

import com.example.userssdk.entities.UserCustomField;
import com.example.userssdk.repositories.UserCustomFieldRepository;
import com.example.userssdk.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final UserCustomFieldRepository customFieldRepository;

    @PostMapping("/users/{userId}/fields")
    public ResponseEntity<UserCustomField> addFieldToUser(
            @PathVariable Long userId,
            @RequestBody UserCustomField fieldRequest
    ) {
        return userRepository.findById(userId)
                .map(user -> { fieldRequest.setUser(user);
                    return ResponseEntity.ok(customFieldRepository.save(fieldRequest));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{userId}/fields")
    public ResponseEntity<List<UserCustomField>> getFieldsForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(customFieldRepository.findByUserId(userId));
    }

    @DeleteMapping("/fields/{fieldId}")
    public ResponseEntity<Void> deleteField(@PathVariable Long fieldId) {
        customFieldRepository.deleteById(fieldId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/fields/{fieldId}")
    @Transactional
    public ResponseEntity<UserCustomField> updateField(
            @PathVariable Long fieldId,
            @RequestBody UserCustomField update
    ) {
        return customFieldRepository.findById(fieldId)
                .map(field -> {
                    field.setFieldName(update.getFieldName());
                    field.setFieldValue(update.getFieldValue());
                    return ResponseEntity.ok(field);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}