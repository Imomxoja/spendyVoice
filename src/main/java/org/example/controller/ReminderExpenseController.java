package org.example.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.ReminderExpenseResponse;
import org.example.service.ReminderExpenseService;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reminder")
@RequiredArgsConstructor
public class ReminderExpenseController {
    private final ReminderExpenseService service;

    @GetMapping("/get-reminders")
    public BaseResponse<List<ReminderExpenseResponse>> loadReminders(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        return service.getUserReminders(userId);
    }

    @PostMapping("/create-reminder")
    public BaseResponse<ReminderExpenseResponse> createReminder(HttpSession session,
                                                                @RequestParam("audio") MultipartFile file) {
        UUID userId = (UUID) session.getAttribute("userId");
        return service.createReminder(userId, file);
    }

    @PostMapping("/mark-as-done")
    public BaseResponse<ReminderExpenseResponse> markAsDone(@Param("reminderId") UUID reminderId) {
        return service.markAsDone(reminderId);
    }

}
