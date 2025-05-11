package org.example.service;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
public class MistralAIService {
    @Value("${together.api}")
    private String TOGETHER_API;
    private final HttpClient httpClient;
    String MODEL = "mistralai/Mistral-7B-Instruct-v0.3";

    public String getAnswerToPrompt (String prompt) {
        JSONObject payload = new JSONObject();
        JSONObject message = new JSONObject();
        payload.put("model", MODEL);
        message.put("role", "user");
        message.put("content", prompt);
        payload.put("messages", new JSONObject[]{message});
        payload.put("max_tokens", 256);
        payload.put("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.together.xyz/v1/chat/completions"))
                .header("Authorization", "Bearer " + TOGETHER_API)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        try {
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(resp.body());
                return jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public String findCategory(String product) {
        String prompt = "categorize this product \"" + product + "\" with this format (main category->sub category->product)without any additional comments" +
                ", if it's not the exact name of a product just return unknown";

        return getAnswerToPrompt(prompt);
    }

    public String getContentsFromReminderAudio(String rawText) {
        String prompt = "[" + rawText + "]" +
                " the text inside the brackets is raw-text and you need to extract few information out of it. These are" +
                " item(it could be anything e.g. bills, fine, fee, product...) that money is about to be spent for, price, quantity and due date" +
                " all extracted information must be in quotation mark and" +
                " you need to extract information just how it is without any extra opinions, " +
                "if either of them doesn't exist just put unknown and also put the date in this sort of formats 'dd-MM-YYYY HH:mm'" +
                "or if there is not given time just put that in this format 'dd-MM-YYYY'" +
                " here's the structure that you must send the answer in " +
                "item : " + "," +
                "price : " + "," +
                "quantity : " + "," +
                "due : " + "." ;
        return getAnswerToPrompt(prompt);
    }
}

