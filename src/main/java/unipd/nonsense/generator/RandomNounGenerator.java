package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Number;
import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.exceptions.InvalidListException;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import java.util.stream.Collectors;


/**
 * A generator for retrieving random nouns from a JSON data source, supporting singular/plural forms.
 * <p>
 * This class implements {@code JsonUpdateObserver} to dynamically reload nouns
 * when the underlying JSON file changes. It maintains separate noun pools for
 * {@code Number.SINGULAR} and {@code Number.PLURAL} forms.
 * </p>
 *
 * <p>Example usage:
 * <pre>{@code
 * try(RandomNounGenerator generator = new RandomNounGenerator())
 * {
 *     Noun randomNoun = generator.getRandomNoun();
 *     System.out.println("Random noun: " + randomNoun.getNoun());
 *
 *     Noun pluralNoun = generator.getRandomNoun(Number.PLURAL);
 * }
 * catch(IOException e)
 * {
 *     // Handle exception
 * }
 * }</pre>
 * </p>
 *
 * @see JsonUpdateObserver
 * @see Noun
 * @see Number
 */
public class RandomNounGenerator implements JsonUpdateObserver
{
	/**
	 * Map storing nouns categorized by their grammatical number.
	 * <p>
	 * Structure:
	 * <ul>
	 *	<li>Key: {@code Number} enum (SINGULAR/PLURAL)</li>
	 *	<li>Value: List of {@code Noun} objects</li>
	 *	<li>Initialized during construction</li>
	 * </ul>
	 * </p>
	 */
	private Map<Number, List<Noun>> nouns;

	/**
	 * Random number generator instance for noun selection.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Static for better randomness across instances</li>
	 *	<li>Thread-safe for concurrent access</li>
	 *	<li>Used in all noun selection methods</li>
	 * </ul>
	 * </p>
	 */
	private static Random random;

	/**
	 * Singleton JSON file handler instance.
	 * <p>
	 * Features:
	 * <ul>
	 *	<li>Centralized JSON parsing</li>
	 *	<li>Thread-safe file operations</li>
	 *	<li>Shared across all generator instances</li>
	 * </ul>
	 * </p>
	 */
	private final static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	/**
	 * Path to the nouns JSON data file.
	 * <p>
	 * Default location: {@code target/resources/nouns.json}
	 * </p>
	 */
	private static String nounsPath;

	/**
	 * Mapping between grammatical numbers and their JSON keys.
	 * <p>
	 * Contains:
	 * <ul>
	 *	<li>{@code Number.SINGULAR} → "singularNouns"</li>
	 *	<li>{@code Number.PLURAL} → "pluralNouns"</li>
	 * </ul>
	 * </p>
	 */
	private final static Map<Number, String> keys = Map.of(
		Number.SINGULAR, "singularNouns",
		Number.PLURAL, "pluralNouns"
	);

	/**
	 * Logger instance for operation tracking.
	 * <p>
	 * Configured to log messages from {@code RandomNounGenerator} class.
	 * </p>
	 */
	private LoggerManager logger = new LoggerManager(RandomNounGenerator.class);

	/**
	 * Constructs a generator using the default JSON file path.
	 *
	 * @throws IOException	if the nouns file cannot be read or parsed
	 */
	public RandomNounGenerator() throws IOException
	{
		logger.logTrace("Starting initialization");
		this.nouns = new HashMap<>();
		this.random = new Random();
		this.nounsPath = "target" + File.separator + "resources" + File.separator + "nouns.json";

		try
		{
			logger.logTrace("Loading nouns from file");
			loadNouns();
			JsonUpdater.addObserver(this);
			logger.logTrace("Successfully initialized");
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize", e);
			throw e;
		}
	}

	/**
	 * Constructs a generator with a custom JSON file path.
	 *
	 * @param filePath	Path to the nouns JSON file
	 * @throws IOException	if the specified file cannot be read or parsed
	 */
	public RandomNounGenerator(String filePath) throws IOException
	{
		logger.logTrace("Starting initialization");
		this.nouns = new HashMap<>();
		this.random = new Random();
		this.nounsPath = filePath;

		try
		{
			logger.logTrace("Loading nouns from file");
			loadNouns();
			JsonUpdater.addObserver(this);
			logger.logTrace("Successfully initialized");
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize", e);
			throw e;
		}
	}

