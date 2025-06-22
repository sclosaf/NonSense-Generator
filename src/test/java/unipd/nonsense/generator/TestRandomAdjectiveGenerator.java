package unipd.nonsense.generator;

import unipd.nonsense.model.Adjective;
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.Random;
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

@DisplayName("Testing RandomAdjectiveGenerator")
@ExtendWith(MockitoExtension.class)
class TestRandomAdjectiveGenerator
{
	private RandomAdjectiveGenerator generator;
	private File testFile;
	private String testFilePath;

	@Mock
	private JsonFileHandler mockJsonHandler;

	@Mock
	private Random mockRandom;

	@BeforeEach
	@DisplayName("Setup environment: Use testAdjectives.json from target/resources")
	void setUp() throws Exception
	{
		String resourcePath = "target" + File.separator + "resources" + File.separator + "testAdjectives.json";
		testFile = new File(resourcePath);
		testFilePath = testFile.getAbsolutePath();

		testFile.getParentFile().mkdirs();

		if(!testFile.exists())
		{
			JsonObject testAdjectives = createDefaultTestAdjectives();
			try(FileWriter writer = new FileWriter(testFile))
			{
				writer.write(testAdjectives.toString());
				writer.flush();
			}
		}

		generator = new RandomAdjectiveGenerator(testFilePath);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if(generator != null)
			generator.cleanup();

		try (InputStream in = getClass().getResourceAsStream("/testAdjectives.json"); FileWriter out = new FileWriter(testFile))
		{
			String content = new String(in.readAllBytes());
			out.write(content);
		}
	}

	private JsonObject createDefaultTestAdjectives()
	{
		JsonObject json = new JsonObject();
		JsonArray adjectives = new JsonArray();

		adjectives.add("happy");
		adjectives.add("sad");
		adjectives.add("angry");
		adjectives.add("funny");
		adjectives.add("serious");

		json.add("adjectives", adjectives);

		return json;
	}

	@Test
	@DisplayName("Test success of getRandomAdjective")
	void testGetRandomAdjective_Success()
	{
		Adjective adjective = generator.getRandomAdjective();
		assertNotNull(adjective, "Should return a valid adjective");
		assertNotNull(adjective.getAdjective(), "Adjective should have a value");
	}

	@Test
	@DisplayName("Test attempt to get adjective from empty list")
	void testGetRandomAdjective_EmptyList() throws Exception
	{
		JsonObject emptyJson = new JsonObject();
		emptyJson.add("adjectives", new JsonArray());

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(emptyJson.toString());
		}

		generator = new RandomAdjectiveGenerator(testFilePath);

