package org.example.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.VoiceCommandResponse;
import org.example.service.VoiceCommandService;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/command")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class VoiceCommandController {
    private final VoiceCommandService service;

    @PostMapping("/input")
    @PreAuthorize("hasAnyAuthority('user:create', 'admin:create')")
    public BaseResponse<VoiceCommandResponse> inputVoice(HttpSession session, @RequestParam("audio") MultipartFile file) {
        UUID userId = (UUID) session.getAttribute("userId");
        return service.comprehend(userId, file);
    }

    @GetMapping("/get-commands")
    @PreAuthorize("hasAnyAuthority('user:read', 'admin:read')")
    public Page<VoiceCommandResponse> getAll(HttpSession session,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        UUID userId = (UUID) session.getAttribute("userId");
        return service.getAll(userId, page, size);
    }

    @GetMapping("/delete-command")
    @PreAuthorize("hasAnyAuthority('user:delete', 'admin:delete')")
    public BaseResponse<VoiceCommandResponse> deleteCommand(@Param("id") UUID voiceCommandID) {
        return service.delete(voiceCommandID);
    }

}
