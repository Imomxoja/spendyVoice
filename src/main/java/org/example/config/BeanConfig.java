package org.example.config;

import com.assemblyai.api.AssemblyAI;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class BeanConfig {
    private final UserRepository userRepository;

    @Value("${google.service.account}")
    private String PATH;
    @Value("${project.id}")
    private String PROJECT_ID;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @SneakyThrows
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public AssemblyAI assemblyAI(@Value("${assemblyai.api.key}") String API_KEY) {
        return AssemblyAI.builder().apiKey(API_KEY).build();
    }

    @Bean
    public Storage getStorage() {
        Storage storage;
        try {
            storage = StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(
                            PATH)))
                    .setProjectId(PROJECT_ID)
                    .build()
                    .getService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return storage;
    }

    @Bean
    public StanfordCoreNLP config() {
        Properties properties = new Properties();
        properties.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        properties.setProperty("sutime.binders", "0");
        return new StanfordCoreNLP(properties);
    }
}
