package org.example.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;
import org.example.domain.entity.user.UserEntity;

import java.time.LocalDateTime;

@Entity(name = "reminder")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReminderExpenseEntity extends BaseEntity {
    private String item;
    private String quantity;
    private String price;
    private LocalDateTime dueDate;
    private Boolean markedAsDone;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
}
