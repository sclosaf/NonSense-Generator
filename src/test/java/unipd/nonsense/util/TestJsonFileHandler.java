package unipd.nonsense.util;

import unipd.nonsense.exceptions.NullJsonKeyException;
import unipd.nonsense.exceptions.InvalidJsonKeyException;
import unipd.nonsense.exceptions.JsonElementIsNotArrayException;
import unipd.nonsense.exceptions.InvalidJsonIndexException;
import unipd.nonsense.exceptions.JsonElementIsNotPrimitiveException;
import unipd.nonsense.exceptions.NullFilePathException;
import unipd.nonsense.exceptions.InvalidFilePathException;
import unipd.nonsense.exceptions.InaccessibleFileException;
import unipd.nonsense.exceptions.UnreadableFileException;
import unipd.nonsense.exceptions.UnwritableFileException;
import unipd.nonsense.exceptions.InvalidJsonStateException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Files;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DisplayName("Testing JsonFileHandler.")
class TestJsonFileHandler
{
	private JsonFileHandler handler;
	private File tempFile;

	@BeforeEach
	@DisplayName("Setup environment: Create and fill up temprary .json test files.")
	void setUp() throws IOException
	{
		handler = JsonFileHandler.getInstance();
		tempFile = Files.createTempFile("test", ".json").toFile();

		JsonObject json = new JsonObject();

		JsonArray arr1 = new JsonArray();
		JsonArray arr2 = new JsonArray();

		arr1.add("item1");
		arr1.add("item2");
		arr1.add("item3");

		arr2.add("item4");
		arr2.add("item5");

		json.add("TestItems1", arr1);
		json.add("TestItems2", arr2);

		try(FileWriter writer = new FileWriter(tempFile))
		{
			writer.write(json.toString());
		}
	}

	@AfterEach
	@DisplayName("Removing environment used for testing.")
	void tearDown()
	{
		if(tempFile != null && tempFile.exists())
			try
			{
				Files.delete(tempFile.toPath());
			}
			catch(IOException e)
			{
				System.err.println("Failed to delete temp file: " + e.getMessage());
			}
	}

	@Test
	@DisplayName("Test success of appendItemToJson.")
	void testAppendItemToJson_Success() throws IOException
	{
		String key = "TestItems1";
		String str = "itemX";

		handler.appendItemToJson(tempFile.getPath(), key, str);

		List<String> items = handler.readListFromJson(tempFile.getPath(), key);
		assertTrue(items.contains(str), "New item should have been added to the array.");

		assertEquals(4, items.size(), "The array should have four elements.");
	}

	@Test
	@DisplayName("Test append duplicate case.")
	void testAppendItemToJson_DuplicateItem() throws IOException
	{
		String key = "TestItems1";
		String str = "item2";

		handler.appendItemToJson(tempFile.getPath(), key, str);

		List<String> items = handler.readListFromJson(tempFile.getPath(), key);
		assertEquals(3, items.size(), "The array schouldn't grow due to duplicate elements.");
	}

	@Test
	@DisplayName("Test attempt to add an element to a non existent key.")
	void testAppentIdemToJson_NonExistentKey()
	{
		String key = "WrongTestItem";
		String str = "itemX";

		assertThrows(InvalidJsonKeyException.class, () -> handler.appendItemToJson(tempFile.getPath(), key, str), "Should throw InvalidJsonKeyException due to non existent key.");
	}

