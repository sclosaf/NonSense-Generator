package unipd.nonsense.generator;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Template;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TemplateGenerator
{
	private List<Template> templates;
	private Random random;

	JsonFileHandler jsonHandler;

	private static String templatesPath = "templates.json";
	private static String templatesKey = "templates";

	public TemplateGenerator() throws IOException
	{
		this.templates = new ArrayList<>();
		this.random = new Random();

		jsonHandler = JsonFileHandler.getInstance();
		loadTemplates();
	}

	private void loadTemplates() throws IOException
	{
		jsonHandler = JsonFileHandler.getInstance();
		List<String> jsonList = jsonHandler.readListFromJson(templatesPath, templatesKey);

		for(String element : jsonList)
			this.templates.add(new Template(element));
	}

	public Template getRandomTemplate()
	{
		if(templates.isEmpty())
			throw new IllegalStateException("No templates loaded");

		int randomIndex = random.nextInt(templates.size());
		return templates.get(randomIndex);
	}
}
