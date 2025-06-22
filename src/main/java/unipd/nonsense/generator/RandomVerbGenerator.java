package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Tense;
import unipd.nonsense.model.Pair;

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

/**
 * A generator for retrieving random verbs from a JSON data source, categorized by tense and grammatical number.
 * <p>
 * This class implements {@code JsonUpdateObserver} to dynamically reload verbs when the underlying JSON file changes.
 * It maintains separate verb pools for different combinations of tenses ({@code Tense}) and numbers ({@code Number}).
 * </p>
 *
 * <p>Example usage:
 * <pre>{@code
 *try(RandomVerbGenerator generator = new RandomVerbGenerator())
 *{
 *    Verb randomVerb = generator.getRandomVerb();
 *    System.out.println("Selected verb: " + randomVerb.getVerb());
 *
 *    Verb futureVerb = generator.getRandomVerb(Tense.FUTURE);
 *}
 *catch(IOException e)
 *{
 *    // Handle exception
 *}
 *}</pre>
 * </p>
 *
 * @see JsonUpdateObserver
 * @see Verb
 * @see Tense
 * @see Number
 */
public class RandomVerbGenerator implements JsonUpdateObserver
{
	/**
	 * Map storing verbs categorized by tense and grammatical number combinations.
	 * <p>
	 * Structure:
	 * <ul>
	 *	<li>Key: {@code Pair<Tense, Number>} representing a tense-number combination</li>
	 *	<li>Value: List of {@code Verb} objects matching the key's criteria</li>
	 *	<li>Initialized during construction</li>
	 * </ul>
	 * </p>
	 */
	private Map<Pair<Tense, Number>, List<Verb>> verbs;

	/**
	 * Random number generator instance for verb selection.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Static for consistent randomness across instances</li>
	 *	<li>Thread-safe for concurrent access</li>
	 *	<li>Used in all verb selection methods</li>
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
	 * Path to the verbs JSON data file.
	 * <p>
	 * Default location: {@code target/resources/verbs.json}
	 * </p>
	 */
	private static String verbsPath = "target" + File.separator + "resources" + File.separator + "verbs.json";

	/**
	 * Mapping between tense-number combinations and their corresponding JSON keys.
	 * <p>
	 * Contains all possible combinations of:
	 * <ul>
	 *	<li>{@code Tense.PAST}, {@code Tense.PRESENT}, {@code Tense.FUTURE}</li>
	 *	<li>{@code Number.SINGULAR} and {@code Number.PLURAL}</li>
	 * </ul>
	 */
	private final static Map<Pair<Tense, Number>, String> keys = Map.of(
		new Pair<>(Tense.PAST, Number.SINGULAR), "pastSingularVerbs",
		new Pair<>(Tense.PAST, Number.PLURAL), "pastPluralVerbs",
		new Pair<>(Tense.PRESENT, Number.SINGULAR), "presentSingularVerbs",
		new Pair<>(Tense.PRESENT, Number.PLURAL), "presentPluralVerbs",
		new Pair<>(Tense.FUTURE, Number.SINGULAR), "futureSingularVerbs",
		new Pair<>(Tense.FUTURE, Number.PLURAL), "futurePluralVerbs"
	);

	/**
	 * Logger instance for operation tracking.
	 * <p>
	 * Configured to log messages from {@code RandomVerbGenerator} class.
	 * </p>
	 */
	private LoggerManager logger = new LoggerManager(RandomVerbGenerator.class);

