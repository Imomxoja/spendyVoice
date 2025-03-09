package org.example.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class GoogleCloudConfig {
    @Bean
    public Storage getStorageService() throws IOException {
        return StorageOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(new FileInputStream(
                        "C:/Users/User/Downloads/spendyvoice-f9a1d464ad18.json")))
                .setProjectId("spendyvoice")
                .build().getService();
    }
}
