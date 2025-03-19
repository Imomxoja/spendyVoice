package org.example.domain.entity.expense;

import jakarta.persistence.Entity;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.*;
import org.example.domain.entity.BaseEntity;
import org.example.domain.entity.UserEntity;
import org.example.domain.entity.VoiceCommandEntity;

@Entity(name = "expenses")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseEntity extends BaseEntity {
    private String product;
    private String quantity;
    private Double price;
    private Currency currency;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
    @OneToOne(mappedBy = "expense")
    private VoiceCommandEntity voiceCommand;
}
