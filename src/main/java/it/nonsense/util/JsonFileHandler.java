package it.nonsense.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonIOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.lang.IndexOutOfBoundsException;

public class JsonFileHandler
{
	private static JsonFileHandler instance = new JsonFileHandler();

	private JsonFileHandler()
	{}

	public static JsonFileHandler getInstance()
	{
		return instance;
	}

	public static void appendItemToJson(String filePath, String key, String str) throws IOException
	{
		if(!instance.hasJsonKey(filePath, key))
			throw new IllegalArgumentException();

		JsonObject json;

		try(FileReader reader = new FileReader(filePath))
		{
			json = JsonParser.parseReader(reader).getAsJsonObject();
		}

		JsonArray elements = json.getAsJsonArray(key);

		for(JsonElement element : elements)
			if(element.getAsString().equals(str))
				return;

		elements.add(str);

		try(FileWriter writer = new FileWriter(filePath))
		{
			writer.write(json.toString());
		}
	}

	public static String readItemFromJson(String filePath, String key, int index) throws IOException
	{
		if(!instance.hasJsonKey(filePath, key))
			throw new IllegalArgumentException();

		if(index < 0)
			throw new IllegalArgumentException();

		try(FileReader reader = new FileReader(filePath))
		{
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray elements = json.getAsJsonArray(key);

			for(JsonElement element : elements)
				if(index-- == 0)
					return element.getAsString();
		}

		throw new IndexOutOfBoundsException();
	}

	public static List<String> readListFromJson(String filePath, String key) throws IOException
	{
		if(!instance.hasJsonKey(filePath, key))
			throw new IllegalArgumentException();

		try(FileReader reader = new FileReader(filePath))
		{
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray elements = json.getAsJsonArray(key);

			List<String> result = new ArrayList<String>();

			for (JsonElement element : elements)
				result.add(element.getAsString());

			return result;
		}
	}

	private static boolean hasJsonKey(String filePath, String key) throws IOException
	{
		if (!filePath.toLowerCase().endsWith(".json"))
			filePath += ".json";

		if(!(new File(filePath).exists()))
			throw new IllegalArgumentException();

		try(FileReader reader = new FileReader(filePath))
		{
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			String[] keys = key.split("\\.");

			for(String k : keys)
			{
				if(!json.has(k))
					return false;

				json = json.getAsJsonObject(k);
			}

			return true;
		}
	}
}
