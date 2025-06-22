package unipd.nonsense.generator;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Template;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Tense;
import unipd.nonsense.model.Placeholder;

import unipd.nonsense.generator.RandomNounGenerator;
import unipd.nonsense.generator.RandomVerbGenerator;
import unipd.nonsense.generator.RandomAdjectiveGenerator;
import unipd.nonsense.generator.RandomTemplateGenerator;

import unipd.nonsense.util.JsonUpdater;
import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.exceptions.InvalidNumberException;
import unipd.nonsense.exceptions.InvalidTemplateException;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

/**
 * A generator for creating random sentences based on templates and word categories.
 * <p>
 * This class combines nouns, verbs, adjectives and templates to generate grammatically correct sentences.
 * It implements {@code AutoCloseable} for proper resource cleanup of its component generators.
 * </p>
 *
 * <p>Example usage:
 * <pre>{@code
 * try(SentenceGenerator generator = new SentenceGenerator())
 * {
 *     Template sentence = generator.generateRandomSentence();
 *     System.out.println(sentence.getPattern());
 *
 *     Template futureSentence = generator.generateSentenceWithTense(Tense.FUTURE);
 * }
 * catch(IOException e)
 * {
 *     // Handle exception
 * }
 * }</pre>
 * </p>
 *
 * @see RandomNounGenerator
 * @see RandomVerbGenerator
 * @see RandomAdjectiveGenerator
 * @see RandomTemplateGenerator
 * @see Template
 */
public class SentenceGenerator implements AutoCloseable
{
	/**
	 * Generator for random nouns.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Initialized during construction</li>
	 *	<li>Used for noun placeholder replacement</li>
	 *	<li>Must be cleaned up when no longer needed</li>
	 * </ul>
	 */
	private final RandomNounGenerator nounGenerator;

	/**
	 * Generator for random adjectives.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Initialized during construction</li>
	 *	<li>Used for adjective placeholder replacement</li>
	 *	<li>Must be cleaned up when no longer needed</li>
	 * </ul>
	 */
	private final RandomAdjectiveGenerator adjectiveGenerator;

	/**
	 * Generator for random verbs.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Initialized during construction</li>
	 *	<li>Used for verb placeholder replacement</li>
	 *	<li>Must be cleaned up when no longer needed</li>
	 * </ul>
	 */
	private final RandomVerbGenerator verbGenerator;

	/**
	 * Generator for random sentence templates.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Initialized during construction</li>
	 *	<li>Provides the sentence structure</li>
	 *	<li>Must be cleaned up when no longer needed</li>
	 * </ul>
	 */
	private final RandomTemplateGenerator templateGenerator;

	/**
	 * Logger instance for operation tracking.
	 * <p>
	 * Configured to log messages from {@code SentenceGenerator} class.
	 */
	private final LoggerManager logger = new LoggerManager(SentenceGenerator.class);

	/**
	 * Random number generator instance.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Static for consistent randomness across instances</li>
	 *	<li>Thread-safe for concurrent access</li>
	 *	<li>Used for random selections throughout the class</li>
	 * </ul>
	 */
	private static final Random random = new Random();

	/**
	 * Constructs a sentence generator with default component generators.
	 *
	 * @throws IOException	if any of the component generators fail to initialize
	 */
	public SentenceGenerator() throws IOException
	{
		logger.logTrace("Starting initialization");

		try
		{
			logger.logTrace("Initializing component generators");

			this.nounGenerator = new RandomNounGenerator();
			this.adjectiveGenerator = new RandomAdjectiveGenerator();
			this.verbGenerator = new RandomVerbGenerator();
			this.templateGenerator = new RandomTemplateGenerator();

			logger.logTrace("Successfully initialized");
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize", e);
			throw e;
		}
	}

