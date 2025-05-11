package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Adjective;

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

	private static String adjectivesPath = "adjectives.json";
	private static List<String> keys = List.of("adjective");

	public RandomAdjectiveGenerator() throws IOException
	{
		this.adjectives = new ArrayList<>();
		this.random = new Random();

		loadAdjectives();
		JsonUpdater.addObserver(this);
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

		}
	}

	public Adjective getRandomAdjective()
	{
		int randomIndex = random.nextInt(adjectives.size());
		return adjectives.get(randomIndex);
	}

	@Override
	public void onJsonUpdate() throws IOException
	{
		loadAdjectives();
	}
}
