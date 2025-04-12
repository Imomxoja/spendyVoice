package org.example.service;

import org.example.domain.entity.VoiceCommandEntity;
import org.example.domain.entity.expense.ExpenseEntity;
import org.example.domain.entity.user.UserEntity;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.ExpenseResponse;
import org.example.repository.ExpenseRepository;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {
    @Mock
    private ExpenseRepository expenseRepository;
    @Spy
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> httpResponse;
    @Spy
    @InjectMocks
    private ExpenseService expenseService;
    private ExpenseEntity expense;
    private static final String BASE_URL = "https://api.example.com/";
    private static final String API = "exchange";

    @BeforeEach
    public void setUp() {
        reset(httpClient, httpResponse);
        expense = ExpenseEntity.builder()
                .user(UserEntity.builder().build())
                .voiceCommand(VoiceCommandEntity.builder().build())
                .product("Tomato")
                .quantity("2kg")
                .price("$0.9")
                .currency("dollar")
                .build();
    }

    @Test
    public void deleteExpense_ExpenseIdNotFound() {
        UUID expenseId = UUID.randomUUID();
        when(expenseService.findById(expenseId)).thenReturn(Optional.empty());

        BaseResponse<ExpenseResponse> res = expenseService.deleteExpense(expenseId);

        assertEquals(400, res.getStatus());
        assertEquals("Expense not found", res.getMessage());
        verify(expenseRepository, times(0)).deleteById(expenseId);
    }

    @Test
    public void deleteExpense_ExpenseIdFound_ReturnsSuccess() {
        UUID expenseId = UUID.randomUUID();
        when(expenseService.findById(expenseId)).thenReturn(Optional.of(ExpenseEntity.builder().build()));

        BaseResponse<ExpenseResponse> res = expenseService.deleteExpense(expenseId);

        assertEquals(200, res.getStatus());
        assertEquals("Deleted successfully", res.getMessage());
        verify(expenseRepository, times(1)).deleteById(expenseId);
    }

    @Test
    public void getAllWithPagination() {
        int page = 0, size = 10;
        UUID userId = UUID.randomUUID();

        Page<ExpenseEntity> mockPagination = new PageImpl<>(List.of(expense));
        PageRequest pageReq =
                PageRequest.of(page, size, Sort.by("createdAt").descending());
        when(expenseRepository.getAll(userId, pageReq)).thenReturn(mockPagination);

        Page<ExpenseResponse> res = expenseService.getAll(userId, page, size);

        assertNotNull(res);
        assertEquals(1, res.getTotalElements());
        verify(expenseRepository).getAll(userId, pageReq);
        verifyNoMoreInteractions(expenseRepository);
    }

    @Test
    public void getAllWithPaginationEmptyInput() {
        int page = 0, size = 10;
        UUID userId = UUID.randomUUID();

        Page<ExpenseEntity> empty = new PageImpl<>(List.of());
        PageRequest pageReq =
                PageRequest.of(page, size, Sort.by("createdAt").descending());
        when(expenseRepository.getAll(userId, pageReq)).thenReturn(empty);

        Page<ExpenseResponse> res = expenseService.getAll(userId, page, size);

        assertNotNull(res);
        assertTrue(res.isEmpty());
        assertEquals(0, res.getTotalElements());
        verify(expenseRepository).getAll(userId, pageReq);
    }

    @Test
    public void exchangeRate_SuccessfulConversion() throws Exception {
        Double price = 100.0;
        String from = "USD";
        String to = "EUR";
        String expectedUrl = BASE_URL + API + "/latest/" + from;

        String mockJson = new JSONObject()
                .put("conversion_rates", new JSONObject().put("EUR", 0.9))
                .toString();

        when(httpResponse.body()).thenReturn(mockJson);
        doReturn(httpResponse)
                .when(httpClient)
                .send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));

        String result = expenseService.exchangeRate(price, from, to);

        assertEquals("90.0", result);
        verify(httpClient).send(argThat(request ->
                request.uri().equals(URI.create(expectedUrl))), any());
    }
}
