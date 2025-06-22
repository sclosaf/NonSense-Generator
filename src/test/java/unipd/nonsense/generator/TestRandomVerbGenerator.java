package unipd.nonsense.generator;

import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Tense;
import unipd.nonsense.model.Pair;
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

@DisplayName("Testing RandomVerbGenerator")
@ExtendWith(MockitoExtension.class)
class TestRandomVerbGenerator
{
	private RandomVerbGenerator generator;
	private File testFile;
	private String testFilePath;

	@Mock
	private JsonFileHandler mockJsonHandler;

	@Mock
	private Random mockRandom;

	@BeforeEach
	@DisplayName("Setup environment: Use testVerbs.json from target/resources")
	void setUp() throws Exception
	{
		String resourcePath = "target" + File.separator + "resources" + File.separator + "testVerbs.json";
		testFile = new File(resourcePath);
		testFilePath = testFile.getAbsolutePath();

		testFile.getParentFile().mkdirs();

		if(!testFile.exists())
		{
			JsonObject testVerbs = createDefaultTestVerbs();
			try(FileWriter writer = new FileWriter(testFile))
			{
				writer.write(testVerbs.toString());
				writer.flush();
			}
		}

		generator = new RandomVerbGenerator(testFilePath);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if(generator != null)
			generator.cleanup();

		try (InputStream in = getClass().getResourceAsStream("/testVerbs.json"); FileWriter out = new FileWriter(testFile))
		{
			String content = new String(in.readAllBytes());
			out.write(content);
		}
	}

	private JsonObject createDefaultTestVerbs()
	{
		JsonObject json = new JsonObject();
		JsonArray pastSingularVerbs = new JsonArray();
		JsonArray pastPluralVerbs = new JsonArray();
		JsonArray presentSingularVerbs = new JsonArray();
		JsonArray presentPluralVerbs = new JsonArray();
		JsonArray futureSingularVerbs = new JsonArray();
		JsonArray futurePluralVerbs = new JsonArray();

		pastSingularVerbs.add("tested");
		pastSingularVerbs.add("climbed");
		pastSingularVerbs.add("jumped");

		pastPluralVerbs.add("tested");
		pastPluralVerbs.add("climbed");
		pastPluralVerbs.add("jumped");

		presentSingularVerbs.add("tests");
		presentSingularVerbs.add("climbs");
		presentSingularVerbs.add("jumps");

		presentPluralVerbs.add("test");
		presentPluralVerbs.add("climb");
		presentPluralVerbs.add("jump");

		futureSingularVerbs.add("will test");
		futureSingularVerbs.add("will climb");
		futureSingularVerbs.add("will jump");

		futurePluralVerbs.add("will test");
		futurePluralVerbs.add("will climb");
		futurePluralVerbs.add("will jump");

		json.add("pastSingularVerbs", pastSingularVerbs);
		json.add("pastPluralVerbs", pastPluralVerbs);
		json.add("presentSingularVerbs", presentSingularVerbs);
		json.add("presentPluralVerbs", presentPluralVerbs);
		json.add("futureSingularVerbs", futureSingularVerbs);
		json.add("futurePluralVerbs", futurePluralVerbs);

		return json;
	}

	@Test
	@DisplayName("Test success of getRandomVerb")
	void testGetRandomVerb_Success()
	{
		Verb verb = generator.getRandomVerb();
		assertNotNull(verb, "Should return a valid verb");
		assertNotNull(verb.getVerb(), "Verb should have a value");
		assertNotNull(verb.getTense(), "Verb should have a tense");
	}

	@Test
	@DisplayName("Test success of getRandomVerb with PAST tense and SINGULAR number")
	void testGetRandomVerb_SingularPastSuccess()
	{
		Verb verb = generator.getRandomVerb(Number.SINGULAR, Tense.PAST);
		assertEquals(Tense.PAST, verb.getTense(), "Verb tense should be PAST");
		assertEquals(Number.SINGULAR, verb.getNumber(), "Verb number should be SINGULAR");
		assertTrue(List.of("tested", "climbed", "jumped").contains(verb.getVerb()), "Verb should be from test list");
	}

