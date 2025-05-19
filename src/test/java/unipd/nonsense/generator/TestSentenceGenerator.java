package unipd.nonsense.generator;

import unipd.nonsense.model.*;
import unipd.nonsense.model.Template.Placeholder;
import unipd.nonsense.exceptions.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

@DisplayName("Extended Testing SentenceGenerator")
class TestSentenceGeneratorExtended
{
	private SentenceGenerator sentenceGenerator;

	@BeforeEach
	void setup() throws IOException
	{
		sentenceGenerator = new SentenceGenerator();
	}

	@AfterEach
	void cleanup()
	{
		sentenceGenerator.close();
	}

	@Test
	@DisplayName("Test sentences have no remaining placeholders")
	void testNoRemainingPlaceholders()
	{
		Template[] sentences =
		{
			sentenceGenerator.generateRandomSentence(),
			sentenceGenerator.generateSentenceWithTense(Verb.Tense.PAST),
			sentenceGenerator.generateSentenceWithNumber(Noun.Number.PLURAL),
			sentenceGenerator.generateSentenceWithTenseAndNumber(Verb.Tense.FUTURE,	 Noun.Number.SINGULAR)
		};

		for(Template sentence : sentences)
		{
			String pattern = sentence.getPattern();
			assertFalse(pattern.contains("[noun]"), "Sentence still contains [noun] placeholder: " + pattern);
			assertFalse(pattern.contains("[adjective]"), "Sentence still contains [adjective] placeholder: " + pattern);
			assertFalse(pattern.contains("[verb]"), "Sentence still contains [verb] placeholder: " + pattern);
		}
	}

