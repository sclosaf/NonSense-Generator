package unipd.nonsense.generator;

import unipd.nonsense.model.Template;
import unipd.nonsense.model.Template.TemplateType;
import unipd.nonsense.exceptions.InvalidListException;
import unipd.nonsense.exceptions.InvalidJsonStateException;
import unipd.nonsense.util.JsonFileHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

@DisplayName("Testing RandomTemplateGenerator")
class TestRandomTemplateGenerator {

    private RandomTemplateGenerator generator;
    private File testFile;

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
}
