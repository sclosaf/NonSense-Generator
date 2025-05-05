import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TemplateGenerator {
    private List<String> templates;
    private Random random;
    private static final String TEMPLATE_FILE = "templates.json";
    
    // Constructor without parameters - always loads templates.json
    public TemplateGenerator() throws IOException, ParseException {
        this.templates = new ArrayList<>();
        this.random = new Random();
        loadTemplates();
    }
    
    // Loads templates from templates.json resource file
    private void loadTemplates() throws IOException, ParseException {
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject;
            
            // Load from classpath resource
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(TEMPLATE_FILE);
            if (inputStream == null) {
                throw new IOException("Resource not found: " + TEMPLATE_FILE);
            }
            
            // Parse JSON from resource
            jsonObject = (JSONObject) parser.parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            
            // Get templates array
            JSONArray templatesArray = (JSONArray) jsonObject.get("templates");
            
            // Add templates to list
            for (Object template : templatesArray) {
                this.templates.add((String) template);
            }
            
            System.out.println("Loaded " + this.templates.size() + " templates from " + TEMPLATE_FILE);
        } catch (IOException e) {
            System.err.println("Error reading template file: " + e.getMessage());
            throw e;
        } catch (ParseException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            throw e;
        }
    }
    
    // Returns a random template
    public String getRandomTemplate() {
        if (templates.isEmpty()) {
            throw new IllegalStateException("No templates loaded");
        }
        
        int randomIndex = random.nextInt(templates.size());
        return templates.get(randomIndex);
    }
    
}