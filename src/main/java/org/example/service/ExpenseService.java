package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.entity.Dataset;
import org.example.domain.entity.user.UserEntity;
import org.example.domain.entity.expense.Currency;
import org.example.domain.entity.expense.ExpenseEntity;
import org.example.domain.request.ExpenseRequest;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.ExpenseResponse;
import org.example.repository.DatasetRepository;
import org.example.repository.ExpenseRepository;
import org.glassfish.jaxb.core.v2.TODO;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final DatasetRepository datasetRepository;
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";
    @Value("${exchange.api}")
    private String API;
    private final HttpClient httpClient;

    public BaseResponse<List<ExpenseResponse>> save(List<ExpenseRequest> expenses, UserEntity user) {
        List<ExpenseResponse> responses = new ArrayList<>();
        // TODO: need to be retested

        for (ExpenseRequest exp : expenses) {
            StringBuilder category = new StringBuilder("unknown");
            if (exp.getProduct() != null) {
                Optional<Dataset> dataset = datasetRepository.searchForCategoryByProductName(exp.getProduct());
                if (dataset.isPresent()) {
                    Dataset data = dataset.get();
                    category.setLength(0);
                    if (data.getMainCategory().equalsIgnoreCase(data.getSubCategory())) category.append(data.getMainCategory());
                    else category.append(data.getMainCategory()).append("/").append(data.getSubCategory());
                }
            }
            ExpenseEntity expense = ExpenseEntity.builder()
                    .product(exp.getProduct() == null ? "not provided" : exp.getProduct())
                    .currency(extractCurrency(exp.getPrice()) == null ? "not provided" : extractCurrency(exp.getPrice()))
                    .price(extractPrice(exp.getPrice()) == null ? "not provided" : extractPrice(exp.getPrice()))
                    .quantity(exp.getQuantity().isEmpty() ? "not provided" : exp.getQuantity())
                    .category(category.toString())
                    .user(user).build();
            expenseRepository.save(expense);

            Optional<ExpenseEntity> expenseEntity = findById(expense.getId());

            expenseEntity.ifPresent(entity -> responses.add(
                    ExpenseResponse.builder()
                            .id(entity.getId())
                            .price(entity.getPrice())
                            .product(entity.getProduct())
                            .quantity(entity.getQuantity())
                            .category(entity.getCategory())
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

    private String extractPrice(String s) {
        if (s == null || s.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();

        for (char c : s.toCharArray()) {
            if (Character.isDigit(c) || c == '.' || c == ',') sb.append(c);
        }

        return sb.toString();
    }

    private String extractCurrency(String s) {
        if (s == null || s.isEmpty()) return null;

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
        String code = expenseEntity.getCurrency();

        if (code == null || code.isEmpty()) return BaseResponse.<ExpenseResponse>builder()
                .message("Couldn't have been exchanged, please try again!")
                .status(400)
                .build();

        String converted = exchangeRate(Double.valueOf(expenseEntity.getPrice()), code, currency);

        if (converted == null) {
            return BaseResponse.<ExpenseResponse>builder()
                    .status(400)
                    .message("failed to convert")
                    .build();
        }

        expenseEntity.setCurrency(currency);
        expenseEntity.setPrice(converted);
        expenseRepository.save(expenseEntity);

        return BaseResponse.<ExpenseResponse>builder()
                .message("Exchanged successfully")
                .status(200)
                .build();
    }

    public String exchangeRate(Double price, String from, String to) {
        String url = BASE_URL + API + "/latest/" + from;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());

            // conversion rates contain all the currencies with the latest rates
            if (!json.has("conversion_rates")) {
                throw new RuntimeException("conversion_rates not found");
            }

            JSONObject conversionRates = json.getJSONObject("conversion_rates");

            if (!conversionRates.has(to)) {
                throw new RuntimeException("Target currency not found in conversion rates: " + to);
            }

            double rate = conversionRates.getDouble(to);
            return "" + price * rate;
        } catch (Exception e) {
            return null;
        }
    }

    public Page<ExpenseResponse> getAll(UUID id, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ExpenseEntity> expenses = expenseRepository.getAll(id, pageable);

        return expenses.map(expense -> ExpenseResponse.builder()
                .user(expense.getUser())
                .quantity(expense.getQuantity())
                .product(expense.getProduct())
                .id(expense.getId())
                .price(expense.getPrice())
                .currency(expense.getCurrency())
                .voiceCommand(expense.getVoiceCommand())
                .createdDate(expense.getCreatedDate())
                .build());
    }

    public BaseResponse<ExpenseResponse> deleteExpense(UUID expenseId) {
        Optional<ExpenseEntity> expense = findById(expenseId);
        if (expense.isEmpty())
            return BaseResponse.<ExpenseResponse>builder().status(400).message("Expense not found").build();

        expenseRepository.deleteById(expenseId);
        return BaseResponse.<ExpenseResponse>builder()
                .status(200)
                .message("Deleted successfully")
                .build();
    }
}
