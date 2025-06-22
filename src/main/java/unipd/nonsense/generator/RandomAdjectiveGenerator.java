package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Adjective;

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
 * A generator for retrieving random adjectives from a JSON data source.
 * <p>
 * This class implements {@code JsonUpdateObserver} to dynamically reload adjectives
 * when the underlying JSON file changes. It provides thread-safe access to a pool
 * of adjectives loaded from a JSON file.
 * </p>
 *
 * <p>Example usage:
 * <pre>{@code
 * try(RandomAdjectiveGenerator generator = new RandomAdjectiveGenerator())
 * {
 *     Adjective randomAdj = generator.getRandomAdjective();
 *     System.out.println("Random adjective: " + randomAdj.getAdjective());
 * }
 * catch(IOException e)
 * {
 *     // Handle exception
 * }
 * }</pre>
 * </p>
 *
 * @see JsonUpdateObserver
 * @see Adjective
 */
public class RandomAdjectiveGenerator implements JsonUpdateObserver
{
	/**
	 * The list of adjectives loaded from the JSON file.
	 * <p>
	 * This list is:
	 * <ul>
	 *	<li>Initialized during construction</li>
	 *	<li>Updated when JSON file changes</li>
	 *	<li>Thread-safe for read operations</li>
	 * </ul>
	 * </p>
	 */
	private List<Adjective> adjectives;

	/**
	 * Random number generator instance for selecting adjectives.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Static to ensure better randomness across instances</li>
	 *	<li>Used in {@code getRandomAdjective()} method</li>
	 *	<li>Thread-safe for concurrent access</li>
	 * </ul>
	 * </p>
	 */
	private static Random random;

	/**
	 * Singleton instance handler for JSON file operations.
	 * <p>
	 * Features:
	 * <ul>
	 *	<li>Provides thread-safe file access</li>
	 *	<li>Handles JSON parsing and serialization</li>
	 *	<li>Shared across all instances</li>
	 * </ul>
	 * </p>
	 */
	private final static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	/**
	 * Default path to the adjectives JSON file.
	 * <p>
	 * Default location: {@code target/resources/adjectives.json}
	 * </p>
	 */
	private static String adjectivesPath = "target" + File.separator + "resources" + File.separator + "adjectives.json";

	/**
	 * JSON keys used to locate adjectives in the file.
	 * <p>
	 * Currently only uses the "adjectives" key.
	 * </p>
	 */
	private final static List<String> keys = List.of("adjectives");

	/**
	 * Logger instance for tracking operations and errors.
	 * <p>
	 * Configured to log messages from {@code RandomAdjectiveGenerator} class.
	 * </p>
	 */
	private LoggerManager logger = new LoggerManager(RandomAdjectiveGenerator.class);

	/**
	 * Constructs a generator using the default JSON file path.
	 *
	 * @throws IOException	if the adjectives file cannot be read or parsed
	 */
	public RandomAdjectiveGenerator() throws IOException
	{
		logger.logTrace("Starting initialization");

		this.adjectives = new ArrayList<>();
		this.random = new Random();
		this.adjectivesPath = "target" + File.separator + "resources" + File.separator + "adjectives.json";


		try
		{
			logger.logTrace("Loading adjectives from file");
			loadAdjectives();
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
	 * @param filePath	Path to the adjectives JSON file
	 * @throws IOException	if the specified file cannot be read or parsed
	 */
	public RandomAdjectiveGenerator(String filePath) throws IOException
	{
		logger.logTrace("Starting initialization");

		this.adjectives = new ArrayList<>();
		this.random = new Random();
		this.adjectivesPath = filePath;

		try
		{
			logger.logTrace("Loading adjectives from file");
			loadAdjectives();
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
	 * Loads adjectives from the configured JSON file.
	 * <p>
	 * Operation details:
	 * <ul>
	 *	<li>Clears existing adjectives list</li>
	 *	<li>Reads JSON file using {@code jsonHandler}</li>
	 *	<li>Converts strings to {@code Adjective} objects</li>
	 *	<li>Handles empty/missing data gracefully</li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException	if file reading fails
	 */
	private void loadAdjectives() throws IOException
	{
		logger.logTrace("loadAdjectives: Starting to load adjectives");

		for(String key : keys)
		{
			logger.logDebug("loadAdjectives: Loading adjectives for key: " + key);
			List<String> jsonList = jsonHandler.readListFromJson(adjectivesPath, key);

			if(jsonList != null && !jsonList.isEmpty())
			{
				logger.logDebug("loadAdjectives: Found " + jsonList.size() + " adjectives for key: " + key);

				List<Adjective> adjectiveList = jsonList.stream().map(adjective -> new Adjective(adjective)).collect(Collectors.toList());
				adjectives = adjectiveList;

			}
			else
				logger.logWarn("loadAdjectives: No adjectives found for key: " + key);
		}

		logger.logTrace("loadAdjectives: Completed loading adjectives");
	}

	/**
	 * Retrieves a random adjective from the loaded pool.
	 * <p>
	 * Selection process:
	 * <ul>
	 *	<li>Uses uniform random distribution</li>
	 *	<li>Logs selected adjective for debugging</li>
	 *	<li>Throws exception if pool is empty</li>
	 * </ul>
	 * </p>
	 *
	 * @return					Randomly selected {@code Adjective}
	 * @throws InvalidListException	if no adjectives are loaded
	 */
	public Adjective getRandomAdjective()
	{
		logger.logTrace("getRandomAdjective: Starting to get random adjective");

		if(adjectives.isEmpty())
		{
			logger.logError("getRandomAdjective: Adjectives list is empty");
			throw new InvalidListException();
		}

		int randomIndex = random.nextInt(adjectives.size());
		Adjective selected = adjectives.get(randomIndex);

		logger.logDebug("getRandomAdjective: Selected adjective: " + selected.getAdjective());
		logger.logTrace("getRandomAdjective: Completed getting random adjective");

		return selected;
	}

	/**
	 * Performs cleanup by unregistering from JSON updates.
	 * <p>
	 * Should be called when the generator is no longer needed to prevent memory leaks.
	 * </p>
	 */
	public void cleanup()
	{
		logger.logTrace("cleanup: Starting cleanup");
		JsonUpdater.removeObserver(this);
		logger.logTrace("cleanup: Completed cleanup");
	}

	/**
	 * Handles JSON file update notifications by reloading adjectives.
	 * <p>
	 * Implementation of {@code JsonUpdateObserver} interface that:
	 * <ul>
	 *	<li>Triggers on external file changes</li>
	 *	<li>Reloads the adjectives list</li>
	 *	<li>Maintains consistency with source data</li>
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
			logger.logTrace("onJsonUpdate: Reloading adjectives");
			loadAdjectives();
			logger.logTrace("onJsonUpdate: Successfully reloaded adjectives");
		}
		catch(IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload adjectives", e);
			throw e;
		}
	}
}
