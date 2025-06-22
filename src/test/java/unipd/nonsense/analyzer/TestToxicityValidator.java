package unipd.nonsense.analyzer;

import com.google.cloud.language.v1.*;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.ModerateTextRequest;
import org.junit.jupiter.api.*;
import unipd.nonsense.exceptions.InvalidTextException;
import unipd.nonsense.exceptions.InvalidThresholdException;
import unipd.nonsense.util.GoogleApiClient;
import unipd.nonsense.util.JsonFileHandler;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;

@DisplayName("Testing ToxicityValidator")
class TestToxicityValidator
{
	private ToxicityValidator validator;
	private GoogleApiClient mockApiClient;
	private LanguageServiceClient mockLanguageClient;

	private static final String TEST_JSON_PATH = "target" + File.separator + "resources" + File.separator + "testToxicity.json";

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
			String categoryName = entry.getKey() != null ? entry.getKey() : "null";
			responseBuilder.addModerationCategories(
				ClassificationCategory.newBuilder()
				.setName(categoryName)
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
		CompletionException ex = assertThrows(CompletionException.class, () -> validator.isTextToxicAsync("text", 1.5f).join());

		assertTrue(ex.getCause() instanceof InvalidThresholdException);
	}

	@Test
	@DisplayName("Null input to moderateText should throw exception")
	void testModerateText_NullInput()
	{
		CompletionException ex = assertThrows(CompletionException.class, () -> validator.moderateTextAsync(null).join());

		assertTrue(ex.getCause() instanceof InvalidTextException);
	}

	@Test
	@DisplayName("Empty input to moderateText should throw exception")
	void testModerateText_EmptyInput()
	{
		CompletionException ex = assertThrows(CompletionException.class, () -> validator.moderateTextAsync("   ").join());

		assertTrue(ex.getCause() instanceof InvalidTextException);
	}

	@Test
	@DisplayName("Text exceeding max length should throw InvalidTextException")
	void testTextExceedsMaxLength()
	{
		String longText = String.join("", Collections.nCopies(1001, "a"));
		CompletionException ex = assertThrows(CompletionException.class,() -> validator.getToxicityScoresAsync(longText).join());

		assertTrue(ex.getCause() instanceof InvalidTextException);
	}

