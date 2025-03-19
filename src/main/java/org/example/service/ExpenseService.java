package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.entity.UserEntity;
import org.example.domain.entity.expense.Currency;
import org.example.domain.entity.expense.ExpenseEntity;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.ExpenseResponse;
import org.example.repository.ExpenseRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";
    @Value("${exchange.api}")
    private String API;

    public BaseResponse<List<ExpenseResponse>> save(List<String> products, List<String> quantities,
                                                    List<String> prices, UserEntity user) {
        int i = 0;

        List<ExpenseResponse> responses = new ArrayList<>();

        while (i < prices.size()) {
            ExpenseEntity expense = ExpenseEntity.builder()
                    .product(products.get(i))
                    .currency(extractCurrency(prices.get(i)))
                    .price(extractPrice(prices.get(i)))
                    .quantity(quantities.get(i))
                    .user(user).build();
            expenseRepository.save(expense);
            i++;

            Optional<ExpenseEntity> expenseEntity = findById(expense.getId());

            expenseEntity.ifPresent(entity -> responses.add(
                    ExpenseResponse.builder()
                            .id(entity.getId())
                            .price(entity.getPrice())
                            .product(entity.getProduct())
                            .quantity(entity.getQuantity())
                            .user(entity.getUser())
                            .build()
            ));
        }

        return BaseResponse.<List<ExpenseResponse>>builder()
                .message("The command saved successfully")
                .data(responses)
                .status(200)
                .build();
    }

    private Double extractPrice(String s) {
        if (s.isEmpty()) return 0D;

        StringBuilder sb = new StringBuilder();

        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
        }

        return Double.valueOf(sb.toString());
    }

    private Currency extractCurrency(String s) {
        if (s.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();

        for (char c : s.toCharArray()) {
            if (!Character.isDigit(c)) sb.append(c);
        }

        return Currency.fromString(sb.toString());
    }

    public Optional<ExpenseEntity> findById(UUID id) {
        return expenseRepository.findById(id);
    }

    public BaseResponse<ExpenseResponse> exchangeCurrency(UUID id, String currency) {
        Optional<ExpenseEntity> expense = findById(id);

        if (expense.isEmpty()) {
            return BaseResponse.<ExpenseResponse>builder()
                    .status(400)
                    .message("Expense wasn't found, please try again!")
                    .build();
        }

        ExpenseEntity expenseEntity = expense.get();

        String code = expenseEntity.getCurrency().getNames()[0];

        if (code.isEmpty()) return BaseResponse.<ExpenseResponse>builder()
                .message("Couldn't have been exchanged, please try again!")
                .status(400)
                .build();

        double converted = exchangeRate(expenseEntity.getPrice(), code, currency);

        if (converted == -1) {
            return BaseResponse.<ExpenseResponse>builder()
                    .status(400)
                    .message("failed to convert")
                    .build();
        }

        expenseEntity.setCurrency(Currency.fromString(currency));
        expenseEntity.setPrice(converted);
        expenseRepository.save(expenseEntity);

        return BaseResponse.<ExpenseResponse>builder()
                .message("Exchanged successfully")
                .status(200)
                .build();
    }

    private double exchangeRate(Double price, String base, String currency) {
        String url = BASE_URL + API + "/latest/" + base;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            double rate = json.getJSONObject("conversion_rates").getDouble(currency);
            return price * rate;
        } catch (Exception e) {
            return -1;
        }
    }
}
