package org.example.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.domain.entity.expense.ExpenseEntity;

@Entity(name = "commands")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VoiceCommandEntity extends BaseEntity {
    private String rawText;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "expense_id", unique = true)
    private ExpenseEntity expense;
}
