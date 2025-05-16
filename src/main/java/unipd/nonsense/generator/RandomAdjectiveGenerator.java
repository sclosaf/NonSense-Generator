package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Adjective;

import unipd.nonsense.util.LoggerManager;

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

	private static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	private static String adjectivesPath = "target" + File.separator + "resources" + File.separator + "adjectives.json";
	private static List<String> keys = List.of("adjectives");

	private LoggerManager logger = new LoggerManager(RandomAdjectiveGenerator.class);

	public RandomAdjectiveGenerator() throws IOException
	{
		this.adjectives = new ArrayList<>();
		this.random = new Random();

		try
		{
			loadAdjectives();
			JsonUpdater.addObserver(this);
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize RandomAdjectiveGenerator", e);
			throw e;
		}
	}

	private void loadAdjectives() throws IOException
	{
		for(String key : keys)
		{
			List<String> jsonList = jsonHandler.readListFromJson(adjectivesPath, key);

			if(jsonList != null)
			{
				List<Adjective> adjectiveList = jsonList.stream().map(adjective -> new Adjective(adjective)).collect(Collectors.toList());
				adjectives = adjectiveList;

			}
			else
				logger.logWarn("loadAdjectives: No adjectives found for key: " + key);
		}
	}

	public Adjective getRandomAdjective()
	{

		if(adjectives.isEmpty())
		{
			logger.logError("getRandomAdjective: Adjectives list is empty");
			return null;
		}

		int randomIndex = random.nextInt(adjectives.size());
		Adjective selected = adjectives.get(randomIndex);

		return selected;
	}

	@Override
	public void onJsonUpdate() throws IOException
	{

		try
		{
			loadAdjectives();
		}
		catch (IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload adjectives", e);
			throw e;
		}
	}
}
