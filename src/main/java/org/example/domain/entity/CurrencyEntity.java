package org.example.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.List;

@Entity(name = "currencies")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyEntity extends BaseEntity {
    private String code;  // Example: USD, EUR
    private String name;  // Example: US Dollar, Euro
    private String symbol; // Example: $, €
    private int decimalPlaces;
    @OneToMany(mappedBy = "currency")
    private List<UserEntity> users;
}
