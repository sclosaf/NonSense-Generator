package it.nonsense.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Files;

import java.util.List;

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
			tempFile.delete();
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
	@DisplayName("Test duplicate case of appendItemToJson.")
	void testAppendItemToJson_DuplicateItem() throws IOException
	{
		String key = "TestItems1";
		String str = "item2";

		handler.appendItemToJson(tempFile.getPath(), key, str);

		List<String> items = handler.readListFromJson(tempFile.getPath(), key);
		assertEquals(3, items.size(), "The array schouldn't grow due to duplicate elements");
	}

	@Test
	@DisplayName("Test attempt to add an element to a non existent key.")
	void testAppentIdemToJson_NonExistentKey()
	{
		String key = "WrongTestItem";
		String str = "itemX";

		assertThrows(IllegalArgumentException.class, () -> handler.appendItemToJson(tempFile.getPath(), key, str), "Should throw IllegalArgumentException due to non existent key");
	}

	@Test
	@DisplayName("Test success of readItemFromJson.")
	void testReadItemFromJson_Success() throws IOException
	{
		String key = "TestItems1";
		int index = 1;

		String result = handler.readItemFromJson(tempFile.getPath(), key, index);
		assertEquals("item2", result, "Read element should be 'item2'");
	}

	@Test
	@DisplayName("Test attempt to access in element out of bounds.")
	void testReadItemFromJson_InvalidIndex()
	{
		String key = "TestItems1";
		int index1 = 10;
		int index2 = -9;

		assertThrows(IndexOutOfBoundsException.class, () -> handler.readItemFromJson(tempFile.getPath(), key, index1), "Should throw IndexOutOfBoundsException due to invalid index");
		assertThrows(IndexOutOfBoundsException.class, () -> handler.readItemFromJson(tempFile.getPath(), key, index2), "Should throw IndexOutOfBoundsException due to invalid index");

	}
}
