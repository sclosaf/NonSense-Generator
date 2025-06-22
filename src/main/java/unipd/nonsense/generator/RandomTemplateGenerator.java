package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;
import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Template;
import unipd.nonsense.model.Number;
import unipd.nonsense.exceptions.InvalidListException;

import unipd.nonsense.util.LoggerManager;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * A generator for retrieving random text templates from a JSON data source, supporting singular/plural forms.
 * <p>
 * This class implements {@code JsonUpdateObserver} to dynamically reload templates
 * when the underlying JSON file changes. It maintains separate template pools for
 * different grammatical numbers ({@code Number.SINGULAR} and {@code Number.PLURAL}).
 * </p>
 *
 * <p>Example usage:
 * <pre>{@code
 *try(RandomTemplateGenerator generator = new RandomTemplateGenerator())
 *{
 *    Template randomTemplate = generator.getRandomTemplate();
 *    System.out.println("Template pattern: " + randomTemplate.getPattern());
 *
 *    Template pluralTemplate = generator.getRandomTemplate(Number.PLURAL);
 *}
 *catch(IOException e)
 *{
 *    // Handle exception
 *}
 *}</pre>
 * </p>
 *
 * @see JsonUpdateObserver
 * @see Template
 * @see Number
 */
public class RandomTemplateGenerator implements JsonUpdateObserver
{
	/**
	 * Map storing templates categorized by their grammatical number.
	 * <p>
	 * Structure:
	 * <ul>
	 *	<li>Key: {@code Number} enum (SINGULAR/PLURAL)</li>
	 *	<li>Value: List of {@code Template} objects</li>
	 *	<li>Initialized during construction</li>
	 * </ul>
	 * </p>
	 */
	private Map<Number, List<Template>> templates;

	/**
	 * Random number generator instance for template selection.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Static for better randomness across instances</li>
	 *	<li>Thread-safe for concurrent access</li>
	 *	<li>Used in all template selection methods</li>
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
	 * Path to the templates JSON data file.
	 * <p>
	 * Default location: {@code target/resources/templates.json}
	 * </p>
	 */
	private static String templatesPath;

	/**
	 * Mapping between grammatical numbers and their JSON keys.
	 * <p>
	 * Contains:
	 * <ul>
	 *	<li>{@code Number.SINGULAR} → "singularTemplates"</li>
	 *	<li>{@code Number.PLURAL} → "pluralTemplates"</li>
	 * </ul>
	 * </p>
	 */
	private final static Map<Number, String> keys = Map.of(
		Number.SINGULAR, "singularTemplates",
		Number.PLURAL, "pluralTemplates"
	);

	/**
	 * Logger instance for operation tracking.
	 * <p>
	 * Configured to log messages from {@code RandomTemplateGenerator} class.
	 * </p>
	 */
	private LoggerManager logger = new LoggerManager(RandomTemplateGenerator.class);

	/**
	 * Constructs a generator using the default JSON file path.
	 *
	 * @throws IOException	if the templates file cannot be read or parsed
	 */
	public RandomTemplateGenerator() throws IOException
	{
		logger.logTrace("Starting initialization");

		this.templates = new HashMap<>();
		this.random = new Random();
		this.templatesPath = "target" + File.separator + "resources" + File.separator + "templates.json";

		try
		{
			logger.logTrace("Loading templates from file");
			loadTemplates();
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
	 * @param filePath	Path to the templates JSON file
	 * @throws IOException	if the specified file cannot be read or parsed
	 */
	public RandomTemplateGenerator(String filePath) throws IOException
	{
		logger.logTrace("Starting initialization");

		this.templates = new HashMap<>();
		this.random = new Random();
		this.templatesPath = filePath;

		try
		{
			logger.logTrace("Loading templates from file");
			loadTemplates();
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
	 * Loads templates from the configured JSON file into categorized lists.
	 * <p>
	 * Operation details:
	 * <ul>
	 *	<li>Processes both singular and plural template entries</li>
	 *	<li>Creates {@code Template} objects with proper {@code Number} association</li>
	 *	<li>Maintains separate lists for each number type</li>
	 *	<li>Logs warnings for missing/empty template lists</li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException	if file reading fails
	 */
	private void loadTemplates() throws IOException
	{
		logger.logTrace("loadTemplates: Starting to load templates");

		for (Map.Entry<Number, String> entry : keys.entrySet())
		{
			Number number = entry.getKey();
			String jsonKey = entry.getValue();

			logger.logDebug("loadTemplates: Processing key: " + jsonKey);
			templates.computeIfAbsent(number, k -> new ArrayList<>());

			List<String> jsonList = jsonHandler.readListFromJson(templatesPath, jsonKey);

			if(jsonList != null && !jsonList.isEmpty())
			{
				logger.logDebug("loadTemplates: Found " + jsonList.size() + " templates for key: " + jsonKey);
				List<Template> templateList = jsonList.stream()
					.map(template -> new Template(template, number))
					.collect(Collectors.toList());
				templates.put(number, templateList);
			}
			else
				logger.logWarn("loadTemplates: No templates found for key: " + jsonKey);
		}

		logger.logTrace("loadTemplates: Completed loading templates");
	}

	/**
	 * Retrieves a random template with random grammatical number.
	 * <p>
	 * Selection process:
	 * <ul>
	 *	<li>Randomly selects between singular/plural forms</li>
	 *	<li>Delegates to {@code getRandomTemplate(Number)}</li>
	 *	<li>Logs the selected number type</li>
	 * </ul>
	 * </p>
	 *
	 * @return					Randomly selected {@code Template}
	 * @throws InvalidListException	if no templates are loaded for the selected number type
	 */
	public Template getRandomTemplate()
	{
		logger.logTrace("getRandomTemplate: Starting to get random template");

		Number[] numbers = Number.values();
		Number randomNumber = numbers[random.nextInt(numbers.length)];

		logger.logDebug("getRandomTemplate: Selected template type: " + randomNumber);

		Template result = getRandomTemplate(randomNumber);

		logger.logTrace("getRandomTemplate: Completed getting random template");
		return result;
	}


	/**
	 * Retrieves a random template with specified grammatical number.
	 * <p>
	 * Selection process:
	 * <ul>
	 *	<li>Uses uniform random distribution</li>
	 *	<li>Validates template list availability</li>
	 *	<li>Logs the selected template pattern</li>
	 * </ul>
	 * </p>
	 *
	 * @param number			The grammatical number to select from
	 * @return					Randomly selected {@code Template}
	 * @throws InvalidListException	if no templates are loaded for the specified number
	 */
	public Template getRandomTemplate(Number number)
	{
		logger.logDebug("getRandomTemplate: Starting to get random template for number: " + number);
		List<Template> templateList = templates.get(number);

		if(templateList == null || templateList.isEmpty())
		{
			logger.logError("getRandomTemplate: No templates available for number: " + number);
			throw new InvalidListException();
		}

		int randomIndex = random.nextInt(templateList.size());

		Template selected = templateList.get(randomIndex);

		logger.logDebug("getRandomTemplate: Selected template: " + selected.getPattern());
		logger.logTrace("getRandomTemplate: Completed getting random template");

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
	 * Handles JSON file update notifications by reloading templates.
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
			logger.logTrace("onJsonUpdate: Reloading templates");
			loadTemplates();
			logger.logTrace("onJsonUpdate: Successfully reloaded templates");
		}
		catch(IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload templates", e);
			throw e;
		}
	}
}
