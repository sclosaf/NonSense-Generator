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
	private final Gson gson;

	private JsonFileHandler()
	{
		gson = new GsonBuilder().setPrettyPrinting().create();
	}

	public static JsonFileHandler getInstance()
	{
		return instance;
	}

	public void appendItemToJson(String filePath, String key, String str) throws IOException, IllegalArgumentException
	{
		if(key == null)
			throw new NullJsonKeyException();

		filePath = validateFile(filePath, true);

		lock.writeLock().lock();

		try
		{
			JsonObject json = readJsonObject(filePath);

			if(!json.has(key))
				throw new InvalidJsonKeyException(key);

			if(!json.get(key).isJsonArray())
				throw new JsonElementIsNotArrayException(key);

			JsonArray elements = json.getAsJsonArray(key);

			for(JsonElement element : elements)
				if(element.isJsonPrimitive() && element.getAsString().equals(str))
					return;

			elements.add(str);
			writeJsonObject(filePath, json);
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}

	public String readItemFromJson(String filePath, String key, int index) throws IOException, IllegalArgumentException, IndexOutOfBoundsException
	{
		if(key == null)
			throw new NullJsonKeyException();

		filePath = validateFile(filePath, false);

		if(index < 0)
			throw new InvalidJsonIndexException(index);

		lock.readLock().lock();

		try
		{
			JsonObject json = readJsonObject(filePath);

			if(!json.has(key))
				throw new InvalidJsonKeyException(key);

			if(!json.get(key).isJsonArray())
				throw new JsonElementIsNotArrayException(key);

			JsonArray elements = json.getAsJsonArray(key);

			if(index >= elements.size())
				throw new InvalidJsonIndexException(index);

			JsonElement element = elements.get(index);

			if(!element.isJsonPrimitive())
				throw new JsonElementIsNotPrimitiveException(element);

			return element.getAsString();
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	public List<String> readListFromJson(String filePath, String key) throws IOException, IllegalArgumentException
	{
		if(key == null)
			throw new NullJsonKeyException();

		filePath = validateFile(filePath, false);

		lock.readLock().lock();

		try
		{
			JsonObject json = readJsonObject(filePath);

			if(!json.has(key))
				throw new InvalidJsonKeyException(key);

			if(!json.get(key).isJsonArray())
				throw new JsonElementIsNotArrayException(key);

			JsonArray elements = json.getAsJsonArray(key);
			List<String> result = new ArrayList<String>();

			for (JsonElement element : elements)
				if(element.isJsonPrimitive())
					result.add(element.getAsString());

			return result;
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	public boolean hasJsonKey(String filePath, String key) throws IOException, IllegalArgumentException
	{
		if(key == null)
			throw new NullJsonKeyException();

		filePath = validateFile(filePath, false);

		lock.readLock().lock();

		try
		{
			JsonObject json = readJsonObject(filePath);
			String[] keys = key.split("\\.");

			JsonElement currentElement = json;

			for(String k : keys)
			{
				if(!currentElement.isJsonObject())
					return false;

				JsonObject currentObject = currentElement.getAsJsonObject();

				if(!currentObject.has(k))
					return false;

				currentElement = currentObject.get(k);
			}

			return true;
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	public JsonObject getJsonObject(String filePath) throws IOException
	{
		filePath = validateFile(filePath, false);

		lock.readLock().lock();

		try
		{
			return readJsonObject(filePath);
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	private String validateFile(String filePath, boolean requiresWrite) throws IOException, IllegalArgumentException
	{
		if(filePath == null)
			throw new NullFilePathException();

		if(filePath.isEmpty())
			throw new InvalidFilePathException(filePath);

		filePath = filePath.toLowerCase().endsWith(".json") ? filePath : filePath + ".json";

		File file = new File(filePath);

		if(!file.exists() || !file.isFile())
			throw new InaccessibleFileException();

		if(!file.canRead())
			throw new UnreadableFileException();

		if(requiresWrite && !file.canWrite())
			throw new UnwritableFileException();

		return filePath;
	}

	private JsonObject readJsonObject(String filePath) throws IOException
	{
		try(FileReader reader = new FileReader(filePath))
		{
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
		catch(IllegalStateException | JsonSyntaxException e)
		{
			throw new InvalidJsonStateException();
		}
	}

	private void writeJsonObject(String filePath, JsonObject json) throws IOException
	{
		try(FileWriter writer = new FileWriter(filePath))
		{
			gson.toJson(json, writer);
		}
		catch(IllegalStateException | JsonSyntaxException e)
		{
			throw new InvalidJsonStateException();
		}
	}
}