		assertThrows(InvalidListException.class, () -> generator.getRandomAdjective(),
			"Should throw InvalidListException when no adjectives available");
	}

	@Test
	@DisplayName("Test initialization with invalid JSON file")
	void testInitialization_InvalidJson() throws Exception
	{
		try (FileWriter writer = new FileWriter(testFile))
		{
			writer.write("invalid json content");
		}

		assertThrows(InvalidJsonStateException.class, () -> new RandomAdjectiveGenerator(testFilePath),
			"Should throw InvalidJsonStateException when JSON is invalid");
	}

	@Test
	@DisplayName("Test adjectives count matches JSON file content")
	void testAdjectivesCount() throws Exception
	{
		Field adjectivesField = RandomAdjectiveGenerator.class.getDeclaredField("adjectives");
		adjectivesField.setAccessible(true);

		@SuppressWarnings("unchecked")
		var adjectivesList = (List<Adjective>) adjectivesField.get(generator);

		JsonObject testAdjectives = JsonParser.parseString(Files.readString(testFile.toPath())).getAsJsonObject();
		int expectedCount = testAdjectives.getAsJsonArray("adjectives").size();

		assertEquals(expectedCount, adjectivesList.size(),
			"Number of adjectives should match the JSON file");
	}

	@Test
	@DisplayName("Test the adjective selection is random")
	void testAdjectiveSelectionIsRandom() throws Exception
	{
		List<String> adjectives = new ArrayList<>();
		for (int i = 0; i < 30; i++)
		{
			Adjective adjective = generator.getRandomAdjective();
			adjectives.add(adjective.getAdjective());
		}

		long distinctAdjectives = adjectives.stream().distinct().count();
		assertTrue(distinctAdjectives > 1, "Adjective selection should be random and return different adjectives");
	}

	@Test
	@DisplayName("Test adjectives properties are preserved")
	void testAdjectivePropertiesPreserved()
	{
		Adjective adjective = generator.getRandomAdjective();

		assertTrue(List.of("happy", "sad", "angry", "funny", "serious").contains(adjective.getAdjective()),
			"Adjective should be from test list");
	}

	@Test
	@DisplayName("Test JSON update observer functionality")
	void testJsonUpdateObserver() throws Exception
	{
		Field adjectivesField = RandomAdjectiveGenerator.class.getDeclaredField("adjectives");
		adjectivesField.setAccessible(true);

		@SuppressWarnings("unchecked")
		var initialAdjectives = (List<Adjective>) adjectivesField.get(generator);
		int initialCount = initialAdjectives.size();

		JsonObject updatedJson = new JsonObject();
		JsonArray updatedAdjectives = new JsonArray();

		updatedAdjectives.add("happy");
		updatedAdjectives.add("sad");
		updatedAdjectives.add("angry");
		updatedAdjectives.add("funny");
		updatedAdjectives.add("serious");
		updatedAdjectives.add("new");
		updatedAdjectives.add("another");

		updatedJson.add("adjectives", updatedAdjectives);

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(updatedJson.toString());
		}

		generator.onJsonUpdate();

		@SuppressWarnings("unchecked")
		var updatedAdjectivesList = (List<Adjective>) adjectivesField.get(generator);
		int updatedCount = updatedAdjectivesList.size();

		assertEquals(7, updatedCount, "Number of adjectives should be updated to 7");
		assertTrue(updatedCount > initialCount, "Adjectives count should increase after update");
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
		partialJson.add("adjectives", new JsonArray());

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(partialJson.toString());
		}

		RandomAdjectiveGenerator partialGenerator = new RandomAdjectiveGenerator(testFilePath);

		assertThrows(InvalidListException.class, () -> partialGenerator.getRandomAdjective(),
			"Should throw InvalidListException when adjectives list is empty");

		partialGenerator.cleanup();
	}

	@Test
	@DisplayName("Test logging behavior")
	void testLoggingBehavior() throws Exception
	{
		LoggerManager mockLogger = mock(LoggerManager.class);

		Field loggerField = RandomAdjectiveGenerator.class.getDeclaredField("logger");
		loggerField.setAccessible(true);
		LoggerManager originalLogger = (LoggerManager) loggerField.get(generator);
		loggerField.set(generator, mockLogger);

		try
		{
			generator.getRandomAdjective();
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
	@DisplayName("Test random distribution of adjectives")
	void testRandomDistribution()
	{
		Map<String, Integer> countMap = new HashMap<>();
		int iterations = 1000;

		for(int i = 0; i < iterations; i++)
		{
			Adjective a = generator.getRandomAdjective();
			countMap.merge(a.getAdjective(), 1, Integer::sum);
		}

		double expected = iterations / (double) countMap.size();
		for(int count : countMap.values())
			assertTrue(Math.abs(count - expected) < expected * 0.3, "Adjective distribution should be roughly uniform");
	}

	@Test
	@DisplayName("Test concurrent access to adjective generation")
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
						Adjective adjective = generator.getRandomAdjective();
						assertNotNull(adjective, "Adjective should not be null");
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
	@DisplayName("Test with maximum adjective length")
	void testMaxAdjectiveLength() throws Exception
	{
		String longAdjective = "a".repeat(10000);

		JsonObject testAdjectives = new JsonObject();
		JsonArray adjectives = new JsonArray();

		adjectives.add(longAdjective);
		testAdjectives.add("adjectives", adjectives);

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(testAdjectives.toString());
		}

		RandomAdjectiveGenerator longAdjectiveGenerator = new RandomAdjectiveGenerator(testFilePath);
		Adjective adjective = longAdjectiveGenerator.getRandomAdjective();

		assertEquals(longAdjective, adjective.getAdjective());
		longAdjectiveGenerator.cleanup();
	}

	@Test
	@DisplayName("Test initialization with null file path")
	void testInitialization_NullFilePath()
	{
		assertThrows(IllegalArgumentException.class, () -> new RandomAdjectiveGenerator(null), "Should throw IllegalArgumentException when file path is null");
	}

	@Test
	@DisplayName("Test initialization with non-existent file path")
	void testInitialization_NonExistentFilePath()
	{
		String nonExistentPath = "nonexistent" + File.separator + "path.json";

		assertThrows(IOException.class, () -> new RandomAdjectiveGenerator(nonExistentPath),
			"Should throw IOException when file does not exist");
	}

	@Test
	@DisplayName("Test initialization with empty file path")
	void testInitialization_EmptyFilePath()
	{
		assertThrows(IllegalArgumentException.class, () -> new RandomAdjectiveGenerator(""), "Should throw IllegalArgumentException when file path is empty");
	}

	@Test
	@DisplayName("Test behavior when JSON file is deleted after initialization")
	void testJsonFileDeletedAfterInitialization() throws Exception
	{
		Files.deleteIfExists(Path.of(testFilePath));

		assertThrows(IOException.class, () -> generator.onJsonUpdate(), "Should throw IOException when JSON file is deleted and update is attempted");
	}

	@Test
	@DisplayName("Test behavior when JSON file is corrupted after initialization")
	void testJsonFileCorruptedAfterInitialization() throws Exception
	{
		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write("corrupted json content");
		}

		assertThrows(InvalidJsonStateException.class, () -> generator.onJsonUpdate(),
			"Should throw IOException when JSON file is corrupted and update is attempted");
	}

	@Test
	@DisplayName("Test behavior with duplicate adjectives in JSON")
	void testDuplicateAdjectives() throws Exception
	{
		JsonObject duplicateJson = new JsonObject();
		JsonArray duplicateAdjectives = new JsonArray();

		duplicateAdjectives.add("duplicate");
		duplicateAdjectives.add("duplicate");
		duplicateAdjectives.add("duplicate");

		duplicateJson.add("adjectives", duplicateAdjectives);

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(duplicateJson.toString());
		}

		RandomAdjectiveGenerator duplicateGenerator = new RandomAdjectiveGenerator(testFilePath);

		for(int i = 0; i < 10; i++)
		{
			Adjective adj = duplicateGenerator.getRandomAdjective();
			assertEquals("duplicate", adj.getAdjective());
		}

		duplicateGenerator.cleanup();
	}

	@Test
	@DisplayName("Test thread safety of adjective list during updates")
	void testThreadSafetyDuringUpdates() throws Exception
	{
		int threadCount = 50;
		CountDownLatch latch = new CountDownLatch(threadCount);
		ExecutorService executor = Executors.newFixedThreadPool(10);
		Random random = new Random();

		Runnable updater = () ->
		{
			try
			{
				JsonObject updatedJson = new JsonObject();
				JsonArray updatedAdjectives = new JsonArray();
				updatedAdjectives.add("new_adjective_" + System.currentTimeMillis());
				updatedJson.add("adjectives", updatedAdjectives);

				try(FileWriter writer = new FileWriter(testFile))
				{
						writer.write(updatedJson.toString());
				}

				generator.onJsonUpdate();
			}
			catch(Exception e)
			{
				fail("Unexpected exception during update: " + e.getMessage());
			}
		};

		for(int i = 0; i < threadCount; i++)
		{
			executor.submit(() ->
			{
				try
				{
					if(random.nextBoolean())
					{
						Adjective adj = generator.getRandomAdjective();
						assertNotNull(adj);
					}
					else
						updater.run();
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
	@DisplayName("Test behavior with extremely large JSON file")
	void testExtremelyLargeJsonFile() throws Exception
	{
		JsonObject largeJson = new JsonObject();
		JsonArray largeAdjectives = new JsonArray();

		for(int i = 0; i < 10000; i++)
			largeAdjectives.add("adjective_" + i);

		largeJson.add("adjectives", largeAdjectives);

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(largeJson.toString());
		}

		RandomAdjectiveGenerator largeGenerator = new RandomAdjectiveGenerator(testFilePath);

		Field adjectivesField = RandomAdjectiveGenerator.class.getDeclaredField("adjectives");
		adjectivesField.setAccessible(true);

		@SuppressWarnings("unchecked")
		List<Adjective> adjectivesList = (List<Adjective>) adjectivesField.get(largeGenerator);

		assertEquals(10000, adjectivesList.size(), "Should handle large JSON files correctly");
		largeGenerator.cleanup();
	}

	@Test
	@DisplayName("Test memory usage with repeated updates")
	void testMemoryUsageWithRepeatedUpdates() throws Exception
	{
		for(int i = 0; i < 1000; i++)
		{
			JsonObject updatedJson = new JsonObject();
			JsonArray updatedAdjectives = new JsonArray();
			updatedAdjectives.add("adjective_" + i);
			updatedJson.add("adjectives", updatedAdjectives);

			try(FileWriter writer = new FileWriter(testFile))
			{
				writer.write(updatedJson.toString());
			}

			generator.onJsonUpdate();

			Field adjectivesField = RandomAdjectiveGenerator.class.getDeclaredField("adjectives");
			adjectivesField.setAccessible(true);

			@SuppressWarnings("unchecked")
			List<Adjective> adjectivesList = (List<Adjective>) adjectivesField.get(generator);

			assertEquals(1, adjectivesList.size(), "Should have exactly one adjective after update");
		}
	}
}
