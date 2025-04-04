package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.entity.user.UserEntity;
import org.example.domain.request.UserRequest;
import org.example.domain.response.AuthenticationResponse;
import org.example.domain.response.BaseResponse;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RequiredArgsConstructor
class AuthServiceTest {
    @InjectMocks
    private AuthService authService;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    private UserRequest validRequest;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        validRequest = UserRequest.builder()
                .email("imom1@gmail.com")
                .password("Password123")
                .fullName("Thorfin")
                .build();
        userEntity = UserEntity.builder()
                .email("imom1@gmail.com")
                .password("Password123")
                .fullName("Thorfin")
                .build();
    }

    @Test
    public void registerWithValidCredentials() {
        when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(jwtService.generateToken(any(UserEntity.class))).thenReturn("token");

        BaseResponse<AuthenticationResponse> auth = authService.register(validRequest);

        assertEquals(200, auth.getStatus());
        assertEquals("User successfully created", auth.getMessage());
        assertNotNull(auth.getData());
        assertNotNull(auth.getData().getToken());
        assertEquals("token", auth.getData().getToken());
    }

    @Test
    public void registerWithInValidCredentials() {
        UserRequest request = UserRequest.builder()
                .fullName(" ")
                .email("imom)(@$!$%.gmail.com")
                .password("aaa")
                .build();

        BaseResponse<AuthenticationResponse> auth = authService.register(request);

        assertEquals(400, auth.getStatus());
    }

    @Test
    public void registerDuplicateUser() {
        when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.of(userEntity));

        BaseResponse<AuthenticationResponse> auth = authService.register(validRequest);

        assertEquals(400, auth.getStatus());
        assertEquals("User already exists, please log in", auth.getMessage());
    }

    @Test
    public void authenticateSuccess() {
        when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.of(userEntity));
        when(jwtService.generateToken(userEntity)).thenReturn("token");

        BaseResponse<AuthenticationResponse> auth = authService.authenticate(validRequest);

        assertEquals(200, auth.getStatus());
        assertNotNull(auth.getData());
        assertEquals("token", auth.getData().getToken());
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(validRequest.getEmail(), validRequest.getPassword()));
    }

    @Test
    public void authenticationFailedThrowException() {
        doThrow(new RuntimeException()).when(authenticationManager).authenticate(any());

        BaseResponse<AuthenticationResponse> auth = authService.authenticate(validRequest);

        assertEquals(403, auth.getStatus());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    public void authenticationWithNoUser() {
        when(userRepository.findByEmail(validRequest.getEmail())).thenReturn(Optional.empty());

        BaseResponse<AuthenticationResponse> auth = authService.authenticate(validRequest);

        assertEquals(400, auth.getStatus());
        assertEquals("Password or email is invalid", auth.getMessage());
    }



}