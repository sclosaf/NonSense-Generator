package unipd.nonsense.generator;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Template;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Tense;
import unipd.nonsense.model.Placeholder;
import unipd.nonsense.exceptions.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@DisplayName("Extended Testing SentenceGenerator")
class TestSentenceGenerator
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
			sentenceGenerator.generateSentenceWithTense(Tense.PAST),
			sentenceGenerator.generateSentenceWithNumber(Number.PLURAL),
			sentenceGenerator.generateSentenceWithTenseAndNumber(Tense.FUTURE, Number.SINGULAR)
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
	}

	@Test
	@DisplayName("Test template with excessive placeholders")
	void testExcessivePlaceholders()
	{
		Template template = new Template("The [adjective] [noun] [verb] while the [adjective] [noun] [verb] and the [adjective] [noun] [verb].", Number.SINGULAR);

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
		List<Noun> nouns = Arrays.asList(new Noun("elephant", Number.SINGULAR), new Noun("mountain", Number.SINGULAR));

		List<Adjective> adjectives = Arrays.asList(new Adjective("enormous"), new Adjective("purple"));

		List<Verb> verbs = Arrays.asList(new Verb("exploded", Number.SINGULAR, Tense.PAST), new Verb("danced", Number.SINGULAR, Tense.PRESENT));

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
System.out.println(sentence);
		assertTrue(hasCustomWords, "Sentence should contain at least one of the custom words");
	}

	@Test
	@DisplayName("Test invalid template throws exception")
	void testInvalidTemplateExceptions()
	{
		assertThrows(InvalidTemplateException.class, () -> {sentenceGenerator.generateSentenceFromTemplate(null);});

		assertThrows(InvalidTemplateException.class, () -> {sentenceGenerator.generateSentenceFromTemplate(new Template("", Number.SINGULAR));});
	}

	@Test
	@DisplayName("Test sentence consistency across methods")
	void testSentenceConsistency()
	{
		Template sentenceWithPastTense = sentenceGenerator.generateSentenceWithTense(Tense.PAST);
		Template sentenceWithPluralNumber = sentenceGenerator.generateSentenceWithNumber(Number.PLURAL);

		assertEquals(Number.PLURAL, sentenceWithPluralNumber.getNumber(), "Template type should be PLURAL when using plural number");

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
		for(Tense tense : Tense.values())
		{
			for(Number number : Number.values())
			{
				Template result = sentenceGenerator.generateSentenceWithTenseAndNumber(tense, number);

				if(number == Number.SINGULAR)
					assertEquals(Number.SINGULAR, result.getNumber(), "Template type should be SINGULAR when number is SINGULAR");

				else
					assertEquals(Number.PLURAL, result.getNumber(), "Template type should be PLURAL when number is PLURAL");

				String sentence = result.getPattern();

				assertNotNull(sentence);
				assertFalse(sentence.isEmpty());
				System.out.println(sentence);
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
		List<Noun> nouns = List.of(new Noun("satellite", Number.SINGULAR));
		List<Adjective> adjectives = List.of(new Adjective("metallic"));
		List<Verb> verbs = List.of(new Verb("orbits", Number.SINGULAR, Tense.PRESENT));

		Template template = new Template("The [adjective] [noun] [verb] while the [adjective] [noun] [verb].", Number.SINGULAR);

		sentenceGenerator.generateSentenceFromTemplate(template);
		Template result = sentenceGenerator.generateSentenceWith(nouns, adjectives, verbs);

		assertNotNull(result);
		String sentence = result.getPattern();

		assertTrue(sentence.contains("satellite") || sentence.contains("metallic") || sentence.contains("orbit"), "Sentence should contain at least one of our custom words");
System.out.println(sentence);
		assertFalse(sentence.contains("[noun]"));
		assertFalse(sentence.contains("[adjective]"));
		assertFalse(sentence.contains("[verb]"));
	}

	@Test
	@DisplayName("Test template with mixed placeholders and literals")
	void testMixedPlaceholdersAndLiterals()
	{
		Template template = new Template("The [adjective] [noun] [verb] quickly.", Number.SINGULAR);
		Template result = sentenceGenerator.generateSentenceFromTemplate(template);
		assertNotNull(result);
		String sentence = result.getPattern();

		assertFalse(sentence.contains("[adjective]"));
		assertFalse(sentence.contains("[noun]"));
		assertFalse(sentence.contains("[verb]"));
		assertTrue(sentence.endsWith("quickly."), "Sentence should preserve literal text");
	}

	@Test
	@DisplayName("Test sentence generation with all custom words")
	void testAllCustomWords() throws IOException
	{
		List<Noun> nouns = List.of(new Noun("dragon", Number.SINGULAR));
		List<Adjective> adjectives = List.of(new Adjective("fiery"));
		List<Verb> verbs = List.of(new Verb("breathes", Number.SINGULAR, Tense.PRESENT));

		Template result = sentenceGenerator.generateSentenceWith(nouns, adjectives, verbs);
		String sentence = result.getPattern();

		assertTrue(sentence.contains("dragon"));
		assertTrue(sentence.contains("fiery"));
		assertTrue(sentence.contains("breathes"));
	}

	@Test
	@DisplayName("Test repeated calls to generateRandomSentence produce varied results")
	void testRandomSentenceVariation()
	{
		Set<String> sentences = new HashSet<>();
		for(int i = 0; i < 10; i++)
			sentences.add(sentenceGenerator.generateRandomSentence().getPattern());

		assertTrue(sentences.size() > 1, "Repeated calls should produce varied sentences");
	}

	@Test
	@DisplayName("Test sentence generation with maximum word length")
	void testMaxWordLength() throws IOException
	{
		List<Noun> nouns = List.of(new Noun("a".repeat(1000), Number.SINGULAR));
		List<Adjective> adjectives = List.of(new Adjective("b".repeat(1000)));
		List<Verb> verbs = List.of(new Verb("c".repeat(1000), Number.SINGULAR, Tense.PRESENT));

		Template result = sentenceGenerator.generateSentenceWith(nouns, adjectives, verbs);
		assertNotNull(result);
		assertFalse(result.getPattern().isEmpty());
	}

	@Test
	@DisplayName("Test template with only verb placeholders")
	void testVerbOnlyTemplate()
	{
		Template template = new Template("[verb] [verb] [verb]", Number.SINGULAR);
		Template result = sentenceGenerator.generateSentenceFromTemplate(template);
		String sentence = result.getPattern();

		assertFalse(sentence.contains("[verb]"));
		assertTrue(sentence.split(" ").length >= 3, "Should contain at least 3 verbs");
	}

	@Test
	@DisplayName("Test concurrent sentence generation")
	void testConcurrentGeneration() throws InterruptedException
	{
		int threadCount = 20;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for(int i = 0; i < threadCount; i++)
		{
			executor.submit(() ->
				{
					try
					{
						Template sentence = sentenceGenerator.generateRandomSentence();
						assertNotNull(sentence);
						assertFalse(sentence.getPattern().isEmpty());
					}
					finally
					{
						latch.countDown();
					}
				});
		}

		latch.await(3, TimeUnit.SECONDS);
		executor.shutdown();
		assertEquals(0, latch.getCount());
	}

	@Test
	@DisplayName("Test template with unusual punctuation")
	void testUnusualPunctuation()
	{
		Template template = new Template("Wait... the [noun] [verb]! Really?", Number.SINGULAR);
		Template result = sentenceGenerator.generateSentenceFromTemplate(template);
		String sentence = result.getPattern();

		assertTrue(sentence.startsWith("Wait..."));
		assertTrue(sentence.endsWith("Really?"));
		assertFalse(sentence.contains("[noun]"));
		assertFalse(sentence.contains("[verb]"));
	}

	@Test
	@DisplayName("Test empty word lists fallback to random generation")
	void testEmptyWordListsFallback() throws IOException
	{
		Template result = sentenceGenerator.generateSentenceWith
		(
			new ArrayList<>(),
			new ArrayList<>(),
			new ArrayList<>()
		);

		assertNotNull(result);
		assertFalse(result.getPattern().isEmpty());
		assertFalse(result.getPattern().contains("[noun]"));
		assertFalse(result.getPattern().contains("[adjective]"));
		assertFalse(result.getPattern().contains("[verb]"));
	}

	@Test
	@DisplayName("Test sentence generation with mixed number requirements")
	void testMixedNumberRequirements() throws IOException
	{
		List<Noun> nouns = Arrays.asList(new Noun("cat", Number.SINGULAR), new Noun("dogs", Number.PLURAL));
		List<Adjective> adjectives = Arrays.asList(new Adjective("sleepy"));
		List<Verb> verbs = Arrays.asList(new Verb("run", Number.PLURAL, Tense.PRESENT));

		Template result = sentenceGenerator.generateSentenceWith(nouns, adjectives, verbs);
		String sentence = result.getPattern();

		assertTrue(sentence.contains("cat") || sentence.contains("dogs"), "Sentence should contain at least one of the provided nouns");
		assertTrue(sentence.contains("sleepy"));
		assertTrue(sentence.contains("run"));
	}

	@Test
	@DisplayName("Test template with all placeholders at the beginning")
	void testPlaceholdersAtBeginning()
	{
		Template template = new Template("[noun] [verb] [adjective] the end.", Number.SINGULAR);
		Template result = sentenceGenerator.generateSentenceFromTemplate(template);
		String sentence = result.getPattern();

		assertFalse(sentence.startsWith("["));
		assertTrue(sentence.endsWith("the end."));
	}

	@Test
	@DisplayName("Test template with repeated same placeholders")
	void testRepeatedSamePlaceholders()
	{
		Template template = new Template("The [noun] and [noun] [verb] together.", Number.PLURAL);
		Template result = sentenceGenerator.generateSentenceFromTemplate(template);
		String sentence = result.getPattern();

		long nounCount = Arrays.stream(sentence.split(" ")).filter(word -> word.equals(result.getPattern().split(" ")[1])).count();
		assertFalse(nounCount >= 2, "Shouldn't contain repeated nouns");
		assertFalse(sentence.contains("[noun]"));
	}

	@Test
	@DisplayName("Test sentence generation with special characters in words")
	void testSpecialCharactersInWords() throws IOException
	{
		List<Noun> nouns = List.of(new Noun("café", Number.SINGULAR));
		List<Adjective> adjectives = List.of(new Adjective("naïve"));
		List<Verb> verbs = List.of(new Verb("cliché", Number.SINGULAR, Tense.PRESENT));

		Template result = sentenceGenerator.generateSentenceWith(nouns, adjectives, verbs);
		String sentence = result.getPattern();

		assertTrue(sentence.contains("café") || sentence.contains("naïve") || sentence.contains("cliché"));
	}

	@Test
	@DisplayName("Test extremely long template")
	void testExtremelyLongTemplate()
	{
		StringBuilder longTemplate = new StringBuilder("The ");
		for(int i = 0; i < 50; i++)
			longTemplate.append("[adjective] [noun] [verb], then ");

		longTemplate.append("finally [verb].");

		Template template = new Template(longTemplate.toString(), Number.SINGULAR);
		Template result = sentenceGenerator.generateSentenceFromTemplate(template);
		String sentence = result.getPattern();

		assertFalse(sentence.contains("[") || sentence.contains("]"));
	}

	@Test
	@DisplayName("Test template with only adjective placeholders")
	void testAdjectiveOnlyTemplate()
	{
		Template template = new Template("[adjective] [adjective] [adjective]", Number.SINGULAR);
		Template result = sentenceGenerator.generateSentenceFromTemplate(template);
		String sentence = result.getPattern();

		assertFalse(sentence.contains("[adjective]"));
		assertTrue(sentence.split(" ").length >= 3, "Should contain at least 3 adjectives");
	}

	@Test
	@DisplayName("Test template with mixed numbers in custom words")
	void testMixedNumbersInCustomWords() throws IOException
	{
		List<Noun> nouns = Arrays.asList(new Noun("child", Number.SINGULAR), new Noun("children", Number.PLURAL));
		List<Verb> verbs = Arrays.asList(new Verb("plays", Number.SINGULAR, Tense.PRESENT), new Verb("play", Number.PLURAL, Tense.PRESENT));

		Template result = sentenceGenerator.generateSentenceWith(nouns, null, verbs);
		String sentence = result.getPattern();

		assertTrue(sentence.contains("child") || sentence.contains("children"));
		assertTrue(sentence.contains("plays") || sentence.contains("play"));
	}

	@Test
	@DisplayName("Test concurrent custom word generation")
	void testConcurrentCustomWordGeneration() throws InterruptedException
	{
		int threadCount = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for(int i = 0; i < threadCount; i++)
		{
			executor.submit(() ->
			{
				try
				{
					List<Noun> nouns = List.of(new Noun("concurrent" + Thread.currentThread().getId(), Number.SINGULAR));
					Template sentence = sentenceGenerator.generateSentenceWith(nouns, null, null);
					assertNotNull(sentence);
					assertTrue(sentence.getPattern().contains("concurrent"));
				}
				catch(IOException e)
				{
					fail("IOException occurred");
				}
				finally
				{
					latch.countDown();
				}
			});
		}

		latch.await(3, TimeUnit.SECONDS);
		executor.shutdown();
		assertEquals(0, latch.getCount());
	}
}
