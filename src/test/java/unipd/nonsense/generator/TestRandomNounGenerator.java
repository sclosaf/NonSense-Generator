package unipd.nonsense.generator;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Noun.Number;
import unipd.nonsense.exceptions.InvalidListException;
import unipd.nonsense.exceptions.InvalidJsonStateException;
import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.util.JsonUpdater;
import unipd.nonsense.util.LoggerManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

@DisplayName("Testing RandomNounGenerator")
@ExtendWith(MockitoExtension.class)
class TestRandomNounGenerator
{
	private RandomNounGenerator generator;
	private File testFile;
	private String testFilePath;

	@Mock
	private JsonFileHandler mockJsonHandler;

	@Mock
	private Random mockRandom;

	@BeforeEach
	@DisplayName("Setup environment: Use testNouns.json from target/resources")
	void setUp() throws Exception
	{
		String resourcePath = "target" + File.separator + "resources" + File.separator + "testNouns.json";
		testFile = new File(resourcePath);
		testFilePath = testFile.getAbsolutePath();

		testFile.getParentFile().mkdirs();

		if(!testFile.exists())
		{
			JsonObject testNouns = createDefaultTestNouns();
			try(FileWriter writer = new FileWriter(testFile))
			{
				writer.write(testNouns.toString());
				writer.flush();
			}
		}

		generator = new RandomNounGenerator(testFilePath);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if(generator != null)
			generator.cleanup();

		try (InputStream in = getClass().getResourceAsStream("/testNouns.json"); FileWriter out = new FileWriter(testFile))
		{
			String content = new String(in.readAllBytes());
			out.write(content);
		}
	}

	private JsonObject createDefaultTestNouns()
	{
		JsonObject json = new JsonObject();
		JsonArray singularNouns = new JsonArray();
		JsonArray pluralNouns = new JsonArray();

		singularNouns.add("test");
		singularNouns.add("noun");
		singularNouns.add("word");

		pluralNouns.add("tests");
		pluralNouns.add("nouns");
		pluralNouns.add("words");

		json.add("singularNouns", singularNouns);
		json.add("pluralNouns", pluralNouns);

		return json;
	}

	@Test
	@DisplayName("Test success of getRandomNoun")
	void testGetRandomNoun_Success()
	{
		Noun noun = generator.getRandomNoun();
		assertNotNull(noun, "Should return a valid noun");
		assertNotNull(noun.getNoun(), "Noun should have a value");
		assertNotNull(noun.getNumber(), "Noun should have a number type");
	}

	@Test
	@DisplayName("Test success of getRandomNoun with SINGULAR type")
	void testGetRandomNoun_SingularSuccess()
	{
		Noun noun = generator.getRandomNoun(Number.SINGULAR);
		assertEquals(Number.SINGULAR, noun.getNumber(), "Noun type should be SINGULAR");
		assertTrue(List.of("test", "noun", "word").contains(noun.getNoun()), "Noun should be from test list");
	}

	@Test
	@DisplayName("Test success of getRandomNoun with PLURAL type")
	void testGetRandomNoun_PluralSuccess()
	{
		Noun noun = generator.getRandomNoun(Number.PLURAL);
		assertEquals(Number.PLURAL, noun.getNumber(), "Noun type should be PLURAL");
		assertTrue(List.of("tests", "nouns", "words").contains(noun.getNoun()), "Noun should be from test list");
	}

	@Test
	@DisplayName("Test attempt to get noun from empty list")
	void testGetRandomNoun_EmptyList() throws Exception
	{
		JsonObject emptyJson = new JsonObject();
		emptyJson.add("singularNouns", new JsonArray());
		emptyJson.add("pluralNouns", new JsonArray());

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(emptyJson.toString());
		}

		generator = new RandomNounGenerator(testFilePath);

