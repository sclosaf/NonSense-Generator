package unipd.nonsense.analyzer;

import com.google.cloud.language.v1.*;
import org.junit.jupiter.api.*;
import unipd.nonsense.exceptions.InvalidTextException;
import unipd.nonsense.exceptions.InvalidThresholdException;
import unipd.nonsense.util.GoogleApiClient;
import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.util.LoggerManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.*;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Testing ToxicityValidator")
class TestToxicityValidator
{
	private ToxicityValidator validator;
	private GoogleApiClient mockApiClient;
	private LoggerManager mockLogger;
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
		mockLogger = mock(LoggerManager.class);
		mockLanguageClient = mock(LanguageServiceClient.class);

		when(mockApiClient.getClient()).thenReturn(mockLanguageClient);

		validator = new ToxicityValidator(mockApiClient, mockLogger);
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
}
