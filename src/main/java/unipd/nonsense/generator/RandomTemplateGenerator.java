package unipd.nonsense.generator;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Template;
import unipd.nonsense.model.Template.TemplateType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomTemplateGenerator
{
	private static Map<TemplateType, List<Template>> templates;
	private static Random random;

	private static JsonFileHandler jsonHandler;

	private static String templatesPath = "templates.json";
	private static List<String> keys = List.of("singularTemplates", "plurarlTemplates");

	public RandomTemplateGenerator() throws IOException
	{
		this.templates = new HashMap<>();
		this.random = new Random();
		this.jsonHandler = JsonFileHandler.getInstance();

		loadTemplates();
	}

	private void loadTemplates() throws IOException
	{
		for(String key : keys)
		{
			TemplateType type;

			if(key == keys.get(0))
				type = TemplateType.SINGULAR;
			else
				type = TemplateType.PLURAL;

			templates.computeIfAbsent(type, k -> new ArrayList<>());

			List<String> jsonList = jsonHandler.readListFromJson(templatesPath, key);

			for(String element : jsonList)
				this.templates.get(type).add(new Template(element, type));
		}
	}

	public Template getRandomTemplate()
	{
		// Choose a random template type
		TemplateType[] types = TemplateType.values();
		TemplateType randomType = types[random.nextInt(types.length)];

		return getRandomTemplate(randomType);
	}

	public Template getRandomTemplate(TemplateType type)
	{
		List<Template> templateList = templates.get(type);

		if(templateList == null || templateList.isEmpty())
			throw new IllegalStateException("No templates loaded for type: " + type);

		int randomIndex = random.nextInt(templateList.size());
		return templateList.get(randomIndex);
	}
}
