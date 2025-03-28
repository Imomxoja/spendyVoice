package org.example.domain.response;

import lombok.*;
import org.example.domain.entity.user.UserEntity;
import org.example.domain.entity.VoiceCommandEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExpenseResponse {
    private UUID id;
    private String product;
    private String quantity;
    private String price;
    private UserEntity user;
    private String currency;
    private VoiceCommandEntity voiceCommand;
    private LocalDateTime createdDate;
}
