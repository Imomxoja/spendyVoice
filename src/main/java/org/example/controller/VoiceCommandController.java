package org.example.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.VoiceCommandResponse;
import org.example.service.VoiceCommandService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/command")
@RequiredArgsConstructor
public class VoiceCommandController {
    private final VoiceCommandService service;

    @PostMapping("/input")
    public BaseResponse<VoiceCommandResponse> inputVoice(HttpSession session, @RequestParam("audio") MultipartFile file) {
        UUID userId = (UUID) session.getAttribute("userId");
        return service.comprehend(userId, file);
    }
}
