package org.example.service;

import com.assemblyai.api.AssemblyAI;
import com.assemblyai.api.resources.transcripts.types.Transcript;
import com.google.cloud.storage.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import lombok.RequiredArgsConstructor;
import org.example.domain.entity.user.UserEntity;
import org.example.domain.entity.VoiceCommandEntity;
import org.example.domain.request.ExpenseRequest;
import org.example.domain.response.BaseResponse;
import org.example.domain.response.ExpenseResponse;
import org.example.domain.response.VoiceCommandResponse;
import org.example.repository.UserRepository;
import org.example.repository.VoiceCommandRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoiceCommandService {
    private final UserRepository userRepository;
    private final VoiceCommandRepository voiceCommandRepository;
    private final ExpenseService expenseService;
    private final StanfordCoreNLP pipeline;
    private final AssemblyAI assembly;
    private final Storage storage;

    @Value("${google.cloud.bucket.name}")
    private String BUCKET_NAME;

    private final Set<String> productVerbs = Set.of( "buy", "purchase", "take", "get", "review", "use");
    private final Pattern quantityPattern = Pattern.compile(
            "\\b(\\d+(?:[.,]\\d+)?)\\s*(tonne|tonnes|unit|piece|pieces|units|pound|pounds|kilogram|gram|milligram|kg|kilograms|g|grams|mg|milligrams|l|liter|liters|ml|milliliters)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private final Pattern pricePattern = Pattern.compile(
            "([$€£¥])\\s*(\\d+(?:[.,]\\d+)?)|(\\d+(?:[.,]\\d+)?)\\s*(dollars|bucks|euros|pounds|yen)",
            Pattern.CASE_INSENSITIVE
    );

    public BaseResponse<VoiceCommandResponse> comprehend(UUID userId, MultipartFile file) {
//        Optional<UserEntity> user = userRepository.findById(UUID.fromString("7a7f4e2f-caa9-41d4-a6e2-8ac6a56b3b98"));
//        if (user.isEmpty()) {
//            return BaseResponse.<VoiceCommandResponse>builder()
//                    .status(400)
//                    .message("User not found")
//                    .build();
//        }

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

        String rawText = transcript.getText().get();

        List<ExpenseRequest> extracted = extractProductInfo(rawText);

        return new BaseResponse<>();
//        return save(extracted, user.get(), rawText);
    }

    private String uploadAudioIntoCloud(MultipartFile file) throws IOException {
        String fileName = "audio/" + file.getOriginalFilename();
        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, file.getBytes());

        return String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileName);
    }

    private BaseResponse<VoiceCommandResponse> save(List<ExpenseRequest> extracted, UserEntity user, String rawText) {
        BaseResponse<List<ExpenseResponse>> response = expenseService.save(extracted, user);

        for (ExpenseResponse resp : response.getData()) {
            VoiceCommandEntity command = VoiceCommandEntity.builder()
                    .rawText(rawText)
                    .user(resp.getUser())
                    .expense(expenseService.findById(resp.getId()).isPresent() ?
                            expenseService.findById(resp.getId()).get() : null)
                    .build();
            voiceCommandRepository.save(command);
        }

        return BaseResponse.<VoiceCommandResponse>builder()
                .message("Expenses saved successfully")
                .status(200)
                .build();
    }

    // ---EXTRACTION LOGIC---

    public List<ExpenseRequest> extractProductInfo(String text) {
        List<ExpenseRequest> productInfoList = new ArrayList<>();
        CoreDocument document = new CoreDocument(text);
        pipeline.annotate(document);

        for (CoreSentence sentence : document.sentences()) {
            if (hasVerbs(sentence)) {
                productInfoList.addAll(extractFromVerbSentence(sentence));
            } else {
                productInfoList.addAll(extractFromNounPhrase(sentence));
            }
        }
        return productInfoList;
    }

    /**
     * this method is for finding a verb from a text,
     * it uses CoreLabel class which represents a single word with its annotation e.g.(NN-noun, VB-verb)
     * @param sentence
     * @return
     */
    private boolean hasVerbs(CoreSentence sentence) {
        for (CoreLabel token : sentence.tokens()) {
            if (token.get(CoreAnnotations.PartOfSpeechAnnotation.class).startsWith("VB")) {
                return true;
            }
        }
        return false;
    }

    /**
     * this method helps to extract some key information such as product name, quantity, price.
     * --------
     * sentence.dependencyParse(); -> represents the grammatical structure of a sentence (e.g. subject-verb, verb-object).
     * --------
     * sentence.text() -> just a raw text for the extraction of quantity and price.
     * --------
     * edge.getRelation() -> type of dependency ("nsubj" - subject, "obj" - object), if the relation is "obj", it
     * connects a verb to its object ("bought" -> "laptop" in "I bought a laptop"). In brief, it focuses on verb-obj
     * relationships to easily find product names.
     * --------
     * IndexedWord verb = edge.getGovernor() -> governor is the verb we are looking for, and the lemma helps to get
     * the base form of a verb, for example if the verb is "bought" it becomes "buy" and we check this verb from the set "productVerbs"
     * which includes shopping related verbs.
     * --------
     * IndexedWord obj = edge.getDependent(); -> dependent is the object (e.g. "laptop").
     * --------
     * getConjoinedNouns(obj, dependencies); -> that finds all object-related nouns (e.g. "logitech mouse" or "mouse and laptop")
     * --------
     * extractModifiers(head, dependencies); -> gets words with their modifiers if they exist (e.g. "red apple", "new laptop")
     * and after that the result list is sorted to get the right order of product name
     * --------
     * in the end, found modifiers and nouns are merged together.
     * @param sentence
     * @return
     */
    private List<ExpenseRequest> extractFromVerbSentence(CoreSentence sentence) {
        List<ExpenseRequest> productInfoList = new ArrayList<>();
        SemanticGraph dependencies = sentence.dependencyParse();
        String sentenceText = sentence.text();

        for (SemanticGraphEdge edge : dependencies.edgeListSorted()) {
            if (edge.getRelation().toString().equals("obj")) {
                IndexedWord verb = edge.getGovernor();
                if (productVerbs.contains(verb.lemma())) {
                    IndexedWord obj = edge.getDependent();
                    List<IndexedWord> heads = getConjoinedNouns(obj, dependencies);
                    for (IndexedWord head : heads) {
                        List<IndexedWord> productWords = extractModifiers(head, dependencies);
                        productWords.sort(Comparator.comparing(IndexedWord::index));
                        String productName = productWords.stream()
                                .map(IndexedWord::word)
                                .collect(Collectors.joining(" "));
                        String quantity = extractQuantity(sentenceText);
                        String price = extractPrice(sentenceText);
                        productInfoList.add(new ExpenseRequest(productName, price, quantity));
                    }
                }
            }
        }
        return productInfoList;
    }

    /**
     * if an input doesn't have any verbs for example -> "5kg rice for $20", this method will get invoked.
     * --------
     * IndexedWord root = dependencies.getFirstRoot(); -> is the noun (e.g. rice) in a noun-based sentence
     * @param sentence
     * @return
     */
    private List<ExpenseRequest> extractFromNounPhrase(CoreSentence sentence) {
        List<ExpenseRequest> productInfoList = new ArrayList<>();
        SemanticGraph dependencies = sentence.dependencyParse();
        IndexedWord root = dependencies.getFirstRoot();
        String sentenceText = sentence.text();

        if (root != null) {
            List<IndexedWord> heads = getConjoinedNouns(root, dependencies);
            for (IndexedWord head : heads) {
                List<IndexedWord> productWords = extractModifiers(head, dependencies);
                productWords.sort(Comparator.comparing(IndexedWord::index));
                String productName = productWords.stream()
                        .map(IndexedWord::word)
                        .collect(Collectors.joining(" "));
                String quantity = extractQuantity(sentenceText);
                String price = extractPrice(sentenceText);
                productInfoList.add(new ExpenseRequest(productName, price, quantity));
            }
        }
        return productInfoList;
    }

    /**
     * "amod" -> checks for adjectives, "compound" -> checks for noun (e.g. "laptop" in "laptop bag")
     * @param head
     * @param dependencies
     * @return
     */
    private List<IndexedWord> extractModifiers(IndexedWord head, SemanticGraph dependencies) {
        List<IndexedWord> modifiers = new ArrayList<>();
        Set<String> relations = new HashSet<>(Arrays.asList("amod", "compound"));
        Set<String> quantityWords = Set.of(
                "tonne","tonnes","unit","piece","pieces","units","kilogram",
                "gram","milligram","kg","kilograms","g","grams","mg","milligrams",
                "l","liter","liters","ml","milliliters");

        modifiers.add(head);

        for (SemanticGraphEdge edge : dependencies.outgoingEdgeList(head)) {
            String relation = edge.getRelation().toString();
            if (relations.contains(relation)) {
                IndexedWord dependent = edge.getDependent();
                if (!modifiers.contains(dependent) && !quantityWords.contains(dependent.word().toLowerCase())) {
                    modifiers.add(dependent);
                }
            }
        }

        return modifiers;
    }

    /**
     * helps to get nouns like -> "mouse and laptop"
     * @param noun
     * @param dependencies
     * @return
     */
    private List<IndexedWord> getConjoinedNouns(IndexedWord noun, SemanticGraph dependencies) {
        List<IndexedWord> conjoined = new ArrayList<>();
        Set<String> tags = Set.of("NN", "NNS", "NNP", "NNPS");
        conjoined.add(noun);
        for (SemanticGraphEdge edge : dependencies.outgoingEdgeList(noun)) {
            if (edge.getRelation().toString().equals("conj") && tags.contains(edge.getDependent().tag())) {
                conjoined.add(edge.getDependent());
            }
        }
        return conjoined;
    }

    private String extractQuantity(String text) {
        Matcher matcher = quantityPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String extractPrice(String text) {
        Matcher matcher = pricePattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public Page<VoiceCommandResponse> getAll(UUID userId, int page, int size) {
        Pageable pagination = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<VoiceCommandEntity> commands = voiceCommandRepository.getAll(userId, pagination);

        return commands.map(command -> VoiceCommandResponse.builder()
                .id(command.getId())
                .user(command.getUser())
                .rawText(command.getRawText())
                .expense(command.getExpense())
                .createdDate(command.getCreatedDate())
                .build()
        );
    }

    public BaseResponse<VoiceCommandResponse> delete(UUID voiceCommandID) {
        Optional<VoiceCommandEntity> command = voiceCommandRepository.findById(voiceCommandID);
        if (command.isEmpty()) return BaseResponse.<VoiceCommandResponse>builder()
                .message("Command not found").status(400).build();

        voiceCommandRepository.deleteById(voiceCommandID);

        return BaseResponse.<VoiceCommandResponse>builder()
                .message("Deleted successfully").status(200).build();
    }
}
