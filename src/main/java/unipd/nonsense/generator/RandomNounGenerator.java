package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Noun.Number;

import unipd.nonsense.exceptions.InvalidListException;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import java.util.stream.Collectors;

public class RandomNounGenerator implements JsonUpdateObserver
{
	private Map<Number, List<Noun>> nouns;
	private static Random random;

	private static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	private static String nounsPath = "nouns.json";
	private static List<String> keys = List.of("singularNouns", "pluralNouns");

	public RandomNounGenerator() throws IOException
	{
		this.nouns = new HashMap<>();
		this.random = new Random();

		loadNouns();
		JsonUpdater.addObserver(this);
	}

	private void loadNouns() throws IOException
	{
		for(String key : keys)
		{
			Number num;

			if(key.equals(keys.get(0)))
				num = Number.SINGULAR;
			else
				num = Number.PLURAL;

			nouns.computeIfAbsent(num, k -> new ArrayList<>());

			List<String> jsonList = jsonHandler.readListFromJson(nounsPath, key);

			if(jsonList != null)
			{
				List<Noun> nounList = jsonList.stream().map(noun -> new Noun(noun, num)).collect(Collectors.toList());
				nouns.put(num, nounList);
			}
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
			throw new InvalidListException();

		int randomIndex = random.nextInt(nounList.size());
		return nounList.get(randomIndex);
	}

	@Override
	public void onJsonUpdate() throws IOException
	{
		loadNouns();
	}
}