	/**
	 * Generates a completely random sentence with random tense and number.
	 * <p>
	 * Process:
	 * <ul>
	 *	<li>Selects random grammatical number</li>
	 *	<li>Selects random tense</li>
	 *	<li>Generates sentence matching these parameters</li>
	 *	<li>Capitalizes the first letter of the result</li>
	 * </ul>
	 *
	 * @return			A {@code Template} containing the generated sentence
	 */
	public Template generateRandomSentence()
	{
		logger.logTrace("generateRandomSentence: Starting sentence generation");

		Number randomNumber = getRandomNumber();
		Tense randomTense = getRandomTense();

		logger.logDebug("generateRandomSentence: Selected number: " + randomNumber + ", tense: " + randomTense);

		Template result = generateSentenceWithTenseAndNumber(randomTense, randomNumber);
		logger.logTrace("generateRandomSentence: Completed sentence generation");
		return new Template(capitalizeFirstLetter(result.getPattern()),result.getNumber());
	}

	/**
	 * Capitalizes the first letter of a string.
	 * <p>
	 * Handles edge cases:
	 * <ul>
	 *	<li>Null input returns null</li>
	 *	<li>Empty string returns empty string</li>
	 *	<li>Single character strings are capitalized</li>
	 * </ul>
	 *
	 * @param sentence	The string to capitalize
	 * @return			The capitalized string or original if null/empty
	 */
	private String capitalizeFirstLetter(String sentence)
	{
		if(sentence == null || sentence.isEmpty())
			return sentence;

		return sentence.substring(0, 1).toUpperCase() + sentence.substring(1);
	}

	/**
	 * Generates a sentence using custom lists of words.
	 * <p>
	 * Process:
	 * <ul>
	 *	<li>Uses provided words before falling back to random generation</li>
	 *	<li>Determines template based on first noun's number if available</li>
	 *	<li>Updates JSON data with provided words</li>
	 *	<li>Replaces placeholders in template</li>
	 * </ul>
	 *
	 * @param nounList			List of nouns to use (can be null)
	 * @param adjectiveList		List of adjectives to use (can be null)
	 * @param verbList			List of verbs to use (can be null)
	 * @return					A {@code Template} containing the generated sentence
	 * @throws IOException		if updating JSON data fails
	 */
	public Template generateSentenceWith(List<Noun> nounList, List<Adjective> adjectiveList, List<Verb> verbList) throws IOException
	{
		logger.logTrace("generateSentenceWith: Starting custom sentence generation");

		List<Noun> nouns = nounList != null ? new ArrayList<>(nounList) : new ArrayList<>();
		List<Adjective> adjectives = adjectiveList != null ? new ArrayList<>(adjectiveList) : new ArrayList<>();
		List<Verb> verbs = verbList != null ? new ArrayList<>(verbList) : new ArrayList<>();

		logger.logDebug("generateSentenceWith: Received " + nouns.size() + " nouns, " + adjectives.size() + " adjectives, " + verbs.size() + " verbs");

		Template template = templateGenerator.getRandomTemplate();

		if(nouns != null && !nouns.isEmpty())
			template = templateGenerator.getRandomTemplate(nouns.get(0).getNumber());

		try
		{
			logger.logTrace("generateSentenceWith: Loading words into JsonUpdater");
			loadWordsIntoJsonUpdater(nouns, adjectives, verbs);
		}
		catch(IOException e)
		{
			logger.logError("generateSentenceWith: Failed to load new words into JsonUpdater", e);
			throw e;
		}

		logger.logTrace("generateSentenceWith: Replacing placeholders");
		replacePlaceholders(template, nouns, adjectives, verbs);

		logger.logTrace("generateSentenceWith: Completed custom sentence generation");
		return template;
	}

	/**
	 * Loads custom words into the JSON updater system.
	 * <p>
	 * Process:
	 * <ul>
	 *	<li>Adds nouns to noun dictionary</li>
	 *	<li>Adds adjectives to adjective dictionary</li>
	 *	<li>Adds verbs to verb dictionary</li>
	 *	<li>Preserves existing words</li>
	 * </ul>
	 *
	 * @param nouns			List of nouns to add
	 * @param adjectives	List of adjectives to add
	 * @param verbs			List of verbs to add
	 * @throws IOException	if JSON update fails
	 */
	private void loadWordsIntoJsonUpdater(List<Noun> nouns, List<Adjective> adjectives, List<Verb> verbs) throws IOException
	{
		logger.logTrace("loadWordsIntoJsonUpdater: Starting to load words");

		for(Noun noun : nouns)
		{
			logger.logDebug("loadWordsIntoJsonUpdater: Loading noun: " + noun.getNoun());
			JsonUpdater.loadNoun(noun);
		}

		for(Adjective adjective : adjectives)
		{
			logger.logDebug("loadWordsIntoJsonUpdater: Loading adjective: " + adjective.getAdjective());
			JsonUpdater.loadAdjective(adjective);
		}

		for(Verb verb : verbs)
		{
			logger.logDebug("loadWordsIntoJsonUpdater: Loading verb: " + verb.getVerb());
			JsonUpdater.loadVerb(verb);
		}

		logger.logTrace("loadWordsIntoJsonUpdater: Completed loading words");
	}

