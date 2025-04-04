package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.entity.user.UserEntity;
import org.example.domain.request.UserRequest;
import org.example.domain.response.AuthenticationResponse;
import org.example.domain.response.BaseResponse;
import org.example.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Stack;

import static org.example.domain.entity.user.Role.USER;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public BaseResponse<AuthenticationResponse> register(UserRequest request) {
        BaseResponse<AuthenticationResponse> response = checkIfCredentialsAreValid(request.getPassword(),
                request.getEmail(), request.getFullName());

        if (response.getStatus() == 400) return response;

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
                .role(USER)
                .build();

        repository.save(userEntity);
        String token = jwtService.generateToken(userEntity);

        return BaseResponse.<AuthenticationResponse>builder()
                .data(AuthenticationResponse.builder().token(token).build())
                .status(200)
                .message("User successfully created")
                .build();
    }

    public BaseResponse<AuthenticationResponse> checkIfCredentialsAreValid(String password, String email, String fullName) {
        StringBuilder message = new StringBuilder();
        Stack<String> messageStack = new Stack<>();

        if (!email.matches("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$")) {
            messageStack.push("Email");
        }
        if (!password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$")) {
            if (messageStack.isEmpty()) messageStack.push("Password");
            else {
                messageStack.push(",");
                messageStack.push("password");
            }
        }
        if (fullName.isBlank()) {
            if (messageStack.isEmpty()) messageStack.push("Fullname");
            else {
                messageStack.push("and");
                messageStack.push("fullname");
            }
        }

        if (messageStack.size() == 1) {
            message.append(messageStack.pop()).append(" is not valid");
        } else if (messageStack.size() > 1) {
            while (!messageStack.isEmpty()) {
                if (messageStack.peek().equals(",")) message.insert(0, messageStack.pop());
                else message.insert(0, " " + messageStack.pop());
            }
            message.append(" are not valid");
        }

        return message.isEmpty() ?
                BaseResponse.<AuthenticationResponse>builder().status(200).build()
                :
                BaseResponse.<AuthenticationResponse>builder().status(400).message(message.toString()).build();
    }

    public BaseResponse<AuthenticationResponse> authenticate(UserRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
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
