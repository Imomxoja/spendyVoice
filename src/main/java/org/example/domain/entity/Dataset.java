package org.example.domain.entity;

import jakarta.persistence.Entity;
import lombok.*;

@Entity(name = "dataset")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Dataset extends BaseEntity {
    private String product;
    private String mainCategory;
    private String subCategory;
}
