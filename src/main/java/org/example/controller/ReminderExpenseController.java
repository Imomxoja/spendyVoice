package org.example.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.ReminderExpenseResponse;
import org.example.service.ReminderExpenseService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
