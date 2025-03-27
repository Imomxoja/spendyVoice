package org.example.domain.response;

import lombok.*;
import org.example.domain.entity.UserEntity;
import org.example.domain.entity.expense.ExpenseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VoiceCommandResponse {
    private UUID id;
    private String rawText;
    private UserEntity user;
    private ExpenseEntity expense;
    private LocalDateTime createdDate;
}
