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

public class RandomTemplateGenerator implements JsonUpdateObserver
{
	private Map<Number, List<Template>> templates;
	private static Random random;

	private final static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	private static String templatesPath;
	private final static Map<Number, String> keys = Map.of(
		Number.SINGULAR, "singularTemplates",
		Number.PLURAL, "pluralTemplates"
	);

	private LoggerManager logger = new LoggerManager(RandomTemplateGenerator.class);

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

	public void cleanup()
	{
		logger.logTrace("cleanup: Starting cleanup");
		JsonUpdater.removeObserver(this);
		logger.logTrace("cleanup: Completed cleanup");
	}

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
