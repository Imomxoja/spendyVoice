package org.example.service;

import com.assemblyai.api.AssemblyAI;
import com.assemblyai.api.resources.transcripts.types.Transcript;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class TranscriptAudioService {
    private final Storage storage;
    private final AssemblyAI assembly;

    @Value("${google.cloud.bucket.name}")
    private String BUCKET_NAME;

    public String uploadAudioIntoCloud(MultipartFile file) throws IOException {
        if (file == null || file.getOriginalFilename() == null) return "";
        String fileName = "audio/" + file.getOriginalFilename();
        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, file.getBytes());

        return String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileName);
    }

    public String getTheText(MultipartFile file) {
        String url;
        try {
            // temporary saving the audio in order to have url for AAI to access the audio
            url = uploadAudioIntoCloud(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (url.isEmpty()) return null;

        Transcript transcript = assembly.transcripts().transcribe(url);

        if (transcript.getText().isEmpty()) return null;

        return transcript.getText().get();
    }
}
