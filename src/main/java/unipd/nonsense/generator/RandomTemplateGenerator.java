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

	private static JsonFileHandler jsonHandler = JsonFileHandler.getInstance();

	private static String templatesPath = "target" + File.separator + "resources" + File.separator + "templates.json";
	private static List<String> keys = List.of("singularTemplates", "pluralTemplates");
	private LoggerManager logger = new LoggerManager(RandomTemplateGenerator.class);

	public RandomTemplateGenerator() throws IOException
	{
		logger.logInfo("Initializing RandomTemplateGenerator");

		this.templates = new HashMap<>();
		this.random = new Random();

		try
		{
			loadTemplates();
			JsonUpdater.addObserver(this);

			logger.logInfo("Initialization completed successfully");
			logger.logDebug("LoadedTemplates counts - Singular: " + (templates.get(TemplateType.SINGULAR) != null ? templates.get(TemplateType.SINGULAR).size() : 0) + ", Plural: " + (templates.get(TemplateType.PLURAL) != null ? templates.get(TemplateType.PLURAL).size() : 0));
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize RandomTemplateGenerator", e);
		}
	}

	private void loadTemplates() throws IOException
	{
		logger.logInfo("loadTemplates: Loading templates from JSON file");

		for(String key : keys)
		{
			TemplateType type;

			if(key.equals(keys.get(0)))
				type = TemplateType.SINGULAR;
			else
				type = TemplateType.PLURAL;

			templates.computeIfAbsent(type, k -> new ArrayList<>());

			List<String> jsonList = jsonHandler.readListFromJson(templatesPath, key);

			if(jsonList != null)
			{
				List<Template> templateList = jsonList.stream().map(template -> new Template(template, type)).collect(Collectors.toList());

				templates.put(type, templateList);
				logger.logDebug("loadTemplates: Loaded " + templateList.size() + " " + type + " templates");
			}
			else
				logger.logWarn("loadTemplates: No templates found for key: " + key);
		}
	}

	public Template getRandomTemplate()
	{
		logger.logInfo("getRandomTemplate: Selecting random template with random type");
		TemplateType[] types = TemplateType.values();
		TemplateType randomType = types[random.nextInt(types.length)];

		logger.logDebug("getRandomTemplate: Selected type: " + randomType);
		return getRandomTemplate(randomType);
	}

	public Template getRandomTemplate(TemplateType type)
	{
		logger.logDebug("getRandomTemplate: Selecting random " + type + " template");
		List<Template> templateList = templates.get(type);

		if(templateList == null || templateList.isEmpty())
		{
			logger.logError("getRandomTemplate: No " + type + " templates available");
			throw new InvalidListException();
		}

		int randomIndex = random.nextInt(templateList.size());

		Template selected = templateList.get(randomIndex);

		logger.logDebug("getRandomTemplate: Selected template: " + selected.getPattern() + " (index " + randomIndex + " of " + templateList.size() + ")");
		return selected;
	}

	@Override
	public void onJsonUpdate() throws IOException
	{
		logger.logInfo("onJsonUpdate: JSON file updated, reloading templates");

		try
		{
			loadTemplates();

			logger.logInfo("onJsonUpdate: Templates reloaded successfully");
			logger.logDebug("Templates count - Singular: " + (templates.get(TemplateType.SINGULAR) != null ? templates.get(TemplateType.SINGULAR).size() : 0) + ", Plural: " + (templates.get(TemplateType.PLURAL) != null ? templates.get(TemplateType.PLURAL).size() : 0));
		}
		catch (IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload templates", e);
			throw e;
        }
	}
}
