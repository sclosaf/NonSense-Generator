package unipd.nonsense.generator;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.model.Template;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TemplateGenerator
{
	private List<Template> templates;  // Changed to store Template objects
	private Random random;
	private static final String TEMPLATE_FILE = "templates.json";

	public TemplateGenerator() throws IOException
	{
		this.templates = new ArrayList<>();
		this.random = new Random();
		loadTemplates();
	}

	private void loadTemplates() throws IOException
	{
		try
		{
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream(TEMPLATE_FILE);

			if(inputStream == null)
				throw new IOException("Resource not found: " + TEMPLATE_FILE);

			java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("templates", ".json");
			java.nio.file.Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			String filePath = tempFile.toString();

			JsonFileHandler jsonHandler = JsonFileHandler.getInstance();
			JsonObject jsonObject = jsonHandler.getJsonObject(filePath);
			JsonArray templatesArray = jsonObject.getAsJsonArray("templates");

			// Create Template objects instead of storing raw strings
			for(var element : templatesArray)
				this.templates.add(new Template(element.getAsString()));

			java.nio.file.Files.delete(tempFile);
			System.out.println("Loaded " + this.templates.size() + " templates from " + TEMPLATE_FILE);
		}
		catch (IOException e)
		{
			System.err.println("Error reading template file: " + e.getMessage());
			throw e;
		}
	}

	// Returns a random Template object
	public Template getRandomTemplate()
	{
		if(templates.isEmpty())
			throw new IllegalStateException("No templates loaded");

		int randomIndex = random.nextInt(templates.size());
		return templates.get(randomIndex);
	}
}
