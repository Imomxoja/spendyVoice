package org.example.domain.response;

import lombok.*;
import org.example.domain.entity.UserEntity;
import org.example.domain.entity.VoiceCommandEntity;

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
    private Double price;
    private UserEntity user;
    private VoiceCommandEntity voiceCommand;
}