	/**
	 * Replaces all placeholders in a template with words.
	 * <p>
	 * Process:
	 * <ul>
	 *	<li>Handles noun placeholders first</li>
	 *	<li>Then handles adjective placeholders</li>
	 *	<li>Finally handles verb placeholders</li>
	 *	<li>Uses custom words when available</li>
	 * </ul>
	 *
	 * @param template		The template to modify
	 * @param nouns			List of nouns for replacement
	 * @param adjectives	List of adjectives for replacement
	 * @param verbs			List of verbs for replacement
	 */
	private void replacePlaceholders(Template template, List<Noun> nouns, List<Adjective> adjectives, List<Verb> verbs)
	{
		logger.logTrace("replacePlaceholders: Starting placeholder replacement");

		replaceNounPlaceholders(template, nouns);
		replaceAdjectivePlaceholders(template, adjectives);
		replaceVerbPlaceholders(template, verbs);

		logger.logTrace("replacePlaceholders: Completed placeholder replacement");
	}

	/**
	 * Replaces noun placeholders in a template.
	 * <p>
	 * Process:
	 * <ul>
	 *	<li>Uses custom nouns first</li>
	 *	<li>Falls back to random generation</li>
	 *	<li>Maintains template's grammatical number</li>
	 *	<li>Processes all noun placeholders</li>
	 * </ul>
	 *
	 * @param template	The template to modify
	 * @param nouns		List of nouns for replacement
	 */
	private void replaceNounPlaceholders(Template template, List<Noun> nouns)
	{
		logger.logTrace("replaceNounPlaceholders: Starting noun replacement");

		while(template.countPlaceholders(Placeholder.NOUN) > 0)
		{
			String noun = !nouns.isEmpty() ? nouns.remove(0).getNoun() : nounGenerator.getRandomNoun(template.getNumber()).getNoun();
			logger.logDebug("replaceNounPlaceholders: Replacing with noun: " + noun);
			template.replacePlaceholder(Placeholder.NOUN, noun);
		}

		logger.logTrace("replaceNounPlaceholders: Completed noun replacement");
	}

	/**
	 * Replaces adjective placeholders in a template.
	 * <p>
	 * Process:
	 * <ul>
	 *	<li>Uses custom adjectives first</li>
	 *	<li>Falls back to random generation</li>
	 *	<li>Processes all adjective placeholders</li>
	 * </ul>
	 *
	 * @param template		The template to modify
	 * @param adjectives	List of adjectives for replacement
	 */
	private void replaceAdjectivePlaceholders(Template template, List<Adjective> adjectives)
	{
		logger.logTrace("replaceAdjectivePlaceholders: Starting adjective replacement");
		while(template.countPlaceholders(Placeholder.ADJECTIVE) > 0)
		{
			String adjective = !adjectives.isEmpty() ? adjectives.remove(0).getAdjective() : adjectiveGenerator.getRandomAdjective().getAdjective();
			logger.logDebug("replaceAdjectivePlaceholders: Replacing with adjective: " + adjective);
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
		}

		logger.logTrace("replaceAdjectivePlaceholders: Completed adjective replacement");
	}

