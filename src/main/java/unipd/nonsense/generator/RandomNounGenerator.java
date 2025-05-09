package unipd.nonsense.generator;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Noun.Number;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

public class RandomNounGenerator
{
	private static Map<Number, List<Noun>> nouns;
	private static Random random;

	private static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	private static String nounsPath = "nouns.json";
	private static List<String> keys = List.of("singularNouns", "pluralNouns");

	public RandomNounGenerator() throws IOException
	{
		this.nouns = new HashMap<>();
		this.random = new Random();

		loadNouns();
	}

	private void loadNouns() throws IOException
	{
		for(String key : keys)
		{
			Number num;

			if(key == keys.get(0))
				num = Number.SINGULAR;
			else
				num = Number.PLURAL;

			nouns.computeIfAbsent(num, k -> new ArrayList<>());

			List<String> jsonList = jsonHandler.readListFromJson(nounsPath, key);

			for(String element : jsonList)
				this.nouns.get(num).add(new Noun(element, num));
		}
	}

	public Noun getRandomNoun()
	{
		Number[] nums = Number.values();
		Number randomNumber = nums[random.nextInt(nums.length)];

		return getRandomNoun(randomNumber);
	}

	public Noun getRandomNoun(Number num)
	{
		List<Noun> nounList = nouns.get(num);

		if(nounList == null || nounList.isEmpty())
			throw new IllegalStateException("No nouns loaded for number: " + num);

		int randomIndex = random.nextInt(nounList.size());
		return nounList.get(randomIndex);
	}
}
