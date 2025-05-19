package unipd.nonsense.model;

import org.junit.jupiter.api.*;
import unipd.nonsense.exceptions.InvalidTemplateException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testing Template")
class TestTemplate
{
	private Template template;

	@BeforeEach
	void setup()
	{
		template = new Template("[noun] is [adjective]", Template.TemplateType.SINGULAR);
	}

	@Test
	@DisplayName("Test valid construction")
	void testValidConstruction()
	{
		assertNotNull(template);
		assertEquals("[noun] is [adjective]", template.getPattern());
		assertEquals(Template.TemplateType.SINGULAR, template.getType());
	}

	@Test
	@DisplayName("Test empty pattern throws exception")
	void testEmptyPattern()
	{
		assertThrows(InvalidTemplateException.class,
			() -> new Template("", Template.TemplateType.PLURAL));
	}

	@Test
	@DisplayName("Test containsPlaceholder")
	void testContainsPlaceholder()
	{
		assertTrue(template.containsPlaceholder(Template.Placeholder.NOUN));
		assertTrue(template.containsPlaceholder(Template.Placeholder.ADJECTIVE));
		assertFalse(template.containsPlaceholder(Template.Placeholder.VERB));
	}

	@Test
	@DisplayName("Test countPlaceholders")
	void testCountPlaceholders()
	{
		Template multiTemplate = new Template("[noun] [noun] [verb]", Template.TemplateType.PLURAL);

		assertEquals(1, template.countPlaceholders(Template.Placeholder.NOUN));
		assertEquals(2, multiTemplate.countPlaceholders(Template.Placeholder.NOUN));
		assertEquals(0, template.countPlaceholders(Template.Placeholder.VERB));
	}

	@Test
	@DisplayName("Test replacePlaceholder")
	void testReplacePlaceholder()
	{
		String original = template.getPattern();
		template.replacePlaceholder(Template.Placeholder.NOUN, "dog");

		assertNotEquals(original, template.getPattern());
		assertTrue(template.getPattern().contains("dog"));
	}

	@Test
	@DisplayName("Test withReplacement creates new instance")
	void testWithReplacement()
	{
		Template newTemplate = template.withReplacement(Template.Placeholder.ADJECTIVE, "happy");
		assertNotSame(template, newTemplate);
		assertTrue(newTemplate.getPattern().contains("happy"));
	}
}
