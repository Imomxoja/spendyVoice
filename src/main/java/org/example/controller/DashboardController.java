package org.example.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.ReminderExpenseResponse;
import org.example.service.ReminderExpenseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final ReminderExpenseService reminderService;

    @GetMapping
    public String getDashboardPage() {
        return "dashboard";
    }

    @GetMapping("/expenses")
    public String getExpensesPage() {
        return "expenses";
    }

    @GetMapping("/reminders")
    public String getRemindersPage() {
        return "reminders";
    }

    @GetMapping("/voice-commands")
    public String getVoiceCommandsPage() {
        return "voice-commands";
    }

    @GetMapping("/get-scheduled-reminders")
    public BaseResponse<List<ReminderExpenseResponse>> getSchedulerReminders(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");

        return reminderService.getScheduledReminders(userId);
    }


}
