package unipd.nonsense.analyzer;

import com.google.cloud.language.v1.AnalyzeEntitiesRequest;
import com.google.cloud.language.v1.AnalyzeEntitiesResponse;
import com.google.cloud.language.v1.AnalyzeSentimentResponse;
import com.google.cloud.language.v1.AnalyzeSyntaxRequest;
import com.google.cloud.language.v1.AnalyzeSyntaxResponse;
import com.google.cloud.language.v1.DependencyEdge;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Entity;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.PartOfSpeech;
import com.google.cloud.language.v1.Sentiment;
import com.google.cloud.language.v1.TextSpan;
import com.google.cloud.language.v1.Token;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import unipd.nonsense.util.GoogleApiClient;
import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.SyntaxToken;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Testing SentenceAnalyzer")
class TestSentenceAnalyzer
{
    private SentenceAnalyzer analyzer;
    private GoogleApiClient mockApiClient;
    private LanguageServiceClient mockLanguageClient;

    private static final String TEST_JSON_PATH = "src/test/resources/TestSentence.json";

    private static String syntaxText;
    private static String sentimentText;
    private static String entitiesText;
    private static List<SyntaxToken> expectedTokens;

    @BeforeAll
    @DisplayName("Load test data from TestSentence.json")
    static void loadTestData() throws Exception
    {
        JsonFileHandler handler = JsonFileHandler.getInstance();
        JsonObject root = handler.getJsonObject(TEST_JSON_PATH);

        syntaxText = root.get("syntaxText").getAsString();
        sentimentText = root.get("sentimentText").getAsString();
        entitiesText = root.get("entitiesText").getAsString();

        expectedTokens = new ArrayList<>();
        JsonArray tokensArray = root.getAsJsonArray("expectedTokens");
        for (var e : tokensArray) 
        {
            JsonObject obj = e.getAsJsonObject();
            SyntaxToken token = new SyntaxToken(
                obj.get("content").getAsString(),
                obj.get("beginOffset").getAsInt(),
                obj.get("lemma").getAsString(),
                PartOfSpeech.newBuilder().setTag(PartOfSpeech.Tag.valueOf(obj.get("pos").getAsString())).build(),
                obj.get("headTokenIndex").getAsInt(),
                DependencyEdge.Label.valueOf(obj.get("label").getAsString())
            );
            expectedTokens.add(token);
        }
    }

    @BeforeEach
    @DisplayName("Initialize mocks and analyzer")
    void setUp()
    {
        mockApiClient = mock(GoogleApiClient.class);
        mockLanguageClient = mock(LanguageServiceClient.class);

        when(mockApiClient.getClient()).thenReturn(mockLanguageClient);

        analyzer = new SentenceAnalyzer(mockApiClient, mockLanguageClient);
    }

    @AfterEach
    @DisplayName("Clean up analyzer resources")
    void tearDown()
    {
        analyzer.close();
    }

    @Test
    @DisplayName("Analyze syntax input returns expected report")
    void testAnalyzeSyntaxInput() throws Exception
    {
        AnalyzeSyntaxResponse mockResponse = mockAnalyzeSyntaxResponse();
        when(mockLanguageClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class))).thenReturn(mockResponse);

        String report = analyzer.analyzeSyntaxAsync(syntaxText).get();

        assertNotNull(report);
        assertTrue(report.contains("Token 1"), "Report should contain token info");
    }

    @Test
    @DisplayName("Analyze sentiment input returns expected result")
    void testAnalyzeSentimentInput() throws Exception
    {
        Sentiment sentiment = Sentiment.newBuilder().setScore(0.8f).setMagnitude(1.2f).build();
        AnalyzeSentimentResponse mockResponse = AnalyzeSentimentResponse.newBuilder().setDocumentSentiment(sentiment).build();
        when(mockLanguageClient.analyzeSentiment(any(Document.class))).thenReturn(mockResponse);

        String result = analyzer.analyzeSentimentAsync(sentimentText).get();

        assertNotNull(result);
        assertTrue(result.contains("Sentiment Score"), "Result should contain sentiment score");
    }

    @Test
    @DisplayName("Analyze entities input returns expected report")
    void testAnalyzeEntitiesInput() throws Exception
    {
        AnalyzeEntitiesResponse mockResponse = mockAnalyzeEntitiesResponse();
        when(mockLanguageClient.analyzeEntities(any(AnalyzeEntitiesRequest.class))).thenReturn(mockResponse);

        String report = analyzer.analyzeEntitiesAsync(entitiesText).get();

        assertNotNull(report);
        assertTrue(report.contains("Entity 1"), "Report should contain entity info");
    }

    @Test
    @DisplayName("Get syntax tokens returns expected tokens")
    void testGetSyntaxTokens() throws Exception
    {
        AnalyzeSyntaxResponse mockResponse = mockAnalyzeSyntaxResponse();
        when(mockLanguageClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class))).thenReturn(mockResponse);

        List<SyntaxToken> tokens = analyzer.getSyntaxTokensAsync(syntaxText).get();

        assertNotNull(tokens);
        assertEquals(expectedTokens.size(), tokens.size(), "Token count mismatch");
    }

    @Test
    @DisplayName("Analyze syntax input throws on null")
    void testAnalyzeSyntaxInputThrowsOnNull() 
    {
        assertThrows(Exception.class, () -> analyzer.analyzeSyntaxAsync(null).get());
    }

    @Test
    @DisplayName("Analyze syntax input throws on empty string")
    void testAnalyzeSyntaxInputThrowsOnEmpty() 
    {
        assertThrows(Exception.class, () -> analyzer.analyzeSyntaxAsync("   ").get());
    }

    @Test
    @DisplayName("Analyzer close does not throw")
    void testAnalyzerClose() 
    {
        assertDoesNotThrow(() -> analyzer.close());
    }

    private AnalyzeSyntaxResponse mockAnalyzeSyntaxResponse() 
    {
        AnalyzeSyntaxResponse.Builder builder = AnalyzeSyntaxResponse.newBuilder();
        for (SyntaxToken t : expectedTokens) 
        {
            Token.Builder tokenBuilder = Token.newBuilder()
                .setText(TextSpan.newBuilder()
                .setContent(t.getText())
                .setBeginOffset(t.getBeginOffset()))
                .setLemma(t.getLemma())
                .setPartOfSpeech(t.getPartOfSpeech())
                .setDependencyEdge(DependencyEdge.newBuilder()
                .setHeadTokenIndex(t.getHeadTokenIndex())
                .setLabel(t.getDependencyLabel())
            );
            builder.addTokens(tokenBuilder);
        }
        return builder.build();
    }

    private AnalyzeEntitiesResponse mockAnalyzeEntitiesResponse() 
    {
        AnalyzeEntitiesResponse.Builder builder = AnalyzeEntitiesResponse.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder()
            .setName("TestEntity")
            .setType(Entity.Type.PERSON)
            .setSalience(0.9f);
        builder.addEntities(entityBuilder);
        return builder.build();
    }
}
