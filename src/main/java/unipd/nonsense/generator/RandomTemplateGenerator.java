package unipd.nonsense.generator;

import unipd.nonsense.util.JsonUpdateObserver;
import unipd.nonsense.util.JsonUpdater;
import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Template;
import unipd.nonsense.model.Template.TemplateType;
import unipd.nonsense.exception.TemplateLoadException;
import unipd.nonsense.exception.TemplateNotFoundException;
import unipd.nonsense.exception.JsonFileAccessException;

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

	private static String templatesPath = "templates.json";
	private static List<String> keys = List.of("singularTemplates", "pluralTemplates");

	public RandomTemplateGenerator() throws TemplateLoadException
	{
		this.templates = new HashMap<>();
		this.random = new Random();

		try {
			loadTemplates();
			JsonUpdater.addObserver(this);
		} catch (IOException e) {
			throw new TemplateLoadException("Failed to load templates", e);
		} catch (JsonFileAccessException e) {
			throw new TemplateLoadException("JSON file access error", e);
		}
	}

	private void loadTemplates() throws IOException, JsonFileAccessException
	{
		for(String key : keys)
		{
			TemplateType type;

			if(key.equals(keys.get(0)))
				type = TemplateType.SINGULAR;
			else
				type = TemplateType.PLURAL;

			templates.computeIfAbsent(type, k -> new ArrayList<>());

			try {
				List<String> jsonList = jsonHandler.readListFromJson(templatesPath, key);

				if(jsonList == null)
				{
					throw new JsonFileAccessException(templatesPath, "Template list for key '" + key + "' is null");
				}

				if(jsonList.isEmpty())
				{
					throw new JsonFileAccessException(templatesPath, "Template list for key '" + key + "' is empty");
				}

				List<Template> templateList = jsonList.stream()
					.map(template -> new Template(template, type))
					.collect(Collectors.toList());

				templates.put(type, templateList);
			} catch (IOException e) {
				throw new JsonFileAccessException(templatesPath, "Error reading template list for key '" + key + "'", e);
			}
		}
	}

	public Template getRandomTemplate()
	{
		if (templates.isEmpty()) {
			throw new TemplateNotFoundException(null);
		}

		TemplateType[] types = TemplateType.values();
		TemplateType randomType = types[random.nextInt(types.length)];

		return getRandomTemplate(randomType);
	}

	public Template getRandomTemplate(TemplateType type)
	{
		List<Template> templateList = templates.get(type);

		if(templateList == null || templateList.isEmpty())
			throw new TemplateNotFoundException(type);

		int randomIndex = random.nextInt(templateList.size());
		return templateList.get(randomIndex);
	}

	@Override
	public void onJsonUpdate()
	{
		try
		{
			loadTemplates();
		}
		catch (IOException | JsonFileAccessException e)
		{
			System.err.println("Error reloading templates: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
