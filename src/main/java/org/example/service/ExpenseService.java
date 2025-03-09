package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.entity.UserEntity;
import org.example.domain.entity.expense.ExpenseEntity;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.ExpenseResponse;
import org.example.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {
     private final ExpenseRepository expenseRepository;

     public BaseResponse<ExpenseResponse> save(List<String> products, List<String> quantities,
                                               List<String> prices, UserEntity user) {
          int i = 0;

          while (i < prices.size()) {
               ExpenseEntity expense = ExpenseEntity.builder()
                       .product(products.get(i))
                       .price(Double.valueOf(prices.get(i)))
                       .quantity(quantities.get(i))
                       .user(user).build();
               expenseRepository.save(expense);
               i++;
          }

          return BaseResponse.<ExpenseResponse>builder()
                  .message("The command saved successfully")
                  .status(200)
                  .build();
     }
}
