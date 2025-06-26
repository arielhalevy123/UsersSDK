package com.example.userssdk.service;

import com.example.userssdk.config.JwtUtil;
import com.example.userssdk.dto.AuthResponse;
import com.example.userssdk.dto.LoginRequest;
import com.example.userssdk.dto.RegisterRequest;
import com.example.userssdk.dto.UserDTO;
import com.example.userssdk.entities.Role;
import com.example.userssdk.entities.User;
import com.example.userssdk.entities.UserCustomField;
import com.example.userssdk.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request) {
        User admin = null;
        if (request.getAdminId() != null) {
            admin = userRepository.findById(request.getAdminId())
                    .orElseThrow(() -> new RuntimeException("Admin not found with id: " + request.getAdminId()));
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .admin(admin)
                .build();

        // המרת שדות מותאמים אישית
        if (request.getCustomFields() != null) {
            List<UserCustomField> fields = request.getCustomFields().stream()
                    .map(dto -> new UserCustomField(dto.getFieldName(), dto.getFieldValue(), user))
                    .collect(Collectors.toList());
            user.setCustomFields(fields);
        }

        userRepository.save(user);
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, new UserDTO(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        User user = optionalUser.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, new UserDTO(user));
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getUsersManagedBy(Long adminId) {
        return userRepository.findByAdminId(adminId)
                .stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
    }
    @Override
    public UserDTO updateUser(Long id, UserDTO userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // עדכון פרטים בסיסיים
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());

        // ננקה את השדות הקודמים כדי למנוע כפילויות
        user.getCustomFields().clear();

        if (userDto.getCustomFields() != null) {
            List<UserCustomField> newFields = userDto.getCustomFields().stream()
                    .map(dto -> new UserCustomField(dto.getFieldName(), dto.getFieldValue(), user))
                    .collect(Collectors.toList());

            user.getCustomFields().addAll(newFields);
        }

        userRepository.save(user);

        return new UserDTO(user);
    }
}