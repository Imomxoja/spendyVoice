package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.entity.ReminderExpenseEntity;
import org.example.domain.entity.user.UserEntity;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.ReminderExpenseResponse;
import org.example.repository.ReminderExpenseRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReminderExpenseService {
    private final ReminderExpenseRepository repository;
    private final UserRepository userRepository;
    private final TranscriptAudioService transcriptService;
    private final VoiceCommandService voiceCommandService;
    private final MistralAIService mistralAIService;

    public BaseResponse<List<ReminderExpenseResponse>> getUserReminders(UUID userId) {
        Optional<UserEntity> user = userRepository.findById(userId);
        if (user.isEmpty()) return BaseResponse.<List<ReminderExpenseResponse>>builder()
                .message("User not found").status(400)
                .build();

        List<ReminderExpenseEntity> reminders = repository.getUserReminders(userId);
        List<ReminderExpenseResponse> res = reminders.stream()
                .map(reminder -> ReminderExpenseResponse.builder()
                        .user(user.get())
                        .item(reminder.getItem())
                        .price(reminder.getPrice())
                        .quantity(reminder.getQuantity())
                        .dueDate(reminder.getDueDate())
                        .build())
                .collect(Collectors.toList());

        return BaseResponse.<List<ReminderExpenseResponse>>builder()
                .data(res)
                .build();
    }

    public BaseResponse<ReminderExpenseResponse> createReminder(UUID userId, MultipartFile file) {
        String rawText = transcriptService.getTheText(file);
        if (rawText == null) return BaseResponse.<ReminderExpenseResponse>builder()
                .message("The audio couldn't be recognized").status(400).build();

        Optional<UserEntity> user = userRepository.findById(userId);
        if (user.isEmpty()) return BaseResponse.<ReminderExpenseResponse>builder()
                .message("User not found").status(400).build();

        String ans = mistralAIService.getContentsFromReminderAudio(rawText);
        if (ans == null) return BaseResponse.<ReminderExpenseResponse>builder()
                .message("The audio couldn't be recognized").status(400).build();

        List<String> values = getTheFieldValues(ans);
        if (values.size() < 4) return BaseResponse.<ReminderExpenseResponse>builder()
                .message("command cannot be processed").status(400).build();

        String item = values.get(0), price = values.get(1), quantity = values.get(2), due = values.get(3);

        LocalDate dueWithDate = formatTheDate(due);
        LocalDateTime dueWithDateAndTime = formatTheDateTime(due);

        if (dueWithDate == null && dueWithDateAndTime == null) return BaseResponse.<ReminderExpenseResponse>builder()
                .message("You forgot to mention a due date").status(400).build();

        LocalDateTime dueDate = (dueWithDate != null) ?
                 dueWithDate.atTime(LocalTime.MIDNIGHT) : dueWithDateAndTime;

        if (dueDate.isBefore(LocalDateTime.now())) return BaseResponse.<ReminderExpenseResponse>builder()
                .message("Due date is not valid").status(400).build();

        ReminderExpenseEntity reminder = ReminderExpenseEntity.builder()
                .item(item)
                .quantity(quantity)
                .price(price)
                .dueDate(dueDate)
                .markedAsDone(false)
                .user(user.get())
                .build();

        ReminderExpenseEntity reminderExpense = repository.save(reminder);
        voiceCommandService.saveForReminder(user.get(), reminderExpense, rawText);

        return BaseResponse.<ReminderExpenseResponse>builder()
                .message("Reminder saved successfully")
                .status(200)
                .build();
    }

    private LocalDateTime formatTheDateTime(String due) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        try {
            return LocalDateTime.parse(due, format);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalDate formatTheDate(String due) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        try {
            return LocalDate.parse(due, format);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private List<String> getTheFieldValues(String ans) {
        List<String> res = new ArrayList<>();
        boolean isOpen = false;
        StringBuilder sb = new StringBuilder();

        for (char c : ans.toCharArray()) {
            if (c == '"') {
                isOpen = !isOpen;
                if (!sb.isEmpty()) res.add(sb.toString());
                sb.setLength(0);
            }
            if (isOpen) sb.append(c);
        }
        return res;
    }

    public BaseResponse<ReminderExpenseResponse> markAsDone(UUID reminderId) {
        Optional<ReminderExpenseEntity> reminderEntity = repository.findById(reminderId);

        if (reminderEntity.isEmpty()) return BaseResponse.<ReminderExpenseResponse>builder()
                .message("Reminder not found").status(400).build();

        ReminderExpenseEntity reminder = reminderEntity.get();
        reminder.setMarkedAsDone(true);

        repository.save(reminder);

        return BaseResponse.<ReminderExpenseResponse>builder()
                .status(200).build();
    }


    public BaseResponse<List<ReminderExpenseResponse>> getScheduledReminders(UUID userId) {
        List<ReminderExpenseEntity> reminders = repository.getScheduledReminders(userId);

        List<ReminderExpenseResponse> result = reminders.stream().map(reminder ->
            ReminderExpenseResponse.builder()
                    .dueDate(reminder.getDueDate())
                    .item(reminder.getItem())
                    .user(reminder.getUser())
                    .price(reminder.getPrice())
                    .quantity(reminder.getQuantity())
                    .build()
        ).collect(Collectors.toList());

        return BaseResponse.<List<ReminderExpenseResponse>>builder()
                .data(result).status(200).message("Scheduled user reminders").build();
    }
}
