package org.example.service;

import org.example.domain.entity.VoiceCommandEntity;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.VoiceCommandResponse;
import org.example.repository.VoiceCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceCommandServiceTest {

    @InjectMocks
    private VoiceCommandService voiceCommandService;

    @Mock
    private VoiceCommandRepository repository;

    @Test
    void testDeleteWhenCommandExists() {
        UUID id = UUID.randomUUID();
        VoiceCommandEntity command = VoiceCommandEntity.builder().build();
        command.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(command));

        BaseResponse<VoiceCommandResponse> resp = voiceCommandService.delete(id);

        assertNotNull(resp);
        assertEquals(200, resp.getStatus());
        assertEquals("Deleted successfully", resp.getMessage());

        verify(repository, times(1)).findById(id);
        verify(repository, times(1)).deleteById(id);
    }

    @Test
    void testDeleteWhenCommandDoesNotExist() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        BaseResponse<VoiceCommandResponse> resp = voiceCommandService.delete(id);

        assertNotNull(resp);
        assertEquals(400, resp.getStatus());
        assertEquals("Command not found", resp.getMessage());

        verify(repository, times(1)).findById(id);
        verify(repository, never()).deleteById(id);
    }
}