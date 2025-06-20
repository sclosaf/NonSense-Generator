package unipd.nonsense.generator;

import unipd.nonsense.model.Template;
import unipd.nonsense.model.Number;
import unipd.nonsense.exceptions.InvalidListException;
import unipd.nonsense.exceptions.InvalidJsonStateException;
import unipd.nonsense.exceptions.InvalidTemplateException;
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

@DisplayName("Testing RandomTemplateGenerator")
@ExtendWith(MockitoExtension.class)
class TestRandomTemplateGenerator
{
	private RandomTemplateGenerator generator;
	private File testFile;
	private String testFilePath;

	@Mock
	private JsonFileHandler mockJsonHandler;

	@Mock
	private Random mockRandom;

	@BeforeEach
	@DisplayName("Setup environment: Use testTemplates.json from target/resources")
	void setUp() throws Exception
	{
		String resourcePath = "target" + File.separator + "resources" + File.separator + "testTemplates.json";
		testFile = new File(resourcePath);
		testFilePath = testFile.getAbsolutePath();

		testFile.getParentFile().mkdirs();

		if(!testFile.exists())
		{
			JsonObject testTemplates = createDefaultTestTemplates();
			try(FileWriter writer = new FileWriter(testFile))
			{
				writer.write(testTemplates.toString());
				writer.flush();
			}
		}

		generator = new RandomTemplateGenerator(testFilePath);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		if(generator != null)
			generator.cleanup();

		try (InputStream in = getClass().getResourceAsStream("/testTemplates.json"); FileWriter out = new FileWriter(testFile))
		{

			String content = new String(in.readAllBytes());
			out.write(content);

		}
	}

	private JsonObject createDefaultTestTemplates()
	{
		JsonObject json = new JsonObject();
		JsonArray singularTemplates = new JsonArray();
		JsonArray pluralTemplates = new JsonArray();

		singularTemplates.add("Test singular template [noun]");
		singularTemplates.add("This is a [noun] template");
		singularTemplates.add("A [noun] for testing");

		pluralTemplates.add("Test plural templates [noun]");
		pluralTemplates.add("These are [noun] templates");
		pluralTemplates.add("Many [noun] for testing");

		json.add("singularTemplates", singularTemplates);
		json.add("pluralTemplates", pluralTemplates);

		return json;
	}

	@Test
	@DisplayName("Test success of getRandomTemplate")
	void testGetRandomTemplate_Success()
	{
		Template template = generator.getRandomTemplate();
		assertNotNull(template, "Should return a valid template");
		assertNotNull(template.getPattern(), "Template should have a pattern");
		assertNotNull(template.getNumber(), "Template should have a number");
	}

	@Test
	@DisplayName("Test success of getRandomTemplate with SINGULAR type")
	void testGetRandomTemplate_SingularSuccess()
	{
		Template template = generator.getRandomTemplate(Number.SINGULAR);
		assertEquals(Number.SINGULAR, template.getNumber(), "Template type should be SINGULAR");
		assertTrue(template.getPattern().contains("[noun]"), "Template should contain a noun placeholder");
	}

	@Test
	@DisplayName("Test success of getRandomTemplate with PLURAL type")
	void testGetRandomTemplate_PluralSuccess()
	{
		Template template = generator.getRandomTemplate(Number.PLURAL);
		assertEquals(Number.PLURAL, template.getNumber(), "Template type should be PLURAL");
		assertTrue(template.getPattern().contains("[noun]"), "Template should contain a noun placeholder");
	}

	@Test
	@DisplayName("Test attempt to get template from empty list")
	void testGetRandomTemplate_EmptyList() throws Exception
	{
		JsonObject emptyJson = new JsonObject();
		emptyJson.add("singularTemplates", new JsonArray());
		emptyJson.add("pluralTemplates", new JsonArray());

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(emptyJson.toString());
		}

		generator = new RandomTemplateGenerator(testFilePath);

