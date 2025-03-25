package org.example.domain.request;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseRequest {
    private String product;
    private String price;
    private String quantity;
}
