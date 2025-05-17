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

	private final static String verbsPath = "target" + File.separator + "resources" + File.separator + "verbs.json";
	private final static List<String> keys = List.of("pastVerbs", "presentVerbs", "futureVerbs");
	private LoggerManager logger = new LoggerManager(RandomVerbGenerator.class);

	public RandomVerbGenerator() throws IOException
	{
		this.verbs = new HashMap<>();
		this.random = new Random();

		try
		{
			loadVerbs();
			JsonUpdater.addObserver(this);
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize RandomVerbGenerator", e);
			throw e;
		}
	}

	private void loadVerbs() throws IOException
	{

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

			}
			else
				logger.logWarn("loadVerbs: No verbs found for key: " + key);
		}
	}

	public Verb getRandomVerb()
	{
		Tense[] tenses = Tense.values();
		Tense randomTense = tenses[random.nextInt(tenses.length)];

		return getRandomVerb(randomTense);
	}

	public Verb getRandomVerb(Tense tense)
	{
		List<Verb> verbList = verbs.get(tense);

		if(verbList == null || verbList.isEmpty())
		{
			logger.logError("getRandomVerb: No " + tense + " verbs available");
			throw new InvalidListException();
		}

		int randomIndex = random.nextInt(verbList.size());
		Verb selected = verbList.get(randomIndex);

		return selected;
	}


	public void cleanup()
	{
		JsonUpdater.removeObserver(this);
	}

	@Override
	public void onJsonUpdate() throws IOException
	{
		try
		{
			loadVerbs();

		}
		catch(IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload verbs", e);
			throw e;
		}
	}
}
