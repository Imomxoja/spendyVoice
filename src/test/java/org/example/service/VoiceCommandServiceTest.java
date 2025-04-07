package org.example.service;

import com.assemblyai.api.AssemblyAI;
import com.assemblyai.api.PollingTranscriptsClient;
import com.assemblyai.api.resources.transcripts.types.Transcript;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import org.example.domain.entity.VoiceCommandEntity;
import org.example.domain.entity.expense.ExpenseEntity;
import org.example.domain.entity.user.UserEntity;
import org.example.domain.request.ExpenseRequest;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.VoiceCommandResponse;
import org.example.repository.UserRepository;
import org.example.repository.VoiceCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceCommandServiceTest {

    @Spy
    @InjectMocks
    private VoiceCommandService voiceCommandService;
    @Mock
    private VoiceCommandRepository repository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExpenseService expenseService;
    @Mock
    private StanfordCoreNLP pipeline;
    @Mock
    private AssemblyAI assembly;
    @Mock
    private Storage storage;
    @Mock
    private MultipartFile file;

    private VoiceCommandEntity command;

    @BeforeEach
    public void setUp() {
        command = VoiceCommandEntity.builder()
                .user(UserEntity.builder().build())
                .expense(ExpenseEntity.builder().build())
                .rawText("20kg rice for $5")
                .build();
    }

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

    @Test
    public void getAllWithPagination() {
        int page = 0, size = 10;
        UUID userId = UUID.randomUUID();

        Page<VoiceCommandEntity> mockPagination = new PageImpl<>(List.of(command));
        PageRequest pageReq =
                PageRequest.of(page, size, Sort.by("createdAt").descending());
        when(repository.getAll(userId, pageReq)).thenReturn(mockPagination);

        Page<VoiceCommandResponse> res = voiceCommandService.getAll(userId, page, size);

        assertNotNull(res);
        assertEquals(1, res.getTotalElements());
        verify(repository).getAll(userId, pageReq);
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void getAllWithPaginationEmptyInput() {
        int page = 0, size = 10;
        UUID userId = UUID.randomUUID();

        Page<VoiceCommandEntity> empty = new PageImpl<>(List.of());
        PageRequest pageReq =
                PageRequest.of(page, size, Sort.by("createdAt").descending());
        when(repository.getAll(userId, pageReq)).thenReturn(empty);

        Page<VoiceCommandResponse> res = voiceCommandService.getAll(userId, page, size);

        assertNotNull(res);
        assertTrue(res.isEmpty());
        assertEquals(0, res.getTotalElements());
        verify(repository).getAll(userId, pageReq);
    }

    @Test
    public void getConjoinedNouns_ReturnsSingleNoun_WhenNoConjunctions() {
        IndexedWord noun = mock(IndexedWord.class);
        SemanticGraph dependencies = mock(SemanticGraph.class);

        when(dependencies.outgoingEdgeList(noun)).thenReturn(List.of());

        List<IndexedWord> res = voiceCommandService.getConjoinedNouns(noun, dependencies);

        assertEquals(1, res.size());
        assertTrue(res.contains(noun));
    }

    @Test
    public void getConjoinedNouns_AddsConjoinedNouns_WhenRelationExists() {
        IndexedWord noun = mock(IndexedWord.class);
        IndexedWord conjoinedNoun = mock(IndexedWord.class);
        SemanticGraph dependencies = mock(SemanticGraph.class);
        SemanticGraphEdge edge = mock(SemanticGraphEdge.class);

        GrammaticalRelation relation = mock(GrammaticalRelation.class);
        when(edge.getRelation()).thenReturn(relation);
        when(relation.toString()).thenReturn("conj");
        when(edge.getDependent()).thenReturn(conjoinedNoun);
        when(conjoinedNoun.tag()).thenReturn("NN");
        when(dependencies.outgoingEdgeList(noun)).thenReturn(List.of(edge));

        List<IndexedWord> res = voiceCommandService.getConjoinedNouns(noun, dependencies);

        assertEquals(2, res.size());
        assertTrue(res.contains(noun));
        assertTrue(res.contains(conjoinedNoun));
    }

    @Test
    public void getConjoinedNouns_IgnoresNonNounConjunctions() {
        IndexedWord noun = mock(IndexedWord.class);
        IndexedWord nonNoun = mock(IndexedWord.class);
        SemanticGraph dependencies = mock(SemanticGraph.class);

        SemanticGraphEdge edge = mock(SemanticGraphEdge.class);
        GrammaticalRelation relation = mock(GrammaticalRelation.class);
        when(edge.getRelation()).thenReturn(relation);
        when(relation.toString()).thenReturn("conj");
        when(edge.getDependent()).thenReturn(nonNoun);
        when(nonNoun.tag()).thenReturn("VB");

        when(dependencies.outgoingEdgeList(noun)).thenReturn(List.of(edge));

        List<IndexedWord> res = voiceCommandService.getConjoinedNouns(noun, dependencies);

        assertEquals(1, res.size());
        assertTrue(res.contains(noun));
        assertFalse(res.contains(nonNoun));
    }

    @Test
    public void extractModifiers_NoModifiersFound() {
        IndexedWord head = mock(IndexedWord.class);
        SemanticGraph dependencies = mock(SemanticGraph.class);

        when(dependencies.outgoingEdgeList(head)).thenReturn(List.of());

        List<IndexedWord> res = voiceCommandService.extractModifiers(head, dependencies);

        assertEquals(1, res.size());
        assertTrue(res.contains(head));
    }

    @Test
    public void extractModifiers_AddsValidModifiers() {
        IndexedWord head = mock(IndexedWord.class);
        IndexedWord modifier = mock(IndexedWord.class);
        SemanticGraph dependencies = mock(SemanticGraph.class);
        SemanticGraphEdge edge = mock(SemanticGraphEdge.class);
        GrammaticalRelation relation = mock(GrammaticalRelation.class);

        when(edge.getRelation()).thenReturn(relation);
        when(relation.toString()).thenReturn("amod");
        when(edge.getDependent()).thenReturn(modifier);
        when(modifier.word()).thenReturn("red");
        when(dependencies.outgoingEdgeList(head)).thenReturn(List.of(edge));

        List<IndexedWord> res = voiceCommandService.extractModifiers(head, dependencies);

        assertEquals(2, res.size());
        assertTrue(res.contains(head));
        assertTrue(res.contains(modifier));
    }

    @Test
    public void extractModifiers_ExcludeQuantityWords() {
        IndexedWord head = mock(IndexedWord.class);
        IndexedWord modifier = mock(IndexedWord.class);
        SemanticGraph dependencies = mock(SemanticGraph.class);
        SemanticGraphEdge edge = mock(SemanticGraphEdge.class);
        GrammaticalRelation relation = mock(GrammaticalRelation.class);

        when(edge.getRelation()).thenReturn(relation);
        when(relation.toString()).thenReturn("compound");
        when(edge.getDependent()).thenReturn(modifier);
        when(modifier.word()).thenReturn("kg");
        when(dependencies.outgoingEdgeList(head)).thenReturn(List.of(edge));

        List<IndexedWord> res = voiceCommandService.extractModifiers(head, dependencies);

        assertEquals(1, res.size());
        assertTrue(res.contains(head));
        assertFalse(res.contains(modifier));
    }

    @Test
    public void extractFromNounPhrase_RootIsNull() {
        CoreSentence sentence = mock(CoreSentence.class);
        SemanticGraph dependencies = mock(SemanticGraph.class);

        when(sentence.dependencyParse()).thenReturn(dependencies);
        when(dependencies.getFirstRoot()).thenReturn(null);

        List<ExpenseRequest> res = voiceCommandService.extractFromNounPhrase(sentence);

        assertEquals(0, res.size());
    }

    @Test
    public void extractFromNounPhrase_RootNotNull_ExtractsCorrectly() {
        CoreSentence sentence = mock(CoreSentence.class);
        SemanticGraph dependencies = mock(SemanticGraph.class);
        IndexedWord root = mock(IndexedWord.class);
        String text = "2 big apples for $3";

        when(sentence.dependencyParse()).thenReturn(dependencies);
        when(dependencies.getFirstRoot()).thenReturn(root);
        when(sentence.text()).thenReturn(text);

        IndexedWord head = mock(IndexedWord.class);
        when(head.word()).thenReturn("apples");
        when(head.index()).thenReturn(2);

        IndexedWord modifier = mock(IndexedWord.class);
        when(modifier.word()).thenReturn("big");
        when(modifier.index()).thenReturn(1);

        /*
          modifier.index() = 1 -> big
          head.index() = 2 -> apples
          index is the position of these words -> big(1) apples(2)
        */

        doReturn(List.of(head)).when(voiceCommandService).getConjoinedNouns(root, dependencies);
        doReturn(new ArrayList<>(List.of(modifier, head))).when(voiceCommandService).extractModifiers(head, dependencies);
        doReturn("2").when(voiceCommandService).extractQuantity(text);
        doReturn("$3").when(voiceCommandService).extractPrice(text);

        List<ExpenseRequest> res = voiceCommandService.extractFromNounPhrase(sentence);

        assertEquals(1, res.size());
        ExpenseRequest expenseRequest = res.get(0);
        assertEquals("big apples", expenseRequest.getProduct());
        assertEquals("$3", expenseRequest.getPrice());
        assertEquals("2", expenseRequest.getQuantity());
    }

    @Test
    public void extractFromVerbSentence_CorrectlyExtracts() {
        IndexedWord verb = mock(IndexedWord.class);
        IndexedWord obj = mock(IndexedWord.class);
        IndexedWord modifier = mock(IndexedWord.class);
        CoreSentence sentence = mock(CoreSentence.class);
        SemanticGraph dependencies = mock(SemanticGraph.class);
        SemanticGraphEdge edge = mock(SemanticGraphEdge.class);
        GrammaticalRelation relation = mock(GrammaticalRelation.class);
        String text = "He bought a Porsche 911 for 500k dollars";

        when(sentence.dependencyParse()).thenReturn(dependencies);
        when(sentence.text()).thenReturn(text);
        when(dependencies.edgeListSorted()).thenReturn(List.of(edge));

        when(edge.getRelation()).thenReturn(relation);
        when(relation.toString()).thenReturn("obj");
        when(edge.getGovernor()).thenReturn(verb);
        when(verb.lemma()).thenReturn("buy");
        when(edge.getDependent()).thenReturn(obj);

        when(obj.word()).thenReturn("Porsche");
        when(obj.index()).thenReturn(1);
        when(modifier.word()).thenReturn("911");
        when(modifier.index()).thenReturn(2);

        doReturn(List.of(obj)).when(voiceCommandService).getConjoinedNouns(obj, dependencies);
        doReturn(new ArrayList<>(List.of(modifier, obj))).when(voiceCommandService).extractModifiers(obj, dependencies);
        doReturn("a").when(voiceCommandService).extractQuantity(text);
        doReturn("500k dollars").when(voiceCommandService).extractPrice(text);

        List<ExpenseRequest> result = voiceCommandService.extractFromVerbSentence(sentence);

        assertEquals(1, result.size());
        ExpenseRequest expense = result.get(0);
        assertEquals("Porsche 911", expense.getProduct());
        assertEquals("a", expense.getQuantity());
        assertEquals("500k dollars", expense.getPrice());
    }


    @Test
    void hasVerbs_ReturnsTrue_WhenVerbPresent() {
        CoreSentence sentence = mock(CoreSentence.class);
        CoreLabel verbToken = mock(CoreLabel.class);

        when(sentence.tokens()).thenReturn(List.of(verbToken));
        when(verbToken.get(CoreAnnotations.PartOfSpeechAnnotation.class)).thenReturn("VB ");

        boolean result = voiceCommandService.hasVerbs(sentence);

        assertTrue(result);
    }

    @Test
    void hasVerbs_ReturnsFalse_WhenNoVerbs() {
        CoreSentence sentence = mock(CoreSentence.class);
        CoreLabel nounToken = mock(CoreLabel.class);

        when(sentence.tokens()).thenReturn(List.of(nounToken));
        when(nounToken.get(CoreAnnotations.PartOfSpeechAnnotation.class)).thenReturn("NN");

        boolean result = voiceCommandService.hasVerbs(sentence);

        assertFalse(result);
    }

    @Test
    public void uploadAudioIntoCloud_WhenFileIsNull() throws Exception {
        String res = voiceCommandService.uploadAudioIntoCloud(null);

        assertEquals("", res);
        verifyNoInteractions(storage);
    }

    @Test
    public void uploadAudioIntoCloud_ValidFile_ReturnsValidURL() throws Exception {
        String BUCKET_NAME = "spendyVoice-bucket";
        String fileName = "audio/test.mp3";

        voiceCommandService.setBUCKET_NAME_OnlyForTesting(BUCKET_NAME);
        when(file.getOriginalFilename()).thenReturn("test.mp3");
        when(file.getBytes()).thenReturn(new byte[]{1,2,3});

        String result = voiceCommandService.uploadAudioIntoCloud(file);

        String url = String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileName);
        assertEquals(url, result);

        BlobId expectedBlobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo expectedBlobInfo = BlobInfo.newBuilder(expectedBlobId).build();
        verify(storage, times(1)).create(eq(expectedBlobInfo), eq(new byte[]{1, 2, 3}));
    }

    @Test
    public void comprehend_UserIdNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BaseResponse<VoiceCommandResponse> res = voiceCommandService.comprehend(userId, file);

        assertEquals(400, res.getStatus());
        assertEquals("User not found", res.getMessage());
    }

    @Test
    public void comprehend_WhenFileIsNull_ReturnsEmptyString() throws IOException {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(UserEntity.builder().build()));

        doReturn("").when(voiceCommandService).uploadAudioIntoCloud(file);

        BaseResponse<VoiceCommandResponse> res = voiceCommandService.comprehend(userId, file);

        assertEquals(400, res.getStatus());
        assertEquals("Voice couldn't be recognized", res.getMessage());
    }

    @Test
    public void comprehend_FailsToGetUrl_ThrowsException() throws IOException {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(UserEntity.builder().build()));

        doThrow(new IOException("Failed to upload audio")).when(voiceCommandService).uploadAudioIntoCloud(file);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> voiceCommandService.comprehend(userId, file));

        assertInstanceOf(IOException.class, exception.getCause());
        assertEquals("Failed to upload audio", exception.getCause().getMessage());

        verify(userRepository, times(1)).findById(userId);
        verify(voiceCommandService, times(1)).uploadAudioIntoCloud(file);
        verifyNoInteractions(assembly);
    }

    @Test
    public void comprehend_FailsToTranscribeURL_ReturnsEmptyText() throws IOException {
        UUID userId = UUID.randomUUID();
        String BUCKET_NAME = "spendyVoice-bucket";
        Transcript transcript = mock(Transcript.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(UserEntity.builder().build()));

        String url = "audio/test.mp3";

        voiceCommandService.setBUCKET_NAME_OnlyForTesting(BUCKET_NAME);
        when(file.getOriginalFilename()).thenReturn("test.mp3");
        when(voiceCommandService.uploadAudioIntoCloud(file)).thenReturn(url);
        when(assembly.transcripts()).thenReturn(mock(PollingTranscriptsClient.class));
        when(assembly.transcripts().transcribe(url)).thenReturn(transcript);
        when(transcript.getText()).thenReturn(Optional.empty());

        BaseResponse<VoiceCommandResponse> res = voiceCommandService.comprehend(userId, file);

        assertEquals(400, res.getStatus());
        assertEquals("Voice couldn't be recognized", res.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(voiceCommandService, times(1)).uploadAudioIntoCloud(file);
        verify(assembly.transcripts(), times(1)).transcribe(url);
        verify(transcript, times(1)).getText();
        verifyNoInteractions(expenseService, repository);
    }
}