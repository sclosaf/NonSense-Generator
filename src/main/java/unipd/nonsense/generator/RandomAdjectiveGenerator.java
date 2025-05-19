package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Adjective;

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

public class RandomAdjectiveGenerator implements JsonUpdateObserver
{
	private List<Adjective> adjectives;
	private static Random random;

	private final static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	private static String adjectivesPath = "target" + File.separator + "resources" + File.separator + "adjectives.json";
	private final static List<String> keys = List.of("adjectives");

	private LoggerManager logger = new LoggerManager(RandomAdjectiveGenerator.class);

	public RandomAdjectiveGenerator() throws IOException
	{
		logger.logTrace("Starting initialization");

		this.adjectives = new ArrayList<>();
		this.random = new Random();
		this.adjectivesPath = "target" + File.separator + "resources" + File.separator + "adjectives.json";


		try
		{
			logger.logTrace("Loading adjectives from file");
			loadAdjectives();
			JsonUpdater.addObserver(this);
			logger.logTrace("Successfully initialized");
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize", e);
			throw e;
		}
	}

	public RandomAdjectiveGenerator(String filePath) throws IOException
	{
		logger.logTrace("Starting initialization");

		this.adjectives = new ArrayList<>();
		this.random = new Random();
		this.adjectivesPath = filePath;

		try
		{
			logger.logTrace("Loading adjectives from file");
			loadAdjectives();
			JsonUpdater.addObserver(this);
			logger.logTrace("Successfully initialized");
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize", e);
			throw e;
		}
	}

	private void loadAdjectives() throws IOException
	{
		logger.logTrace("loadAdjectives: Starting to load adjectives");

		for(String key : keys)
		{
			logger.logDebug("loadAdjectives: Loading adjectives for key: " + key);
			List<String> jsonList = jsonHandler.readListFromJson(adjectivesPath, key);

			if(jsonList != null && !jsonList.isEmpty())
			{
				logger.logDebug("loadAdjectives: Found " + jsonList.size() + " adjectives for key: " + key);

				List<Adjective> adjectiveList = jsonList.stream().map(adjective -> new Adjective(adjective)).collect(Collectors.toList());
				adjectives = adjectiveList;

			}
			else
				logger.logWarn("loadAdjectives: No adjectives found for key: " + key);
		}

		logger.logTrace("loadAdjectives: Completed loading adjectives");
	}

	public Adjective getRandomAdjective()
	{
		logger.logTrace("getRandomAdjective: Starting to get random adjective");

		if(adjectives.isEmpty())
		{
			logger.logError("getRandomAdjective: Adjectives list is empty");
			throw new InvalidListException();
		}

		int randomIndex = random.nextInt(adjectives.size());
		Adjective selected = adjectives.get(randomIndex);

		logger.logDebug("getRandomAdjective: Selected adjective: " + selected.getAdjective());
		logger.logTrace("getRandomAdjective: Completed getting random adjective");

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
			logger.logTrace("onJsonUpdate: Reloading adjectives");
			loadAdjectives();
			logger.logTrace("onJsonUpdate: Successfully reloaded adjectives");
		}
		catch(IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload adjectives", e);
			throw e;
		}
	}
}
