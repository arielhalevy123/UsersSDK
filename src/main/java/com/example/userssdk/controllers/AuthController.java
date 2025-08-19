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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    // ===== Auth =====

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    // ===== Users listing =====

    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/admin/{adminId}/users")
    public ResponseEntity<List<UserDTO>> getUsersByAdmin(@PathVariable Long adminId) {
        return ResponseEntity.ok(userService.getUsersManagedBy(adminId));
    }

    // ===== Current principal =====

    /**
     * מחזיר את פרטי המשתמש המחובר עצמו (כולל customFields).
     * מבוסס על @AuthenticationPrincipal שמוזן ע"י JwtFilter.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal com.example.userssdk.entities.User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new UserDTO(principal));
    }

    /**
     * למשתמש "USER" – יחזיר את האדמין שלו.
     * לאדמין – נחזיר את האדמין של עצמו אם קיים; אחרת נחזיר את עצמו (כדי שיהיה שימושי).
     */
    @GetMapping("/my-admin")
    public ResponseEntity<UserDTO> getMyAdmin(@AuthenticationPrincipal com.example.userssdk.entities.User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (principal.getAdmin() != null) {
            return ResponseEntity.ok(new UserDTO(principal.getAdmin()));
        }
        // ADMIN ללא אדמין מעליו – נחזיר את עצמו.
        return ResponseEntity.ok(new UserDTO(principal));
    }

    /**
     * אם המחובר ADMIN – מחזיר את כל המשתמשים שהוא מנהל.
     * אם המחובר USER – מחזיר את כל המשתמשים של האדמין שמנהל אותו.
     */
    @GetMapping("/my-users")
    public ResponseEntity<List<UserDTO>> getUsersManagedByCurrentPrincipal(
            @AuthenticationPrincipal com.example.userssdk.entities.User principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long adminId;
        boolean isAdmin = "ADMIN".equals(principal.getRole().name());
        if (isAdmin) {
            adminId = principal.getId();
        } else {
            Long maybe = null;
            if (principal.getAdmin() != null) {
                maybe = principal.getAdmin().getId();
            } else {
                try {
                    var full = userRepository.findById(principal.getId()).orElse(null);
                    if (full != null && full.getAdmin() != null) {
                        maybe = full.getAdmin().getId();
                    }
                } catch (Exception ignored) {}
            }
            if (maybe == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            adminId = maybe;
        }

        List<UserDTO> users = userService.getUsersManagedBy(adminId);
        return ResponseEntity.ok(users);
    }

    // ===== Update user (by admin or self) =====

    @PutMapping("/users/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal com.example.userssdk.entities.User principal,
            @RequestBody UserDTO userDto) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isAdmin = "ADMIN".equals(principal.getRole().name());
        boolean isSelf  = Objects.equals(principal.getId(), id);

        if (!isAdmin && !isSelf) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserDTO updatedUser = userService.updateUser(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }
}
