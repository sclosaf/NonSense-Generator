package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;
import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Template;
import unipd.nonsense.model.Template.TemplateType;
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
	private Map<TemplateType, List<Template>> templates;
	private static Random random;

	private final static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	private static String templatesPath = "target" + File.separator + "resources" + File.separator + "templates.json";
	private final static List<String> keys = List.of("singularTemplates", "pluralTemplates");
	private LoggerManager logger = new LoggerManager(RandomTemplateGenerator.class);

	public RandomTemplateGenerator() throws IOException
	{
		logger.logTrace("Starting initialization");

		this.templates = new HashMap<>();
		this.random = new Random();

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

		for(String key : keys)
		{
			logger.logDebug("loadTemplates: Processing key: " + key);

			TemplateType type;

			if(key.equals(keys.get(0)))
				type = TemplateType.SINGULAR;
			else
				type = TemplateType.PLURAL;

			templates.computeIfAbsent(type, k -> new ArrayList<>());

			List<String> jsonList = jsonHandler.readListFromJson(templatesPath, key);

			if(jsonList != null && !jsonList.isEmpty())
			{
				logger.logDebug("loadTemplates: Found " + jsonList.size() + " templates for key: " + key);
				List<Template> templateList = jsonList.stream().map(template -> new Template(template, type)).collect(Collectors.toList());

				templates.put(type, templateList);
			}
			else
				logger.logWarn("loadTemplates: No templates found for key: " + key);
		}

		logger.logTrace("loadTemplates: Completed loading templates");
	}

	public Template getRandomTemplate()
	{
		logger.logTrace("getRandomTemplate: Starting to get random template");

		TemplateType[] types = TemplateType.values();
		TemplateType randomType = types[random.nextInt(types.length)];

		logger.logDebug("getRandomTemplate: Selected template type: " + randomType);

		Template result = getRandomTemplate(randomType);

		logger.logTrace("getRandomTemplate: Completed getting random template");
		return result;
	}

	public Template getRandomTemplate(TemplateType type)
	{
		logger.logDebug("getRandomTemplate: Starting to get random template for type: " + type);
		List<Template> templateList = templates.get(type);

		if(templateList == null || templateList.isEmpty())
		{
			logger.logError("getRandomTemplate: No templates available for type: " + type);
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
