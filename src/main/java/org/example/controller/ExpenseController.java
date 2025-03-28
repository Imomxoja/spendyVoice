package org.example.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.ExpenseResponse;
import org.example.service.ExpenseService;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/expense")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class ExpenseController {
    private final ExpenseService expenseService;

    @GetMapping("/exchange")
    @PreAuthorize("hasAnyAuthority('user:update', 'admin:update')")
    public BaseResponse<ExpenseResponse> currencyExchange(@Param("id") UUID id, String currency) {
        return expenseService.exchangeCurrency(id, currency);
    }

    @GetMapping("/expenses")
    @PreAuthorize("hasAnyAuthority('user:read', 'admin:read')")
    public Page<ExpenseResponse> getAll(HttpSession session,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        UUID id = (UUID) session.getAttribute("userId");
        return expenseService.getAll(id, page, size);
    }

    @GetMapping("/delete-expense")
    @PreAuthorize("hasAnyAuthority('user:delete', 'admin:delete')")
    public BaseResponse<ExpenseResponse> delete(@Param("id") UUID expenseId) {
        return expenseService.deleteExpense(expenseId);
    }
}