		assertThrows(InvalidListException.class, () -> generator.getRandomNoun(),
			"Should throw InvalidListException when no nouns available");
		assertThrows(InvalidListException.class, () -> generator.getRandomNoun(Number.SINGULAR),
			"Should throw InvalidListException when no singular nouns available");
		assertThrows(InvalidListException.class, () -> generator.getRandomNoun(Number.PLURAL),
			"Should throw InvalidListException when no plural nouns available");
	}

	@Test
	@DisplayName("Test initialization with invalid JSON file")
	void testInitialization_InvalidJson() throws Exception
	{
		try (FileWriter writer = new FileWriter(testFile))
		{
			writer.write("invalid json content");
		}

		assertThrows(InvalidJsonStateException.class, () -> new RandomNounGenerator(testFilePath),
			"Should throw InvalidJsonStateException when JSON is invalid");
	}

	@Test
	@DisplayName("Test nouns count matches JSON file content")
	void testNounsCount() throws Exception
	{
		Field nounsField = RandomNounGenerator.class.getDeclaredField("nouns");
		nounsField.setAccessible(true);

		@SuppressWarnings("unchecked")
		var nounsMap = (Map<Number, List<Noun>>) nounsField.get(generator);

		JsonObject testNouns = JsonParser.parseString(Files.readString(testFile.toPath())).getAsJsonObject();
		int expectedSingularCount = testNouns.getAsJsonArray("singularNouns").size();
		int expectedPluralCount = testNouns.getAsJsonArray("pluralNouns").size();

		assertEquals(expectedSingularCount, nounsMap.get(Number.SINGULAR).size(),
			"Number of singular nouns should match the JSON file");
		assertEquals(expectedPluralCount, nounsMap.get(Number.PLURAL).size(),
			"Number of plural nouns should match the JSON file");
	}

	@Test
	@DisplayName("Test the noun selection is random")
	void testNounSelectionIsRandom() throws Exception
	{
		List<String> nouns = new ArrayList<>();
		for (int i = 0; i < 30; i++)
		{
			Noun noun = generator.getRandomNoun();
			nouns.add(noun.getNoun());
		}

		long distinctNouns = nouns.stream().distinct().count();
		assertTrue(distinctNouns > 1, "Noun selection should be random and return different nouns");
	}

	@Test
	@DisplayName("Test nouns properties are preserved")
	void testNounPropertiesPreserved()
	{
		Noun singularNoun = generator.getRandomNoun(Number.SINGULAR);
		Noun pluralNoun = generator.getRandomNoun(Number.PLURAL);

		assertTrue(List.of("test", "noun", "word").contains(singularNoun.getNoun()), "Singular noun should be from test list");
		assertTrue(List.of("tests", "nouns", "words").contains(pluralNoun.getNoun()), "Plural noun should be from test list");

		assertEquals(Number.SINGULAR, singularNoun.getNumber(), "Noun type should be SINGULAR");
		assertEquals(Number.PLURAL, pluralNoun.getNumber(), "Noun type should be PLURAL");
	}

	@Test
	@DisplayName("Test JSON update observer functionality")
	void testJsonUpdateObserver() throws Exception
	{
		Field nounsField = RandomNounGenerator.class.getDeclaredField("nouns");
		nounsField.setAccessible(true);

		@SuppressWarnings("unchecked")
		var initialNounsMap = (Map<Number, List<Noun>>) nounsField.get(generator);
		int initialSingularCount = initialNounsMap.get(Number.SINGULAR).size();

		JsonObject updatedJson = new JsonObject();
		JsonArray updatedSingularNouns = new JsonArray();
		JsonArray updatedPluralNouns = new JsonArray();

		updatedSingularNouns.add("test");
		updatedSingularNouns.add("noun");
		updatedSingularNouns.add("word");
		updatedSingularNouns.add("new");
		updatedSingularNouns.add("another");

		updatedPluralNouns.add("tests");
		updatedPluralNouns.add("nouns");
		updatedPluralNouns.add("words");

		updatedJson.add("singularNouns", updatedSingularNouns);
		updatedJson.add("pluralNouns", updatedPluralNouns);

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(updatedJson.toString());
		}

		generator.onJsonUpdate();

		@SuppressWarnings("unchecked")
		var updatedNounsMap = (Map<Number, List<Noun>>) nounsField.get(generator);
		long updatedSingularCount = updatedNounsMap.get(Number.SINGULAR).size();

		assertEquals(5, updatedSingularCount, "Number of singular nouns should be updated to 5");
		assertTrue(updatedSingularCount > initialSingularCount, "Noun count should increase after update");
	}

	@Test
	@DisplayName("Test cleanup removes observer")
	void testCleanup() throws Exception
	{
		try(MockedStatic<JsonUpdater> mockedJsonUpdater = Mockito.mockStatic(JsonUpdater.class))
		{
			generator.cleanup();
			mockedJsonUpdater.verify(() -> JsonUpdater.removeObserver(generator), times(1));
		}
	}

	@Test
	@DisplayName("Test behavior with missing JSON keys")
	void testMissingJsonKeys() throws Exception
	{
		JsonObject partialJson = new JsonObject();
		JsonArray singularNouns = new JsonArray();
		singularNouns.add("test");
		partialJson.add("singularNouns", singularNouns);
		partialJson.add("pluralNouns", new JsonArray());

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(partialJson.toString());
		}

		RandomNounGenerator partialGenerator = new RandomNounGenerator(testFilePath);

		Noun singularNoun = partialGenerator.getRandomNoun(Number.SINGULAR);
		assertNotNull(singularNoun, "Should return a singular noun");

		assertThrows(InvalidListException.class, () -> partialGenerator.getRandomNoun(Number.PLURAL),
			"Should throw InvalidListException when plural nouns list is empty");

		partialGenerator.cleanup();
	}

	@Test
	@DisplayName("Test logging behavior")
	void testLoggingBehavior() throws Exception
	{
		LoggerManager mockLogger = mock(LoggerManager.class);

		Field loggerField = RandomNounGenerator.class.getDeclaredField("logger");
		loggerField.setAccessible(true);
		LoggerManager originalLogger = (LoggerManager) loggerField.get(generator);
		loggerField.set(generator, mockLogger);

		try
		{
			generator.getRandomNoun();
			generator.getRandomNoun(Number.SINGULAR);
			generator.cleanup();

			verify(mockLogger, atLeastOnce()).logTrace(anyString());
			verify(mockLogger, atLeastOnce()).logDebug(anyString());
		}
		finally
		{
			loggerField.set(generator, originalLogger);
		}
	}

	@Test
	@DisplayName("Test random distribution of nouns")
	void testRandomDistribution()
	{
		Map<String, Integer> countMap = new HashMap<>();
		int iterations = 1000;

		for(int i = 0; i < iterations; i++)
		{
			Noun n = generator.getRandomNoun();
			countMap.merge(n.getNoun(), 1, Integer::sum);
		}

		double expected = iterations / (double) countMap.size();
		for(int count : countMap.values())
			assertTrue(Math.abs(count - expected) < expected * 0.3, "Noun distribution should be roughly uniform");
	}

	@Test
	@DisplayName("Test concurrent access to noun generation")
	void testConcurrentAccess() throws Exception
	{
		int threadCount = 100;
		CountDownLatch latch = new CountDownLatch(threadCount);
		ExecutorService executor = Executors.newFixedThreadPool(10);

		for (int i = 0; i < threadCount; i++)
		{
			executor.submit(() ->
				{
					try
					{
						Noun noun = generator.getRandomNoun();
						assertNotNull(noun, "Noun should not be null");
					}
					finally
					{
						latch.countDown();
					}
				});
		}

		latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
		executor.shutdown();
		assertEquals(0, latch.getCount(), "All threads should have completed");
	}

	@Test
	@DisplayName("Test with maximum noun length")
	void testMaxNounLength() throws Exception
	{
		String longNoun = "a".repeat(10000);

		JsonObject testNouns = new JsonObject();
		JsonArray singularNouns = new JsonArray();

		singularNouns.add(longNoun);
		testNouns.add("singularNouns", singularNouns);
		testNouns.add("pluralNouns", new JsonArray());

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(testNouns.toString());
		}

		RandomNounGenerator longNounGenerator = new RandomNounGenerator(testFilePath);
		Noun noun = longNounGenerator.getRandomNoun(Number.SINGULAR);

		assertEquals(longNoun, noun.getNoun());
		longNounGenerator.cleanup();
	}
}
