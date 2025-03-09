package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.domain.request.UserRequest;
import org.example.domain.response.AuthenticationResponse;
import org.example.domain.response.BaseResponse;
import org.example.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthService authService;

    @PostMapping("/register")
    public BaseResponse<AuthenticationResponse> register(@RequestBody UserRequest request) {
        return authService.register(request);
    }

    @PostMapping("/authenticate")
    public BaseResponse<AuthenticationResponse> authenticate(@RequestBody UserRequest request) {
        return authService.authenticate(request);
    }
}
