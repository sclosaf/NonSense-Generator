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
		this.templates = new HashMap<>();
		this.random = new Random();

		try
		{
			loadTemplates();
			JsonUpdater.addObserver(this);

		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize RandomTemplateGenerator", e);
		}
	}

	private void loadTemplates() throws IOException
	{
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
			}
			else
				logger.logWarn("loadTemplates: No templates found for key: " + key);
		}
	}

	public Template getRandomTemplate()
	{
		TemplateType[] types = TemplateType.values();
		TemplateType randomType = types[random.nextInt(types.length)];

		return getRandomTemplate(randomType);
	}

	public Template getRandomTemplate(TemplateType type)
	{
		List<Template> templateList = templates.get(type);

		if(templateList == null || templateList.isEmpty())
		{
			logger.logError("getRandomTemplate: No " + type + " templates available");
			throw new InvalidListException();
		}

		int randomIndex = random.nextInt(templateList.size());

		Template selected = templateList.get(randomIndex);

		return selected;
	}

	public void cleanup()
	{
		JsonUpdater.removeObserver(this);
	}

	@Override
	public void onJsonUpdate() throws IOException
	{
		try
		{
			loadTemplates();
		}
		catch (IOException e)
		{
			logger.logError("onJsonUpdate: Failed to reload templates", e);
			throw e;
        }
	}
}
