package unipd.nonsense.generator;

import unipd.nonsense.model.Template;
import unipd.nonsense.model.Template.TemplateType;
import unipd.nonsense.exceptions.InvalidListException;
import unipd.nonsense.exceptions.InvalidJsonStateException;
import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.util.JsonUpdater;
import unipd.nonsense.util.LoggerManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

@DisplayName("Testing RandomTemplateGenerator")
@ExtendWith(MockitoExtension.class)
class TestRandomTemplateGenerator {

    private RandomTemplateGenerator generator;
    private File testFile;
    
    @Mock
    private JsonFileHandler mockJsonHandler;
    
    @Mock
    private Random mockRandom;

    @BeforeEach
    @DisplayName("Setup environment: Copy and use testTemplates.json for tests")
    void setUp(@TempDir Path tempDir) throws Exception {
        testFile = tempDir.resolve("templates.json").toFile();

        JsonObject testTemplates = readAndModifyTestTemplatesJson();

        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(testTemplates.toString());
        }

        Field field = RandomTemplateGenerator.class.getDeclaredField("templatesPath");
        field.setAccessible(true);
        field.set(null, testFile.getAbsolutePath());

        generator = new RandomTemplateGenerator();
    }
    
    @AfterEach
    void tearDown() {
        if (generator != null) {
            generator.cleanup();
        }
    }

    private JsonObject readAndModifyTestTemplatesJson() throws IOException {
        String resourcePath = "/testTemplates.json";

        String content;
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                JsonObject json = new JsonObject();
                JsonArray singularTemplates = new JsonArray();
                JsonArray pluralTemplates = new JsonArray();

                singularTemplates.add("Test singular template [noun]");
                singularTemplates.add("This is a [noun] template");
                singularTemplates.add("A [noun] for testing");

                pluralTemplates.add("Test plural templates [noun]");
                pluralTemplates.add("These are [noun] templates");
                pluralTemplates.add("Many [noun] for testing");

                json.add("singularTemplates", singularTemplates);
                json.add("pluralTemplates", pluralTemplates);

                return json;
            }

            content = new String(inputStream.readAllBytes());
        }

        JsonObject json = JsonParser.parseString(content).getAsJsonObject();

        if (!json.has("singularTemplates")) {
            JsonArray singularTemplates = new JsonArray();
            singularTemplates.add("Test singular template [noun]");
            singularTemplates.add("This is a [noun] template");
            singularTemplates.add("A [noun] for testing");
            json.add("singularTemplates", singularTemplates);
        }

        if (!json.has("pluralTemplates")) {
            JsonArray pluralTemplates = new JsonArray();
            pluralTemplates.add("Test plural templates [noun]");
            pluralTemplates.add("These are [noun] templates");
            pluralTemplates.add("Many [noun] for testing");
            json.add("pluralTemplates", pluralTemplates);
        }

        return json;
    }

    @Test
    @DisplayName("Test success of getRandomTemplate")
    void testGetRandomTemplate_Success() {
        Template template = generator.getRandomTemplate();
        assertNotNull(template, "Should return a valid template");
        assertNotNull(template.getPattern(), "Template should have a pattern");
        assertNotNull(template.getType(), "Template should have a type");
    }

    @Test
    @DisplayName("Test success of getRandomTemplate with SINGULAR type")
    void testGetRandomTemplate_SingularSuccess() {
        Template template = generator.getRandomTemplate(TemplateType.SINGULAR);
        assertEquals(TemplateType.SINGULAR, template.getType(), "Template type should be SINGULAR");
        assertTrue(template.getPattern().contains("[noun]"), "Template should contain a noun placeholder");
    }

    @Test
    @DisplayName("Test success of getRandomTemplate with PLURAL type")
    void testGetRandomTemplate_PluralSuccess() {
        Template template = generator.getRandomTemplate(TemplateType.PLURAL);
        assertEquals(TemplateType.PLURAL, template.getType(), "Template type should be PLURAL");
        assertTrue(template.getPattern().contains("[noun]"), "Template should contain a noun placeholder");
    }

    @Test
    @DisplayName("Test attempt to get template from empty list")
    void testGetRandomTemplate_EmptyList() throws Exception {
        JsonObject emptyJson = new JsonObject();
        emptyJson.add("singularTemplates", new JsonArray());
        emptyJson.add("pluralTemplates", new JsonArray());

        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(emptyJson.toString());
        }

        generator = new RandomTemplateGenerator();

        assertThrows(InvalidListException.class, () -> generator.getRandomTemplate(),
            "Should throw InvalidListException when no templates available");
        assertThrows(InvalidListException.class, () -> generator.getRandomTemplate(TemplateType.SINGULAR),
            "Should throw InvalidListException when no singular templates available");
        assertThrows(InvalidListException.class, () -> generator.getRandomTemplate(TemplateType.PLURAL),
            "Should throw InvalidListException when no plural templates available");
    }

    @Test
    @DisplayName("Test initialization with invalid JSON file")
    void testInitialization_InvalidJson() throws Exception {
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("invalid json content");
        }

        assertThrows(InvalidJsonStateException.class, () -> new RandomTemplateGenerator(),
            "Should throw InvalidJsonStateException when JSON is invalid");
    }

    @Test
    @DisplayName("Test templates count matches JSON file content")
    void testTemplatesCount() throws Exception {
        Field templatesField = RandomTemplateGenerator.class.getDeclaredField("templates");
        templatesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        var templatesMap = (java.util.Map<TemplateType, java.util.List<Template>>)templatesField.get(generator);

        JsonObject testTemplates = readAndModifyTestTemplatesJson();
        int expectedSingularCount = testTemplates.getAsJsonArray("singularTemplates").size();
        int expectedPluralCount = testTemplates.getAsJsonArray("pluralTemplates").size();

        assertEquals(expectedSingularCount, templatesMap.get(TemplateType.SINGULAR).size(),
            "Number of singular templates should match the JSON file");
        assertEquals(expectedPluralCount, templatesMap.get(TemplateType.PLURAL).size(),
            "Number of plural templates should match the JSON file");
    }
    
    // New tests below
    
    @Test
    @DisplayName("Test the template selection is random")
    void testTemplateSelectionIsRandom() throws Exception {
        // Collect multiple templates to verify randomness
        List<String> templates = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Template template = generator.getRandomTemplate();
            templates.add(template.getPattern());
        }
        
        // Check if there's at least two different templates (indicating randomness)
        long distinctTemplates = templates.stream().distinct().count();
        assertTrue(distinctTemplates > 1, "Template selection should be random and return different templates");
    }
    
    @Test
    @DisplayName("Test templates properties are preserved")
    void testTemplatePropertiesPreserved() {
        Template singularTemplate = generator.getRandomTemplate(TemplateType.SINGULAR);
        Template pluralTemplate = generator.getRandomTemplate(TemplateType.PLURAL);
        
        // Check if the templates have expected properties
        assertTrue(singularTemplate.getPattern().contains("[noun]"), "Singular template should contain [noun] placeholder");
        assertTrue(pluralTemplate.getPattern().contains("[noun]"), "Plural template should contain [noun] placeholder");
        
        // Check if the types are correctly assigned
        assertEquals(TemplateType.SINGULAR, singularTemplate.getType(), "Template type should be SINGULAR");
        assertEquals(TemplateType.PLURAL, pluralTemplate.getType(), "Template type should be PLURAL");
    }
    
    @Test
    @DisplayName("Test JSON update observer functionality")
    void testJsonUpdateObserver() throws Exception {
        // Initial template count
        Field templatesField = RandomTemplateGenerator.class.getDeclaredField("templates");
        templatesField.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        var initialTemplatesMap = (Map<TemplateType, List<Template>>)templatesField.get(generator);
        int initialSingularCount = initialTemplatesMap.get(TemplateType.SINGULAR).size();
        
        // Modify the JSON file with new content
        JsonObject updatedJson = new JsonObject();
        JsonArray updatedSingularTemplates = new JsonArray();
        JsonArray updatedPluralTemplates = new JsonArray();
        
        // Add more templates
        updatedSingularTemplates.add("Test singular template [noun]");
        updatedSingularTemplates.add("This is a [noun] template");
        updatedSingularTemplates.add("A [noun] for testing");
        updatedSingularTemplates.add("New singular [noun] template");
        updatedSingularTemplates.add("Another new [noun] template");
        
        updatedPluralTemplates.add("Test plural templates [noun]");
        updatedPluralTemplates.add("These are [noun] templates");
        updatedPluralTemplates.add("Many [noun] for testing");
        
        updatedJson.add("singularTemplates", updatedSingularTemplates);
        updatedJson.add("pluralTemplates", updatedPluralTemplates);
        
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(updatedJson.toString());
        }
        
        // Trigger the onJsonUpdate method
        generator.onJsonUpdate();
        
        // Check if templates were updated
        @SuppressWarnings("unchecked")
        var updatedTemplatesMap = (Map<TemplateType, List<Template>>)templatesField.get(generator);
        int updatedSingularCount = updatedTemplatesMap.get(TemplateType.SINGULAR).size();
        
        assertEquals(5, updatedSingularCount, "Number of singular templates should be updated to 5");
        assertTrue(updatedSingularCount > initialSingularCount, "Template count should increase after update");
    }
    
    @Test
    @DisplayName("Test cleanup removes observer")
    void testCleanup() throws Exception {
        // Use Mockito to verify JsonUpdater.removeObserver is called
        try (MockedStatic<JsonUpdater> mockedJsonUpdater = Mockito.mockStatic(JsonUpdater.class)) {
            generator.cleanup();
            mockedJsonUpdater.verify(() -> JsonUpdater.removeObserver(generator), times(1));
        }
    }
    
    @Test
    @DisplayName("Test IOException handling during onJsonUpdate")
    void testOnJsonUpdateIOException() throws Exception {
        // Save the original file path
        String originalPath = testFile.getAbsolutePath();
        
        try {
            // Change the templates path to a non-existent directory which will cause IOException
            Field field = RandomTemplateGenerator.class.getDeclaredField("templatesPath");
            field.setAccessible(true);
            field.set(null, "/nonexistent/directory/templates.json");
            
            // Test that IOException is properly propagated
            assertThrows(IOException.class, () -> generator.onJsonUpdate(),
                "onJsonUpdate should propagate IOException when file path is invalid");
        } finally {
            // Restore the original path
            Field field = RandomTemplateGenerator.class.getDeclaredField("templatesPath");
            field.setAccessible(true);
            field.set(null, originalPath);
        }
    }
    
    @Test
    @DisplayName("Test RandomTemplateGenerator with controlled randomness")
    void testRandomTemplateWithControlledRandomness() throws Exception {
        // Setup controlled randomness by injecting mock Random
        Field randomField = RandomTemplateGenerator.class.getDeclaredField("random");
        randomField.setAccessible(true);
        Random originalRandom = (Random) randomField.get(null);
        
        try {
            Random mockedRandom = mock(Random.class);
            // Make the mock return a specific index
            when(mockedRandom.nextInt(anyInt())).thenReturn(1); // Always choose the second template
            randomField.set(null, mockedRandom);
            
            // Get template with controlled randomness
            Template template = generator.getRandomTemplate(TemplateType.SINGULAR);
            
            // Verify we got the expected template (the second one)
            assertEquals("This is a [noun] template", template.getPattern(), 
                "Should select the template at index 1 with controlled randomness");
            
            // Verify for random type selection
            when(mockedRandom.nextInt(TemplateType.values().length)).thenReturn(0); // Select SINGULAR type
            template = generator.getRandomTemplate();
            assertEquals(TemplateType.SINGULAR, template.getType(), 
                "Should select SINGULAR type with controlled randomness");
            
        } finally {
            // Restore original random
            randomField.set(null, originalRandom);
        }
    }
    
    @Test
    @DisplayName("Test behavior with missing JSON keys")
    void testMissingJsonKeys() throws Exception {
        // Create JSON with only singular templates
        JsonObject partialJson = new JsonObject();
        JsonArray singularTemplates = new JsonArray();
        singularTemplates.add("Test singular template [noun]");
        partialJson.add("singularTemplates", singularTemplates);
        // Add empty pluralTemplates array to avoid InvalidJsonKey exception
        partialJson.add("pluralTemplates", new JsonArray());
        
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(partialJson.toString());
        }
        
        // Create a new generator with the partial JSON
        RandomTemplateGenerator partialGenerator = new RandomTemplateGenerator();
        
        // Should work for singular templates
        Template singularTemplate = partialGenerator.getRandomTemplate(TemplateType.SINGULAR);
        assertNotNull(singularTemplate, "Should return a singular template");
        
        // Should throw exception for plural templates (empty list)
        assertThrows(InvalidListException.class, () -> partialGenerator.getRandomTemplate(TemplateType.PLURAL),
            "Should throw InvalidListException when plural templates list is empty");
            
        partialGenerator.cleanup();
    }
    
    @Test
    @DisplayName("Test behavior with non-existent template file")
    void testNonExistentTemplateFile() throws Exception {
        // Delete the test file
        testFile.delete();
        
        // Attempt to create a generator with non-existent file
        assertThrows(IOException.class, () -> new RandomTemplateGenerator(), 
            "Should throw IOException when template file doesn't exist");
    }
    
    @Test
    @DisplayName("Test logging behavior")
    void testLoggingBehavior() throws Exception {
        // Create a mock logger
        LoggerManager mockLogger = mock(LoggerManager.class);
        
        // Replace the logger in the generator
        Field loggerField = RandomTemplateGenerator.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        LoggerManager originalLogger = (LoggerManager) loggerField.get(generator);
        loggerField.set(generator, mockLogger);
        
        try {
            // Perform operations that should trigger logging
            generator.getRandomTemplate();
            generator.getRandomTemplate(TemplateType.SINGULAR);
            generator.cleanup();
            
            // Verify logging calls
            verify(mockLogger, atLeastOnce()).logTrace(anyString());
            verify(mockLogger, atLeastOnce()).logDebug(anyString());
        } finally {
            // Restore original logger
            loggerField.set(generator, originalLogger);
        }
    }
}