	/**
	 * Replaces verb placeholders in a template.
	 * <p>
	 * Process:
	 * <ul>
	 *	<li>Uses custom verbs first</li>
	 *	<li>Falls back to random generation</li>
	 *	<li>Uses random tense for all verbs</li>
	 *	<li>Maintains template's grammatical number</li>
	 *	<li>Processes all verb placeholders</li>
	 * </ul>
	 *
	 * @param template	The template to modify
	 * @param verbs		List of verbs for replacement
	 */
	private void replaceVerbPlaceholders(Template template, List<Verb> verbs)
	{
		logger.logTrace("replaceVerbPlaceholders: Starting verb replacement");
		Tense tense = getRandomTense();

		while(template.countPlaceholders(Placeholder.VERB) > 0)
		{
			String verb = !verbs.isEmpty() ? verbs.remove(0).getVerb() : verbGenerator.getRandomVerb(template.getNumber(), tense).getVerb();
			logger.logDebug("replaceVerbPlaceholders: Replacing with verb: " + verb);
			template.replacePlaceholder(Placeholder.VERB, verb);
		}

		logger.logTrace("replaceVerbPlaceholders: Completed verb replacement");
	}

	/**
	 * Generates a sentence with specified tense and random number.
	 * <p>
	 * Process:
	 * <ul>
	 *	<li>Selects random grammatical number</li>
	 *	<li>Uses specified tense for all verbs</li>
	 *	<li>Generates matching sentence structure</li>
	 * </ul>
	 *
	 * @param tense		The tense to use for verbs
	 * @return			A {@code Template} containing the generated sentence
	 */
	public Template generateSentenceWithTense(Tense tense)
	{
		logger.logDebug("generateSentenceWithTense: Starting generation with tense: " + tense);
		Template result = generateSentenceWithTenseAndNumber(tense, getRandomNumber());
		logger.logTrace("generateSentenceWithTense: Completed generation");
		return result;
	}

	/**
	 * Generates a sentence with specified number and random tense.
	 * <p>
	 * Process:
	 * <ul>
	 *	<li>Uses specified grammatical number</li>
	 *	<li>Selects random tense for verbs</li>
	 *	<li>Generates matching sentence structure</li>
	 * </ul>
	 *
	 * @param number	The grammatical number to use
	 * @return			A {@code Template} containing the generated sentence
	 */
	public Template generateSentenceWithNumber(Number number)
	{
		logger.logDebug("generateSentenceWithNumber: Starting generation with number: " + number);
		Template result = generateSentenceWithTenseAndNumber(getRandomTense(), number);
		logger.logTrace("generateSentenceWithNumber: Completed generation");
		return result;
	}

