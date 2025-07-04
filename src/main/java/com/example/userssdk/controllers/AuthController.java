package com.example.userssdk.controllers;

import com.example.userssdk.dto.AuthResponse;
import com.example.userssdk.dto.LoginRequest;
import com.example.userssdk.dto.RegisterRequest;
import com.example.userssdk.dto.UserDTO;
import com.example.userssdk.entities.User;
import com.example.userssdk.service.UserService;
import com.example.userssdk.config.JwtUtil;
import com.example.userssdk.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/admin/{adminId}/users")
    public ResponseEntity<List<UserDTO>> getUsersByAdmin(@PathVariable Long adminId) {
        return ResponseEntity.ok(userService.getUsersManagedBy(adminId));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return ResponseEntity.ok(new UserDTO(user));  // משתמש בקונסטרקטור שמכניס גם customFields
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/my-users")
    public ResponseEntity<List<UserDTO>> getUsersManagedByCurrentAdmin(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractUsername(token);

        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));

        List<UserDTO> users = userService.getUsersManagedBy(admin.getId());

        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @RequestBody UserDTO userDto) {

        UserDTO updatedUser = userService.updateUser(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }

}