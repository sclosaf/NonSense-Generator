package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Noun.Number;
import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.exceptions.InvalidListException;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import java.util.stream.Collectors;

public class RandomNounGenerator implements JsonUpdateObserver
{
	private Map<Number, List<Noun>> nouns;
	private static Random random;

	private final static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	private final static String nounsPath = "target" + File.separator + "resources" + File.separator + "nouns.json";
	private final static List<String> keys = List.of("singularNouns", "pluralNouns");
	private LoggerManager logger = new LoggerManager(RandomNounGenerator.class);

	public RandomNounGenerator() throws IOException
	{
		logger.logTrace("Starting initialization");
		this.nouns = new HashMap<>();
		this.random = new Random();

		try
		{
			logger.logTrace("Loading nouns from file");
			loadNouns();
			JsonUpdater.addObserver(this);
			logger.logTrace("Successfully initialized");
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize", e);
			throw e;
		}
	}

	private void loadNouns() throws IOException
	{
		logger.logTrace("loadNouns: Starting to load nouns");

		for(String key : keys)
		{
			logger.logDebug("loadNouns: Processing key: " + key);
			Number num;

			if(key.equals(keys.get(0)))
				num = Number.SINGULAR;
			else
				num = Number.PLURAL;

			nouns.computeIfAbsent(num, k -> new ArrayList<>());

			List<String> jsonList = jsonHandler.readListFromJson(nounsPath, key);

			if(jsonList != null && !jsonList.isEmpty())
			{
				logger.logDebug("loadNouns: Found " + jsonList.size() + " nouns for key: " + key);
				List<Noun> nounList = jsonList.stream().map(noun -> new Noun(noun, num)).collect(Collectors.toList());
				nouns.put(num, nounList);
			}
			else
				logger.logWarn("loadNouns: No nouns found for key: " + key);
		}

		logger.logTrace("loadNouns: Completed loading nouns");
	}

	public Noun getRandomNoun()
	{
		logger.logTrace("getRandomNoun: Starting to get random noun");

		Number[] nums = Number.values();
		Number randomNumber = nums[random.nextInt(nums.length)];

		logger.logDebug("getRandomNoun: Selected number type: " + randomNumber);

		Noun result = getRandomNoun(randomNumber);

		logger.logTrace("getRandomNoun: Completed getting random noun");
		return result;
	}

	public Noun getRandomNoun(Number num)
	{
		logger.logDebug("getRandomNoun: Starting to get random noun for number: " + num);

		List<Noun> nounList = nouns.get(num);

		if(nounList == null || nounList.isEmpty())
		{
			logger.logError("getRandomNoun: No nouns available for number: " + num);
			throw new InvalidListException();
		}

		int randomIndex = random.nextInt(nounList.size());
		Noun selected = nounList.get(randomIndex);

		logger.logDebug("getRandomNoun: Selected noun: " + selected.getNoun());
		logger.logTrace("getRandomNoun: Completed getting random noun");

		return selected;
	}

	public void cleanup()
	{
		logger.logTrace("cleanup: Starting cleanup");
		JsonUpdater.removeObserver(this);
		logger.logTrace("cleanup: Completed cleanup");
	}

	@Override
	public void onJsonUpdate() throws IOException
	{
		logger.logTrace("onJsonUpdate: Starting JSON update handling");

		try
		{
			logger.logTrace("onJsonUpdate: Reloading nouns");
			loadNouns();
			logger.logTrace("onJsonUpdate: Successfully reloaded nouns");
		}
		catch(IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload nouns", e);
			throw e;
		}
	}
}
