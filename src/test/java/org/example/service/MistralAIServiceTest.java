package org.example.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MistralAIServiceTest {
    @Mock
    private HttpClient httpClient;
    @InjectMocks
    private MistralAIService mistralAIService;

    @Test
    public void getAnswerToPrompt_BadRequest() throws IOException, InterruptedException {
        HttpResponse response = mock(HttpResponse.class);
        when(httpClient.send(any(), any())).thenReturn(response);
        when(response.statusCode()).thenReturn(400);

        String res = mistralAIService.getAnswerToPrompt("");
        assertNull(res);
    }

    @Test
    public void getAnswerToPrompt_ReturnsSuccess() throws IOException, InterruptedException {
        String respBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "bla bla bla."
                      }
                    }
                  ]
                }""";
        HttpResponse response = mock(HttpResponse.class);
        when(httpClient.send(any(), any())).thenReturn(response);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(respBody);

        String res = mistralAIService.getAnswerToPrompt("");
        assertNotNull(res);
    }


}
