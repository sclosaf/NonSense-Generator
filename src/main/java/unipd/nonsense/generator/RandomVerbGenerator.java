package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Verb.Tense;

import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.exceptions.InvalidListException;

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import java.util.stream.Collectors;

public class RandomVerbGenerator implements JsonUpdateObserver
{
	private Map<Tense, List<Verb>> verbs;
	private static Random random;

	private final static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	private static String verbsPath = "target" + File.separator + "resources" + File.separator + "verbs.json";
	private final static List<String> keys = List.of("pastVerbs", "presentVerbs", "futureVerbs");
	private LoggerManager logger = new LoggerManager(RandomVerbGenerator.class);

	public RandomVerbGenerator() throws IOException
	{
		logger.logTrace("Starting initialization");

		this.verbs = new HashMap<>();
		this.random = new Random();
		this.verbsPath = "target" + File.separator + "resources" + File.separator + "verbs.json";

		try
		{
			logger.logTrace("Loading verbs from file");
			loadVerbs();
			JsonUpdater.addObserver(this);
			logger.logTrace("Successfully initialized");
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize", e);
			throw e;
		}
	}

	public RandomVerbGenerator(String filePath) throws IOException
	{
		logger.logTrace("Starting initialization");

		this.verbs = new HashMap<>();
		this.random = new Random();
		this.verbsPath = filePath;

		try
		{
			logger.logTrace("Loading verbs from file");
			loadVerbs();
			JsonUpdater.addObserver(this);
			logger.logTrace("Successfully initialized");
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize", e);
			throw e;
		}
	}

	private void loadVerbs() throws IOException
	{
		logger.logTrace("loadVerbs: Starting to load verbs");

		for(String key : keys)
		{
			logger.logDebug("loadVerbs: Processing key: " + key);
			Tense tense;

			if(key.equals(keys.get(0)))
				tense = Tense.PAST;
			else if(key.equals(keys.get(1)))
				tense = Tense.PRESENT;
			else
				tense = Tense.FUTURE;

			verbs.computeIfAbsent(tense, k -> new ArrayList<>());

			List<String> jsonList = jsonHandler.readListFromJson(verbsPath, key);

			if(jsonList != null && !jsonList.isEmpty())
			{
				logger.logDebug("loadVerbs: Found " + jsonList.size() + " verbs for key: " + key);
				List<Verb> verbList = jsonList.stream().map(verb -> new Verb(verb, tense)).collect(Collectors.toList());
				verbs.put(tense, verbList);

			}
			else
				logger.logWarn("loadVerbs: No verbs found for key: " + key);
		}

		logger.logTrace("loadVerbs: Completed loading verbs");
	}

	public Verb getRandomVerb()
	{
		logger.logTrace("getRandomVerb: Starting to get random verb");

		Tense[] tenses = Tense.values();
		Tense randomTense = tenses[random.nextInt(tenses.length)];

		logger.logDebug("getRandomVerb: Selected tense: " + randomTense);

		Verb result = getRandomVerb(randomTense);

		logger.logTrace("getRandomVerb: Completed getting random verb");
		return result;
	}

	public Verb getRandomVerb(Tense tense)
	{
		logger.logTrace("getRandomVerb: Starting to get random verb for tense: " + tense);

		List<Verb> verbList = verbs.get(tense);

		if(verbList == null || verbList.isEmpty())
		{
			logger.logError("getRandomVerb: No verbs available for tense: " + tense);
			throw new InvalidListException();
		}

		int randomIndex = random.nextInt(verbList.size());
		Verb selected = verbList.get(randomIndex);

		logger.logDebug("getRandomVerb: Selected verb: " + selected.getVerb());
		logger.logTrace("getRandomVerb: Completed getting random verb");

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
			logger.logTrace("onJsonUpdate: Reloading verbs");
			loadVerbs();
			logger.logTrace("onJsonUpdate: Successfully reloaded verbs");
		}
		catch(IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload verbs", e);
			throw e;
		}
	}
}