		assertThrows(InvalidListException.class, () -> generator.getRandomTemplate(),
			"Should throw InvalidListException when no templates available");
		assertThrows(InvalidListException.class, () -> generator.getRandomTemplate(Number.SINGULAR),
			"Should throw InvalidListException when no singular templates available");
		assertThrows(InvalidListException.class, () -> generator.getRandomTemplate(Number.PLURAL),
			"Should throw InvalidListException when no plural templates available");
	}

	@Test
	@DisplayName("Test initialization with invalid JSON file")
	void testInitialization_InvalidJson() throws Exception
	{
		try (FileWriter writer = new FileWriter(testFile))
		{
			writer.write("invalid json content");
		}

		assertThrows(InvalidJsonStateException.class, () -> new RandomTemplateGenerator(testFilePath),
			"Should throw InvalidJsonStateException when JSON is invalid");
	}

	@Test
	@DisplayName("Test templates count matches JSON file content")
	void testTemplatesCount() throws Exception
	{
		Field templatesField = RandomTemplateGenerator.class.getDeclaredField("templates");
		templatesField.setAccessible(true);

		@SuppressWarnings("unchecked")
		var templatesMap = (Map<Number, List<Template>>) templatesField.get(generator);

		JsonObject testTemplates = JsonParser.parseString(Files.readString(testFile.toPath())).getAsJsonObject();
		int expectedSingularCount = testTemplates.getAsJsonArray("singularTemplates").size();
		int expectedPluralCount = testTemplates.getAsJsonArray("pluralTemplates").size();

		assertEquals(expectedSingularCount, templatesMap.get(Number.SINGULAR).size(),
			"Number of singular templates should match the JSON file");
		assertEquals(expectedPluralCount, templatesMap.get(Number.PLURAL).size(),
			"Number of plural templates should match the JSON file");
	}

	@Test
	@DisplayName("Test the template selection is random")
	void testTemplateSelectionIsRandom() throws Exception
	{
		List<String> templates = new ArrayList<>();
		for (int i = 0; i < 30; i++)
		{
			Template template = generator.getRandomTemplate();
			templates.add(template.getPattern());
		}

		long distinctTemplates = templates.stream().distinct().count();
		assertTrue(distinctTemplates > 1, "Template selection should be random and return different templates");
	}

	@Test
	@DisplayName("Test templates properties are preserved")
	void testTemplatePropertiesPreserved()
	{
		Template singularTemplate = generator.getRandomTemplate(Number.SINGULAR);
		Template pluralTemplate = generator.getRandomTemplate(Number.PLURAL);

		assertTrue(singularTemplate.getPattern().contains("[noun]"), "Singular template should contain [noun] placeholder");
		assertTrue(pluralTemplate.getPattern().contains("[noun]"), "Plural template should contain [noun] placeholder");

		assertEquals(Number.SINGULAR, singularTemplate.getNumber(), "Template type should be SINGULAR");
		assertEquals(Number.PLURAL, pluralTemplate.getNumber(), "Template type should be PLURAL");
	}

	@Test
	@DisplayName("Test JSON update observer functionality")
	void testJsonUpdateObserver() throws Exception
	{
		Field templatesField = RandomTemplateGenerator.class.getDeclaredField("templates");
		templatesField.setAccessible(true);

		@SuppressWarnings("unchecked")
		var initialTemplatesMap = (Map<Number, List<Template>>) templatesField.get(generator);
		int initialSingularCount = initialTemplatesMap.get(Number.SINGULAR).size();

		JsonObject updatedJson = new JsonObject();
		JsonArray updatedSingularTemplates = new JsonArray();
		JsonArray updatedPluralTemplates = new JsonArray();

		updatedSingularTemplates.add("Test singular template [noun]");
		updatedSingularTemplates.add("This is a [noun] template");
		updatedSingularTemplates.add("A [noun] for testing");
		updatedSingularTemplates.add("New singular [noun] template");
		updatedSingularTemplates.add("Another new [noun] template");

		updatedPluralTemplates.add("Test plural templates [noun]");
		updatedPluralTemplates.add("These are [noun] templates");
		updatedPluralTemplates.add("Many [noun] for testing");

		updatedJson.add("singularTemplates", updatedSingularTemplates);
		updatedJson.add("pluralTemplates", updatedPluralTemplates);

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(updatedJson.toString());
		}

		generator.onJsonUpdate();

		@SuppressWarnings("unchecked")
		var updatedTemplatesMap = (Map<Number, List<Template>>) templatesField.get(generator);
		int updatedSingularCount = updatedTemplatesMap.get(Number.SINGULAR).size();

		assertEquals(5, updatedSingularCount, "Number of singular templates should be updated to 5");
		assertTrue(updatedSingularCount > initialSingularCount, "Template count should increase after update");
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
		JsonArray singularTemplates = new JsonArray();
		singularTemplates.add("Test singular template [noun]");
		partialJson.add("singularTemplates", singularTemplates);
		partialJson.add("pluralTemplates", new JsonArray());

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(partialJson.toString());
		}

		RandomTemplateGenerator partialGenerator = new RandomTemplateGenerator(testFilePath);

		Template singularTemplate = partialGenerator.getRandomTemplate(Number.SINGULAR);
		assertNotNull(singularTemplate, "Should return a singular template");

		assertThrows(InvalidListException.class, () -> partialGenerator.getRandomTemplate(Number.PLURAL),
			"Should throw InvalidListException when plural templates list is empty");

		partialGenerator.cleanup();
	}

	@Test
	@DisplayName("Test logging behavior")
	void testLoggingBehavior() throws Exception
	{
		LoggerManager mockLogger = mock(LoggerManager.class);

		Field loggerField = RandomTemplateGenerator.class.getDeclaredField("logger");
		loggerField.setAccessible(true);
		LoggerManager originalLogger = (LoggerManager) loggerField.get(generator);
		loggerField.set(generator, mockLogger);

		try
		{
			generator.getRandomTemplate();
			generator.getRandomTemplate(Number.SINGULAR);
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
	@DisplayName("Test random distribution of templates")
	void testRandomDistribution()
	{
		Map<String, Integer> countMap = new HashMap<>();
		int iterations = 1000;

		for(int i = 0; i < iterations; i++)
		{
			Template t = generator.getRandomTemplate();
			countMap.merge(t.getPattern(), 1, Integer::sum);
		}

		double expected = iterations / (double) countMap.size();
		for(int count : countMap.values())
			assertTrue(Math.abs(count - expected) < expected * 0.3, "Template distribution should be roughly uniform");
	}

	@Test
	@DisplayName("Test concurrent access to template generation")
	void testConcurrentAccess() throws Exception
	{
		int threadCount = 100;
		CountDownLatch latch = new CountDownLatch(threadCount);
		ExecutorService executor = Executors.newFixedThreadPool(10);

		for(int i = 0; i < threadCount; i++)
		{
			executor.submit(() ->
				{
					try
					{
						Template template = generator.getRandomTemplate();
						assertNotNull(template, "Template should not be null");
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
	@DisplayName("Test with maximum template length")
	void testMaxTemplateLength() throws Exception
	{
		String longTemplate = "a".repeat(10000) + " [noun]";

		JsonObject testTemplates = new JsonObject();
		JsonArray singularTemplates = new JsonArray();

		singularTemplates.add(longTemplate);
		testTemplates.add("singularTemplates", singularTemplates);
		testTemplates.add("pluralTemplates", new JsonArray());


		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(testTemplates.toString());
		}

		RandomTemplateGenerator longTemplateGenerator = new RandomTemplateGenerator(testFilePath);
		Template template = longTemplateGenerator.getRandomTemplate(Number.SINGULAR);

		assertEquals(longTemplate, template.getPattern());
		longTemplateGenerator.cleanup();
	}

	@Test
	@DisplayName("Test constructor with invalid file path")
	void testConstructorWithInvalidFilePath()
	{
		String nonExistentPath = "non/existent/path/templates.json";
		assertThrows(IOException.class, () -> new RandomTemplateGenerator(nonExistentPath),
			"Should throw IOException when file path does not exist");
	}

	@Test
	@DisplayName("Test template retrieval with mocked Random")
	void testTemplateRetrievalWithMockedRandom() throws Exception
	{
		Field randomField = RandomTemplateGenerator.class.getDeclaredField("random");
		randomField.setAccessible(true);

		Random originalRandom = (Random) randomField.get(null);

		try
		{
			Random mockRandom = mock(Random.class);
			when(mockRandom.nextInt(anyInt())).thenReturn(0);

			randomField.set(null, mockRandom);
			Template firstSingularTemplate = generator.getRandomTemplate(Number.SINGULAR);
			Template secondSingularTemplate = generator.getRandomTemplate(Number.SINGULAR);
			assertEquals(firstSingularTemplate.getPattern(), secondSingularTemplate.getPattern(),
				"With mocked Random, templates should be identical");

			Template firstPluralTemplate = generator.getRandomTemplate(Number.PLURAL);
			Template secondPluralTemplate = generator.getRandomTemplate(Number.PLURAL);
			assertEquals(firstPluralTemplate.getPattern(), secondPluralTemplate.getPattern(),
				"With mocked Random, templates should be identical");

			when(mockRandom.nextInt(eq(Number.values().length))).thenReturn(0);
			Template randomTemplate = generator.getRandomTemplate();
			assertEquals(Number.SINGULAR, randomTemplate.getNumber(),
				"With mocked Random, type should be SINGULAR");
		}
		finally
		{
			randomField.set(null, originalRandom);
		}
	}

	@Test
	@DisplayName("Test behavior with large number of templates")
	void testLargeNumberOfTemplates() throws Exception
	{
		JsonObject largeTemplatesJson = new JsonObject();
		JsonArray largeSingularArray = new JsonArray();
		JsonArray largePluralArray = new JsonArray();

		for (int i = 0; i < 1000; i++)
			largeSingularArray.add("Singular template " + i + " with [noun]");

		for (int i = 0; i < 1000; i++)
			largePluralArray.add("Plural templates " + i + " with [noun]");

		largeTemplatesJson.add("singularTemplates", largeSingularArray);
		largeTemplatesJson.add("pluralTemplates", largePluralArray);

		try (FileWriter writer = new FileWriter(testFile))
		{
			writer.write(largeTemplatesJson.toString());
		}

		RandomTemplateGenerator largeGenerator = new RandomTemplateGenerator(testFilePath);

		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 100; i++)
		{
			Template template = largeGenerator.getRandomTemplate();
			assertNotNull(template, "Template should not be null");
		}

		long endTime = System.currentTimeMillis();

		assertTrue((endTime - startTime) < 1000,
			"Template generation with large file should be efficient (< 1 second for 100 operations)");

		Field templatesField = RandomTemplateGenerator.class.getDeclaredField("templates");
		templatesField.setAccessible(true);

		@SuppressWarnings("unchecked")
		Map<Number, List<Template>> templatesMap = (Map<Number, List<Template>>) templatesField.get(largeGenerator);

		assertEquals(1000, templatesMap.get(Number.SINGULAR).size(),
			"Should have loaded 1000 singular templates");
		assertEquals(1000, templatesMap.get(Number.PLURAL).size(),
			"Should have loaded 1000 plural templates");

		largeGenerator.cleanup();
	}

	@Test
	@DisplayName("Test invalid templates handling")
	void testInvalidTemplatesHandling() throws Exception
	{
		JsonObject invalidTemplatesJson = new JsonObject();
		JsonArray invalidSingularArray = new JsonArray();
		JsonArray validPluralArray = new JsonArray();

		invalidSingularArray.add("Invalid template without placeholder");
		invalidSingularArray.add("Another invalid template");
		invalidSingularArray.add("This one is valid with [noun]");

		validPluralArray.add("Valid plural template with [noun]");

		invalidTemplatesJson.add("singularTemplates", invalidSingularArray);
		invalidTemplatesJson.add("pluralTemplates", validPluralArray);

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(invalidTemplatesJson.toString());
		}

		RandomTemplateGenerator invalidGenerator = new RandomTemplateGenerator(testFilePath);

		Field templatesField = RandomTemplateGenerator.class.getDeclaredField("templates");
		templatesField.setAccessible(true);

		@SuppressWarnings("unchecked")
		Map<Number, List<Template>> templatesMap = (Map<Number, List<Template>>) templatesField.get(invalidGenerator);

		assertEquals(3, templatesMap.get(Number.SINGULAR).size(),
			"Should have loaded all templates regardless of validity");

		boolean foundInvalidTemplate = false;
		for (int i = 0; i < 30; i++)
		{
			Template template = invalidGenerator.getRandomTemplate(Number.SINGULAR);

			if(!template.getPattern().contains("[noun]"))
			{
				foundInvalidTemplate = true;
				break;
			}
		}

		assertTrue(foundInvalidTemplate, "Should be able to return templates without [noun] placeholder");

		invalidGenerator.cleanup();
	}

	@Test
	@DisplayName("Test recovery after JSON file corruption during update")
	void testRecoveryAfterJsonCorruption() throws Exception
	{
		Field templatesField = RandomTemplateGenerator.class.getDeclaredField("templates");
		templatesField.setAccessible(true);

		@SuppressWarnings("unchecked")
		Map<Number, List<Template>> initialTemplatesMap = (Map<Number, List<Template>>) templatesField.get(generator);
		int initialSingularCount = initialTemplatesMap.get(Number.SINGULAR).size();

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write("{ corrupted json content");
		}

		assertThrows(InvalidJsonStateException.class, () -> generator.onJsonUpdate(),
			"Should throw InvalidJsonStateException when JSON is corrupted during update");

		Template template = generator.getRandomTemplate(Number.SINGULAR);
		assertNotNull(template, "Should still be able to generate templates after failed update");

		JsonObject fixedJson = createDefaultTestTemplates();
		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(fixedJson.toString());
		}

		generator.onJsonUpdate();

		@SuppressWarnings("unchecked")
		Map<Number, List<Template>> recoveredTemplatesMap = (Map<Number, List<Template>>) templatesField.get(generator);
		int recoveredSingularCount = recoveredTemplatesMap.get(Number.SINGULAR).size();

		assertEquals(initialSingularCount, recoveredSingularCount,
			"Should recover and have the same number of templates after fixing JSON");
	}

	@Test
	@DisplayName("Test thread safety of template retrieval")
	void testThreadSafetyOfTemplateRetrieval() throws Exception
	{
		int threadCount = 20;
		int iterationsPerThread = 100;
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		List<Exception> exceptions = new ArrayList<>();

		for (int i = 0; i < threadCount; i++)
		{
			executor.submit(() ->
				{
					try
					{
						startLatch.await();

						for(int j = 0; j < iterationsPerThread; j++)
						{
							Template template = generator.getRandomTemplate();
							assertNotNull(template);
							assertNotNull(template.getPattern());
							assertNotNull(template.getNumber());
						}
					}
					catch(Exception e)
					{
							synchronized(exceptions)
							{
								exceptions.add(e);
							}
					}
					finally
					{
						doneLatch.countDown();
					}
				});
		}

		startLatch.countDown();

		boolean allDone = doneLatch.await(30, java.util.concurrent.TimeUnit.SECONDS);
		executor.shutdown();

		assertTrue(allDone, "All threads should complete in time");
		assertTrue(exceptions.isEmpty(), "No exceptions should be thrown during concurrent access");
	}

	@Test
	@DisplayName("Test with malformed template entries (expecting InvalidTemplateException)")
	void testWithMalformedTemplateEntries() throws Exception
	{
		JsonObject malformedJson = new JsonObject();
		JsonArray malformedSingularArray = new JsonArray();
		JsonArray malformedPluralArray = new JsonArray();

		malformedSingularArray.add("Valid template [noun]");
		malformedSingularArray.add("");
		JsonObject objElement = new JsonObject();
		objElement.addProperty("text", "Object as element");
		malformedSingularArray.add(objElement);

		malformedPluralArray.add("Valid plural template [noun]");

		malformedJson.add("singularTemplates", malformedSingularArray);
		malformedJson.add("pluralTemplates", malformedPluralArray);

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(malformedJson.toString());
		}

		Exception thrownException = assertThrows(InvalidTemplateException.class, () ->
			{
				new RandomTemplateGenerator(testFilePath);
			}, "Expected RandomTemplateGenerator constructor to throw InvalidTemplateException " +
				"when a JsonObject is provided as a template string.");
	}

	@Test
	@DisplayName("Test constructor with temp file")
	void testConstructorWithTempFile() throws Exception
	{
		File tempFile = File.createTempFile("tempTemplates", ".json");
		String tempFilePath = tempFile.getAbsolutePath();

		try(FileWriter writer = new FileWriter(tempFile))
		{
			writer.write(createDefaultTestTemplates().toString());
		}

		RandomTemplateGenerator tempGenerator = new RandomTemplateGenerator(tempFilePath);

		Template template = tempGenerator.getRandomTemplate();
		assertNotNull(template, "Should generate template from temporary file");

		boolean deleted = tempFile.delete();

		Template templateAfterDeletion = tempGenerator.getRandomTemplate();
		assertNotNull(templateAfterDeletion, "Should still generate template after file deletion");

		tempGenerator.cleanup();
	}

	@Test
	@DisplayName("Test with empty JSON file")
	void testWithEmptyJsonFile() throws Exception
	{
		JsonObject emptyJson = new JsonObject();

		try(FileWriter writer = new FileWriter(testFile))
		{
			writer.write(emptyJson.toString());
		}

		assertThrows(Exception.class, () -> new RandomTemplateGenerator(testFilePath),
			"Should throw exception when JSON file doesn't contain required keys");
	}

	@Test
	@DisplayName("Test performance of repeated template generations")
	void testGenerationPerformance()
	{
		int iterations = 10000;
		long startTime = System.currentTimeMillis();

		for(int i = 0; i < iterations; i++)
			generator.getRandomTemplate();

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		assertTrue(duration < 2000, "Should be able to generate 10000 templates in less than 2 seconds (took " + duration + "ms)");
	}
}
