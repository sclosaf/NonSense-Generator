package unipd.nonsense.analyzer;

import com.google.cloud.language.v1.*;
import org.junit.jupiter.api.*;
import unipd.nonsense.exceptions.InvalidTextException;
import unipd.nonsense.exceptions.InvalidThresholdException;
import unipd.nonsense.util.GoogleApiClient;
import unipd.nonsense.util.JsonFileHandler;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Testing ToxicityValidator")
class TestToxicityValidator
{
	private ToxicityValidator validator;
	private GoogleApiClient mockApiClient;
	private LanguageServiceClient mockLanguageClient;

	private static final String TEST_JSON_PATH = "src/test/resources/TestToxicity.json";

	private static Map<String, Float> cleanScores;
	private static Map<String, Float> toxicScores;
	private static String cleanText;
	private static String toxicText;

	@BeforeAll
	@DisplayName("Load test data from TestToxicity.json")
	static void loadTestData() throws Exception
	{
		JsonFileHandler handler = JsonFileHandler.getInstance();
		JsonObject root = handler.getJsonObject(TEST_JSON_PATH);

		JsonObject scores = root.getAsJsonObject("scores");

		JsonArray cleanArray = scores.getAsJsonArray("clean");
		JsonArray toxicArray = scores.getAsJsonArray("toxic");

		List<String> cleanScoreList = new ArrayList<>();
		List<String> toxicScoreList = new ArrayList<>();

		for(var e : cleanArray)
			cleanScoreList.add(e.getAsString());

		for(var e : toxicArray)
			toxicScoreList.add(e.getAsString());

		cleanScores = parseScoreMap(cleanScoreList);
		toxicScores = parseScoreMap(toxicScoreList);

		cleanText = root.getAsJsonArray("cleanText").get(0).getAsString();
		toxicText = root.getAsJsonArray("toxicText").get(0).getAsString();
	}

	private static Map<String, Float> parseScoreMap(List<String> scoreList)
	{
		Map<String, Float> map = new HashMap<>();
		for(String entry : scoreList)
		{
			String[] parts = entry.split(":");
			map.put(parts[0], Float.parseFloat(parts[1]));
		}
		return map;
	}

	@BeforeEach
	@DisplayName("Initialize mocks and validator")
	void setUp()
	{
		mockApiClient = mock(GoogleApiClient.class);
		mockLanguageClient = mock(LanguageServiceClient.class);

		when(mockApiClient.getClient()).thenReturn(mockLanguageClient);

		validator = new ToxicityValidator(mockApiClient);
	}

	private ModerateTextResponse mockResponse(Map<String, Float> scores)
	{
		ModerateTextResponse.Builder responseBuilder = ModerateTextResponse.newBuilder();
		for(Map.Entry<String, Float> entry : scores.entrySet())
		{
			responseBuilder.addModerationCategories(
				ClassificationCategory.newBuilder()
					.setName(entry.getKey())
					.setConfidence(entry.getValue())
					.build()
			);
		}
		return responseBuilder.build();
	}

	@AfterEach
	@DisplayName("Clean up validator resources")
	void tearDown()
	{
		validator.close();
	}