	@Test
	@DisplayName("Test success of getRandomVerb with PAST tense and PLURAL number")
	void testGetRandomVerb_PluralPastSuccess()
	{
		Verb verb = generator.getRandomVerb(Number.PLURAL, Tense.PAST);
		assertEquals(Tense.PAST, verb.getTense(), "Verb tense should be PAST");
		assertEquals(Number.PLURAL, verb.getNumber(), "Verb number should be PLURAL");
		assertTrue(List.of("tested", "climbed", "jumped").contains(verb.getVerb()), "Verb should be from test list");
	}

	@Test
	@DisplayName("Test success of getRandomVerb with PRESENT tense and SINGULAR number")
	void testGetRandomVerb_SingularPresentSuccess()
	{
		Verb verb = generator.getRandomVerb(Number.SINGULAR, Tense.PRESENT);
		assertEquals(Tense.PRESENT, verb.getTense(), "Verb tense should be PRESENT");
		assertEquals(Number.SINGULAR, verb.getNumber(), "Verb number should be SINGULAR");
		assertTrue(List.of("tests", "climbs", "jumps").contains(verb.getVerb()), "Verb should be from test list");
	}
	@Test
	@DisplayName("Test success of getRandomVerb with PRESENT tense and PLURAL number")
	void testGetRandomVerb_PluralPresentSuccess()
	{
		Verb verb = generator.getRandomVerb(Number.PLURAL, Tense.PRESENT);
		assertEquals(Tense.PRESENT, verb.getTense(), "Verb tense should be PRESENT");
		assertEquals(Number.PLURAL, verb.getNumber(), "Verb number should be PLURAL");
		assertTrue(List.of("test", "climb", "jump").contains(verb.getVerb()), "Verb should be from test list");
	}

	@Test
	@DisplayName("Test success of getRandomVerb with FUTURE tense and SINGULAR number")
	void testGetRandomVerb_SingularFutureSuccess()
	{
		Verb verb = generator.getRandomVerb(Number.SINGULAR, Tense.FUTURE);
		assertEquals(Tense.FUTURE, verb.getTense(), "Verb tense should be FUTURE");
		assertEquals(Number.SINGULAR, verb.getNumber(), "Verb number should be SINGULAR");
		assertTrue(List.of("will test", "will climb", "will jump").contains(verb.getVerb()), "Verb should be from test list");
	}
	@Test
	@DisplayName("Test success of getRandomVerb with FUTURE tense and PLURAL number")
	void testGetRandomVerb_PluralFutureSuccess()
	{
		Verb verb = generator.getRandomVerb(Number.PLURAL, Tense.FUTURE);
		assertEquals(Tense.FUTURE, verb.getTense(), "Verb tense should be FUTURE");
		assertEquals(Number.PLURAL, verb.getNumber(), "Verb number should be PLURAL");
		assertTrue(List.of("will test", "will climb", "will jump").contains(verb.getVerb()), "Verb should be from test list");
	}

	@Test
	@DisplayName("Test getRandomVerb with specific Number only returns verbs with that Number")
	void testGetRandomVerb_WithNumberOnly()
	{
			Verb singularVerb = generator.getRandomVerb(Number.SINGULAR);
			Verb pluralVerb = generator.getRandomVerb(Number.PLURAL);

			assertEquals(Number.SINGULAR, singularVerb.getNumber(), "Verb should be SINGULAR");
			assertEquals(Number.PLURAL, pluralVerb.getNumber(), "Verb should be PLURAL");
	}

	@Test
	@DisplayName("Test getRandomVerb with specific Tense only returns verbs with that Tense")
	void testGetRandomVerb_WithTenseOnly()
	{
		Verb pastVerb = generator.getRandomVerb(Tense.PAST);
		Verb presentVerb = generator.getRandomVerb(Tense.PRESENT);
		Verb futureVerb = generator.getRandomVerb(Tense.FUTURE);

		assertEquals(Tense.PAST, pastVerb.getTense(), "Verb should be PAST");
		assertEquals(Tense.PRESENT, presentVerb.getTense(), "Verb should be PRESENT");
		assertEquals(Tense.FUTURE, futureVerb.getTense(), "Verb should be FUTURE");
	}

	@Test
	@DisplayName("Test all possible Tense and Number combinations")
	void testAllTenseNumberCombinations()
	{
		for (Tense tense : Tense.values())
		{
			for (Number number : Number.values())
			{
				Verb verb = generator.getRandomVerb(number, tense);
				assertEquals(number, verb.getNumber(), "Verb number should match requested number");
				assertEquals(tense, verb.getTense(), "Verb tense should match requested tense");
			}
		}
	}

