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

	@Test
	@DisplayName("Test null pattern throws exception")
	void testNullPattern()
	{
		assertThrows(InvalidTemplateException.class,
			() -> new Template(null, Number.SINGULAR));
	}

	@Test
	@DisplayName("Test null number throws exception")
	void testNullNumber()
	{
		assertThrows(InvalidTemplateException.class,
			() -> new Template("[noun]", null));
	}

	@Test
	@DisplayName("Test pattern with only whitespace throws exception")
	void testWhitespacePattern()
	{
		assertThrows(InvalidTemplateException.class,
			() -> new Template("   ", Number.SINGULAR));
	}

	@Test
	@DisplayName("Test pattern trimming")
	void testPatternTrimming()
	{
		Template t = new Template("  [noun]  ", Number.SINGULAR);
		assertEquals("[noun]", t.getPattern());
	}

	@Test
	@DisplayName("Test multiple replacements of same placeholder")
	void testMultipleReplacements()
	{
		Template t = new Template("[noun] and [noun]", Number.SINGULAR);
		t.replacePlaceholder(Placeholder.NOUN, "cat");
		assertTrue(t.getPattern().contains("cat"));
		assertEquals(1, t.getPattern().split("cat", -1).length - 1);
	}

	@Test
	@DisplayName("Test replace non-existent placeholder")
	void testReplaceNonExistentPlaceholder()
	{
		String original = template.getPattern();
		template.replacePlaceholder(Placeholder.VERB, "run");
		assertEquals(original, template.getPattern());
	}

	@Test
	@DisplayName("Test complex pattern with multiple placeholders")
	void testComplexPattern()
	{
		Template t = new Template("[noun] [verb] [adjective] [noun]", Number.PLURAL);
		assertTrue(t.containsPlaceholder(Placeholder.NOUN));
		assertTrue(t.containsPlaceholder(Placeholder.VERB));
		assertTrue(t.containsPlaceholder(Placeholder.ADJECTIVE));
		assertEquals(2, t.countPlaceholders(Placeholder.NOUN));
	}

	@Test
	@DisplayName("Test case-insensitive placeholder matching")
	void testCaseInsensitivePlaceholders()
	{
		Template t = new Template("[NOUN] [Adjective]", Number.SINGULAR);
		assertFalse(t.containsPlaceholder(Placeholder.NOUN));
		assertFalse(t.containsPlaceholder(Placeholder.ADJECTIVE));
	}

	@Test
	@DisplayName("Test toString returns pattern")
	void testToString()
	{
		assertEquals(template.getPattern(), template.toString());
	}

	@Test
	@DisplayName("Test very long pattern")
	void testVeryLongPattern()
	{
		String longPattern = "[noun] ".repeat(100) + "[adjective]";
		Template t = new Template(longPattern, Number.SINGULAR);
		assertEquals(100, t.countPlaceholders(Placeholder.NOUN));
		assertEquals(1, t.countPlaceholders(Placeholder.ADJECTIVE));
	}

	@Test
	@DisplayName("Test special characters in pattern")
	void testSpecialCharacters()
	{
		String pattern = "[noun] café";
		Template t = new Template(pattern, Number.SINGULAR);
		assertEquals(pattern, t.getPattern());
	}

	@Test
	@DisplayName("Test replacement with empty string")
	void testEmptyReplacement()
	{
		template.replacePlaceholder(Placeholder.NOUN, "");
		assertFalse(template.getPattern().contains("[noun]"));
		assertTrue(template.getPattern().contains("is [adjective]"));
	}

	@Test
	@DisplayName("Test withReplacement maintains number")
	void testWithReplacementMaintainsNumber()
	{
		Template newTemplate = template.withReplacement(Placeholder.NOUN, "dog");
		assertEquals(template.getNumber(), newTemplate.getNumber());
	}

	@Test
	@DisplayName("Test multiple sequential replacements")
	void testMultipleSequentialReplacements()
	{
		template.replacePlaceholder(Placeholder.NOUN, "dog");
		template.replacePlaceholder(Placeholder.ADJECTIVE, "happy");
		assertEquals("dog is happy", template.getPattern());
	}
}