	@Test
	@DisplayName("Get toxicity scores from clean text")
	void testGetToxicityScores_Clean()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(cleanScores));

		Map<String, Float> result = validator.getToxicityScoresAsync(cleanText).join();

		assertEquals(cleanScores.size(), result.size(), "Score size mismatch");
		cleanScores.forEach((k, v) -> assertEquals(v, result.get(k), "Mismatched score for " + k));
	}

	@Test
	@DisplayName("Get toxicity scores from toxic text")
	void testGetToxicityScores_Toxic()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		Map<String, Float> result = validator.getToxicityScoresAsync(toxicText).join();

		assertEquals(toxicScores.size(), result.size(), "Score size mismatch");
		toxicScores.forEach((k, v) -> assertEquals(v, result.get(k), "Mismatched score for " + k));
	}

	@Test
	@DisplayName("Detect toxic text above threshold")
	void testIsTextToxic_Positive()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		assertTrue(validator.isTextToxicAsync(toxicText, 0.7f).join(), "Expected text to be toxic");
	}

	@Test
	@DisplayName("Detect clean text below threshold")
	void testIsTextToxic_Negative()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(cleanScores));

		assertFalse(validator.isTextToxicAsync(cleanText, 0.7f).join(), "Expected text to be non-toxic");
	}

	@Test
	@DisplayName("Invalid threshold should throw exception")
	void testIsTextToxic_InvalidThreshold()
	{
		CompletionException ex = assertThrows(CompletionException.class,
			() -> validator.isTextToxicAsync("text", 1.5f).join()
			);

		assertTrue(ex.getCause() instanceof InvalidThresholdException);
	}

	@Test
	@DisplayName("Null input to moderateText should throw exception")
	void testModerateText_NullInput()
	{
		CompletionException ex = assertThrows(CompletionException.class,
			() -> validator.moderateTextAsync(null).join()
			);

		assertTrue(ex.getCause() instanceof InvalidTextException);
	}

	@Test
	@DisplayName("Empty input to moderateText should throw exception")
	void testModerateText_EmptyInput()
	{
		CompletionException ex = assertThrows(CompletionException.class,
			() -> validator.moderateTextAsync("   ").join()
			);

		assertTrue(ex.getCause() instanceof InvalidTextException);
	}

	@Test
	@DisplayName("Text exceeding max length should throw InvalidTextException")
	void testTextExceedsMaxLength()
	{
		String longText = String.join("", Collections.nCopies(1001, "a"));
		CompletionException ex = assertThrows(CompletionException.class,
			() -> validator.getToxicityScoresAsync(longText).join()
			);

		assertTrue(ex.getCause() instanceof InvalidTextException);
	}

	@Test
	@DisplayName("Test getToxicityReport with clean text")
	void testGetToxicityReport_Clean() {
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(cleanScores));

		String report = validator.getToxicityReportAsync(cleanText).join();

		assertNotNull(report, "Report should not be null");
		assertFalse(report.isEmpty(), "Report should not be empty");

		for (String category : cleanScores.keySet()) {
			assertTrue(report.contains(category), "Report should contain category: " + category);
		}
	}

	@Test
	@DisplayName("Test getToxicityReport with toxic text")
	void testGetToxicityReport_Toxic() {
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		String report = validator.getToxicityReportAsync(toxicText).join();

		assertNotNull(report, "Report should not be null");
		assertFalse(report.isEmpty(), "Report should not be empty");

		for (String category : toxicScores.keySet()) {
			assertTrue(report.contains(category), "Report should contain category: " + category);
			String expectedPercentage = String.format("%.1f%%", toxicScores.get(category) * 100);
			assertTrue(report.contains(expectedPercentage),
				"Report should contain formatted percentage: " + expectedPercentage);
		}
	}

	@Test
	@DisplayName("Test getToxicityReport with empty response")
	void testGetToxicityReport_EmptyResponse() {
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(new HashMap<>()));

		String report = validator.getToxicityReportAsync("Valid text").join();

		assertNotNull(report, "Report should not be null");
		assertFalse(report.isEmpty(), "Report should not be empty");
		assertTrue(report.contains("No toxicity categories found"),
			"Report should indicate no categories were found");
	}

	@Test
	@DisplayName("Test isTextToxic with default threshold - toxic")
	void testIsTextToxic_DefaultThreshold_Toxic() {
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		boolean result = validator.isTextToxicAsync(toxicText).join();

		assertTrue(result, "Text should be detected as toxic with default threshold");
	}

	@Test
	@DisplayName("Test isTextToxic with default threshold - clean")
	void testIsTextToxic_DefaultThreshold_Clean() {
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(cleanScores));

		boolean result = validator.isTextToxicAsync(cleanText).join();

		assertFalse(result, "Text should be detected as non-toxic with default threshold");
	}

	@Test
	@DisplayName("Test isTextToxic with custom threshold exactly at score value")
	void testIsTextToxic_ExactThreshold() {
		Map<String, Float> testScores = new HashMap<>();
		testScores.put("TEST_CATEGORY", 0.5f);

		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(testScores));

		assertFalse(validator.isTextToxicAsync("test", 0.5f).join(),
			"Text should not be toxic when threshold equals score");

		assertTrue(validator.isTextToxicAsync("test", 0.49f).join(),
			"Text should be toxic when threshold is below score");
	}

	@Test
	@DisplayName("Test lower boundary threshold value")
	void testIsTextToxic_LowerBoundaryThreshold() {
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		assertDoesNotThrow(() -> validator.isTextToxicAsync(toxicText, 0.0f).join());
	}

	@Test
	@DisplayName("Test upper boundary threshold value")
	void testIsTextToxic_UpperBoundaryThreshold() {
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		assertDoesNotThrow(() -> validator.isTextToxicAsync(toxicText, 1.0f).join());
	}

	@Test
	@DisplayName("Test negative threshold value")
	void testIsTextToxic_NegativeThreshold() {
		CompletionException ex = assertThrows(CompletionException.class,
			() -> validator.isTextToxicAsync(toxicText, -0.1f).join()
		);

		assertTrue(ex.getCause() instanceof InvalidThresholdException);
	}

	@Test
	@DisplayName("Test service exception handling")
	void testServiceExceptionHandling() {
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenThrow(new RuntimeException("API Service error"));

		assertThrows(CompletionException.class,
			() -> validator.getToxicityScoresAsync("test text").join());
	}

	@Test
	@DisplayName("Test API client with null credentials throws NullPointerException")
	void testApiClientWithNullCredentials() {
		when(mockApiClient.getClient()).thenThrow(new NullPointerException("Null credentials"));

		CompletionException ex = assertThrows(CompletionException.class,
			() -> validator.getToxicityScoresAsync("test").join());
		assertTrue(ex.getCause() instanceof NullPointerException,
			"Expected NullPointerException to be wrapped in CompletionException");
	}

	@Test
	@DisplayName("Test getToxicityScoresAsync result structure")
	void testGetToxicityScoresAsyncStructure() {
		Map<String, Float> testScores = new HashMap<>();
		testScores.put("TOXICITY", 0.7f);
		testScores.put("PROFANITY", 0.5f);

		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(testScores));

		CompletableFuture<Map<String, Float>> future = validator.getToxicityScoresAsync("test");

		assertNotNull(future, "Future result should not be null");
		assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS), "Future should complete within timeout");

		Map<String, Float> result = future.join();
		assertEquals(2, result.size(), "Result should contain all scores");
		assertEquals(0.7f, result.get("TOXICITY"), "TOXICITY score should match");
		assertEquals(0.5f, result.get("PROFANITY"), "PROFANITY score should match");
	}

	@Test
	@DisplayName("Test moderateTextAsync function")
	void testModerateTextAsync() {
		ModerateTextResponse expectedResponse = mockResponse(toxicScores);
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(expectedResponse);

		ModerateTextResponse response = validator.moderateTextAsync("test text").join();

		assertNotNull(response, "Response should not be null");
		assertEquals(expectedResponse.getModerationCategoriesCount(), response.getModerationCategoriesCount(),
			"Response should have the same number of categories");
	}

	@Test
	@DisplayName("Test client is correctly used in moderateText")
	void testClientUsage() {
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(new HashMap<>()));

		validator.moderateTextAsync("test text").join();

		verify(mockApiClient, times(1)).getClient();
		verify(mockLanguageClient, times(1)).moderateText(any(ModerateTextRequest.class));
	}

	@Test
	@DisplayName("Test text with boundary length")
	void testBoundaryTextLength() {
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(new HashMap<>()));

		String maxLengthText = String.join("", Collections.nCopies(1000, "a"));
		assertDoesNotThrow(() -> validator.getToxicityScoresAsync(maxLengthText).join());

		String tooLongText = maxLengthText + "a";
		assertThrows(CompletionException.class,
			() -> validator.getToxicityScoresAsync(tooLongText).join());
	}

	@Test
	@DisplayName("Test validator close method")
	void testClose() {
		GoogleApiClient tempMockClient = mock(GoogleApiClient.class);
		ToxicityValidator tempValidator = new ToxicityValidator(tempMockClient);

		tempValidator.close();

		verify(tempMockClient, times(1)).close();
	}

	@Test
	@DisplayName("Test exception during close")
	void testExceptionDuringClose() {
		GoogleApiClient tempMockClient = mock(GoogleApiClient.class);
		doThrow(new RuntimeException("Close error")).when(tempMockClient).close();

		ToxicityValidator tempValidator = new ToxicityValidator(tempMockClient);

		assertThrows(RuntimeException.class, tempValidator::close);
	}
}
