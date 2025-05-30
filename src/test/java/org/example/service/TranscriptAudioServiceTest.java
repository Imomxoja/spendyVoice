package org.example.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class TranscriptAudioServiceTest {
    @Mock
    private Storage storage;
    @Mock
    private MultipartFile file;
    @InjectMocks
    private TranscriptAudioService audioService;


    @Test
    public void uploadAudioIntoCloud_WhenFileIsNull() throws Exception {
        String res = audioService.uploadAudioIntoCloud(null);

        assertEquals("", res);
        verifyNoInteractions(storage);
    }

    @Test
    public void uploadAudioIntoCloud_ValidFile_ReturnsValidURL() throws Exception {
        String BUCKET_NAME = "spendyVoice-bucket";
        String fileName = "audio/test.mp3";

        audioService.setBUCKET_NAME_OnlyForTesting(BUCKET_NAME);
        when(file.getOriginalFilename()).thenReturn("test.mp3");
        when(file.getBytes()).thenReturn(new byte[]{1,2,3});

        String result = audioService.uploadAudioIntoCloud(file);

        String url = String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileName);
        assertEquals(url, result);

        BlobId expectedBlobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo expectedBlobInfo = BlobInfo.newBuilder(expectedBlobId).build();
        verify(storage, times(1)).create(eq(expectedBlobInfo), eq(new byte[]{1, 2, 3}));
      }
}
