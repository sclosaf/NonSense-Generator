package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Verb.Tense;

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

	public RandomVerbGenerator() throws IOException
	{
		this.verbs = new HashMap<>();
		this.random = new Random();

		loadVerbs();
		JsonUpdater.addObserver(this);
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
			throw new IllegalStateException("No verbs loaded for tense: " + tense);

		int randomIndex = random.nextInt(verbList.size());
		return verbList.get(randomIndex);
	}

	@Override
	public void onJsonUpdate()
	{
		try
		{
			loadVerbs();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
