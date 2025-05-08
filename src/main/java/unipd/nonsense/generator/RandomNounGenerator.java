package unipd.nonsense.generator;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Noun;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

public class RandomNounGenerator
{
	private static Map<String, List<Noun>> nouns;
	private static Random random;

	private static JsonFileHandler jsonHandler;

	private static String nounsPath = "nouns.json";
	private static List<String> keys = List.of("singularNouns", "pluralNouns");

	public RandomNounGenerator() throws IOException
	{
		this.nouns = new HashMap<>();
		this.random = new Random();
		this.jsonHandler = JsonFileHandler.getInstance();

		loadNouns();
	}

	private void loadNouns() throws IOException
	{
		for(String key : keys)
		{
			List<String> jsonList = jsonHandler.readListFromJson(nounsPath, key);

			nouns.computeIfAbsent(key, k -> new ArrayList<>());

			Noun.Number num;

			if(key == keys.get(0))
				num = Noun.Number.SINGULAR;
			else
				num = Noun.Number.PLURAL;

			for(String element : jsonList)
				this.nouns.get(key).add(new Noun(element, num));
		}
	}

	public Noun getRandomNoun(Noun.Number num)
	{
		if(nouns.isEmpty())
			throw new IllegalStateException("No nouns loaded");

		String key;
		List<Noun> nounList;
		int randomIndex;

		switch(num)
		{
			case SINGULAR:
				key = keys.get(0);
				nounList = nouns.get(key);
				randomIndex = random.nextInt(nounList.size());
				return nounList.get(randomIndex);
			case PLURAL:
				key = keys.get(1);
				nounList = nouns.get(key);
				randomIndex = random.nextInt(nounList.size());
				return nounList.get(randomIndex);
			default:
				throw new IllegalArgumentException();
		}
	}

}
