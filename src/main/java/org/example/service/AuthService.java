package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.entity.UserEntity;
import org.example.domain.request.UserRequest;
import org.example.domain.response.AuthenticationResponse;
import org.example.domain.response.BaseResponse;
import org.example.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public BaseResponse<AuthenticationResponse> register(UserRequest request) {
        Optional<UserEntity> user = repository.findByEmail(request.getEmail());

        if (user.isPresent()) {
            return BaseResponse.<AuthenticationResponse>builder()
                    .status(400)
                    .message("User already exists, please log in")
                    .build();
        }

        UserEntity userEntity = UserEntity.builder()
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .build();

        repository.save(userEntity);
        String token = jwtService.generateToken(userEntity);

        return BaseResponse.<AuthenticationResponse>builder()
                .data(AuthenticationResponse.builder().token(token).build())
                .status(200)
                .message("User successfully created")
                .build();
    }

    public BaseResponse<AuthenticationResponse> authenticate(UserRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            e.printStackTrace(); // Print the real authentication error
            return BaseResponse.<AuthenticationResponse>builder()
                    .message("Authentication failed: " + e.getMessage())
                    .status(403)
                    .build();
        }

        Optional<UserEntity> user = repository.findByEmail(request.getEmail());

        if (user.isEmpty()) {
            return BaseResponse.<AuthenticationResponse>builder()
                    .message("Password or email is invalid")
                    .status(400)
                    .build();
        }

        String token = jwtService.generateToken(user.get());

        return BaseResponse.<AuthenticationResponse>builder()
                .data(AuthenticationResponse.builder().token(token).build())
                .status(200)
                .build();
    }
}
