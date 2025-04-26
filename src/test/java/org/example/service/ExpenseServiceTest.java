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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
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
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> httpResponse;
    @Spy
    @InjectMocks
    private ExpenseService expenseService;
    private ExpenseEntity expense;
    @Value("${exchange.api}")
    private String apiKey;
    @Value("${together.api}")
    private String TOGETHER_API;

    @BeforeEach
    public void setUp() {
        reset(httpClient, httpResponse);
        expense = ExpenseEntity.builder()
                .user(UserEntity.builder().build())
                .voiceCommand(VoiceCommandEntity.builder().build())
                .product("Tomato")
                .quantity("2kg")
                .price("100.0")
                .currency("USD")
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
        String expectedUrl = "https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/" + from;

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

    @Test
    public void exchangeRate_NoConversionRates_ReturnsNull() throws Exception {
        when(httpResponse.body()).thenReturn("");
        doReturn(httpResponse)
                .when(httpClient)
                .send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));

        String resp = expenseService.exchangeRate(100D, "USD", "EUR");

        assertNull(resp);
    }

    @Test
    public void exchangeRate_InvalidCurrency_ReturnsNull() throws Exception {
        JSONObject conversionRates = new JSONObject();
        conversionRates.put("EUR", 0.9);
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("conversion_rates", conversionRates);

        when(httpResponse.body()).thenReturn(jsonResponse.toString());
        doReturn(httpResponse)
                .when(httpClient)
                .send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));

        String res = expenseService.exchangeRate(100D, "USD", "ABCDEFG");

        assertNull(res);
    }

    @Test
    public void exchangeCurrency_IdNotFound() {
        UUID id = UUID.randomUUID();
        when(expenseService.findById(id)).thenReturn(Optional.empty());

        BaseResponse<ExpenseResponse> res = expenseService.exchangeCurrency(id, "EUR");

        assertEquals(400, res.getStatus());
        assertEquals("Expense wasn't found, please try again!", res.getMessage());
    }

    @Test
    public void exchangeCurrency_CodeIsEmpty() {
        UUID id = UUID.randomUUID();
        Optional<ExpenseEntity> expenseEntity =
                Optional.of(ExpenseEntity.builder().build());
        when(expenseService.findById(id)).thenReturn(expenseEntity);

        BaseResponse<ExpenseResponse> res = expenseService.exchangeCurrency(id, "EUR");

        assertEquals(400, res.getStatus());
        assertEquals("Couldn't have been exchanged, please try again!", res.getMessage());
    }

    @Test
    public void exchangeCurrency_Success() {
        UUID id = UUID.randomUUID();
        String currency = "EUR";
        String convertedPrice = "90.0";

        when(expenseService.findById(id)).thenReturn(Optional.of(expense));
        doReturn(convertedPrice).when(expenseService).exchangeRate(100.0, "USD", currency);
        when(expenseRepository.save(any(ExpenseEntity.class))).thenReturn(expense);

        BaseResponse<ExpenseResponse> res = expenseService.exchangeCurrency(id, currency);

        assertEquals(200, res.getStatus());
        assertEquals("Exchanged successfully", res.getMessage());
        verify(expenseRepository).findById(id);
        verify(expenseService).exchangeRate(100.0, "USD", currency);
        verify(expenseRepository).save(expense);
        verifyNoInteractions(httpClient);
    }

    @Test
    public void exchangeCurrency_FailsToConvert() {
        UUID id = UUID.randomUUID();
        String currency = "EUR";

        when(expenseService.findById(id)).thenReturn(Optional.of(expense));
        doReturn(null).when(expenseService).exchangeRate(100.0, "USD", currency);

        BaseResponse<ExpenseResponse> res = expenseService.exchangeCurrency(id, currency);

        assertEquals(400, res.getStatus());
        assertEquals("failed to convert", res.getMessage());
        verify(expenseRepository).findById(id);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    public void findCategory_SuccessWithSuitableProduct () throws IOException, InterruptedException {
        String product = "marshall headphone";
        String respCategory = "Electronics->Headphones->Marshall headphone";
        String resp = "{\"choices\":[{\"message\":{\"content\":\"" + respCategory + "\"}}]}";

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(resp);
        doReturn(httpResponse)
                .when(httpClient)
                .send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));

        String res = expenseService.findCategory(product);
        assertEquals(respCategory, res);
        verify(httpClient).send(argThat(request ->
                request.uri().equals(URI.create("https://api.together.xyz/v1/chat/completions")) &&
                        request.headers().firstValue("Authorization").orElse("").equals("Bearer " + TOGETHER_API) &&
                        request.method().equals("POST")
        ), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    public void findCategory_ReturnsNull() throws IOException, InterruptedException {
        String product = ".";
        String resp = "{\"choices\":[{\"message\":{\"content\":\"unknown\"}}]}";

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(resp);
        doReturn(httpResponse)
                .when(httpClient)
                .send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
        String res = expenseService.findCategory(product);
        assertNull(res);
    }

    @Test
    public void findCategory_ReturnsNullDueToHttpError() throws IOException, InterruptedException {
        String product = "marshall headphone";

        when(httpResponse.statusCode()).thenReturn(500);
        doReturn(httpResponse)
                .when(httpClient)
                .send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));

        String res = expenseService.findCategory(product);

        assertNull(res);
    }

}
