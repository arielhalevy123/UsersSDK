package com.example.userssdk.repositories;

import com.example.userssdk.entities.UserCustomField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCustomFieldRepository extends JpaRepository<UserCustomField, Long> {
    List<UserCustomField> findByUserId(Long userId);
}