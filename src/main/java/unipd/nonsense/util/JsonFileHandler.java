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

import unipd.nonsense.util.LoggerManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.lang.IllegalStateException;
import com.google.gson.JsonSyntaxException;

public class JsonFileHandler
{
	private static volatile JsonFileHandler instance = new JsonFileHandler();
	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	private LoggerManager logger = new LoggerManager(JsonFileHandler.class);
	private final Gson gson;

	private JsonFileHandler()
	{
		logger.logTrace("Initializing JsonFileHandler");
		gson = new GsonBuilder().setPrettyPrinting().create();
		logger.logTrace("JsonFileHandler created successfully");
	}

	public static JsonFileHandler getInstance()
	{
		return instance;
	}

	public void appendItemToJson(String filePath, String key, String str) throws IOException, IllegalArgumentException
	{
		logger.logTrace("appendItemToJson: Attempting to append item to JSON");
		logger.logDebug("appendItemToJson: Appending to filePath " + filePath + ", key = " + key + ", str = " + str);

		if(key == null)
		{
			logger.logError("appendItemToJson: Null key provided");
			throw new NullJsonKeyException();
		}

		filePath = validateFile(filePath, true);

		lock.writeLock().lock();
		logger.logTrace("appendItemToJson: Acquired write lock");

		try
		{
			JsonObject json = readJsonObject(filePath);

			if(!json.has(key))
			{
				logger.logError("appendItemToJson: Invalid key in JSON");
				throw new InvalidJsonKeyException(key);
			}

			if(!json.get(key).isJsonArray())
			{
				logger.logError("appendItemToJson: Element is not an array");
				throw new JsonElementIsNotArrayException(key);
			}

			JsonArray elements = json.getAsJsonArray(key);


			for(JsonElement element : elements)
			{
				if(element.isJsonPrimitive() && element.getAsString().equals(str))
				{
					logger.logTrace("appendItemToJson: Item already exists in array, skipping");
					return;
				}
			}

			elements.add(str);
			logger.logTrace("appendItemToJson: Added new item to array");
			writeJsonObject(filePath, json);
			logger.logTrace("appendItemToJson: Successfully updated JSON file");
		}
		finally
		{
			lock.writeLock().unlock();
			logger.logTrace("appendItemToJson: Released write lock");
		}
	}

	public String readItemFromJson(String filePath, String key, int index) throws IOException, IllegalArgumentException, IndexOutOfBoundsException
	{
		logger.logTrace("readItemFromJson: Attempting to read item from JSON");
		logger.logDebug("readItemFromJson: FilePath " + filePath + ", key = " + key);

		if(key == null)
		{
			logger.logError("readItemFromJson: Null key provided");
			throw new NullJsonKeyException();
		}

		filePath = validateFile(filePath, false);

		if(index < 0)
		{
			logger.logError("readItemFromJson: Invalid index");
			throw new InvalidJsonIndexException(index);
		}

		lock.readLock().lock();
		logger.logTrace("readItemFromJson: Acquired read lock");

		try
		{
			JsonObject json = readJsonObject(filePath);

			if(!json.has(key))
			{
				logger.logError("readItemFromJson: Invalid key in JSON");
				throw new InvalidJsonKeyException(key);
			}

			if(!json.get(key).isJsonArray())
			{
				logger.logError("readItemFromJson: Element is not an array");
				throw new JsonElementIsNotArrayException(key);
			}

			JsonArray elements = json.getAsJsonArray(key);

			if(index >= elements.size())
			{
				logger.logError("readItemFromJson: Index out of bounds");
				throw new InvalidJsonIndexException(index);
			}

			JsonElement element = elements.get(index);

			if(!element.isJsonPrimitive())
			{
				logger.logError("readItemFromJson: Element is not primitive");
				throw new JsonElementIsNotPrimitiveException(element);
			}

			String result = element.getAsString();
			logger.logDebug("readItemFromJson: Retrieved value: " + result);
			logger.logTrace("readItemFromJson: Successfully read item from JSON");

			return result;
		}
		finally
		{
			lock.readLock().unlock();
			logger.logTrace("readItemFromJson: Released read lock");
		}
	}

	public List<String> readListFromJson(String filePath, String key) throws IOException, IllegalArgumentException
	{
		logger.logTrace("readListFromJson: Attempting to read list from JSON");
		logger.logDebug("readListFromJson: Appending to filePath " + filePath + ", key = " + key);

		if(key == null)
		{
			logger.logError("readListFromJson: Null key provided");
			throw new NullJsonKeyException();
		}

		filePath = validateFile(filePath, false);

		lock.readLock().lock();
		logger.logTrace("readListFromJson: Acquired read lock");

		try
		{
			JsonObject json = readJsonObject(filePath);

			if(!json.has(key))
			{
				logger.logError("readListFromJson: Invalid key in JSON");
				throw new InvalidJsonKeyException(key);
			}

			if(!json.get(key).isJsonArray())
			{
				logger.logError("readListFromJson: Element is not an array");
				throw new JsonElementIsNotArrayException(key);
			}

			JsonArray elements = json.getAsJsonArray(key);
			List<String> result = new ArrayList<String>();

			for(JsonElement element : elements)
				if(element.isJsonPrimitive())
					result.add(element.getAsString());

			logger.logDebug("readListFromJson: Retrieved list with " + result.size() + " items");
			logger.logTrace("readListFromJson: Successfully read list from JSON");

			return result;
		}
		finally
		{
			lock.readLock().unlock();
			logger.logTrace("readListFromJson: Released read lock");
		}
	}