	@Test
	@DisplayName("Test null inputs handling")
	void testNullInputHandling() throws IOException
	{
		Template result = sentenceGenerator.generateSentenceWith(null, null, null);
		assertNotNull(result);
		assertFalse(result.getPattern().isEmpty());

		result = sentenceGenerator.generateSentenceWith(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		assertNotNull(result);
		assertFalse(result.getPattern().isEmpty());

		List<Noun> nouns = new ArrayList<>();
		nouns.add(new Noun("computer", Noun.Number.SINGULAR));

		result = sentenceGenerator.generateSentenceWith(nouns, null, null);
		assertNotNull(result);
		assertFalse(result.getPattern().isEmpty());

		assertTrue(result.getPattern().contains("computer"), "Sentence should contain the provided noun");
	}

	@Test
	@DisplayName("Test template with excessive placeholders")
	void testExcessivePlaceholders()
	{
		Template template = new Template("The [adjective] [noun] [verb] while the [adjective] [noun] [verb] and the [adjective] [noun] [verb].", Template.TemplateType.SINGULAR);

		Template result = sentenceGenerator.generateSentenceFromTemplate(template);
		assertNotNull(result);
		assertFalse(result.getPattern().isEmpty());

		assertFalse(result.getPattern().contains("[noun]"));
		assertFalse(result.getPattern().contains("[adjective]"));
		assertFalse(result.getPattern().contains("[verb]"));
	}

	@Test
	@DisplayName("Test custom words actually appear in sentence")
	void testCustomWordsAppearance() throws IOException
	{
		List<Noun> nouns = Arrays.asList(new Noun("elephant", Noun.Number.SINGULAR), new Noun("mountain", Noun.Number.SINGULAR));

		List<Adjective> adjectives = Arrays.asList(new Adjective("enormous"), new Adjective("purple"));

		List<Verb> verbs = Arrays.asList(new Verb("exploded", Verb.Tense.PAST), new Verb("danced", Verb.Tense.PRESENT));

		Template result = sentenceGenerator.generateSentenceWith(nouns, adjectives, verbs);
		String sentence = result.getPattern();

		boolean hasCustomWords = false;

		for(String word : new String[] {"elephant", "mountain", "enormous", "purple", "exploded", "danced"})
		{
			if (sentence.contains(word))
			{
				hasCustomWords = true;
				break;
			}
		}

		assertTrue(hasCustomWords, "Sentence should contain at least one of the custom words");
	}

	@Test
	@DisplayName("Test invalid template throws exception")
	void testInvalidTemplateExceptions()
	{
		assertThrows(InvalidTemplateException.class, () -> {sentenceGenerator.generateSentenceFromTemplate(null);});

		assertThrows(InvalidTemplateException.class, () -> {sentenceGenerator.generateSentenceFromTemplate(new Template("", Template.TemplateType.SINGULAR));});
	}

	@Test
	@DisplayName("Test sentence consistency across methods")
	void testSentenceConsistency()
	{
		Template sentenceWithPastTense = sentenceGenerator.generateSentenceWithTense(Verb.Tense.PAST);
		Template sentenceWithPluralNumber = sentenceGenerator.generateSentenceWithNumber(Noun.Number.PLURAL);

		assertEquals(Template.TemplateType.PLURAL, sentenceWithPluralNumber.getType(), "Template type should be PLURAL when using plural number");

		List<Template> templates = sentenceGenerator.getRandomTemplates();
		assertEquals(5, templates.size());

		boolean hasVariation = false;
		String firstPattern = templates.get(0).getPattern();

		for(int i = 1; i < templates.size(); i++)
		{
			if(!templates.get(i).getPattern().equals(firstPattern))
			{
				hasVariation = true;
				break;
			}
		}

		assertTrue(hasVariation, "Random templates should show some variation");
	}

	@Test
	@DisplayName("Test specific tense and number combinations")
	void testSpecificCombinations()
	{
		for(Verb.Tense tense : Verb.Tense.values())
		{
			for(Noun.Number number : Noun.Number.values())
			{
				Template result = sentenceGenerator.generateSentenceWithTenseAndNumber(tense, number);

				if(number == Noun.Number.SINGULAR)
					assertEquals(Template.TemplateType.SINGULAR, result.getType(), "Template type should be SINGULAR when number is SINGULAR");

				else
					assertEquals(Template.TemplateType.PLURAL, result.getType(), "Template type should be PLURAL when number is PLURAL");

				String sentence = result.getPattern();

				assertNotNull(sentence);
				assertFalse(sentence.isEmpty());
				assertTrue(sentence.matches(".*[.!?]\\s*$"), "Sentence should end with proper punctuation");
			}
		}
	}

	@Test
	@DisplayName("Test sentence capitalization and punctuation")
	void testSentenceFormat()
	{
		for(int i = 0; i < 5; i++)
		{
			Template template = sentenceGenerator.generateRandomSentence();
			String sentence = template.getPattern();

			assertTrue(Character.isUpperCase(sentence.charAt(0)), "Sentence should start with uppercase: " + sentence);

			assertTrue(sentence.matches(".*[.!?]\\s*$"), "Sentence should end with proper punctuation: " + sentence);
		}
	}

	@Test
	@DisplayName("Test custom words mixed with random words")
	void testMixedCustomAndRandomWords() throws IOException
	{
		List<Noun> nouns = List.of(new Noun("satellite", Noun.Number.SINGULAR));
		List<Adjective> adjectives = List.of(new Adjective("metallic"));
		List<Verb> verbs = List.of(new Verb("orbits", Verb.Tense.PRESENT));

		Template template = new Template("The [adjective] [noun] [verb] while the [adjective] [noun] [verb].", Template.TemplateType.SINGULAR);

		sentenceGenerator.generateSentenceFromTemplate(template);
		Template result = sentenceGenerator.generateSentenceWith(nouns, adjectives, verbs);

		assertNotNull(result);
		String sentence = result.getPattern();

		assertTrue(sentence.contains("satellite") || sentence.contains("metallic") || sentence.contains("orbit"), "Sentence should contain at least one of our custom words");

		assertFalse(sentence.contains("[noun]"));
		assertFalse(sentence.contains("[adjective]"));
		assertFalse(sentence.contains("[verb]"));
	}
}
