package com.example.userssdk.service;

import com.example.userssdk.dto.AuthResponse;
import com.example.userssdk.dto.LoginRequest;
import com.example.userssdk.dto.RegisterRequest;
import com.example.userssdk.dto.UserDTO;

import java.util.List;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    List<UserDTO> getAllUsers();
    List<UserDTO> getUsersManagedBy(Long adminId);
    UserDTO updateUser(Long id, UserDTO userDTO);
}