	@Test
	@DisplayName("Test writing concurrency.")
	void testAppenItemToJson_ConcurrencyWriting() throws Exception
	{
		String key = "TestItems1";
		int threads = 10;

		CountDownLatch latch = new CountDownLatch(threads);
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		for(int i = 0; i < threads; ++i)
		{
			final int itemNum = i + 4;

			executor.submit(() ->
				{
					try
					{
						handler.appendItemToJson(tempFile.getPath(), key, "item" + itemNum);
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
					finally
					{
						latch.countDown();
					}
				});
		}

		latch.await();
		executor.shutdown();

		List<String> items = handler.readListFromJson(tempFile.getPath(), key);

		assertEquals(threads + 3, items.size(), "Should be contained " + threads + " new elements plus the initial 3.");
	}

	@Test
	@DisplayName("Test append to non array key.")
	void testAppendItemToJson_NonArrayKey() throws IOException
	{
		File file = Files.createTempFile("nonArray", ".json").toFile();
		file.deleteOnExit();

		JsonObject json = new JsonObject();
		json.addProperty("TestKey", "notAnArray");

		try(FileWriter writer = new FileWriter(file))
		{
			writer.write(json.toString());
		}

		assertThrows(JsonElementIsNotArrayException.class, () -> handler.appendItemToJson(file.getPath(), "TestKey", "itemX"), "Should throw JsonElementIsNotArrayException due to non array key.");
	}


	@Test
	@DisplayName("Test appending to non writable file.")
	void testAppendItemToJson_NonWritableFile() throws IOException
	{
		File nonWritableFile = Files.createTempFile("nonwritable", ".json").toFile();
		nonWritableFile.deleteOnExit();

		JsonObject json = new JsonObject();
		JsonArray arr = new JsonArray();

		arr.add("item1");
		json.add("TestItems1", arr);

		try(FileWriter writer = new FileWriter(nonWritableFile))
		{
			writer.write(json.toString());
		}

		boolean setWritableFailed = !nonWritableFile.setWritable(false);
		Assumptions.assumeFalse(setWritableFailed, "Could not make file non-writable, skipping test");


		String key = "TestItems1";

		assertThrows(UnwritableFileException.class, () -> handler.appendItemToJson(nonWritableFile.getPath(), key, "itemX"), "Should throw UnwritableFileException for non writable file.");
	}

	@Test
	@DisplayName("Test success of readItemFromJson.")
	void testReadItemFromJson_Success() throws IOException
	{
		String key = "TestItems1";
		int index = 1;

		String result = handler.readItemFromJson(tempFile.getPath(), key, index);
		assertEquals("item2", result, "Read element should be 'item2'.");
	}

	@Test
	@DisplayName("Test attempt to access in element out of bounds.")
	void testReadItemFromJson_InvalidIndex()
	{
		String key = "TestItems1";
		int index1 = 10;
		int index2 = -9;

		assertThrows(InvalidJsonIndexException.class, () -> handler.readItemFromJson(tempFile.getPath(), key, index1), "Should throw InvalidJsonIndexException due to out of bound index.");
		assertThrows(InvalidJsonIndexException.class, () -> handler.readItemFromJson(tempFile.getPath(), key, index2), "Should throw InvalidJsonIndexException due to invalid index.");
	}

	@Test
	@DisplayName("Test attempt to read an element of a non existent key.")
	void testReadItemFromJson_NonExistentKey()
	{
		String key = "WrongTestItem";
		int index = 0;

		assertThrows(InvalidJsonKeyException.class, () -> handler.readItemFromJson(tempFile.getPath(), key, index), "Should throw InvalidJsonKeyException due to non existent key.");
	}

	@Test
	@DisplayName("Test readItemFromJson with empty JSON file.")
	void testReadItemFromJson_EmptyFile() throws IOException
	{
		File emptyFile = Files.createTempFile("empty", ".json").toFile();
		emptyFile.deleteOnExit();

		String key = "TestItems1";

		assertThrows(InvalidJsonStateException.class, () -> handler.readItemFromJson(emptyFile.getPath(), key, 0), "Should throw InvalidJsonStateException for empty JSON file.");
	}

	@Test
	@DisplayName("Test reading item from non readable file.")
	void testReadItemFromJson_NonReadableFile() throws IOException
	{
		File nonReadableFile = Files.createTempFile("nonreadable", ".json").toFile();
		nonReadableFile.deleteOnExit();

		try(FileWriter writer = new FileWriter(nonReadableFile))
		{
			writer.write("{}");
		}

		boolean setReadableFailed = !nonReadableFile.setReadable(false);

		Assumptions.assumeFalse(setReadableFailed, "Could not make file non-readable, skipping test");

		String key = "TestItems1";

		assertThrows(UnreadableFileException.class, () -> handler.readItemFromJson(nonReadableFile.getPath(), key, 0), "Should throw UnreadableFileException due to non readable file.");
	}

	@Test
	@DisplayName("Test reading item concurrency.")
	void testReadItemFromJson_ConcurrencyReading() throws Exception
	{
		String key = "TestItems1";
		int threads = 10;

		CountDownLatch latch = new CountDownLatch(threads);
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		executor.submit(() ->
			{
				try
				{
					handler.appendItemToJson(tempFile.getPath(), key, "itemX");
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
				finally
				{
					latch.countDown();
				}
			});

		for(int i = 0; i < threads; ++i)
		{
			executor.submit(() ->
				{
					try
					{
						handler.readItemFromJson(tempFile.getPath(), key, 3);
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
					finally
					{
						latch.countDown();
					}
				});
		}

		latch.await();
		executor.shutdown();

		String item = handler.readItemFromJson(tempFile.getPath(), key, 3);
		assertEquals(item, "itemX", "'itemX' should have been appended.");
	}

	@Test
	@DisplayName("Test handling long value of json file.")
	void testReadItemFromJson_LongValue() throws IOException
	{
		JsonObject json = handler.getJsonObject(tempFile.getPath());
		json.add("TestItem", new JsonArray());

		try(FileWriter writer = new FileWriter(tempFile))
		{
			writer.write(json.toString());
		}

		StringBuilder longValueBuilder = new StringBuilder();
		for(int i = 0; i < 100000; ++i)
			longValueBuilder.append("X");

		String longValue = longValueBuilder.toString();

		handler.appendItemToJson(tempFile.getPath(), "TestItem", longValue);

		String readValue = handler.readItemFromJson(tempFile.getPath(), "TestItem", 0);
		assertEquals(longValue, readValue, "Should be able to read the long value");
	}

	@Test
	@DisplayName("Stress test concurrent read and write.")
	void testReadItemFromJson_StressConcurrentReadWrite() throws Exception
	{
		String key = "TestItems1";
		int threads = 100;

		CountDownLatch latch = new CountDownLatch(2 * threads);
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		for(int i = 0; i < threads; ++i)
		{
			final int itemNum = i + 4;
			executor.submit(() ->
				{
					try
					{
						handler.appendItemToJson(tempFile.getPath(), key, "item" + itemNum);
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
					finally
					{
						latch.countDown();
					}
				});
		}

		for(int i = 0; i < threads; ++i)
		{
			executor.submit(() ->
				{
					try
					{
						handler.readListFromJson(tempFile.getPath(), key);
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
					finally
					{
						latch.countDown();
					}
				});
		}

		latch.await();
		executor.shutdown();

		List<String> items = new ArrayList<String>();

		for(int i = 0; i < 3 + threads; ++i)
			items.add(handler.readItemFromJson(tempFile.getPath(), key, i));

		assertTrue(items.size() == 3 + threads, "Should contain " + threads + " items plus the initial 3.");
	}

	@Test
	@DisplayName("Test file path without JSON extension for ReadItemFromJson.")
	void testReadItemFromJson_NoJsonExtension() throws IOException
	{
		File noExtFile = Files.createTempFile("noext", ".json").toFile();
		noExtFile.deleteOnExit();

		JsonObject json = new JsonObject();
		JsonArray arr = new JsonArray();

		arr.add("item1");
		json.add("TestKey", arr);

		try (FileWriter writer = new FileWriter(noExtFile))
		{
			writer.write(json.toString());
		}

		String pathWithoutExtension = noExtFile.getPath().replace(".json", "");
		String item = handler.readItemFromJson(pathWithoutExtension, "TestKey", 0);

		assertEquals("item1", item, "Should contain 'item1'.");
	}

	@Test
	@DisplayName("Test success of ReadListFromJson.")
	void testReadListFromJson_Success() throws IOException
	{
		String key = "TestItems2";

		List<String> items = handler.readListFromJson(tempFile.getPath(), key);

		assertEquals(2, items.size(), "The array should have 2 elements.");
		assertTrue(items.contains("item4") && items.contains("item5"), "The elements should be 'item4' and 'item5'.");
	}

	@Test
	@DisplayName("Test attempt to read a list of a non existent key.")
	void testReadListFromJson_NonExistentKey()
	{
		String key = "WrongTestItem";

		assertThrows(InvalidJsonKeyException.class, () -> handler.readListFromJson(tempFile.getPath(), key), "Should throw InvalidJsonKeyException due to non existent key.");
	}

	@Test
	@DisplayName("Test reading from empty JSON file.")
	void testReadListFromJson_EmptyFile() throws IOException
	{
		File emptyFile = Files.createTempFile("empty", ".json").toFile();
		emptyFile.deleteOnExit();

		String key = "TestItems1";

		assertThrows(InvalidJsonStateException.class, () -> handler.readListFromJson(emptyFile.getPath(), key), "Should throw InvalidJsonStateException due to empty json file.");
	}

	@Test
	@DisplayName("Test reading from malformed JSON file.")
	void testReadListFromJson_MalformedFile() throws IOException
	{
		File malformedFile = Files.createTempFile("malformed", ".json").toFile();
		malformedFile.deleteOnExit();

		try(FileWriter writer = new FileWriter(malformedFile))
		{
			writer.write("{ \"key\": [ \"item1\", \"item2\" ");
		}

		String key = "TestItems1";
		assertThrows(InvalidJsonStateException.class, () -> handler.readListFromJson(malformedFile.getPath(), key), "Should throw InvalidJsonStateException due to malformed json file.");
	}

	@Test
	@DisplayName("Test handling long key of json file.")
	void testReadListFromJson_LongKey() throws IOException
	{
		StringBuilder longKeyBuilder = new StringBuilder();
		for(int i = 0; i < 100000; ++i)
			longKeyBuilder.append("X");

		String longKey = longKeyBuilder.toString();

		File longKeyFile = Files.createTempFile("longKey", ".json").toFile();
		longKeyFile.deleteOnExit();

		JsonObject json = new JsonObject();
		JsonArray array = new JsonArray();

		array.add("itemX");
		json.add(longKey, array);

		try(FileWriter writer = new FileWriter(longKeyFile))
		{
			writer.write(json.toString());
		}

		List<String> items = handler.readListFromJson(longKeyFile.getPath(), longKey);
		assertEquals(1, items.size(), "Should be able to read the array with the long key.");
	}

	@Test
	@DisplayName("Test handling large JSON file.")
	void testReadListJsonFile_LargeFile() throws IOException
	{
		File largeFile = Files.createTempFile("large", ".json").toFile();
		largeFile.deleteOnExit();

		JsonObject json = new JsonObject();
		JsonArray largeArray = new JsonArray();

		for (int i = 0; i < 100000; ++i)
			largeArray.add("item" + i);

		json.add("LargeArray", largeArray);

		try (FileWriter writer = new FileWriter(largeFile))
		{
			writer.write(json.toString());
		}

		List<String> items = handler.readListFromJson(largeFile.getPath(), "LargeArray");
		assertEquals(100000, items.size(), "Should read all 100,000 items.");
	}

	@Test
	@DisplayName("Test invalid file names.")
	void testValidateFileExists_InvalidFiles()
	{
		String key = "TestItems1";

		assertThrows(InaccessibleFileException.class, () -> handler.readListFromJson("invalid.json", key), "Should throw InaccessibleFileException due to invalid file name.");
		assertThrows(InaccessibleFileException.class, () -> handler.readListFromJson("nonexistent.json", key), "Should throw InaccessibleFileException due to invalid file name.");
		assertThrows(InaccessibleFileException.class, () -> handler.readListFromJson("noextension", key), "Should throw InaccessibleFileException due to invalid file name.");
	}

	@Test
	@DisplayName("Test null or empty file path and key.")
	void testNullOrEmptyInputs()
	{
		assertThrows(NullFilePathException.class, () -> handler.readListFromJson(null, "TestKey"), "Should throw NullFilePathException due to null file path.");
		assertThrows(InvalidFilePathException.class, () -> handler.readListFromJson("", "TestKey"), "Should throw InvalidFilePathException due to empty file path.");
		assertThrows(NullJsonKeyException.class, () -> handler.readListFromJson(tempFile.getPath(), null), "Should throw NullJsonKeyException due to null key.");
		assertThrows(InvalidJsonKeyException.class, () -> handler.readListFromJson(tempFile.getPath(), ""), "Should throw InvalidJsonKeyException due to empty key.");
	}

	@Test
	@DisplayName("Test reading list concurrency.")
	void testReadListFromJson_ConcurrencyReading() throws Exception
	{
		String key = "TestItems1";
		int threads = 10;

		CountDownLatch latch = new CountDownLatch(threads);
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		executor.submit(() ->
			{
				try
				{
					handler.appendItemToJson(tempFile.getPath(), key, "itemX");
				}
				catch(IOException e)
				{
				}
				finally
				{
					latch.countDown();
				}
			});

		for(int i = 0; i < threads; ++i)
		{
			executor.submit(() ->
				{
					try
					{
						handler.readListFromJson(tempFile.getPath(), key);
					}
					catch(IOException e)
					{
					}
					finally
					{
						latch.countDown();
					}
				});
		}

		latch.await();
		executor.shutdown();

		List<String> items = handler.readListFromJson(tempFile.getPath(), key);
		assertTrue(items.contains("itemX"), "'itemX' should have been appended.");
	}

	@Test
	@DisplayName("Test file path without JSON extension for ReadListFromJson.")
	void testReadListFromJson_NoJsonExtension() throws IOException
	{
		File noExtFile = Files.createTempFile("noext", ".json").toFile();
		noExtFile.deleteOnExit();

		JsonObject json = new JsonObject();
		JsonArray arr = new JsonArray();

		arr.add("item1");
		json.add("TestKey", arr);

		try(FileWriter writer = new FileWriter(noExtFile))
		{
			writer.write(json.toString());
		}

		String pathWithoutExtension = noExtFile.getPath().replace(".json", "");
		List<String> items = handler.readListFromJson(pathWithoutExtension, "TestKey");

		assertEquals(1, items.size(), "Should read the array correctly.");
		assertEquals("item1", items.get(0), "Should contain 'item1'.");
	}

	@Test
	@DisplayName("Test reading from non readable file.")
	void testReadListFromJson_NonReadableFile() throws IOException
	{
		File nonReadableFile = Files.createTempFile("nonreadable", ".json").toFile();
		nonReadableFile.deleteOnExit();

		try(FileWriter writer = new FileWriter(nonReadableFile))
		{
			writer.write("{}");
		}

		boolean setReadableFailed = !nonReadableFile.setReadable(false);
		Assumptions.assumeFalse(setReadableFailed, "Could not make file non-readable, skipping test");

		String key = "TestItems1";

		assertThrows(UnreadableFileException.class, () -> handler.readListFromJson(nonReadableFile.getPath(), key), "Should throw UnreadableFileException due to non readable file.");
	}

	@Test
	@DisplayName("Test file with path extremely long.")
	void testReadListFromJson_LongPath() throws IOException
	{
		StringBuilder pathBuilder = new StringBuilder();
		String basePath = System.getProperty("java.io.tmpdir");

		pathBuilder.append(basePath);

		if (!basePath.endsWith(File.separator))
			pathBuilder.append(File.separator);

		int maxDir = 10;

		for(int i = 0; i < maxDir; ++i)
			pathBuilder.append("subdir").append(i).append(File.separator);

		pathBuilder.append("testFile.json");

		String longPath = pathBuilder.toString();

		File pathDir = new File(pathBuilder.toString()).getParentFile();

		if (!pathDir.exists())
			pathDir.mkdirs();

		File pathFile = new File(longPath);
		if(!pathFile.exists())
			pathFile.createNewFile();

		JsonObject json = new JsonObject();
		JsonArray array = new JsonArray();

		array.add("itemX");
		json.add("TestItems", array);

		try(FileWriter writer = new FileWriter(pathFile))
		{
			writer.write(json.toString());
		}

		List<String> items = handler.readListFromJson(longPath, "TestItems");
		assertEquals(1, items.size(), "Should be able to read from a long path.");

		pathFile.delete();
	}

	@Test
	@DisplayName("Test success of hasJsonKey.")
	void testHasJsonKey_Success() throws IOException
	{
		String key = "TestItems1";
		assertTrue(handler.hasJsonKey(tempFile.getPath(), key), "Key 'TestItems1' should exist.");
	}

	@Test
	@DisplayName("Test success of checking for nested keys.")
	void testHasJsonKey_NestedKey() throws IOException
	{
		JsonObject json = handler.getJsonObject(tempFile.getPath());

		JsonObject nestedObject = new JsonObject();

		nestedObject.addProperty("NestedItem1", "nestedItemX");
		nestedObject.addProperty("NestedItem2", "nestedItemY");

		json.add("TestItem3", nestedObject);

		try(FileWriter writer = new FileWriter(tempFile))
		{
			writer.write(json.toString());
		}

		assertTrue(handler.hasJsonKey(tempFile.getPath(), "TestItem3.NestedItem2"), "Nested key 'TestItem3.NestedItem2' should exist.");
	}

	@Test
	@DisplayName("Test attempt check non existent nested key.")
	void testHasJsonKey_NonExistentNestedKey() throws IOException
	{
		JsonObject json = handler.getJsonObject(tempFile.getPath());

		JsonObject nestedObject = new JsonObject();

		nestedObject.addProperty("NestedItem1", "nestedItemX");
		nestedObject.addProperty("NestedItem2", "nestedItemY");

		json.add("TestItem3", nestedObject);

		try(FileWriter writer = new FileWriter(tempFile))
		{
			writer.write(json.toString());
		}

		assertFalse(handler.hasJsonKey(tempFile.getPath(), "TestItem3.nonExisting"), "Non existent nested key should return false.");
	}

	@Test
	@DisplayName("Test checking key in non readable file.")
	void testHasJsonKey_NonReadableFile() throws IOException
	{
		File nonReadableFile = Files.createTempFile("nonreadable", ".json").toFile();
		nonReadableFile.deleteOnExit();

		try(FileWriter writer = new FileWriter(nonReadableFile))
		{
			writer.write("{}");
		}

		boolean setReadableFailed = !nonReadableFile.setReadable(false);

		Assumptions.assumeFalse(setReadableFailed, "Could not make file non-readable, skipping test");


		String key = "TestItems1";

		assertThrows(UnreadableFileException.class, () -> handler.hasJsonKey(nonReadableFile.getPath(), key), "Should throw UnreadableFileException due to non readable file.");
	}

	@Test
	@DisplayName("Test deeply nested key.")
	void testHasJsonKey_DeeplyNestedKey() throws IOException
	{
		File file = Files.createTempFile("nested", ".json").toFile();
		file.deleteOnExit();

		JsonObject json = new JsonObject();
		JsonObject level1 = new JsonObject();
		JsonObject level2 = new JsonObject();
		JsonObject level3 = new JsonObject();

		level3.addProperty("DeepKey", "value");
		level2.add("Level3", level3);
		level1.add("Level2", level2);
		json.add("Level1", level1);

		try(FileWriter writer = new FileWriter(file))
		{
			writer.write(json.toString());
		}

		assertTrue(handler.hasJsonKey(file.getPath(), "Level1.Level2.Level3.DeepKey"), "Deeply nested key should exist.");
		assertFalse(handler.hasJsonKey(file.getPath(), "Level1.Level2.NonExistent.DeepKey"), "Non-existent deeply nested key should return false.");
	}

	@Test
	@DisplayName("Test extreme nested key.")
	void testHasJsonKey_ExtremelyDeeplyNestedKey() throws IOException
	{
		File nestedFile = Files.createTempFile("nested", ".json").toFile();
		nestedFile.deleteOnExit();

		JsonObject root = new JsonObject();
		JsonObject current = root;

		for(int i = 0; i < 100; ++i)
		{
			JsonObject next = new JsonObject();
			current.add("level" + i, next);
			current = next;
		}

		current.addProperty("value", "itemX");

		try(FileWriter writer = new FileWriter(nestedFile))
		{
			writer.write(root.toString());
		}

		StringBuilder keyBuilder = new StringBuilder();
		for(int i = 0; i < 100; ++i)
		{
			if(i > 0)
				keyBuilder.append(".");

			keyBuilder.append("level").append(i);
		}

		keyBuilder.append(".value");

		String key = keyBuilder.toString();

		boolean exists = handler.hasJsonKey(nestedFile.getPath(), key);
		assertTrue(exists, "Should be able to access extremely deeply nested key.");
	}

	@Test
	@DisplayName("Test success of getJsonObject.")
	void testGetJsonObject_Success() throws IOException
	{
		JsonObject json = handler.getJsonObject(tempFile.getPath());

		assertTrue(json.has("TestItems1") && json.has("TestItems2"), "The object json should have keys 'TestItem1' and 'TestItems2'.");
		assertEquals(3, json.getAsJsonArray("TestItems1").size(), "The array should have 3 elements.");
		assertEquals(2, json.getAsJsonArray("TestItems2").size(), "The array should have 2 elements.");
	}

	@Test
	@DisplayName("Test getting JSON object from non readable file.")
	void testGetJsonObject_NonReadableFile() throws IOException
	{
		File nonReadableFile = Files.createTempFile("nonreadable", ".json").toFile();
		nonReadableFile.deleteOnExit();

		try(FileWriter writer = new FileWriter(nonReadableFile))
		{
			writer.write("{}");
		}

		boolean setReadableFailed = !nonReadableFile.setReadable(false);

		Assumptions.assumeFalse(setReadableFailed, "Could not make file non-readable, skipping test.");

		assertThrows(UnreadableFileException.class, () -> handler.getJsonObject(nonReadableFile.getPath()), "Should throw UnreadableFileException due to non readable file.");
	}

	@Test
	@DisplayName("Test appending to empty array in JSON.")
	void testAppendItemToJson_EmptyArray() throws IOException
	{
		File emptyArrayFile = Files.createTempFile("emptyArray", ".json").toFile();
		emptyArrayFile.deleteOnExit();

		JsonObject json = new JsonObject();
		json.add("EmptyArray", new JsonArray());

		try(FileWriter writer = new FileWriter(emptyArrayFile))
		{
			writer.write(json.toString());
		}

		String key = "EmptyArray";
		String str = "firstItem";

		handler.appendItemToJson(emptyArrayFile.getPath(), key, str);
		List<String> items = handler.readListFromJson(emptyArrayFile.getPath(), key);

		assertEquals(1, items.size(), "Array should have one element after append.");
		assertEquals(str, items.get(0), "First element should match the appended item.");
	}

	@Test
	@DisplayName("Test appending multiple items rapidly to same array.")
	void testAppendItemToJson_RapidSequence() throws IOException
	{
		String key = "TestItems1";
		int iterations = 1000;

		for(int i = 0; i < iterations; i++)
			handler.appendItemToJson(tempFile.getPath(), key, "rapidItem" + i);

		List<String> items = handler.readListFromJson(tempFile.getPath(), key);
		assertTrue(items.size() >= iterations + 3, "Should contain all rapidly appended items plus original 3.");
	}

	@Test
	@DisplayName("Test reading from file with special characters in content.")
	void testReadItemFromJson_SpecialCharacters() throws IOException
	{
		File specialCharFile = Files.createTempFile("specialChars", ".json").toFile();
		specialCharFile.deleteOnExit();

		JsonObject json = new JsonObject();
		JsonArray array = new JsonArray();
		array.add("item\u00E9\u00DF\u20AC");
		json.add("SpecialChars", array);

		try(FileWriter writer = new FileWriter(specialCharFile))
		{
			writer.write(json.toString());
		}

		String result = handler.readItemFromJson(specialCharFile.getPath(), "SpecialChars", 0);
		assertEquals("item\u00E9\u00DF\u20AC", result, "Should correctly handle special characters.");
	}

	@Test
	@DisplayName("Test handling extremely large JSON file.")
	void testReadListFromJson_ExtremelyLargeFile() throws IOException
	{
		File hugeFile = Files.createTempFile("huge", ".json").toFile();
		hugeFile.deleteOnExit();

		JsonObject json = new JsonObject();
		JsonArray hugeArray = new JsonArray();

		for(int i = 0; i < 500000; i++)
			hugeArray.add("item" + i);

		json.add("HugeArray", hugeArray);

		try(FileWriter writer = new FileWriter(hugeFile))
		{
			writer.write(json.toString());
		}

		List<String> items = handler.readListFromJson(hugeFile.getPath(), "HugeArray");
		assertEquals(500000, items.size(), "Should handle extremely large arrays.");
	}

	@Test
	@DisplayName("Test handling extremely long key names.")
	void testHasJsonKey_ExtremelyLongKey() throws IOException
	{
		File longKeyFile = Files.createTempFile("longKey", ".json").toFile();
		longKeyFile.deleteOnExit();

		StringBuilder longKey = new StringBuilder();
		for(int i = 0; i < 10000; i++)
			longKey.append("a");


		JsonObject json = new JsonObject();
		json.addProperty(longKey.toString(), "value");

		try(FileWriter writer = new FileWriter(longKeyFile))
		{
			writer.write(json.toString());
		}

		assertTrue(handler.hasJsonKey(longKeyFile.getPath(), longKey.toString()),
			"Should handle extremely long key names.");
	}

	@Test
	@DisplayName("Test handling file with BOM (Byte Order Mark).")
	void testReadListFromJson_FileWithBOM() throws IOException
	{
		File bomFile = Files.createTempFile("bom", ".json").toFile();
		bomFile.deleteOnExit();

		try(FileWriter writer = new FileWriter(bomFile))
		{
			writer.write("\uFEFF{\"BOMArray\":[\"item1\"]}");
		}

		List<String> items = handler.readListFromJson(bomFile.getPath(), "BOMArray");
		assertEquals(1, items.size(), "Should handle files with BOM.");
	}

	@Test
	@DisplayName("Test handling file deletion during operation.")
	void testAppendItemToJson_FileDeleted() throws IOException
	{
		File volatileFile = Files.createTempFile("volatile", ".json").toFile();

		JsonObject json = new JsonObject();
		json.add("VolatileArray", new JsonArray());

		try(FileWriter writer = new FileWriter(volatileFile))
		{
			writer.write(json.toString());
		}

		volatileFile.delete();

		assertThrows(InaccessibleFileException.class,
			() -> handler.appendItemToJson(volatileFile.getPath(), "VolatileArray", "testItem"),
			"Should handle deleted files.");
	}

	@Test
	@DisplayName("Test handling extremely deep JSON structures.")
	void testHasJsonKey_ExtremeDepth() throws IOException
	{
		File deepFile = Files.createTempFile("deep", ".json").toFile();
		deepFile.deleteOnExit();

		JsonObject current = new JsonObject();
		JsonObject root = current;
		int depth = 1000;

		for(int i = 0; i < depth; i++)
		{
			JsonObject next = new JsonObject();
			current.add("level" + i, next);
			current = next;
		}

		current.addProperty("value", "deepValue");

		try(FileWriter writer = new FileWriter(deepFile))
		{
			writer.write(root.toString());
		}

		StringBuilder keyBuilder = new StringBuilder();
		for(int i = 0; i < depth; i++)
		{
			if(i > 0)
				keyBuilder.append(".");

			keyBuilder.append("level").append(i);
		}

		keyBuilder.append(".value");

		assertTrue(handler.hasJsonKey(deepFile.getPath(), keyBuilder.toString()),
			"Should handle extremely deep JSON structures.");
	}

	@Test
	@DisplayName("Test handling file with trailing garbage data.")
	void testReadJsonObject_TrailingGarbage() throws IOException
	{
		File garbageFile = Files.createTempFile("garbage", ".json").toFile();
		garbageFile.deleteOnExit();

		try(FileWriter writer = new FileWriter(garbageFile))
		{
			writer.write("{\"ValidKey\":[\"item1\"]}/* Trailing garbage */");
		}

		assertThrows(InvalidJsonStateException.class,
			() -> handler.readListFromJson(garbageFile.getPath(), "ValidKey"),
			"Should reject JSON with trailing garbage.");
	}

	@Test
	@DisplayName("Test handling file with duplicate keys.")
	void testReadJsonObject_DuplicateKeys() throws IOException
	{
		File dupKeyFile = Files.createTempFile("duplicate", ".json").toFile();
		dupKeyFile.deleteOnExit();

		try(FileWriter writer = new FileWriter(dupKeyFile))
		{
			writer.write("{\"DupeKey\":1,\"DupeKey\":2}");
		}

		JsonObject json = handler.getJsonObject(dupKeyFile.getPath());
		assertEquals(2, json.get("DupeKey").getAsInt(), "Should handle duplicate keys by taking last value.");
	}
}