	/**
	 * Loads nouns from the configured JSON file into categorized lists.
	 * <p>
	 * Operation details:
	 * <ul>
	 *	<li>Processes both singular and plural noun entries</li>
	 *	<li>Creates {@code Noun} objects with proper {@code Number} association</li>
	 *	<li>Maintains separate lists for each number type</li>
	 *	<li>Logs warnings for missing/empty noun lists</li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException	if file reading fails
	 */
	private void loadNouns() throws IOException
	{
		logger.logTrace("loadNouns: Starting to load nouns");

		for(Map.Entry<Number, String> entry : keys.entrySet())
		{
			Number num = entry.getKey();
			String jsonKey = entry.getValue();

			logger.logDebug("loadNouns: Processing key: " + jsonKey);
			nouns.computeIfAbsent(num, k -> new ArrayList<>());

			List<String> jsonList = jsonHandler.readListFromJson(nounsPath, jsonKey);

			if (jsonList != null && !jsonList.isEmpty())
			{
				logger.logDebug("loadNouns: Found " + jsonList.size() + " nouns for key: " + jsonKey);
				List<Noun> nounList = jsonList.stream()
					.map(noun -> new Noun(noun, num))
					.collect(Collectors.toList());

				nouns.put(num, nounList);
			}
			else
				logger.logWarn("loadNouns: No nouns found for key: " + jsonKey);
		}

		logger.logTrace("loadNouns: Completed loading nouns");
	}

	/**
	 * Retrieves a random noun with random grammatical number.
	 * <p>
	 * Selection process:
	 * <ul>
	 *	<li>Randomly selects between singular/plural forms</li>
	 *	<li>Delegates to {@code getRandomNoun(Number)}</li>
	 *	<li>Logs the selected number type</li>
	 * </ul>
	 * </p>
	 *
	 * @return					Randomly selected {@code Noun}
	 * @throws InvalidListException	if no nouns are loaded for the selected number type
	 */
	public Noun getRandomNoun()
	{
		logger.logTrace("getRandomNoun: Starting to get random noun");

		Number[] nums = Number.values();
		Number randomNumber = nums[random.nextInt(nums.length)];

		logger.logDebug("getRandomNoun: Selected number type: " + randomNumber);

		Noun result = getRandomNoun(randomNumber);

		logger.logTrace("getRandomNoun: Completed getting random noun");
		return result;
	}

	/**
	 * Retrieves a random noun with specified grammatical number.
	 * <p>
	 * Selection process:
	 * <ul>
	 *	<li>Uses uniform random distribution</li>
	 *	<li>Validates noun list availability</li>
	 *	<li>Logs the selected noun</li>
	 * </ul>
	 * </p>
	 *
	 * @param number			The grammatical number to select from
	 * @return					Randomly selected {@code Noun}
	 * @throws InvalidListException	if no nouns are loaded for the specified number
	 */
	public Noun getRandomNoun(Number number)
	{
		logger.logDebug("getRandomNoun: Starting to get random noun for number: " + number);

		List<Noun> nounList = nouns.get(number);

		if(nounList == null || nounList.isEmpty())
		{
			logger.logError("getRandomNoun: No nouns available for number: " + number);
			throw new InvalidListException();
		}

		int randomIndex = random.nextInt(nounList.size());
		Noun selected = nounList.get(randomIndex);

		logger.logDebug("getRandomNoun: Selected noun: " + selected.getNoun());
		logger.logTrace("getRandomNoun: Completed getting random noun");

		return selected;
	}

	/**
	 * Performs cleanup by unregistering from JSON updates.
	 * <p>
	 * Should be called when the generator is no longer needed to prevent:
	 * <ul>
	 *	<li>Memory leaks</li>
	 *	<li>Unnecessary file monitoring</li>
	 * </ul>
	 * </p>
	 */
	public void cleanup()
	{
		logger.logTrace("cleanup: Starting cleanup");
		JsonUpdater.removeObserver(this);
		logger.logTrace("cleanup: Completed cleanup");
	}

	/**
	 * Handles JSON file update notifications by reloading nouns.
	 * <p>
	 * Implementation of {@code JsonUpdateObserver} that:
	 * <ul>
	 *	<li>Maintains data consistency with source file</li>
	 *	<li>Preserves number categorization</li>
	 *	<li>Logs reloading process</li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException	if reloading fails
	 */
	@Override
	public void onJsonUpdate() throws IOException
	{
		logger.logTrace("onJsonUpdate: Starting JSON update handling");

		try
		{
			logger.logTrace("onJsonUpdate: Reloading nouns");
			loadNouns();
			logger.logTrace("onJsonUpdate: Successfully reloaded nouns");
		}
		catch(IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload nouns", e);
			throw e;
		}
	}
}
