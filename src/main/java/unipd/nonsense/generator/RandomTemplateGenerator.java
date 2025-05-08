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
	private Map<TemplateType, List<Template>> templateMap;
	private Random random;

	JsonFileHandler jsonHandler;

	private static String templatesPath = "templates.json";
	private static String singularTemplatesKey = "singularTemplates";
	private static String pluralTemplatesKey = "pluralTemplates";

	public RandomTemplateGenerator() throws IOException
	{
		this.templateMap = new HashMap<>();
		this.random = new Random();

		jsonHandler = JsonFileHandler.getInstance();
		loadTemplates();
	}

	private void loadTemplates() throws IOException
	{
		// Load singular templates
		List<String> singularJsonList = jsonHandler.readListFromJson(templatesPath, singularTemplatesKey);
		List<Template> singularTemplates = new ArrayList<>();
		
		for(String element : singularJsonList)
			singularTemplates.add(new Template(element, TemplateType.SINGULAR));
		
		templateMap.put(TemplateType.SINGULAR, singularTemplates);

		// Load plural templates
		List<String> pluralJsonList = jsonHandler.readListFromJson(templatesPath, pluralTemplatesKey);
		List<Template> pluralTemplates = new ArrayList<>();
		
		for(String element : pluralJsonList)
			pluralTemplates.add(new Template(element, TemplateType.PLURAL));
		
		templateMap.put(TemplateType.PLURAL, pluralTemplates);
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
		List<Template> templates = templateMap.get(type);
		
		if(templates == null || templates.isEmpty())
			throw new IllegalStateException("No templates loaded for type: " + type);

		int randomIndex = random.nextInt(templates.size());
		return templates.get(randomIndex);
	}
}