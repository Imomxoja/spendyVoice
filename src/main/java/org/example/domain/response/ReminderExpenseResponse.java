package org.example.domain.response;

import lombok.*;
import org.example.domain.entity.user.UserEntity;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReminderExpenseResponse {
    private UserEntity user;
    private String item;
    private String quantity;
    private String price;
    private LocalDateTime dueDate;
}
