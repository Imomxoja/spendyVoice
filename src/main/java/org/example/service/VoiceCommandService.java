package org.example.service;

import com.assemblyai.api.AssemblyAI;
import com.assemblyai.api.resources.transcripts.types.Transcript;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.RequiredArgsConstructor;
import org.example.domain.entity.UserEntity;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.VoiceCommandResponse;
import org.example.repository.UserRepository;
import org.example.repository.VoiceCommandRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VoiceCommandService {
    private final UserRepository userRepository;
    private final VoiceCommandRepository voiceCommandRepository;
    private final ExpenseService expenseService;
    private final StanfordCoreNLP stanfordCoreNLP;
    private final AssemblyAI assembly;


    @Value("${google.cloud.bucket.name}")
    private String BUCKET_NAME;
    @Value("${project.id}")
    private String PROJECT_ID;
    @Value("${google.service.account}")
    private String PATH;
    private static final Set<String> CURRENCY_WORDS =
            Set.of("dollars", "euros", "pounds", "dollar", "euro", "pound");
    private static final Set<String> MEASUREMENT_WORDS =
            Set.of("tonne", "tonnes","kilograms", "grams", "liters", "milliliters", "units", "pieces", "kg", "g",
                    "l", "ml", "kilogram", "gram", "liter", "milliliter", "unit", "piece");


    public BaseResponse<VoiceCommandResponse> comprehend(UUID userId, MultipartFile file) {
        Optional<UserEntity> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return BaseResponse.<VoiceCommandResponse>builder()
                    .status(400)
                    .message("User not found")
                    .build();
        }

        String url;
        try {
            // temporary saving the audio in order to have url for AAI to access the audio
            url = uploadAudioIntoCloud(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Transcript transcript = assembly.transcripts().transcribe(url);

        if (transcript.getText().isEmpty()) {
            return BaseResponse.<VoiceCommandResponse>builder()
                    .message("Voice couldn't be recognized")
                    .status(400)
                    .build();
        }

        Map<String, List<String>> extracted = analyzeText(transcript.getText().get());

        return save(extracted, user.get());
    }

    private BaseResponse<VoiceCommandResponse> save(Map<String, List<String>> extracted, UserEntity user) {
        List<String> products = extracted.get("PRODUCT");
        List<String> quantities = extracted.get("QUANTITY");
        List<String> prices = extracted.get("PRICE");
        int p = products.size(), q = quantities.size(), pr = prices.size();

        if (p == 0 && q == 0 && pr == 0 || p != q || p != pr) return BaseResponse.<VoiceCommandResponse>builder()
                .message("The command couldn't have been recognized, please record yourself accurately!")
                .status(400)
                .build();

        expenseService.save(products, quantities, prices, user);

        return BaseResponse.<VoiceCommandResponse>builder()
                .message("Expenses saved successfully")
                .status(200)
                .build();
    }

    private String uploadAudioIntoCloud(MultipartFile file) throws IOException {
        Storage storage = StorageOptions.newBuilder()
                .setCredentials(ServiceAccountCredentials.fromStream(
                        new FileInputStream(PATH)
                ))
                .setProjectId(PROJECT_ID)
                .build()
                .getService();

        String fileName = "audio/" + file.getOriginalFilename();
        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, file.getBytes());

        return String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileName);
    }

    public Map<String, List<String>> analyzeText(String text) {
        CoreDocument coreDocument = new CoreDocument(text);
        stanfordCoreNLP.annotate(coreDocument);
        List<CoreLabel> coreLabels = coreDocument.tokens();

        Map<String, List<String>> extractedData = new HashMap<>();
        extractedData.put("PRODUCT", new ArrayList<>());
        extractedData.put("PRICE", new ArrayList<>());
        extractedData.put("QUANTITY", new ArrayList<>());

        for (int i = 0; i < coreLabels.size(); i++) {
            CoreLabel token = coreLabels.get(i);
            String word = token.originalText();
            String posTag = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            String nerTag = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

            if ("PRODUCT".equalsIgnoreCase(nerTag) || (posTag.startsWith("NN")
                    && !CURRENCY_WORDS.contains(word) && !MEASUREMENT_WORDS.contains(word))) {
                if (i-1 >= 0 && coreLabels.get(i - 1).get(CoreAnnotations.PartOfSpeechAnnotation.class).equals("JJ")) {
                    String prevAdjWord = coreLabels.get(i - 1).word();
                    extractedData.get("PRODUCT").add(prevAdjWord + " " + word);
                } else {
                    extractedData.get("PRODUCT").add(word);
                }
            }

            if ("MONEY".equalsIgnoreCase(nerTag)) {
                if (i < coreLabels.size() - 1) {
                    String nextWord = coreLabels.get(i + 1).originalText();
                    if (nextWord.matches("\\d+")) {
                        extractedData.get("PRICE").add(nextWord + word);
                        i++;
                    }
                }
            } else if (CURRENCY_WORDS.contains(word)) {
                if (i < coreLabels.size() - 1) {
                    String nextWord = coreLabels.get(i + 1).originalText();
                    if (nextWord.matches("\\d+")) {
                        extractedData.get("PRICE").add(word + nextWord);
                        i++;
                    }
                }
            }

            if ("NUMBER".equalsIgnoreCase(nerTag) && i < coreLabels.size() - 1) {
                String nextWord = coreLabels.get(i + 1).originalText().toLowerCase();
                if (MEASUREMENT_WORDS.contains(nextWord)) {
                    extractedData.get("QUANTITY").add(word + " " + nextWord);
                    i++;
                }
            }
        }

        return extractedData;
    }
}