	@Test
	@DisplayName("Test getToxicityReport with clean text")
	void testGetToxicityReport_Clean()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(cleanScores));

		String report = validator.getToxicityReportAsync(cleanText).join();

		assertNotNull(report, "Report should not be null");
		assertFalse(report.isEmpty(), "Report should not be empty");

		for(String category : cleanScores.keySet())
			assertTrue(report.contains(category), "Report should contain category: " + category);
	}

	@Test
	@DisplayName("Test getToxicityReport with toxic text")
	void testGetToxicityReport_Toxic()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		String report = validator.getToxicityReportAsync(toxicText).join();

		assertNotNull(report, "Report should not be null");
		assertFalse(report.isEmpty(), "Report should not be empty");

		for(String category : toxicScores.keySet())
		{
			assertTrue(report.contains(category), "Report should contain category: " + category);
			String expectedPercentage = String.format("%.1f%%", toxicScores.get(category) * 100);
			assertTrue(report.contains(expectedPercentage),
				"Report should contain formatted percentage: " + expectedPercentage);
		}
	}

	@Test
	@DisplayName("Test getToxicityReport with empty response")
	void testGetToxicityReport_EmptyResponse()
	{
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
	void testIsTextToxic_DefaultThreshold_Toxic()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		boolean result = validator.isTextToxicAsync(toxicText).join();

		assertTrue(result, "Text should be detected as toxic with default threshold");
	}

	@Test
	@DisplayName("Test isTextToxic with default threshold - clean")
	void testIsTextToxic_DefaultThreshold_Clean()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(cleanScores));

		boolean result = validator.isTextToxicAsync(cleanText).join();

		assertFalse(result, "Text should be detected as non-toxic with default threshold");
	}

	@Test
	@DisplayName("Test isTextToxic with custom threshold exactly at score value")
	void testIsTextToxic_ExactThreshold()
	{
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
	void testIsTextToxic_LowerBoundaryThreshold()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		assertDoesNotThrow(() -> validator.isTextToxicAsync(toxicText, 0.0f).join());
	}

	@Test
	@DisplayName("Test upper boundary threshold value")
	void testIsTextToxic_UpperBoundaryThreshold()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		assertDoesNotThrow(() -> validator.isTextToxicAsync(toxicText, 1.0f).join());
	}

	@Test
	@DisplayName("Test negative threshold value")
	void testIsTextToxic_NegativeThreshold()
	{
		CompletionException ex = assertThrows(CompletionException.class, () -> validator.isTextToxicAsync(toxicText, -0.1f).join());

		assertTrue(ex.getCause() instanceof InvalidThresholdException);
	}

	@Test
	@DisplayName("Test service exception handling")
	void testServiceExceptionHandling()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenThrow(new RuntimeException("API Service error"));

		assertThrows(CompletionException.class, () -> validator.getToxicityScoresAsync("test text").join());
	}

	@Test
	@DisplayName("Test API client with null credentials throws NullPointerException")
	void testApiClientWithNullCredentials()
	{
		when(mockApiClient.getClient()).thenThrow(new NullPointerException("Null credentials"));

		CompletionException ex = assertThrows(CompletionException.class, () -> validator.getToxicityScoresAsync("test").join());

		assertTrue(ex.getCause() instanceof NullPointerException,"Expected NullPointerException to be wrapped in CompletionException");
	}

	@Test
	@DisplayName("Test getToxicityScoresAsync result structure")
	void testGetToxicityScoresAsyncStructure()
	{
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
	void testModerateTextAsync()
	{
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
	void testClientUsage()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(new HashMap<>()));

		validator.moderateTextAsync("test text").join();

		verify(mockApiClient, times(1)).getClient();
		verify(mockLanguageClient, times(1)).moderateText(any(ModerateTextRequest.class));
	}

	@Test
	@DisplayName("Test text with boundary length")
	void testBoundaryTextLength()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(new HashMap<>()));

		String maxLengthText = String.join("", Collections.nCopies(1000, "a"));
		assertDoesNotThrow(() -> validator.getToxicityScoresAsync(maxLengthText).join());

		String tooLongText = maxLengthText + "a";
		assertThrows(CompletionException.class, () -> validator.getToxicityScoresAsync(tooLongText).join());
	}

	@Test
	@DisplayName("Test validator close method")
	void testClose()
	{
		GoogleApiClient tempMockClient = mock(GoogleApiClient.class);
		ToxicityValidator tempValidator = new ToxicityValidator(tempMockClient);

		tempValidator.close();

		verify(tempMockClient, times(1)).close();
	}

	@Test
	@DisplayName("Test exception during close")
	void testExceptionDuringClose()
	{
		GoogleApiClient tempMockClient = mock(GoogleApiClient.class);
		doThrow(new RuntimeException("Close error")).when(tempMockClient).close();

		ToxicityValidator tempValidator = new ToxicityValidator(tempMockClient);

		assertThrows(RuntimeException.class, tempValidator::close);
	}

	@Test
	@DisplayName("Test concurrent toxicity checks with multiple texts")
	void testConcurrentToxicityChecks()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		List<String> testTexts = IntStream.range(0, 10)
			.mapToObj(i -> "Test text " + i)
			.collect(Collectors.toList());

		List<CompletableFuture<Boolean>> futures = testTexts.stream()
			.map(validator::isTextToxicAsync)
			.collect(Collectors.toList());

		CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

		allFutures.join();

		for(CompletableFuture<Boolean> future : futures)
		{
			assertTrue(future.isDone(), "Future should be completed");
			assertDoesNotThrow(() -> future.get(), "Future should complete without exception");
		}
	}

	@Test
	@DisplayName("Test multiple concurrent requests with different methods")
	void testMultipleConcurrentRequests()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		CompletableFuture<Boolean> toxicFuture = validator.isTextToxicAsync("Toxic text");
		CompletableFuture<Map<String, Float>> scoresFuture = validator.getToxicityScoresAsync("Score text");
		CompletableFuture<String> reportFuture = validator.getToxicityReportAsync("Report text");
		CompletableFuture<ModerateTextResponse> moderateFuture = validator.moderateTextAsync("Moderate text");

		CompletableFuture<Void> allFutures = CompletableFuture.allOf(toxicFuture, scoresFuture, reportFuture, moderateFuture);

		allFutures.join();

		assertTrue(toxicFuture.isDone(), "Toxicity check future should be completed");
		assertTrue(scoresFuture.isDone(), "Scores future should be completed");
		assertTrue(reportFuture.isDone(), "Report future should be completed");
		assertTrue(moderateFuture.isDone(), "Moderate future should be completed");
	}

	@Test
	@DisplayName("Test toxicity report formatting with multiple categories")
	void testToxicityReportFormatting()
	{
		Map<String, Float> formattedScores = new HashMap<>();
		formattedScores.put("TOXICITY", 0.7532f);
		formattedScores.put("PROFANITY", 0.4267f);
		formattedScores.put("SEXUALLY_EXPLICIT", 0.0123f);

		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(formattedScores));

		String report = validator.getToxicityReportAsync("Test text").join();

		assertNotNull(report, "Report should not be null");
		assertFalse(report.isEmpty(), "Report should not be empty");

		assertTrue(Pattern.compile("TOXICITY\\s+:\\s*75[\\.,]3%").matcher(report).find(),
			"Report should format TOXICITY score correctly");
		assertTrue(Pattern.compile("PROFANITY\\s+:\\s*42[\\.,]7%").matcher(report).find(),
			"Report should format PROFANITY score correctly");
		assertTrue(Pattern.compile("SEXUALLY_EXPLICIT\\s+:\\s*1[\\.,]2%").matcher(report).find(),
			"Report should format SEXUALLY_EXPLICIT score correctly");

		String[] lines = report.split("\n");
		for(String line : lines)
		{
			if(!line.isEmpty())
			{
				assertTrue(line.contains(": "), "Each line should contain ': ' separator");
				String[] parts = line.split(":");
				assertEquals(25, parts[0].length(), "Category column should be 25 characters wide");
			}
		}
	}

	@Test
	@DisplayName("Test toxicity report formatting with extreme values")
	void testToxicityReportFormatting_ExtremeValues()
	{
		Map<String, Float> extremeScores = new HashMap<>();
		extremeScores.put("ZERO_SCORE", 0.0f);
		extremeScores.put("FULL_SCORE", 1.0f);
		extremeScores.put("TINY_SCORE", 0.001f);
		extremeScores.put("HIGH_SCORE", 0.999f);

		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(extremeScores));

		String report = validator.getToxicityReportAsync("Test text").join();

		assertTrue(Pattern.compile("ZERO_SCORE\\s+:\\s*0[\\.,]0%").matcher(report).find(),
			"Report should format zero score correctly (0.0%)");
		assertTrue(Pattern.compile("FULL_SCORE\\s+:\\s*100[\\.,]0%").matcher(report).find(),
			"Report should format full score correctly (100.0%)");
		assertTrue(Pattern.compile("TINY_SCORE\\s+:\\s*0[\\.,]1%").matcher(report).find(),
			"Report should format tiny score correctly (0.1%)");
		assertTrue(Pattern.compile("HIGH_SCORE\\s+:\\s*99[\\.,]9%").matcher(report).find(),
			"Report should format high score correctly (99.9%)");
	}

	@Test
	@DisplayName("Test validateInput with exactly 1000 characters")
	void testValidateInput_ExactMaxLength()
	{
		String exactLengthText = String.join("", Collections.nCopies(1000, "a"));

		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(new HashMap<>()));

		assertDoesNotThrow(() -> validator.getToxicityScoresAsync(exactLengthText).join());
	}

	@Test
	@DisplayName("Test getToxicityScores with null response from API")
	void testGetToxicityScores_NullResponse()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(null);

		CompletionException ex = assertThrows(CompletionException.class,
			() -> validator.getToxicityScoresAsync("test").join());

		assertTrue(ex.getCause() instanceof NullPointerException);
	}

	@Test
	@DisplayName("Test isTextToxic with empty categories list")
	void testIsTextToxic_EmptyCategories()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(new HashMap<>()));

		assertFalse(validator.isTextToxicAsync("test").join(), "Empty categories should return non-toxic");
	}

	@Test
	@DisplayName("Test document building through public API")
	void testDocumentBuilding()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(new HashMap<>()));

		String specialCharsText = "Text with special chars: \n\t\\\"'!@#$%^&*()_+-=[]{}|;:,.<>/?";

		assertDoesNotThrow(() -> validator.getToxicityScoresAsync(specialCharsText).join());

		verify(mockLanguageClient).moderateText(argThat((ModerateTextRequest request) ->
		{
			Document doc = request.getDocument();
			return doc.getContent().equals(specialCharsText) && doc.getType() == Document.Type.PLAIN_TEXT;
		}));
	}

	@Test
	@DisplayName("Test document type is always PLAIN_TEXT")
	void testDocumentType()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(new HashMap<>()));

		validator.getToxicityScoresAsync("test").join();

		verify(mockLanguageClient).moderateText(argThat((ModerateTextRequest request) -> request.getDocument().getType() == Document.Type.PLAIN_TEXT));
	}

	@Test
	@DisplayName("Test multiple sequential operations with same validator instance")
	void testMultipleSequentialOperations()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(cleanScores))
			.thenReturn(mockResponse(toxicScores))
			.thenReturn(mockResponse(cleanScores));

		assertFalse(validator.isTextToxicAsync(cleanText).join());
		assertTrue(validator.isTextToxicAsync(toxicText).join());
		assertFalse(validator.isTextToxicAsync(cleanText).join());
	}

	@Test
	@DisplayName("Test getToxicityScores with duplicate categories")
	void testGetToxicityScores_DuplicateCategories()
	{
		Map<String, Float> duplicateScores = new HashMap<>();
		duplicateScores.put("DUPLICATE", 0.5f);
		duplicateScores.put("DUPLICATE", 0.7f);

		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(duplicateScores));

		Map<String, Float> result = validator.getToxicityScoresAsync("test").join();
		assertEquals(1, result.size(), "Should handle duplicate categories by overwriting");
		assertEquals(0.7f, result.get("DUPLICATE"), "Should keep last value for duplicate key");
	}

	@Test
	@DisplayName("Test isTextToxic with threshold at 0.0")
	void testIsTextToxic_ZeroThreshold()
	{
		Map<String, Float> minimalScores = new HashMap<>();
		minimalScores.put("MINIMAL", 0.0001f);

		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(minimalScores));

		assertTrue(validator.isTextToxicAsync("test", 0.0f).join(), "Any score should be toxic when threshold is 0.0");
	}

	@Test
	@DisplayName("Test isTextToxic with threshold at 1.0")
	void testIsTextToxic_MaxThreshold()
	{
		Map<String, Float> maxScores = new HashMap<>();
		maxScores.put("MAX", 1.0f);

		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(maxScores));

		assertFalse(validator.isTextToxicAsync("test", 1.0f).join(), "Only scores > 1.0 would be toxic at threshold 1.0 (impossible)");
	}

	@Test
	@DisplayName("Test getToxicityReport with null category name")
	void testGetToxicityReport_NullCategoryName()
	{
		Map<String, Float> nullNameScores = new HashMap<>();
		nullNameScores.put(null, 0.5f);

		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(nullNameScores));

		String report = validator.getToxicityReportAsync("test").join();
		assertTrue(report.contains("null"), "Report should handle null category names");
	}

	@Test
	@DisplayName("Test getToxicityReport with very long category names")
	void testGetToxicityReport_LongCategoryNames()
	{
		Map<String, Float> longNameScores = new HashMap<>();
		String longName = "VERY_LONG_CATEGORY_NAME_THAT_EXCEEDS_TYPICAL_LENGTH_BY_A_SIGNIFICANT_MARGIN";
		longNameScores.put(longName, 0.5f);

		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(longNameScores));

		String report = validator.getToxicityReportAsync("test").join();
		assertTrue(report.contains(longName), "Report should handle very long category names");
	}

	@Test
	@DisplayName("Test stress test with many concurrent requests")
	void testStressTest_ManyConcurrentRequests()
	{
		when(mockLanguageClient.moderateText(any(ModerateTextRequest.class)))
			.thenReturn(mockResponse(toxicScores));

		int requestCount = 100;
		List<CompletableFuture<Boolean>> futures = new ArrayList<>();

		for(int i = 0; i < requestCount; i++)
			futures.add(validator.isTextToxicAsync("stress test " + i));

		CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

		assertDoesNotThrow(() -> allFutures.get(10, TimeUnit.SECONDS), "Should handle many concurrent requests without timeout");
	}

	@Test
	@DisplayName("Test resource cleanup after validation failure")
	void testResourceCleanupAfterValidationFailure()
	{
		GoogleApiClient tempMockClient = mock(GoogleApiClient.class);
		ToxicityValidator tempValidator = new ToxicityValidator(tempMockClient);

		assertThrows(CompletionException.class,() -> tempValidator.getToxicityScoresAsync("").join());

		verify(tempMockClient, times(0)).close();
		tempValidator.close();
		verify(tempMockClient, times(1)).close();
	}
}