	@Test
	@DisplayName("Test attempt to get verb from empty list")
	void testGetRandomVerb_EmptyList() throws Exception
	{
		JsonObject emptyJson = new JsonObject();
		emptyJson.add("pastSingularVerbs", new JsonArray());
		emptyJson.add("pastPluralVerbs", new JsonArray());
		emptyJson.add("presentSingularVerbs", new JsonArray());
		emptyJson.add("presentPluralVerbs", new JsonArray());
		emptyJson.add("futureSingularVerbs", new JsonArray());
		emptyJson.add("futurePluralVerbs", new JsonArray());

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(emptyJson.toString());
		}

		generator = new RandomVerbGenerator(testFilePath);

		assertThrows(InvalidListException.class, () -> generator.getRandomVerb(),
			"Should throw InvalidListException when no verbs available");
		assertThrows(InvalidListException.class, () -> generator.getRandomVerb(Tense.PAST),
			"Should throw InvalidListException when no past verbs available");
		assertThrows(InvalidListException.class, () -> generator.getRandomVerb(Tense.PRESENT),
			"Should throw InvalidListException when no present verbs available");
		assertThrows(InvalidListException.class, () -> generator.getRandomVerb(Tense.FUTURE),
			"Should throw InvalidListException when no future verbs available");
	}

	@Test
	@DisplayName("Test initialization with invalid JSON file")
	void testInitialization_InvalidJson() throws Exception
	{
		try (FileWriter writer = new FileWriter(testFile))
		{
			writer.write("invalid json content");
		}

		assertThrows(InvalidJsonStateException.class, () -> new RandomVerbGenerator(testFilePath),
			"Should throw InvalidJsonStateException when JSON is invalid");
	}

	@Test
	@DisplayName("Test verbs count matches JSON file content")
	void testVerbsCount() throws Exception
	{
		Field verbsField = RandomVerbGenerator.class.getDeclaredField("verbs");
		verbsField.setAccessible(true);

		@SuppressWarnings("unchecked")
		var verbsMap = (Map<Tense, List<Verb>>) verbsField.get(generator);

		JsonObject testVerbs = JsonParser.parseString(Files.readString(testFile.toPath())).getAsJsonObject();
		int expectedPastSingularCount = testVerbs.getAsJsonArray("pastSingularVerbs").size();
		int expectedPastPluralCount = testVerbs.getAsJsonArray("pastPluralVerbs").size();
		int expectedPresentSingularCount = testVerbs.getAsJsonArray("presentSingularVerbs").size();
		int expectedPresentPluralCount = testVerbs.getAsJsonArray("presentPluralVerbs").size();
		int expectedFutureSingularCount = testVerbs.getAsJsonArray("futureSingularVerbs").size();
		int expectedFuturePluralCount = testVerbs.getAsJsonArray("futurePluralVerbs").size();

		assertEquals(expectedPastSingularCount,verbsMap.get(new Pair<>(Tense.PAST, Number.SINGULAR)).size(), "Number of past singular verbs should match the JSON file");
		assertEquals(expectedPastPluralCount, verbsMap.get(new Pair<>(Tense.PAST, Number.PLURAL)).size(), "Number of past plural verbs should match the JSON file");
	}

	@Test
	@DisplayName("Test the verb selection is random")
	void testVerbSelectionIsRandom() throws Exception
	{
		List<String> verbs = new ArrayList<>();
		for (int i = 0; i < 30; i++)
		{
			Verb verb = generator.getRandomVerb();
			verbs.add(verb.getVerb());
		}

		long distinctVerbs = verbs.stream().distinct().count();
		assertTrue(distinctVerbs > 1, "Verb selection should be random and return different verbs");
	}

	@Test
	@DisplayName("Test verbs properties are preserved")
	void testVerbPropertiesPreserved()
	{
		Verb pastVerb = generator.getRandomVerb(Number.SINGULAR, Tense.PAST);
		Verb presentVerb = generator.getRandomVerb(Number.PLURAL, Tense.PRESENT);
		Verb futureVerb = generator.getRandomVerb(Number.SINGULAR, Tense.FUTURE);

		assertTrue(List.of("tested", "climbed", "jumped").contains(pastVerb.getVerb()), "Past verb should be from test list");
		assertTrue(List.of("test", "climb", "jump").contains(presentVerb.getVerb()), "Present verb should be from test list");
		assertTrue(List.of("will test", "will climb", "will jump").contains(futureVerb.getVerb()), "Future verb should be from test list");

		assertEquals(Tense.PAST, pastVerb.getTense(), "Verb tense should be PAST");
		assertEquals(Tense.PRESENT, presentVerb.getTense(), "Verb tense should be PRESENT");
		assertEquals(Tense.FUTURE, futureVerb.getTense(), "Verb tense should be FUTURE");
	}

	@Test
	@DisplayName("Test JSON update observer functionality")
	void testJsonUpdateObserver() throws Exception
	{
		Field verbsField = RandomVerbGenerator.class.getDeclaredField("verbs");
		verbsField.setAccessible(true);

		@SuppressWarnings("unchecked")
		var initialVerbsMap = (Map<Pair<Tense, Number>, List<Verb>>) verbsField.get(generator);

		int initialPastSingularCount = initialVerbsMap.get(new Pair<>(Tense.PAST, Number.SINGULAR)).size();
		int initialPastPluralCount = initialVerbsMap.get(new Pair<>(Tense.PAST, Number.PLURAL)).size();
		int initialPresentSingularCount = initialVerbsMap.get(new Pair<>(Tense.PRESENT, Number.SINGULAR)).size();
		int initialPresentPluralCount = initialVerbsMap.get(new Pair<>(Tense.PRESENT, Number.PLURAL)).size();
		int initialFutureSingularCount = initialVerbsMap.get(new Pair<>(Tense.FUTURE, Number.SINGULAR)).size();
		int initialFuturePluralCount = initialVerbsMap.get(new Pair<>(Tense.FUTURE, Number.PLURAL)).size();

		JsonObject updatedJson = new JsonObject();

		JsonArray updatedPastSingularVerbs = new JsonArray();
		updatedPastSingularVerbs.add("tested");
		updatedPastSingularVerbs.add("climbed");
		updatedPastSingularVerbs.add("jumped");
		updatedPastSingularVerbs.add("walked");
		updatedPastSingularVerbs.add("talked");

		JsonArray updatedPastPluralVerbs = new JsonArray();
		updatedPastPluralVerbs.add("tested");
		updatedPastPluralVerbs.add("climbed");
		updatedPastPluralVerbs.add("jumped");

		JsonArray updatedPresentSingularVerbs = new JsonArray();
		updatedPresentSingularVerbs.add("tests");
		updatedPresentSingularVerbs.add("climbs");
		updatedPresentSingularVerbs.add("jumps");
		updatedPresentSingularVerbs.add("walks");

		JsonArray updatedPresentPluralVerbs = new JsonArray();
		updatedPresentPluralVerbs.add("test");
		updatedPresentPluralVerbs.add("climb");

		JsonArray updatedFutureSingularVerbs = new JsonArray();
		updatedFutureSingularVerbs.add("will test");
		updatedFutureSingularVerbs.add("will climb");
		updatedFutureSingularVerbs.add("will jump");

		JsonArray updatedFuturePluralVerbs = new JsonArray();
		updatedFuturePluralVerbs.add("will test");
		updatedFuturePluralVerbs.add("will climb");
		updatedFuturePluralVerbs.add("will jump");
		updatedFuturePluralVerbs.add("will walk");

		updatedJson.add("pastSingularVerbs", updatedPastSingularVerbs);
		updatedJson.add("pastPluralVerbs", updatedPastPluralVerbs);
		updatedJson.add("presentSingularVerbs", updatedPresentSingularVerbs);
		updatedJson.add("presentPluralVerbs", updatedPresentPluralVerbs);
		updatedJson.add("futureSingularVerbs", updatedFutureSingularVerbs);
		updatedJson.add("futurePluralVerbs", updatedFuturePluralVerbs);

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(updatedJson.toString());
		}

		generator.onJsonUpdate();

		@SuppressWarnings("unchecked")
		var updatedVerbsMap = (Map<Pair<Tense, Number>, List<Verb>>) verbsField.get(generator);

		assertEquals(5, updatedVerbsMap.get(new Pair<>(Tense.PAST, Number.SINGULAR)).size(),
			"Number of past singular verbs should be updated to 5");
		assertEquals(3, updatedVerbsMap.get(new Pair<>(Tense.PAST, Number.PLURAL)).size(),
			"Number of past plural verbs should remain 3");
		assertEquals(4, updatedVerbsMap.get(new Pair<>(Tense.PRESENT, Number.SINGULAR)).size(),
			"Number of present singular verbs should be updated to 4");
		assertEquals(2, updatedVerbsMap.get(new Pair<>(Tense.PRESENT, Number.PLURAL)).size(),
			"Number of present plural verbs should be reduced to 2");
		assertEquals(3, updatedVerbsMap.get(new Pair<>(Tense.FUTURE, Number.SINGULAR)).size(),
			"Number of future singular verbs should remain 3");
		assertEquals(4, updatedVerbsMap.get(new Pair<>(Tense.FUTURE, Number.PLURAL)).size(),
			"Number of future plural verbs should be increased to 4");

		assertTrue(updatedVerbsMap.get(new Pair<>(Tense.PAST, Number.SINGULAR)).size() > initialPastSingularCount,
			"Past singular verb count should increase");
		assertEquals(initialPastPluralCount, updatedVerbsMap.get(new Pair<>(Tense.PAST, Number.PLURAL)).size(),
			"Past plural verb count should remain the same");
		assertTrue(updatedVerbsMap.get(new Pair<>(Tense.PRESENT, Number.SINGULAR)).size() > initialPresentSingularCount,
			"Present singular verb count should increase");
		assertTrue(updatedVerbsMap.get(new Pair<>(Tense.PRESENT, Number.PLURAL)).size() < initialPresentPluralCount,
			"Present plural verb count should decrease");
		assertEquals(initialFutureSingularCount, updatedVerbsMap.get(new Pair<>(Tense.FUTURE, Number.SINGULAR)).size(),
			"Future singular verb count should remain the same");
		assertTrue(updatedVerbsMap.get(new Pair<>(Tense.FUTURE, Number.PLURAL)).size() > initialFuturePluralCount,
			"Future plural verb count should increase");
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
		JsonArray pastSingularVerbs = new JsonArray();
		pastSingularVerbs.add("tested");

		partialJson.add("pastSingularVerbs", pastSingularVerbs);
		partialJson.add("pastPluralVerbs", new JsonArray());
		partialJson.add("presentSingularVerbs", new JsonArray());
		partialJson.add("presentPluralVerbs", new JsonArray());
		partialJson.add("futureSingularVerbs", new JsonArray());
		partialJson.add("futurePluralVerbs", new JsonArray());

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(partialJson.toString());
		}

		RandomVerbGenerator partialGenerator = new RandomVerbGenerator(testFilePath);

		Verb pastVerb = partialGenerator.getRandomVerb(Number.SINGULAR, Tense.PAST);
		assertNotNull(pastVerb, "Should return a past verb");

		assertThrows(InvalidListException.class, () -> partialGenerator.getRandomVerb(Tense.PRESENT),
			"Should throw InvalidListException when present verbs list is empty");
		assertThrows(InvalidListException.class, () -> partialGenerator.getRandomVerb(Tense.FUTURE),
			"Should throw InvalidListException when future verbs list is empty");

		partialGenerator.cleanup();
	}

	@Test
	@DisplayName("Test logging behavior")
	void testLoggingBehavior() throws Exception
	{
		LoggerManager mockLogger = mock(LoggerManager.class);

		Field loggerField = RandomVerbGenerator.class.getDeclaredField("logger");
		loggerField.setAccessible(true);
		LoggerManager originalLogger = (LoggerManager) loggerField.get(generator);
		loggerField.set(generator, mockLogger);

		try
		{
			generator.getRandomVerb();
			generator.getRandomVerb(Tense.PAST);
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
	@DisplayName("Test concurrent access to verb generation")
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
						Verb verb = generator.getRandomVerb();
						assertNotNull(verb, "Verb should not be null");
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
	@DisplayName("Test with maximum verb length")
	void testMaxVerbLength() throws Exception
	{
		String longVerb = "a".repeat(10000);

		JsonObject testVerbs = new JsonObject();
		JsonArray pastSingularVerbs = new JsonArray();

		pastSingularVerbs.add(longVerb);
		testVerbs.add("pastSingularVerbs", pastSingularVerbs);
		testVerbs.add("pastPluralVerbs", new JsonArray());
		testVerbs.add("presentSingularVerbs", new JsonArray());
		testVerbs.add("presentPluralVerbs", new JsonArray());
		testVerbs.add("futureSingularVerbs", new JsonArray());
		testVerbs.add("futurePluralVerbs", new JsonArray());

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(testVerbs.toString());
		}

		RandomVerbGenerator longVerbGenerator = new RandomVerbGenerator(testFilePath);
		Verb verb = longVerbGenerator.getRandomVerb(Number.SINGULAR, Tense.PAST);

		assertEquals(longVerb, verb.getVerb());
		longVerbGenerator.cleanup();
	}

	@Test
	@DisplayName("Test initialization with non-existent file path")
	void testInitialization_NonExistentFile()
	{
		String nonExistentPath = "non/existent/path/verbs.json";
		assertThrows(IOException.class, () -> new RandomVerbGenerator(nonExistentPath),
			"Should throw IOException when file path does not exist");
	}

	@Test
	@DisplayName("Test thread safety during initialization")
	void testThreadSafeInitialization() throws Exception
	{
		int threadCount = 10;
		CountDownLatch latch = new CountDownLatch(threadCount);
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		List<RandomVerbGenerator> generators = new ArrayList<>();

		for (int i = 0; i < threadCount; i++)
		{
			executor.submit(() ->
			{
				try
				{
					RandomVerbGenerator localGenerator = new RandomVerbGenerator(testFilePath);
					synchronized (generators)
					{
						generators.add(localGenerator);
					}
				}
				catch(IOException e)
				{
					fail("Initialization should not fail");
				}
				finally
				{
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();

		assertEquals(threadCount, generators.size(), "All generators should be created successfully");
		for(RandomVerbGenerator gen : generators)
		{
			assertNotNull(gen.getRandomVerb(), "Each generator should work correctly");
			gen.cleanup();
		}
	}

	@Test
	@DisplayName("Test constructor with null file path")
	void testConstructor_NullFilePath()
	{
		assertThrows(unipd.nonsense.exceptions.NullFilePathException.class, () -> new RandomVerbGenerator(null),
			"Should throw NullPointerException for null file path");
	}

	@Test
	@DisplayName("Test repeated JSON updates")
	void testRepeatedJsonUpdates() throws Exception
	{
		int updateCount = 10;
		for(int i = 0; i < updateCount; i++)
		{
			JsonObject updatedJson = createDefaultTestVerbs();

			try(FileWriter writer = new FileWriter(testFile))
			{
				writer.write(updatedJson.toString());
			}
			generator.onJsonUpdate();
		}

		assertNotNull(generator.getRandomVerb(), "Generator should work after multiple JSON updates");
	}

	@Test
	@DisplayName("Test verb distribution across tenses and numbers")
	void testVerbTenseNumberDistribution()
	{
		int sampleSize = 1000;
		Map<Tense, Integer> tenseCounts = new HashMap<>();

		Map<Number, Integer> numberCounts = new HashMap<>();
		Map<Pair<Tense, Number>, Integer> combinationCounts = new HashMap<>();

		for(int i = 0; i < sampleSize; i++)
		{
			Verb verb = generator.getRandomVerb();

			tenseCounts.merge(verb.getTense(), 1, Integer::sum);

			numberCounts.merge(verb.getNumber(), 1, Integer::sum);

			Pair<Tense, Number> combination = new Pair<>(verb.getTense(), verb.getNumber());
			combinationCounts.merge(combination, 1, Integer::sum);
		}

		for(Tense tense : Tense.values())
		{
			assertTrue(tenseCounts.getOrDefault(tense, 0) > 0,
				"Tense " + tense + " should appear in random distribution");
		}

		for(Number number : Number.values())
		{
			assertTrue(numberCounts.getOrDefault(number, 0) > 0,
				"Number " + number + " should appear in random distribution");
		}

		double expectedCombinationCount = sampleSize / (Tense.values().length * Number.values().length);
		double tolerance = expectedCombinationCount * 0.3;

		combinationCounts.forEach((combination, count) ->
		{
			assertTrue(Math.abs(count - expectedCombinationCount) < tolerance, "Combination " + combination + " appears " + count + " times (expected ~" + expectedCombinationCount + ")");
		});
	}
}