	public boolean hasJsonKey(String filePath, String key) throws IOException, IllegalArgumentException
	{
		logger.logTrace("hasJsonKey: Checking if JSON has key");
		logger.logDebug("hasJsonKey: Appending to filePath " + filePath + ", key = " + key);

		if(key == null)
		{
			logger.logError("hasJsonKey: Null key provided");
			throw new NullJsonKeyException();
		}

		filePath = validateFile(filePath, false);

		lock.readLock().lock();
		logger.logTrace("hasJsonKey: Acquired read lock");

		try
		{
			JsonObject json = readJsonObject(filePath);
			String[] keys = key.split("\\.");

			JsonElement currentElement = json;

			for(String k : keys)
			{
				if(!currentElement.isJsonObject())
				{
					logger.logDebug("hasJsonKey: Current element is not an object, key not found");
					return false;
				}

				JsonObject currentObject = currentElement.getAsJsonObject();

				if(!currentObject.has(k))
				{
					logger.logDebug("hasJsonKey: Sub-key not found: " + k);
					return false;
				}

				currentElement = currentObject.get(k);
			}

			logger.logTrace("hasJsonKey: Key found in JSON");
			logger.logTrace("hasJsonKey: Key check completed");

			return true;
		}
		finally
		{
			lock.readLock().unlock();
			logger.logTrace("hasJsonKey: Released read lock");
		}
	}

	public JsonObject getJsonObject(String filePath) throws IOException
	{
		logger.logTrace("getJsonObject: Retrieving JSON object from file");
		logger.logDebug("getJsonObject: filePath=" + filePath);

		filePath = validateFile(filePath, false);

		lock.readLock().lock();
		logger.logTrace("getJsonObject: Acquired read lock");

		try
		{
			JsonObject result = readJsonObject(filePath);
			logger.logTrace("getJsonObject: Successfully retrieved JSON object");
			return result;
		}
		finally
		{
			lock.readLock().unlock();
			logger.logTrace("getJsonObject: Released read lock");
		}
	}

	private String validateFile(String filePath, boolean requiresWrite) throws IOException, IllegalArgumentException
	{
		logger.logTrace("validateFile: Validating file");
		logger.logDebug("validateFile: filePath = " + filePath + ", requiresWrite = " + requiresWrite);


		if(filePath == null)
		{
			logger.logError("validateFile: Null file path provided");
			throw new NullFilePathException();
		}

		if(filePath.isEmpty())
		{
			logger.logError("validateFile: Empty file path provided");
			throw new InvalidFilePathException(filePath);
		}

		filePath = filePath.toLowerCase().endsWith(".json") ? filePath : filePath + ".json";

		logger.logDebug("validateFile: Formatted file path: " + filePath);

		File file = new File(filePath);

		if(!file.exists() || !file.isFile())
		{
			logger.logError("validateFile: File does not exist or is not accessible");
			throw new InaccessibleFileException();
		}

		if(!file.canRead())
		{
			logger.logError("validateFile: File cannot be read");
			throw new UnreadableFileException();
		}

		if(requiresWrite && !file.canWrite())
		{
			logger.logError("validateFile: File cannot be written");
			throw new UnwritableFileException();
		}

		logger.logTrace("validateFile: File validation successful");
		return filePath;
	}

	private JsonObject readJsonObject(String filePath) throws IOException
	{
		logger.logTrace("readJsonObject: Reading JSON object from file");
		logger.logDebug("readJsonObject: filePath = " + filePath);

		try(FileReader reader = new FileReader(filePath))
		{
			JsonObject result = JsonParser.parseReader(reader).getAsJsonObject();
			logger.logTrace("readJsonObject: Successfully parsed JSON object");
			return result;
		}
		catch(IllegalStateException | JsonSyntaxException e)
		{
			logger.logError("readJsonObject: Invalid JSON state or syntax", e);
			throw new InvalidJsonStateException();
		}
	}

	private void writeJsonObject(String filePath, JsonObject json) throws IOException
	{
		logger.logTrace("writeJsonObject: Writing JSON object to file");
		logger.logDebug("writeJsonObject: filePath = " + filePath);

		try(FileWriter writer = new FileWriter(filePath))
		{
			gson.toJson(json, writer);
			logger.logTrace("writeJsonObject: Successfully wrote JSON object");
		}
		catch(IllegalStateException | JsonSyntaxException e)
		{
			logger.logError("writeJsonObject: Invalid JSON state or syntax", e);
			throw new InvalidJsonStateException();
		}
	}
}
