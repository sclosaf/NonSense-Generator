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

	private static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	private static String nounsPath = "target" + File.separator + "resources" + File.separator + "nouns.json";
	private static List<String> keys = List.of("singularNouns", "pluralNouns");
	private LoggerManager logger = new LoggerManager(RandomNounGenerator.class);

	public RandomNounGenerator() throws IOException
	{
		logger.logInfo("Initializing RandomNounGenerator");
		this.nouns = new HashMap<>();
		this.random = new Random();

		try
		{
			loadNouns();
			JsonUpdater.addObserver(this);

			logger.logInfo("Initialization completed successfully");
			logger.logDebug("Loaded nouns counts - Singular: " + (nouns.get(Number.SINGULAR) != null ? nouns.get(Number.SINGULAR).size() : 0) + ", Plural: " + (nouns.get(Number.PLURAL) != null ? nouns.get(Number.PLURAL).size() : 0));
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize RandomNounGenerator", e);
			throw e;
		}
	}

	private void loadNouns() throws IOException
	{
		logger.logInfo("loadNouns: Loading nouns from JSON file");

		for(String key : keys)
		{
			Number num;

			if(key.equals(keys.get(0)))
				num = Number.SINGULAR;
			else
				num = Number.PLURAL;

			nouns.computeIfAbsent(num, k -> new ArrayList<>());

			List<String> jsonList = jsonHandler.readListFromJson(nounsPath, key);

			if(jsonList != null)
			{
				List<Noun> nounList = jsonList.stream().map(noun -> new Noun(noun, num)).collect(Collectors.toList());
				nouns.put(num, nounList);

				logger.logDebug("loadNouns: Loaded " + nounList.size() + " " + num + " nouns");
			}
			else
				logger.logWarn("loadNouns: No nouns found for key: " + key);
		}
	}

	public Noun getRandomNoun()
	{
		logger.logInfo("getRandomNoun: Selecting random noun with random number");

		Number[] nums = Number.values();
		Number randomNumber = nums[random.nextInt(nums.length)];

		logger.logDebug("getRandomNoun: Selected number: " + randomNumber);

		return getRandomNoun(randomNumber);
	}

	public Noun getRandomNoun(Number num)
	{
		logger.logDebug("getRandomNoun: Selecting random " + num + " noun");
		List<Noun> nounList = nouns.get(num);

		if(nounList == null || nounList.isEmpty())
		{
			logger.logError("getRandomNoun: No " + num + " nouns available");
			throw new InvalidListException();
		}

		int randomIndex = random.nextInt(nounList.size());
		Noun selected = nounList.get(randomIndex);

		logger.logDebug("getRandomNoun: Selected noun: " + selected.getNoun());
		return selected;
	}

	@Override
	public void onJsonUpdate() throws IOException
	{
		logger.logInfo("onJsonUpdate: JSON file updated, reloading nouns");

		try
		{
			loadNouns();
			logger.logInfo("onJsonUpdate: Nouns reloaded successfully");
			logger.logDebug("Reloaded nouns counts - Singular: " + (nouns.get(Number.SINGULAR) != null ? nouns.get(Number.SINGULAR).size() : 0) + ", Plural: " + (nouns.get(Number.PLURAL) != null ? nouns.get(Number.PLURAL).size() : 0));
		}
		catch (IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload nouns", e);
			throw e;
		}
	}
}
