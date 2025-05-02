package it.nonsense.util;

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

import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.lang.IllegalStateException;
import com.google.gson.JsonSyntaxException;

public final class JsonFileHandler
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
		if(key == null || filePath == null)
			throw new IllegalArgumentException();

		filePath = ensureJsonExtension(filePath);
		validateFileExists(filePath);

		lock.writeLock().lock();

		try
		{
			JsonObject json = readJsonObject(filePath);

			if(!json.has(key) || !json.get(key).isJsonArray())
				throw new IllegalArgumentException();

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
		if(key == null || filePath == null)
			throw new IllegalArgumentException();

		filePath = ensureJsonExtension(filePath);
		validateFileExists(filePath);

		if(index < 0)
			throw new IndexOutOfBoundsException();

		lock.readLock().lock();

		try
		{
			JsonObject json = readJsonObject(filePath);

			if(!json.has(key) || !json.get(key).isJsonArray())
				throw new IllegalArgumentException();

			JsonArray elements = json.getAsJsonArray(key);

			if(index >= elements.size())
				throw new IndexOutOfBoundsException();

			JsonElement element = elements.get(index);

			if(!element.isJsonPrimitive())
				throw new IllegalArgumentException();

			return element.getAsString();
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	public List<String> readListFromJson(String filePath, String key) throws IOException, IllegalArgumentException
	{
		if(key == null || filePath == null)
			throw new IllegalArgumentException();

		filePath = ensureJsonExtension(filePath);
		validateFileExists(filePath);

		lock.readLock().lock();

		try
		{
			JsonObject json = readJsonObject(filePath);

			if(!json.has(key) || !json.get(key).isJsonArray())
				throw new IllegalArgumentException();

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
		if(key == null || filePath == null)
			throw new IllegalArgumentException();

		filePath = ensureJsonExtension(filePath);
		validateFileExists(filePath);

		lock.readLock().lock();

		try
		{
			JsonObject json = readJsonObject(filePath);
			String[] keys = key.split("\\.");

			for(String k : keys)
			{
				if(!json.has(k))
					return false;

				JsonElement element = json.get(k);

				if(element.isJsonObject())
					json = element.getAsJsonObject();
				else if(k != keys[keys.length - 1])
					return false;
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
		if(filePath == null)
			throw new IllegalArgumentException();

		filePath = ensureJsonExtension(filePath);
		validateFileExists(filePath);

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

	private void validateFileExists(String filePath) throws IllegalArgumentException
	{
		File file = new File(filePath);

		if(!file.exists() || !file.isFile())
			throw new IllegalArgumentException();
	}

	private String ensureJsonExtension(String filePath)
	{
		return filePath.toLowerCase().endsWith(".json") ? filePath : filePath + ".json";
	}

	private JsonObject readJsonObject(String filePath) throws IOException
	{
		try(FileReader reader = new FileReader(filePath))
		{
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
		catch(IllegalStateException | JsonSyntaxException e)
		{
			throw new IllegalArgumentException();
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
			throw new IllegalArgumentException();
		}
	}
}
