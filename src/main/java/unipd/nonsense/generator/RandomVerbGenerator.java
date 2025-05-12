package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Verb.Tense;

import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.exceptions.InvalidListException;

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

	private static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	private static String verbsPath = "verbs.json";
	private static List<String> keys = List.of("pastVerbs", "presentVerbs", "futureVerbs");
	private LoggerManager logger = new LoggerManager(RandomVerbGenerator.class);

	public RandomVerbGenerator() throws IOException
	{
		logger.logInfo("Initializing RandomVerbGenerator");

		this.verbs = new HashMap<>();
		this.random = new Random();

		try
		{
			loadVerbs();
			JsonUpdater.addObserver(this);

			logger.logInfo("Initialization completed successfully");
			logger.logDebug("Loaded verbs counts - Past: " + (verbs.get(Tense.PAST) != null ? verbs.get(Tense.PAST).size() : 0) + ", Present: " + (verbs.get(Tense.PRESENT) != null ? verbs.get(Tense.PRESENT).size() : 0) + ", Future: " + (verbs.get(Tense.FUTURE) != null ? verbs.get(Tense.FUTURE).size() : 0));
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize RandomVerbGenerator", e);
			throw e;
		}
	}

	private void loadVerbs() throws IOException
	{
		logger.logInfo("loadVerbs: Loading verbs from JSON file");

		for(String key : keys)
		{
			Tense tense;

			if(key.equals(keys.get(0)))
				tense = Tense.PAST;
			else if(key.equals(keys.get(1)))
				tense = Tense.PRESENT;
			else
				tense = Tense.FUTURE;

			verbs.computeIfAbsent(tense, k -> new ArrayList<>());

			List<String> jsonList = jsonHandler.readListFromJson(verbsPath, key);

			if(jsonList != null)
			{
				List<Verb> verbList = jsonList.stream().map(verb -> new Verb(verb, tense)).collect(Collectors.toList());
				verbs.put(tense, verbList);

				logger.logDebug("loadVerbs: Loaded " + verbList.size() + " " + tense + " verbs");
			}
			else
				logger.logWarn("loadVerbs: No verbs found for key: " + key);
		}
	}

	public Verb getRandomVerb()
	{
		logger.logInfo("getRandomVerb: Selecting random verb with random tense");
		Tense[] tenses = Tense.values();
		Tense randomTense = tenses[random.nextInt(tenses.length)];

		logger.logDebug("getRandomVerb: Selected tense: " + randomTense);

		return getRandomVerb(randomTense);
	}

	public Verb getRandomVerb(Tense tense)
	{
		logger.logDebug("getRandomVerb: Selecting random " + tense + " verb");
		List<Verb> verbList = verbs.get(tense);

		if(verbList == null || verbList.isEmpty())
		{
			logger.logError("getRandomVerb: No " + tense + " verbs available");
			throw new InvalidListException();
		}

		int randomIndex = random.nextInt(verbList.size());
		Verb selected = verbList.get(randomIndex);

		logger.logDebug("getRandomVerb: Selected verb: " + selected.getVerb() + " (index " + randomIndex + " of " + verbList.size() + ")");
		return selected;
	}

	@Override
	public void onJsonUpdate() throws IOException
	{
		logger.logInfo("onJsonUpdate: JSON file updated, reloading verbs");

		try
		{
			loadVerbs();

			logger.logInfo("onJsonUpdate: Verbs reloaded successfully");

			logger.logDebug("Verbs count - Past:" + (verbs.get(Tense.PAST) != null ? verbs.get(Tense.PAST).size() : 0) + ", Present: " + (verbs.get(Tense.PRESENT) != null ? verbs.get(Tense.PRESENT).size() : 0) + ", Future: " + (verbs.get(Tense.FUTURE) != null ? verbs.get(Tense.FUTURE).size() : 0));
		}
		catch(IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload verbs", e);
			throw e;
		}
	}
}
