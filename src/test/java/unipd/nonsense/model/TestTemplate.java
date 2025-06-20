package unipd.nonsense.model;

import unipd.nonsense.model.Template;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Placeholder;

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
		template = new Template("[noun] is [adjective]", Number.SINGULAR);
	}

	@Test
	@DisplayName("Test valid construction")
	void testValidConstruction()
	{
		assertNotNull(template);
		assertEquals("[noun] is [adjective]", template.getPattern());
		assertEquals(Number.SINGULAR, template.getNumber());
	}

	@Test
	@DisplayName("Test empty pattern throws exception")
	void testEmptyPattern()
	{
		assertThrows(InvalidTemplateException.class,
			() -> new Template("", Number.PLURAL));
	}

	@Test
	@DisplayName("Test containsPlaceholder")
	void testContainsPlaceholder()
	{
		assertTrue(template.containsPlaceholder(Placeholder.NOUN));
		assertTrue(template.containsPlaceholder(Placeholder.ADJECTIVE));
		assertFalse(template.containsPlaceholder(Placeholder.VERB));
	}

	@Test
	@DisplayName("Test countPlaceholders")
	void testCountPlaceholders()
	{
		Template multiTemplate = new Template("[noun] [noun] [verb]", Number.PLURAL);

		assertEquals(1, template.countPlaceholders(Placeholder.NOUN));
		assertEquals(2, multiTemplate.countPlaceholders(Placeholder.NOUN));
		assertEquals(0, template.countPlaceholders(Placeholder.VERB));
	}

	@Test
	@DisplayName("Test replacePlaceholder")
	void testReplacePlaceholder()
	{
		String original = template.getPattern();
		template.replacePlaceholder(Placeholder.NOUN, "dog");

		assertNotEquals(original, template.getPattern());
		assertTrue(template.getPattern().contains("dog"));
	}

	@Test
	@DisplayName("Test withReplacement creates new instance")
	void testWithReplacement()
	{
		Template newTemplate = template.withReplacement(Placeholder.ADJECTIVE, "happy");
		assertNotSame(template, newTemplate);
		assertTrue(newTemplate.getPattern().contains("happy"));
	}
}
