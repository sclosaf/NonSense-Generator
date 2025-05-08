package unipd.nonsense.generator;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Adjective;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

public class RandomAdjectiveGenerator
{
	private static List<Adjective> adjectives;
	private static Random random;

	private static JsonFileHandler jsonHandler;

	private static String adjectivesPath = "adjectives.json";
	private static List<String> keys = List.of("adjective");

	public RandomAdjectiveGenerator() throws IOException
	{
		this.adjectives = new ArrayList<>();
		this.random = new Random();
		this.jsonHandler = JsonFileHandler.getInstance();

		loadAdjectives();
	}

	private void loadAdjectives() throws IOException
	{
		for(String key : keys)
		{
			List<String> jsonList = jsonHandler.readListFromJson(adjectivesPath, key);

			for(String element : jsonList)
				this.adjectives.add(new Adjective(element));
		}
	}

	public Adjective getRandomAdjective()
	{
		int randomIndex = random.nextInt(adjectives.size());
		return adjectives.get(randomIndex);
	}
}