	/**
	 * Constructs a generator using the default JSON file path.
	 *
	 * @throws IOException	if the verbs file cannot be read or parsed
	 */
	public RandomVerbGenerator() throws IOException
	{
		logger.logTrace("Starting initialization");

		this.verbs = new HashMap<>();
		this.random = new Random();
		this.verbsPath = "target" + File.separator + "resources" + File.separator + "verbs.json";

		try
		{
			logger.logTrace("Loading verbs from file");
			loadVerbs();
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
	 * @param filePath	Path to the verbs JSON file
	 * @throws IOException	if the specified file cannot be read or parsed
	 */
	public RandomVerbGenerator(String filePath) throws IOException
	{
		logger.logTrace("Starting initialization");

		this.verbs = new HashMap<>();
		this.random = new Random();
		this.verbsPath = filePath;

		try
		{
			logger.logTrace("Loading verbs from file");
			loadVerbs();
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
	 * Loads verbs from the configured JSON file into categorized lists.
	 * <p>
	 * Operation details:
	 * <ul>
	 *	<li>Processes all tense-number combinations defined in {@code keys}</li>
	 *	<li>Creates {@code Verb} objects with proper tense and number associations</li>
	 *	<li>Maintains separate lists for each tense-number combination</li>
	 *	<li>Logs warnings for missing/empty verb lists</li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException	if file reading fails
	 */
	private void loadVerbs() throws IOException
	{
		logger.logTrace("loadVerbs: Starting to load verbs");

		for(Map.Entry<Pair<Tense, Number>, String> entry : keys.entrySet())
		{
			Pair<Tense, Number> pair = entry.getKey();
			String jsonKey = entry.getValue();

			logger.logDebug("loadVerbs: Processing key: " + jsonKey);

			verbs.computeIfAbsent(pair, k -> new ArrayList<>());

			List<String> jsonList = jsonHandler.readListFromJson(verbsPath, jsonKey);
			if (jsonList != null && !jsonList.isEmpty())
			{
				logger.logDebug("loadVerbs: Found " + jsonList.size() + " verbs for key: " + jsonKey);
				List<Verb> verbList = jsonList.stream().map(verb -> new Verb(verb, pair.getSecond(), pair.getFirst())).collect(Collectors.toList());
				verbs.put(pair, verbList);
			}
			else
				logger.logWarn("loadVerbs: No verbs found for key: " + jsonKey);
		}

		logger.logTrace("loadVerbs: Completed loading verbs");
	}

	/**
	 * Retrieves a completely random verb with random tense and number.
	 * <p>
	 * Selection process:
	 * <ul>
	 *	<li>Randomly selects between all available tenses</li>
	 *	<li>Randomly selects between singular/plural forms</li>
	 *	<li>Delegates to {@code getRandomVerb(Number, Tense)}</li>
	 *	<li>Logs the selected tense and number</li>
	 * </ul>
	 * </p>
	 *
	 * @return					Randomly selected {@code Verb}
	 * @throws InvalidListException	if no verbs are loaded for the selected combination
	 */
	public Verb getRandomVerb()
	{
		logger.logTrace("getRandomVerb: Starting to get random verb");

		Tense[] tenses = Tense.values();
		Tense randomTense = tenses[random.nextInt(tenses.length)];

		logger.logDebug("getRandomVerb: Selected tense: " + randomTense);

		Number[] numbers = Number.values();
		Number randomNumber = numbers[random.nextInt(numbers.length)];

		logger.logDebug("getRandomVerb: Selected number: " + randomNumber);

		Verb result = getRandomVerb(randomNumber, randomTense);

		logger.logTrace("getRandomVerb: Completed getting random verb");
		return result;
	}

	/**
	 * Retrieves a random verb with specified grammatical number and random tense.
	 * <p>
	 * Selection process:
	 * <ul>
	 *	<li>Randomly selects between all available tenses</li>
	 *	<li>Uses the specified grammatical number</li>
	 *	<li>Delegates to {@code getRandomVerb(Number, Tense)}</li>
	 *	<li>Logs the selected tense</li>
	 * </ul>
	 * </p>
	 *
	 * @param number			The grammatical number to select from
	 * @return					Randomly selected {@code Verb}
	 * @throws InvalidListException	if no verbs are loaded for the specified number
	 */
	public Verb getRandomVerb(Number number)
	{
		logger.logTrace("getRandomVerb: Starting to get random verb for number " + number);

		Tense[] tenses = Tense.values();
		Tense randomTense = tenses[random.nextInt(tenses.length)];

		logger.logDebug("getRandomVerb : Selected tense: " + randomTense);

		Verb result = getRandomVerb(number, randomTense);

		logger.logTrace("getRandomVerb: Completed getting random verb");
		return result;
	}

	/**
	 * Retrieves a random verb with specified tense and random grammatical number.
	 * <p>
	 * Selection process:
	 * <ul>
	 *	<li>Uses the specified tense</li>
	 *	<li>Randomly selects between singular/plural forms</li>
	 *	<li>Delegates to {@code getRandomVerb(Number, Tense)}</li>
	 *	<li>Logs the selected number</li>
	 * </ul>
	 * </p>
	 *
	 * @param tense			The tense to select from
	 * @return				Randomly selected {@code Verb}
	 * @throws InvalidListException	if no verbs are loaded for the specified tense
	 */
	public Verb getRandomVerb(Tense tense)
	{
		logger.logTrace("getRandomVerb: Starting to get random verb for tense " + tense);

		Number[] numbers = Number.values();
		Number randomNumber = numbers[random.nextInt(numbers.length)];

		logger.logDebug("getRandomVerb: Selected number: " + randomNumber);

		Verb result = getRandomVerb(randomNumber, tense);

		logger.logTrace("getRandomVerb: Completed getting random verb");
		return result;
	}

	/**
	 * Retrieves a random verb with specified tense and grammatical number.
	 * <p>
	 * Selection process:
	 * <ul>
	 *	<li>Validates verb list availability</li>
	 *	<li>Uses uniform random distribution</li>
	 *	<li>Logs the selected verb</li>
	 * </ul>
	 * </p>
	 *
	 * @param number			The grammatical number to select from
	 * @param tense			The tense to select from
	 * @return				Randomly selected {@code Verb}
	 * @throws InvalidListException	if no verbs are loaded for the specified combination
	 */
	public Verb getRandomVerb(Number number, Tense tense)
	{
		logger.logTrace("getRandomVerb: Starting to get random verb for tense: " + tense + " and number: " + number);

		Pair<Tense, Number> chosen = new Pair<>(tense, number);

		List<Verb> verbList = verbs.get(chosen);

		if(verbList == null || verbList.isEmpty())
		{
			logger.logError("getRandomVerb: No verbs available for tense: " + tense);
			throw new InvalidListException();
		}

		int randomIndex = random.nextInt(verbList.size());
		Verb selected = verbList.get(randomIndex);

		logger.logDebug("getRandomVerb: Selected verb: " + selected.getVerb());
		logger.logTrace("getRandomVerb: Completed getting random verb");

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
	 * Handles JSON file update notifications by reloading verbs.
	 * <p>
	 * Implementation of {@code JsonUpdateObserver} that:
	 * <ul>
	 *	<li>Maintains data consistency with source file</li>
	 *	<li>Preserves tense-number categorization</li>
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
			logger.logTrace("onJsonUpdate: Reloading verbs");
			loadVerbs();
			logger.logTrace("onJsonUpdate: Successfully reloaded verbs");
		}
		catch(IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload verbs", e);
			throw e;
		}
	}
}
