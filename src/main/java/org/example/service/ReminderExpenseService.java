package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.entity.ReminderExpenseEntity;
import org.example.domain.entity.user.UserEntity;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.ReminderExpenseResponse;
import org.example.repository.ReminderExpenseRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReminderExpenseService {
    private final ReminderExpenseRepository repository;
    private final UserRepository userRepository;

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
}