	/**
	 * Generates a sentence with specified tense and number.
	 * <p>
	 * Process:
	 * <ul>
	 *	<li>Gets template matching specified number</li>
	 *	<li>Replaces all placeholders with matching words</li>
	 *	<li>Uses specified tense for verbs</li>
	 *	<li>Maintains grammatical consistency</li>
	 * </ul>
	 *
	 * @param tense		The tense to use for verbs
	 * @param number	The grammatical number to use
	 * @return			A {@code Template} containing the generated sentence
	 */
	public Template generateSentenceWithTenseAndNumber(Tense tense, Number number)
	{
		logger.logDebug("generateSentenceWithTenseAndNumber: Starting generation with tense: " + tense + ", number: " + number);
		Template template = templateGenerator.getRandomTemplate(number);

		while(template.countPlaceholders(Placeholder.NOUN) != 0)
		{
			String noun = nounGenerator.getRandomNoun(number).getNoun();
			logger.logDebug("generateSentenceWithTenseAndNumber: Replacing noun with: " + noun);
			template.replacePlaceholder(Placeholder.NOUN, noun);
		}

		while(template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
		{
			String adjective = adjectiveGenerator.getRandomAdjective().getAdjective();
			logger.logDebug("generateSentenceWithTenseAndNumber: Replacing adjective with: " + adjective);
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
		}

		while(template.countPlaceholders(Placeholder.VERB) != 0)
		{
			String verb = verbGenerator.getRandomVerb(number, tense).getVerb();
			logger.logDebug("generateSentenceWithTenseAndNumber: Replacing verb with: " + verb);
			template.replacePlaceholder(Placeholder.VERB, verb);
		}

		logger.logTrace("generateSentenceWithTenseAndNumber: Completed generation");
		return template;
	}

	/**
	 * Generates a sentence from an existing template.
	 * <p>
	 * Process:
	 * <ul>
	 *	<li>Validates template structure</li>
	 *	<li>Replaces all placeholders with random words</li>
	 *	<li>Maintains template's grammatical number</li>
	 *	<li>Uses random tenses for verbs</li>
	 * </ul>
	 *
	 * @param template	The template to populate
	 * @return			The populated template
	 * @throws InvalidTemplateException	if template is null or empty
	 */
	public Template generateSentenceFromTemplate(Template template)
	{
		logger.logTrace("generateSentenceFromTemplate: Starting generation from template");

		if(template == null || template.getPattern().isEmpty())
		{
			logger.logError("generateSentenceFromTemplate: Invalid template provided");
			throw new InvalidTemplateException();
		}

		while(template.countPlaceholders(Placeholder.NOUN) != 0)
		{
			String noun = nounGenerator.getRandomNoun(template.getNumber()).getNoun();
			logger.logDebug("generateSentenceFromTemplate: Replacing noun with: " + noun);
			template.replacePlaceholder(Placeholder.NOUN, noun);
		}

		while(template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
		{
			String adjective = adjectiveGenerator.getRandomAdjective().getAdjective();
			logger.logDebug("generateSentenceFromTemplate: Replacing adjective with: " + adjective);
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
		}

		while(template.countPlaceholders(Placeholder.VERB) != 0)
		{
			String verb = verbGenerator.getRandomVerb(template.getNumber(), getRandomTense()).getVerb();
			logger.logDebug("generateSentenceFromTemplate: Replacing verb with: " + verb);
			template.replacePlaceholder(Placeholder.VERB, verb);
		}

		logger.logTrace("generateSentenceFromTemplate: Completed generation");
		return template;
	}

	/**
	 * Gets a list of 5 random templates.
	 * <p>
	 * Process:
	 * <ul>
	 *	<li>Selects templates randomly</li>
	 *	<li>Returns unpopulated templates</li>
	 *	<li>Maintains original template structure</li>
	 * </ul>
	 *
	 * @return	List of 5 random {@code Template} objects
	 */
	public List<Template> getRandomTemplates()
	{
		logger.logTrace("getRandomTemplates: Starting to get random templates");
		List<Template> templateList = new ArrayList<>();

		for(int i = 0; i < 5; ++i)
		{
			Template template = templateGenerator.getRandomTemplate();
			templateList.add(template);
			logger.logDebug("getRandomTemplates: Added template: " + template.getPattern());
		}

		logger.logTrace("getRandomTemplates: Completed getting templates");

		return templateList;
	}

	/**
	 * Gets a random grammatical number.
	 * <p>
	 * Selection:
	 * <ul>
	 *	<li>Uniform distribution between options</li>
	 *	<li>Logs the selection</li>
	 * </ul>
	 *
	 * @return	A randomly selected {@code Number}
	 */
	private Number getRandomNumber()
	{
		logger.logTrace("getRandomNumber: Getting random number");
		Number[] numbers = Number.values();
		Number randomNumber = numbers[random.nextInt(numbers.length)];
		logger.logDebug("getRandomNumber: Selected number: " + randomNumber);

		return randomNumber;
	}

	/**
	 * Gets a random verb tense.
	 * <p>
	 * Selection:
	 * <ul>
	 *	<li>Uniform distribution between options</li>
	 *	<li>Logs the selection</li>
	 * </ul>
	 *
	 * @return	A randomly selected {@code Tense}
	 */
	private Tense getRandomTense()
	{
		logger.logTrace("getRandomTense: Getting random tense");
		Tense[] tenses = Tense.values();
		Tense randomTense = tenses[random.nextInt(tenses.length)];
		logger.logDebug("getRandomTense: Selected tense: " + randomTense);

		return randomTense;
	}

	/**
	 * Performs resource cleanup.
	 * <p>
	 * Implementation of {@code AutoCloseable} that:
	 * <ul>
	 *	<li>Cleans up all component generators</li>
	 *	<li>Should be called when generator is no longer needed</li>
	 *	<li>Prevents resource leaks</li>
	 * </ul>
	 */
	@Override
	public void close()
	{
		logger.logTrace("close: Starting cleanup");

		nounGenerator.cleanup();
		adjectiveGenerator.cleanup();
		verbGenerator.cleanup();
		templateGenerator.cleanup();

		logger.logTrace("close: Completed cleanup");
	}
}
