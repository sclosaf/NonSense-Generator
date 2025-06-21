package unipd.nonsense.analyzer;

import com.google.cloud.language.v1.AnalyzeEntitiesRequest;
import com.google.cloud.language.v1.AnalyzeEntitiesResponse;
import com.google.cloud.language.v1.AnalyzeSentimentResponse;
import com.google.cloud.language.v1.AnalyzeSyntaxRequest;
import com.google.cloud.language.v1.AnalyzeSyntaxResponse;
import com.google.cloud.language.v1.ClassificationCategory;
import com.google.cloud.language.v1.DependencyEdge;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Entity;
import com.google.cloud.language.v1.EntityMention;
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

import unipd.nonsense.exceptions.InvalidTextException;
import unipd.nonsense.util.GoogleApiClient;
import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.SyntaxToken;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Testing SentenceAnalyzer")
class TestSentenceAnalyzer
{
	private SentenceAnalyzer analyzer;
	private GoogleApiClient mockApiClient;
	private LanguageServiceClient mockLanguageClient;

	private static final String TEST_JSON_PATH = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "TestSentence.json";

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
	@DisplayName("Analyze syntax input returns expected report with valid tokens")
	void testAnalyzeSyntax_Success() throws Exception {
		AnalyzeSyntaxResponse mockResponse = mockAnalyzeSyntaxResponse();
		when(mockLanguageClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class))).thenReturn(mockResponse);

		String report = analyzer.analyzeSyntaxAsync(syntaxText).get();

		assertNotNull(report);
		assertFalse(report.isEmpty(), "Report should not be empty");
		for (SyntaxToken expected : expectedTokens) {
			assertTrue(report.contains(expected.getText()),
				"Report should contain token text: " + expected.getText());
			assertTrue(report.contains(expected.getLemma()),
				"Report should contain lemma: " + expected.getLemma());
		}
	}

	@Test
	@DisplayName("Analyze sentiment input returns complete sentiment analysis")
	void testAnalyzeSentiment_Success() throws Exception {
		Sentiment sentiment = Sentiment.newBuilder()
			.setScore(0.8f)
			.setMagnitude(1.2f)
			.build();
		AnalyzeSentimentResponse mockResponse = AnalyzeSentimentResponse.newBuilder()
			.setDocumentSentiment(sentiment)
			.build();
		when(mockLanguageClient.analyzeSentiment(any(Document.class))).thenReturn(mockResponse);

		String result = analyzer.analyzeSentimentAsync(sentimentText).get();

		assertNotNull(result);
		assertTrue(Pattern.compile("Sentiment Score: 0[\\.,]80 \\(Magnitude: 1[\\.,]20\\)").matcher(result).matches(), "Should format sentiment results correctly");
	}

	@Test
	@DisplayName("Analyze entities input returns complete entity analysis")
	void testAnalyzeEntities_Success() throws Exception {
		Entity entity = Entity.newBuilder()
			.setName("TestEntity")
			.setType(Entity.Type.PERSON)
			.setSalience(0.9f)
			.addMentions(EntityMention.newBuilder()
				.setText(TextSpan.newBuilder().setContent("mention").setBeginOffset(0))
				.setType(EntityMention.Type.PROPER))
			.build();

		AnalyzeEntitiesResponse mockResponse = AnalyzeEntitiesResponse.newBuilder()
			.addEntities(entity)
			.build();
		when(mockLanguageClient.analyzeEntities(any(AnalyzeEntitiesRequest.class))).thenReturn(mockResponse);

		String report = analyzer.analyzeEntitiesAsync(entitiesText).get();

		assertNotNull(report);
		assertTrue(report.contains("Entity 1: TestEntity"), "Should contain entity name");
		assertTrue(report.contains("Type: PERSON"), "Should contain entity type");
		assertTrue(Pattern.compile("Salience: 0[\\.,]900").matcher(report).find(), "Should contain salience score");
	}

	@Test
	@DisplayName("Get syntax tokens returns complete token information")
	void testGetSyntaxTokens_Success() throws Exception {
		AnalyzeSyntaxResponse mockResponse = mockAnalyzeSyntaxResponse();
		when(mockLanguageClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class))).thenReturn(mockResponse);

		List<SyntaxToken> tokens = analyzer.getSyntaxTokensAsync(syntaxText).get();

		assertNotNull(tokens);
		assertEquals(expectedTokens.size(), tokens.size(), "Should return all expected tokens");
		for (int i = 0; i < expectedTokens.size(); i++) {
			SyntaxToken expected = expectedTokens.get(i);
			SyntaxToken actual = tokens.get(i);
			assertEquals(expected.getText(), actual.getText(), "Token text mismatch at index " + i);
			assertEquals(expected.getLemma(), actual.getLemma(), "Lemma mismatch at index " + i);
			assertEquals(expected.getPartOfSpeech(), actual.getPartOfSpeech(), "POS mismatch at index " + i);
		}
	}

	@Test
	@DisplayName("Analyze syntax with null input throws InvalidTextException")
	void testAnalyzeSyntax_NullInput() {
		ExecutionException ex = assertThrows(ExecutionException.class,
			() -> analyzer.analyzeSyntaxAsync(null).get());

		assertTrue(ex.getCause() instanceof InvalidTextException,
			"Should throw InvalidTextException for null input");
	}

	@Test
	@DisplayName("Analyze syntax with empty input throws InvalidTextException")
	void testAnalyzeSyntax_EmptyInput() {
		ExecutionException ex = assertThrows(ExecutionException.class,
			() -> analyzer.analyzeSyntaxAsync("   ").get());

		assertTrue(ex.getCause() instanceof InvalidTextException,
			"Should throw InvalidTextException for empty input");
	}

	@Test
	@DisplayName("Analyzer close completes successfully")
	void testAnalyzerClose() {
		GoogleApiClient tempMockClient = mock(GoogleApiClient.class);
		LanguageServiceClient tempLanguageClient = mock(LanguageServiceClient.class);
		SentenceAnalyzer tempAnalyzer = new SentenceAnalyzer(tempMockClient, tempLanguageClient);

		assertDoesNotThrow(tempAnalyzer::close, "Close should not throw exceptions");
		verify(tempLanguageClient, times(1)).close();
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

	@Test
	@DisplayName("Analyze syntax with empty tokens list")
	void testAnalyzeSyntax_EmptyTokens() throws Exception
	{
		AnalyzeSyntaxResponse mockResponse = AnalyzeSyntaxResponse.newBuilder().build();
		when(mockLanguageClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class))).thenReturn(mockResponse);

		String result = analyzer.analyzeSyntaxAsync("Test").get();

		assertNotNull(result);
		assertTrue(result.isEmpty(), "Should return empty string for no tokens");
	}

	@Test
	@DisplayName("Analyze sentiment with null sentiment")
	void testAnalyzeSentiment_NullSentiment() throws Exception
	{
		AnalyzeSentimentResponse mockResponse = AnalyzeSentimentResponse.newBuilder().build();
		when(mockLanguageClient.analyzeSentiment(any(Document.class))).thenReturn(mockResponse);

		String result = analyzer.analyzeSentimentAsync("Test").get();

		assertTrue(Pattern.compile("Sentiment Score: 0[\\.,]00 \\(Magnitude: 0[\\.,]00\\)").matcher(result).matches());
	}

	@Test
	@DisplayName("Analyze entities with empty entities list")
	void testAnalyzeEntities_EmptyEntities() throws Exception
	{
		AnalyzeEntitiesResponse mockResponse = AnalyzeEntitiesResponse.newBuilder().build();
		when(mockLanguageClient.analyzeEntities(any(AnalyzeEntitiesRequest.class))).thenReturn(mockResponse);

		String result = analyzer.analyzeEntitiesAsync("Test").get();

		assertNotNull(result);
		assertEquals("No entities detected.", result);
	}

	@Test
	@DisplayName("Get syntax tokens with empty response")
	void testGetSyntaxTokens_EmptyResponse() throws Exception
	{
		AnalyzeSyntaxResponse mockResponse = AnalyzeSyntaxResponse.newBuilder().build();
		when(mockLanguageClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class))).thenReturn(mockResponse);

		List<SyntaxToken> tokens = analyzer.getSyntaxTokensAsync("Test").get();

		assertNotNull(tokens);
		assertTrue(tokens.isEmpty());
	}

	@Test
	@DisplayName("Analyze syntax with maximum length text")
	void testAnalyzeSyntax_MaxLengthText() throws Exception
	{
		String maxLengthText = String.join("", Collections.nCopies(1000, "a"));
		AnalyzeSyntaxResponse mockResponse = mockAnalyzeSyntaxResponse();
		when(mockLanguageClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class))).thenReturn(mockResponse);

		assertDoesNotThrow(() -> analyzer.analyzeSyntaxAsync(maxLengthText).get());
	}

	@Test
	@DisplayName("Analyze syntax with text exceeding maximum length")
	void testAnalyzeSyntax_TextExceedsMaxLength()
	{
		String longText = String.join("", Collections.nCopies(1001, "a"));

		ExecutionException ex = assertThrows(ExecutionException.class,
			() -> analyzer.analyzeSyntaxAsync(longText).get());

		assertTrue(ex.getCause() instanceof InvalidTextException);
	}

	@Test
	@DisplayName("Analyze sentiment with extreme values")
	void testAnalyzeSentiment_ExtremeValues() throws Exception
	{
		Sentiment minSentiment = Sentiment.newBuilder().setScore(-1.0f).setMagnitude(0.0f).build();
		AnalyzeSentimentResponse minResponse = AnalyzeSentimentResponse.newBuilder().setDocumentSentiment(minSentiment).build();
		when(mockLanguageClient.analyzeSentiment(any(Document.class))).thenReturn(minResponse);

		String minResult = analyzer.analyzeSentimentAsync("Negative").get();
		assertTrue(minResult.contains("-1.00"), "Should handle minimum sentiment score");

		Sentiment maxSentiment = Sentiment.newBuilder().setScore(1.0f).setMagnitude(10.0f).build();
		AnalyzeSentimentResponse maxResponse = AnalyzeSentimentResponse.newBuilder().setDocumentSentiment(maxSentiment).build();
		when(mockLanguageClient.analyzeSentiment(any(Document.class))).thenReturn(maxResponse);

		String maxResult = analyzer.analyzeSentimentAsync("Positive").get();
		assertTrue(maxResult.contains("1.00"), "Should handle maximum sentiment score");
	}

	@Test
	@DisplayName("Analyze entities with multiple mentions")
	void testAnalyzeEntities_MultipleMentions() throws Exception
	{
		Entity.Builder entityBuilder = Entity.newBuilder()
			.setName("TestEntity")
			.setType(Entity.Type.PERSON)
			.setSalience(0.9f)
			.addMentions(EntityMention.newBuilder()
				.setText(TextSpan.newBuilder().setContent("First").setBeginOffset(0))
				.setType(EntityMention.Type.PROPER))
			.addMentions(EntityMention.newBuilder()
				.setText(TextSpan.newBuilder().setContent("Second").setBeginOffset(10))
				.setType(EntityMention.Type.COMMON));

		AnalyzeEntitiesResponse mockResponse = AnalyzeEntitiesResponse.newBuilder()
			.addEntities(entityBuilder)
			.build();

		when(mockLanguageClient.analyzeEntities(any(AnalyzeEntitiesRequest.class))).thenReturn(mockResponse);

		String result = analyzer.analyzeEntitiesAsync("Test").get();

		assertNotNull(result);
		assertTrue(result.contains("First"), "Should include first mention");
		assertTrue(result.contains("Second"), "Should include second mention");
	}

	@Test
	@DisplayName("Get syntax tokens with all POS features")
	void testGetSyntaxTokens_AllPOSFeatures() throws Exception
	{
		PartOfSpeech pos = PartOfSpeech.newBuilder()
			.setTag(PartOfSpeech.Tag.NOUN)
			.setAspect(PartOfSpeech.Aspect.PERFECTIVE)
			.setCase(PartOfSpeech.Case.ACCUSATIVE)
			.setForm(PartOfSpeech.Form.ADNOMIAL)
			.setGender(PartOfSpeech.Gender.FEMININE)
			.setMood(PartOfSpeech.Mood.CONDITIONAL_MOOD)
			.setNumber(PartOfSpeech.Number.PLURAL)
			.setPerson(PartOfSpeech.Person.FIRST)
			.setProper(PartOfSpeech.Proper.PROPER)
			.setReciprocity(PartOfSpeech.Reciprocity.RECIPROCAL)
			.setTense(PartOfSpeech.Tense.PAST)
			.setVoice(PartOfSpeech.Voice.ACTIVE)
			.build();

		Token token = Token.newBuilder()
			.setText(TextSpan.newBuilder().setContent("Test").setBeginOffset(0))
			.setLemma("test")
			.setPartOfSpeech(pos)
			.setDependencyEdge(DependencyEdge.newBuilder()
				.setHeadTokenIndex(0)
				.setLabel(DependencyEdge.Label.ROOT))
			.build();

		AnalyzeSyntaxResponse mockResponse = AnalyzeSyntaxResponse.newBuilder()
			.addTokens(token)
			.build();

		when(mockLanguageClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class))).thenReturn(mockResponse);

		List<SyntaxToken> tokens = analyzer.getSyntaxTokensAsync("Test").get();

		assertNotNull(tokens);
		assertEquals(1, tokens.size());
		assertEquals(pos, tokens.get(0).getPartOfSpeech());
	}

	@Test
	@DisplayName("Test service exception handling")
	void testServiceExceptionHandling()
	{
		when(mockLanguageClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class)))
			.thenThrow(new RuntimeException("API Service error"));

		assertThrows(CompletionException.class,
			() -> analyzer.analyzeSyntaxAsync("test text").join());
	}

	@Test
	@DisplayName("Test API client with null credentials throws NullPointerException")
	void testApiClientWithNullCredentials()
	{
		when(mockApiClient.getClient()).thenThrow(new NullPointerException("Null credentials"));

		CompletionException ex = assertThrows(CompletionException.class,
			() -> analyzer.analyzeSyntaxAsync("test").join());
		assertTrue(ex.getCause() instanceof NullPointerException,
			"Expected NullPointerException to be wrapped in CompletionException");
	}

	@Test
	@DisplayName("Test concurrent analysis requests")
	void testConcurrentRequests()
	{
		AnalyzeSyntaxResponse syntaxResponse = mockAnalyzeSyntaxResponse();
		AnalyzeSentimentResponse sentimentResponse = AnalyzeSentimentResponse.newBuilder()
			.setDocumentSentiment(Sentiment.newBuilder().setScore(0.5f).setMagnitude(1.0f))
			.build();
		AnalyzeEntitiesResponse entitiesResponse = AnalyzeEntitiesResponse.newBuilder()
			.addEntities(Entity.newBuilder().setName("Test").setType(Entity.Type.PERSON))
			.build();

		when(mockLanguageClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class)))
			.thenReturn(syntaxResponse);
		when(mockLanguageClient.analyzeSentiment(any(Document.class)))
			.thenReturn(sentimentResponse);
		when(mockLanguageClient.analyzeEntities(any(AnalyzeEntitiesRequest.class)))
			.thenReturn(entitiesResponse);

		CompletableFuture<String> syntaxFuture = analyzer.analyzeSyntaxAsync("Syntax");
		CompletableFuture<String> sentimentFuture = analyzer.analyzeSentimentAsync("Sentiment");
		CompletableFuture<String> entitiesFuture = analyzer.analyzeEntitiesAsync("Entities");
		CompletableFuture<List<SyntaxToken>> tokensFuture = analyzer.getSyntaxTokensAsync("Tokens");

		CompletableFuture<Void> allFutures = CompletableFuture.allOf(
			syntaxFuture, sentimentFuture, entitiesFuture, tokensFuture
		);

		allFutures.join();

		assertTrue(syntaxFuture.isDone());
		assertTrue(sentimentFuture.isDone());
		assertTrue(entitiesFuture.isDone());
		assertTrue(tokensFuture.isDone());
	}

	@Test
	@DisplayName("Test analyzer close method")
	void testClose()
	{
		GoogleApiClient tempMockClient = mock(GoogleApiClient.class);
		LanguageServiceClient tempLanguageClient = mock(LanguageServiceClient.class);
		SentenceAnalyzer tempAnalyzer = new SentenceAnalyzer(tempMockClient, tempLanguageClient);

		tempAnalyzer.close();

		verify(tempLanguageClient, times(1)).close();
	}

	@Test
	@DisplayName("Test exception during close")
	void testExceptionDuringClose()
	{
		GoogleApiClient tempMockClient = mock(GoogleApiClient.class);
		LanguageServiceClient tempLanguageClient = mock(LanguageServiceClient.class);
		doThrow(new RuntimeException("Close error")).when(tempLanguageClient).close();

		SentenceAnalyzer tempAnalyzer = new SentenceAnalyzer(tempMockClient, tempLanguageClient);

		assertThrows(RuntimeException.class, tempAnalyzer::close);
	}

	@Test
	@DisplayName("Test analyze syntax report formatting")
	void testAnalyzeSyntaxReportFormatting() throws Exception
	{
		AnalyzeSyntaxResponse mockResponse = mockAnalyzeSyntaxResponse();
		when(mockLanguageClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class))).thenReturn(mockResponse);

		String report = analyzer.analyzeSyntaxAsync("Test").get();

		assertNotNull(report);
		String[] lines = report.split("\n");
		for (String line : lines) {
			if (line.startsWith("Token")) {
				assertTrue(line.matches("Token \\d+: .+"), "Token line format incorrect");
			} else if (line.trim().startsWith("Lemma:")) {
				assertTrue(line.contains(": "), "Lemma line should contain ': '");
			}
		}
	}

	@Test
	@DisplayName("Test analyze entities report formatting")
	void testAnalyzeEntitiesReportFormatting() throws Exception
	{
		Entity entity = Entity.newBuilder()
			.setName("TestEntity")
			.setType(Entity.Type.PERSON)
			.setSalience(0.9f)
			.addMentions(EntityMention.newBuilder()
				.setText(TextSpan.newBuilder().setContent("Test").setBeginOffset(0))
				.setType(EntityMention.Type.PROPER))
			.build();

		AnalyzeEntitiesResponse mockResponse = AnalyzeEntitiesResponse.newBuilder()
			.addEntities(entity)
			.build();

		when(mockLanguageClient.analyzeEntities(any(AnalyzeEntitiesRequest.class))).thenReturn(mockResponse);

		String report = analyzer.analyzeEntitiesAsync("Test").get();

		assertNotNull(report);
		assertTrue(report.contains("Entity 1: TestEntity"), "Should contain entity name");
		assertTrue(report.contains("Type: PERSON"), "Should contain entity type");
		assertTrue(Pattern.compile("Salience: 0[\\.,]900").matcher(report).find(), "Should contain salience score");
		assertTrue(report.contains("- Test (Type: PROPER"), "Should contain mention");
	}
}
