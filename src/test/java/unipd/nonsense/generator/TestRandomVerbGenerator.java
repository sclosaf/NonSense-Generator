package unipd.nonsense.generator;

import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Verb.Tense;
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
		JsonArray pastVerbs = new JsonArray();
		JsonArray presentVerbs = new JsonArray();
		JsonArray futureVerbs = new JsonArray();

		pastVerbs.add("tested");
		pastVerbs.add("climbed");
		pastVerbs.add("jumped");

		presentVerbs.add("test");
		presentVerbs.add("climb");
		presentVerbs.add("jump");

		futureVerbs.add("will test");
		futureVerbs.add("will climb");
		futureVerbs.add("will jump");

		json.add("pastVerbs", pastVerbs);
		json.add("presentVerbs", presentVerbs);
		json.add("futureVerbs", futureVerbs);

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
	@DisplayName("Test success of getRandomVerb with PAST tense")
	void testGetRandomVerb_PastSuccess()
	{
		Verb verb = generator.getRandomVerb(Tense.PAST);
		assertEquals(Tense.PAST, verb.getTense(), "Verb tense should be PAST");
		assertTrue(List.of("tested", "climbed", "jumped").contains(verb.getVerb()), "Verb should be from test list");
	}

	@Test
	@DisplayName("Test success of getRandomVerb with PRESENT tense")
	void testGetRandomVerb_PresentSuccess()
	{
		Verb verb = generator.getRandomVerb(Tense.PRESENT);
		assertEquals(Tense.PRESENT, verb.getTense(), "Verb tense should be PRESENT");
		assertTrue(List.of("test", "climb", "jump").contains(verb.getVerb()), "Verb should be from test list");
	}

	@Test
	@DisplayName("Test success of getRandomVerb with FUTURE tense")
	void testGetRandomVerb_FutureSuccess()
	{
		Verb verb = generator.getRandomVerb(Tense.FUTURE);
		assertEquals(Tense.FUTURE, verb.getTense(), "Verb tense should be FUTURE");
		assertTrue(List.of("will test", "will climb", "will jump").contains(verb.getVerb()), "Verb should be from test list");
	}

	@Test
	@DisplayName("Test attempt to get verb from empty list")
	void testGetRandomVerb_EmptyList() throws Exception
	{
		JsonObject emptyJson = new JsonObject();
		emptyJson.add("pastVerbs", new JsonArray());
		emptyJson.add("presentVerbs", new JsonArray());
		emptyJson.add("futureVerbs", new JsonArray());

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
		int expectedPastCount = testVerbs.getAsJsonArray("pastVerbs").size();
		int expectedPresentCount = testVerbs.getAsJsonArray("presentVerbs").size();
		int expectedFutureCount = testVerbs.getAsJsonArray("futureVerbs").size();

		assertEquals(expectedPastCount, verbsMap.get(Tense.PAST).size(),
			"Number of past verbs should match the JSON file");
		assertEquals(expectedPresentCount, verbsMap.get(Tense.PRESENT).size(),
			"Number of present verbs should match the JSON file");
		assertEquals(expectedFutureCount, verbsMap.get(Tense.FUTURE).size(),
			"Number of future verbs should match the JSON file");
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
		Verb pastVerb = generator.getRandomVerb(Tense.PAST);
		Verb presentVerb = generator.getRandomVerb(Tense.PRESENT);
		Verb futureVerb = generator.getRandomVerb(Tense.FUTURE);

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
		var initialVerbsMap = (Map<Tense, List<Verb>>) verbsField.get(generator);
		int initialPastCount = initialVerbsMap.get(Tense.PAST).size();

		JsonObject updatedJson = new JsonObject();
		JsonArray updatedPastVerbs = new JsonArray();
		JsonArray updatedPresentVerbs = new JsonArray();
		JsonArray updatedFutureVerbs = new JsonArray();

		updatedPastVerbs.add("tested");
		updatedPastVerbs.add("climbed");
		updatedPastVerbs.add("jumped");
		updatedPastVerbs.add("walked");
		updatedPastVerbs.add("talked");

		updatedPresentVerbs.add("test");
		updatedPresentVerbs.add("climb");
		updatedPresentVerbs.add("jump");

		updatedFutureVerbs.add("will test");
		updatedFutureVerbs.add("will climb");
		updatedFutureVerbs.add("will jump");

		updatedJson.add("pastVerbs", updatedPastVerbs);
		updatedJson.add("presentVerbs", updatedPresentVerbs);
		updatedJson.add("futureVerbs", updatedFutureVerbs);

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(updatedJson.toString());
		}

		generator.onJsonUpdate();

		@SuppressWarnings("unchecked")
		var updatedVerbsMap = (Map<Tense, List<Verb>>) verbsField.get(generator);
		int updatedPastCount = updatedVerbsMap.get(Tense.PAST).size();

		assertEquals(5, updatedPastCount, "Number of past verbs should be updated to 5");
		assertTrue(updatedPastCount > initialPastCount, "Verb count should increase after update");
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
		JsonArray pastVerbs = new JsonArray();
		pastVerbs.add("tested");
		partialJson.add("pastVerbs", pastVerbs);
		partialJson.add("presentVerbs", new JsonArray());
		partialJson.add("futureVerbs", new JsonArray());

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(partialJson.toString());
		}

		RandomVerbGenerator partialGenerator = new RandomVerbGenerator(testFilePath);

		Verb pastVerb = partialGenerator.getRandomVerb(Tense.PAST);
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
	@DisplayName("Test random distribution of verbs")
	void testRandomDistribution()
	{
		Map<String, Integer> countMap = new HashMap<>();
		int iterations = 1000;

		for(int i = 0; i < iterations; i++)
		{
			Verb v = generator.getRandomVerb();
			countMap.merge(v.getVerb(), 1, Integer::sum);
		}

		double expected = iterations / (double) countMap.size();
		for(int count : countMap.values())
			assertTrue(Math.abs(count - expected) < expected * 0.3, "Verb distribution should be roughly uniform");
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
		JsonArray pastVerbs = new JsonArray();

		pastVerbs.add(longVerb);
		testVerbs.add("pastVerbs", pastVerbs);
		testVerbs.add("presentVerbs", new JsonArray());
		testVerbs.add("futureVerbs", new JsonArray());

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(testVerbs.toString());
		}

		RandomVerbGenerator longVerbGenerator = new RandomVerbGenerator(testFilePath);
		Verb verb = longVerbGenerator.getRandomVerb(Tense.PAST);

		assertEquals(longVerb, verb.getVerb());
		longVerbGenerator.cleanup();
	}
}
