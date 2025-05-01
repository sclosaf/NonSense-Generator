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
	@DisplayName("Test basic success of appendItemToJson.")
	void testAppendItemToJson_Success() throws IOException
	{
		String key = "TestItems1";
		String str = "ItemX";

		handler.appendItemToJson(tempFile.getPath(), key, str);

		List<String> items = handler.readListFromJson(tempFile.getPath(), key);
		assertTrue(items.contains(str), "New item should have been added to array.");

		assertEquals(4, items.size(), "Array should have four elements.");
	}
}
