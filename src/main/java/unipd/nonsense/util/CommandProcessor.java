package unipd.nonsense.util;

import unipd.nonsense.generator.SyntaxTreeBuilder;
import unipd.nonsense.model.SyntaxToken;

import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.analyzer.SentenceAnalyzer;
import unipd.nonsense.analyzer.ToxicityValidator;

import unipd.nonsense.generator.SentenceGenerator;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Tense;
import unipd.nonsense.model.Template;

import unipd.nonsense.exceptions.SentenceNotCachedException;
import unipd.nonsense.exceptions.IllegalToleranceException;
import unipd.nonsense.exceptions.InvalidNumberException;
import unipd.nonsense.exceptions.InvalidTenseException;
import unipd.nonsense.exceptions.InvalidTextException;
import unipd.nonsense.exceptions.InvalidTemplateException;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

import com.google.cloud.language.v1.PartOfSpeech;

import static com.google.cloud.language.v1.PartOfSpeech.Number.SINGULAR;
import static com.google.cloud.language.v1.PartOfSpeech.Number.PLURAL;

import static com.google.cloud.language.v1.PartOfSpeech.Tense.PAST;
import static com.google.cloud.language.v1.PartOfSpeech.Tense.PRESENT;
import static com.google.cloud.language.v1.PartOfSpeech.Tense.FUTURE;

/**
 * A processor for handling various commands related to sentence generation, analysis, and toxicity validation.
 * <p>
 * This class serves as the main interface for:
 * <ul>
 *	<li>Generating sentences with different constraints</li>
 *	<li>Analyzing text for syntax, sentiment, and entities</li>
 *	<li>Validating text toxicity</li>
 *	<li>Managing cached sentences and settings</li>
 * </ul>
 * </p>
 *
 * <p>Implements {@code AutoCloseable} for proper resource management.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * try(CommandProcessor processor = new CommandProcessor())
 * {
 *     String sentence = processor.generateRandom();
 *     String toxicityReport = processor.analyzeToxicity(sentence);
 *     System.out.println(toxicityReport);
 * }
 * catch(IOException e)
 * {
 *     // Handle exception
 * }
 * }</pre>
 * </p>
 *
 * @see AutoCloseable
 * @see SyntaxTreeBuilder
 * @see SentenceAnalyzer
 * @see ToxicityValidator
 * @see SentenceGenerator
 */
public class CommandProcessor implements AutoCloseable
{
	/**
	 * Builder for generating syntax trees from text analysis.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Initialized during construction</li>
	 *	<li>Used in {@code generateSyntaxTree()} method</li>
	 *	<li>Thread-safe for concurrent operations</li>
	 * </ul>
	 * </p>
	 */
	private SyntaxTreeBuilder treeBuilder;

	/**
	 * Analyzer for performing various text analyses.
	 * <p>
	 * Capabilities:
	 * <ul>
	 *	<li>Syntax analysis</li>
	 *	<li>Sentiment analysis</li>
	 *	<li>Entity recognition</li>
	 *	<li>Async operation support</li>
	 * </ul>
	 * </p>
	 */
	private SentenceAnalyzer analyzer;

	/**
	 * Validator for toxicity detection in text.
	 * <p>
	 * Features:
	 * <ul>
	 *	<li>Toxicity scoring</li>
	 *	<li>Detailed toxicity reports</li>
	 *	<li>Threshold-based validation</li>
	 *	<li>Async operation support</li>
	 * </ul>
	 * </p>
	 */
	private ToxicityValidator validator;

	/**
	 * Generator for creating sentences with various constraints.
	 * <p>
	 * Generation options:
	 * <ul>
	 *	<li>Random sentences</li>
	 *	<li>Templated sentences</li>
	 *	<li>Number/Tense-specific sentences</li>
	 * </ul>
	 * </p>
	 */
	private SentenceGenerator generator;

	/**
	 * Logger for tracking operations and errors.
	 * <p>
	 * Configured to log messages from {@code CommandProcessor} class.
	 * </p>
	 */
	private LoggerManager logger = new LoggerManager(CommandProcessor.class);

	/**
	 * Cache for storing the last used/processed/generated sentence.
	 * <p>
	 * Behavior:
	 * <ul>
	 *	<li>Updated by most operations</li>
	 *	<li>Accessed via {@code getCachedSentence()}</li>
	 *	<li>Checked via {@code isSentenceCached()}</li>
	 * </ul>
	 * </p>
	 */
	private String cachedString;

	/**
	 * Threshold for toxicity validation.
	 * <p>
	 * Properties:
	 * <ul>
	 *	<li>Default value: 0.7</li>
	 *	<li>Range: 0.0 to 1.0</li>
	 *	<li>Configurable via {@code setTolerance()}</li>
	 * </ul>
	 * </p>
	 */
	private float toxicityTolerance;

	/**
	 * Constructs a new CommandProcessor with default settings.
	 *
	 * @throws IOException	if initialization of components fails
	 */
	public CommandProcessor() throws IOException
	{
		logger.logTrace("Initializing CommandProcessor");

		this.treeBuilder = new SyntaxTreeBuilder();
		this.analyzer = new SentenceAnalyzer();
		this.validator = new ToxicityValidator();
		this.generator = new SentenceGenerator();

		this.cachedString = "";
		toxicityTolerance = 0.7f;

		logger.logTrace("Initialized successfully");
	}

	/**
	 * Generates a completely random sentence.
	 * <p>
	 * Operation details:
	 * <ul>
	 *	<li>Uses {@code SentenceGenerator} internally</li>
	 *	<li>Caches the generated sentence</li>
	 * </ul>
	 * </p>
	 *
	 * @return		The generated random sentence
	 */
	public String generateRandom()
	{
		logger.logTrace("generateRandom: Generating random sentence");
		String result = generator.generateRandomSentence().getPattern();
		cachedString = result;
		logger.logDebug("generateRandom: Generated sentence: " + result);

		return result;
	}

	/**
	 * Generates a sentence based on input text.
	 * <p>
	 * Processing steps:
	 * <ul>
	 *	<li>Analyzes input text syntax</li>
	 *	<li>Extracts nouns, adjectives, and verbs</li>
	 *	<li>Generates new sentence with extracted words</li>
	 *	<li>Caches the result</li>
	 * </ul>
	 * </ul>
	 * </p>
	 *
	 * @param str			The input text to analyze
	 * @return				The generated sentence
	 * @throws IOException			if text analysis fails
	 * @throws InvalidTextException	if input text is null or empty
	 */
	public String generateFrom(String str) throws IOException
	{
		logger.logTrace("generateFrom: Generating sentence from input");

		if(str == null || str.isEmpty())
		{
			logger.logError("generateFrom: Invalid text input - null");
			throw new InvalidTextException();
		}

		List<SyntaxToken> analysis = analyzer.getSyntaxTokensAsync(str).join();
		logger.logDebug("generateFrom: Retrieved syntax tokens: " + analysis.size());

		List<Noun> nounList = new ArrayList<>();
		List<Adjective> adjectiveList = new ArrayList<>();
		List<Verb> verbList = new ArrayList<>();

		for(SyntaxToken token : analysis)
		{
			String posTag = token.getPosTag();
			if(posTag.equals("NOUN"))
			{
				PartOfSpeech.Number number = token.getPartOfSpeech().getNumber();
				if(number == PartOfSpeech.Number.SINGULAR || number == PartOfSpeech.Number.PLURAL)
					nounList.add(new Noun(token.getText(), fromPartOfSpeechNumberToNumber(number)));
			}
			else if(posTag.equals("ADJ"))
				adjectiveList.add(new Adjective(token.getText()));
			else if(posTag.equals("VERB"))
			{
				PartOfSpeech.Tense tense = token.getPartOfSpeech().getTense();
				PartOfSpeech.Number number = token.getPartOfSpeech().getNumber();
				if((tense == PartOfSpeech.Tense.PAST || tense == PartOfSpeech.Tense.PRESENT || tense == PartOfSpeech.Tense.FUTURE) && (number == PartOfSpeech.Number.SINGULAR || number == PartOfSpeech.Number.PLURAL))
					verbList.add(new Verb(token.getText(), fromPartOfSpeechNumberToNumber(number), fromPartOfSpeechTenseToTense(tense)));
			}
		}

		logger.logDebug("generateFrom: Extracted - Nouns: " + nounList.size() + ", Adjectives: " + adjectiveList.size() + ", Verbs: " + verbList.size());

		String result = generator.generateSentenceWith(nounList, adjectiveList, verbList).getPattern();
		cachedString = result;

		logger.logDebug("generateFrom: Generated sentence: " + result);
		return result;
	}

	/**
	 * Generates a sentence with specific grammatical number.
	 * <p>
	 * Constraints:
	 * <ul>
	 *	<li>Sentence will match specified number</li>
	 *	<li>All verbs/nouns will conform to number</li>
	 *	<li>Caches the result</li>
	 * </ul>
	 * </ul>
	 * </p>
	 *
	 * @param number	The grammatical number to use (singular/plural)
	 * @return			The generated sentence
	 * @throws InvalidNumberException	if number parameter is null
	 */
	public String generateWithNumber(Number number)
	{
		if(number == null)
			throw new InvalidNumberException();

		logger.logTrace("generateWithNumber: Generating sentence with number");
		String result = generator.generateSentenceWithNumber(number).getPattern();
		cachedString = result;
		logger.logDebug("generateWithNumber: Generated sentence with number " + number + ": " + result);

		return result;
	}

	/**
	 * Generates a sentence with specific grammatical tense.
	 * <p>
	 * Constraints:
	 * <ul>
	 *	<li>Sentence will match specified tense</li>
	 *	<li>All verbs will conform to tense</li>
	 *	<li>Caches the result</li>
	 * </ul>
	 * </ul>
	 * </p>
	 *
	 * @param tense		The grammatical tense to use (past/present/future)
	 * @return			The generated sentence
	 * @throws InvalidTenseException	if tense parameter is null
	 */
	public String generateWithTense(Tense tense)
	{
		if(tense == null)
			throw new InvalidTenseException();

		logger.logTrace("generateWithTense: Generating sentence with tense");
		String result = generator.generateSentenceWithTense(tense).getPattern();
		cachedString = result;
		logger.logDebug("generateWithTense: Generated sentence with tense " + tense + ": " + result);

		return result;
	}

	/**
	 * Generates a sentence with both specific number and tense.
	 * <p>
	 * Combines constraints from:
	 * <ul>
	 *	<li>{@code generateWithNumber()}</li>
	 *	<li>{@code generateWithTense()}</li>
	 * </ul>
	 * </p>
	 *
	 * @param number		The grammatical number to use
	 * @param tense			The grammatical tense to use
	 * @return				The generated sentence
	 * @throws InvalidNumberException	if number parameter is null
	 * @throws InvalidTenseException	if tense parameter is null
	 */
	public String generateWithBoth(Number number, Tense tense)
	{
		if(number == null)
			throw new InvalidNumberException();

		if(tense == null)
			throw new InvalidTenseException();

		logger.logTrace("generateWithBoth: Generating sentence with number and tense");
		String result = generator.generateSentenceWithTenseAndNumber(tense, number).getPattern();
		cachedString = result;
		logger.logDebug("generateWithBoth: Generated sentence with number " + number + " and tense " + tense + ": " + result);

		return result;

	}

	/**
	 * Generates a sentence from a predefined template.
	 * <p>
	 * The template defines:
	 * <ul>
	 *	<li>Sentence structure</li>
	 *	<li>Word types for each position</li>
	 * </ul>
	 * </p>
	 *
	 * @param template	The template to use for generation
	 * @return			The generated sentence
	 * @throws InvalidTemplateException	if template parameter is null
	 */
	public String generateWithTemplate(Template template)
	{
		logger.logTrace("generateWithTemplate: Generating sentence from template");

		if(template == null)
		{
			logger.logError("generateWithTemplate: Invalid template - null");
			throw new InvalidTemplateException();
		}

		String result = generator.generateSentenceFromTemplate(template).getPattern();
		cachedString = result;
		logger.logDebug("generateWithTemplate: Generated sentence from template: " + result);

		return result;
	}

	/**
	 * Retrieves a list of random templates for sentence generation.
	 * <p>
	 * Template characteristics:
	 * <ul>
	 *	<li>Predefined in the system</li>
	 *	<li>Can be used with {@code generateWithTemplate()}</li>
	 *	<li>Vary in complexity and structure</li>
	 * </ul>
	 * </p>
	 *
	 * @return	List of available templates
	 */
	public List<Template> getRandomTemplates()
	{
		logger.logTrace("getRandomTemplates: Getting random templates");
		List<Template> templates = generator.getRandomTemplates();
		logger.logDebug("getRandomTemplates: Retrieved " + templates.size() + " templates");

		return templates;
	}

	/**
	 * Converts Google Cloud POS number to internal {@code Number} enum.
	 * <p>
	 * Supported conversions:
	 * <ul>
	 *	<li>{@code SINGULAR} → {@code Number.SINGULAR}</li>
	 *	<li>{@code PLURAL} → {@code Number.PLURAL}</li>
	 * </ul>
	 * </p>
	 *
	 * @param number	The Google Cloud POS number to convert
	 * @return			The corresponding internal {@code Number} enum
	 * @throws InvalidNumberException	if conversion is not possible
	 */
	private Number fromPartOfSpeechNumberToNumber(PartOfSpeech.Number number)
	{
		logger.logTrace("fromPartOfSpeechNumberToNumber: Converting number");

		try
		{
			switch(number)
			{
				case SINGULAR: return Number.SINGULAR;
				case PLURAL: return Number.PLURAL;
				default: throw new InvalidNumberException();
			}
		}
		catch(InvalidNumberException e)
		{
			logger.logError("fromPartOfSpeechNumberToNumber: Invalid number conversion", e);
			throw e;
		}
	}

	/**
	 * Converts Google Cloud POS tense to internal {@code Tense} enum.
	 * <p>
	 * Supported conversions:
	 * <ul>
	 *	<li>{@code PAST} → {@code Tense.PAST}</li>
	 *	<li>{@code PRESENT} → {@code Tense.PRESENT}</li>
	 *	<li>{@code FUTURE} → {@code Tense.FUTURE}</li>
	 * </ul>
	 * </p>
	 *
	 * @param tense	The Google Cloud POS tense to convert
	 * @return		The corresponding internal {@code Tense} enum
	 * @throws InvalidTenseException	if conversion is not possible
	 */
	private Tense fromPartOfSpeechTenseToTense(PartOfSpeech.Tense tense)
	{
		logger.logTrace("fromPartOfSpeechTenseToTense: Converting tense");

		try
		{
			switch(tense)
			{
				case PAST: return Tense.PAST;
				case PRESENT: return Tense.PRESENT;
				case FUTURE: return Tense.FUTURE;
				default: throw new InvalidTenseException();
			}
		}
		catch(InvalidTenseException e)
		{
			logger.logError("fromPartOfSpeechTenseToTense: Invalid tense conversion", e);
			throw e;
		}
	}

	/**
	 * Generates a syntax tree representation of input text.
	 * <p>
	 * The syntax tree:
	 * <ul>
	 *	<li>Shows grammatical structure</li>
	 *	<li>Identifies parts of speech</li>
	 *	<li>Shows relationships between words</li>
	 * </ul>
	 * </p>
	 *
	 * @param str			The text to analyze
	 * @return				Formatted syntax tree
	 * @throws IOException	if analysis fails
	 * @throws InvalidTextException	if input text is null
	 */
	public String generateSyntaxTree(String str) throws IOException
	{
		logger.logTrace("generateSyntaxTree: Generating syntax tree");

		if(str == null)
		{
			logger.logError("generateSyntaxTree: Text input null");
			throw new InvalidTextException();
		}

		cachedString = str;
		String result = treeBuilder.getSyntaxTree(analyzer.getSyntaxTokensAsync(str).join());
		logger.logDebug("generateSyntaxTree: Generated syntax tree for: " + str);

		return result;
	}

	/**
	 * Performs detailed syntax analysis on input text.
	 * <p>
	 * Analysis includes:
	 * <ul>
	 *	<li>Part-of-speech tagging</li>
	 *	<li>Dependency parsing</li>
	 *	<li>Morphological analysis</li>
	 * </ul>
	 * </p>
	 *
	 * @param str	The text to analyze
	 * @return		Formatted analysis results
	 * @throws InvalidTextException	if input text is null
	 */
	public String analyzeSyntax(String str)
	{
		logger.logTrace("analyzeSyntax: Analyzing syntax");

		if(str == null)
		{
			logger.logError("analyzeSyntax: Text input null");
			throw new InvalidTextException();
		}

		cachedString = str;
		String result = analyzer.analyzeSyntaxAsync(str).join();
		logger.logDebug("analyzeSyntax: Analyzed syntax for: " + str);

		return result;
	}

	/**
	 * Analyzes the sentiment of input text.
	 * <p>
	 * Sentiment analysis:
	 * <ul>
	 *	<li>Scores positivity/negativity</li>
	 *	<li>Provides magnitude score</li>
	 *	<li>Identifies key emotional phrases</li>
	 * </ul>
	 * </p>
	 *
	 * @param str	The text to analyze
	 * @return		Formatted sentiment analysis
	 * @throws InvalidTextException	if input text is null
	 */
	public String analyzeSentiment(String str)
	{
		logger.logTrace("analyzeSentiment: Analyzing sentiment");

		if(str == null)
		{
			logger.logError("analyzeSentiment: Text input null");
			throw new InvalidTextException();
		}

		cachedString = str;
		String result = analyzer.analyzeSentimentAsync(str).join();
		logger.logDebug("analyzeSentiment: Analyzed sentiment for: " + str);

		return result;
	}

	/**
	 * Identifies and classifies entities in input text.
	 * <p>
	 * Entity types detected:
	 * <ul>
	 *	<li>People, organizations, locations</li>
	 *	<li>Dates, numbers, percentages</li>
	 *	<li>Custom entity types</li>
	 * </ul>
	 * </p>
	 *
	 * @param str	The text to analyze
	 * @return		Formatted entity analysis
	 * @throws InvalidTextException	if input text is null
	 */
	public String analyzeEntity(String str)
	{
		logger.logTrace("analyzeEntity: Analyzing entities");

		if(str == null)
		{
			logger.logError("analyzeEntity: Text input null");
			throw new InvalidTextException();
		}

		cachedString = str;
		String result = analyzer.analyzeEntitiesAsync(str).join();
		logger.logDebug("analyzeEntity: Analyzed entities for: " + str);

		return result;
	}

	/**
	 * Analyzes text for potentially toxic content.
	 * <p>
	 * Toxicity analysis:
	 * <ul>
	 *	<li>Scores multiple toxicity dimensions</li>
	 *	<li>Compares against current tolerance</li>
	 *	<li>Provides detailed category scores</li>
	 * </ul>
	 * </p>
	 *
	 * @param str	The text to analyze
	 * @return		Formatted toxicity report
	 * @throws InvalidTextException	if input text is null
	 */
	public String analyzeToxicity(String str)
	{
		logger.logTrace("analyzeToxicity: Analyzing toxicity");

		if(str == null)
		{
			logger.logError("analyzeToxicity: Text input null");
			throw new InvalidTextException();
		}

		cachedString = str;

		String report = validator.getToxicityReportAsync(str).join();
		boolean isToxic = validator.isTextToxicAsync(cachedString, toxicityTolerance).join();

		StringBuilder result = new StringBuilder();
		result.append(report);

		result.append("\nTolerance threshold set to: ").append(toxicityTolerance).append("\n");
		result.append("Overall Assessment: ");
		result.append(isToxic ? "TEXT FLAGGED AS POTENTIALLY INAPPROPRIATE" : "Text within acceptable parameters");

		logger.logDebug("analyzeToxicity: Toxicity analysis completed. Toxic: " + isToxic + ", Tolerance: " + toxicityTolerance);
		return result.toString();
	}

	/**
	 * Appends new words to the system dictionaries.
	 * <p>
	 * Supported word types:
	 * <ul>
	 *	<li>Nouns (with number information)</li>
	 *	<li>Adjectives</li>
	 *	<li>Verbs (with number and tense)</li>
	 * </ul>
	 * </p>
	 *
	 * @param nounList			List of nouns to add
	 * @param adjectiveList		List of adjectives to add
	 * @param verbList			List of verbs to add
	 * @throws IOException	if dictionary update fails
	 */
	public void append(List<Noun> nounList, List<Adjective> adjectiveList, List<Verb> verbList) throws IOException
	{
		logger.logTrace("append: Appending words to dictionary");

		if(!nounList.isEmpty())
		{
			for(Noun noun : nounList)
				JsonUpdater.loadNoun(noun);

			logger.logDebug("append: Loaded " + nounList.size() + " nouns");
		}

		if(!adjectiveList.isEmpty())
		{
			for(Adjective adjective : adjectiveList)
				JsonUpdater.loadAdjective(adjective);

			logger.logDebug("append: Loaded " + adjectiveList.size() + " adjectives");
		}

		if(!verbList.isEmpty())
		{
			for(Verb verb : verbList)
				JsonUpdater.loadVerb(verb);

			logger.logDebug("append: Loaded " + verbList.size() + " verbs");
		}

		logger.logTrace("append: Appending completed");
	}

	/**
	 * Retrieves the current toxicity tolerance threshold.
	 * <p>
	 * The threshold:
	 * <ul>
	 *	<li>Range: 0.0 (strict) to 1.0 (lenient)</li>
	 *	<li>Affects toxicity validation</li>
	 *	<li>Default: 0.7</li>
	 * </ul>
	 * </p>
	 *
	 * @return	Current toxicity tolerance value
	 */
	public float getTolerance()
	{
		logger.logTrace("getTolerance: Getting toxicity tolerance");
		logger.logDebug("getTolerance: Current tolerance: " + toxicityTolerance);

		return toxicityTolerance;
	}

	/**
	 * Sets the toxicity tolerance threshold.
	 * <p>
	 * The new value:
	 * <ul>
	 *	<li>Must be between 0.0 and 1.0</li>
	 *	<li>Immediately affects future validations</li>
	 *	<li>Logged for auditing</li>
	 * </ul>
	 * </p>
	 *
	 * @param newTolerance	The new tolerance value
	 * @throws IllegalToleranceException	if value is outside valid range
	 */
	public void setTolerance(float newTolerance)
	{
		logger.logTrace("setTolerance: Setting toxicity tolerance");

		if(newTolerance < 0.0f || newTolerance > 1.0f)
		{
			logger.logError("setTolerance: Invalid tolerance value: " + newTolerance);
			throw new IllegalToleranceException();
		}

		toxicityTolerance = newTolerance;
		logger.logDebug("setTolerance: Tolerance set to: " + newTolerance);
	}

	/**
	 * Checks if a sentence is currently cached.
	 * <p>
	 * The cache:
	 * <ul>
	 *	<li>Stores last processed sentence</li>
	 *	<li>Updated by most operations</li>
	 *	<li>Can be retrieved with {@code getCachedSentence()}</li>
	 * </ul>
	 * </p>
	 *
	 * @return	{@code true} if a sentence is cached, {@code false} otherwise
	 */
	public boolean isSentenceCached()
	{
		logger.logTrace("isSentenceCached: Checking if sentence is cached");
		boolean isCached = cachedString != null && !cachedString.isEmpty();
		logger.logDebug("isSentenceCached: " + isCached);

		return isCached;
	}

	/**
	 * Retrieves the currently cached sentence.
	 * <p>
	 * Cache behavior:
	 * <ul>
	 *	<li>Most operations update the cache</li>
	 *	<li>Check cache status with {@code isSentenceCached()}</li>
	 *	<li>Empty cache throws exception</li>
	 * </ul>
	 * </p>
	 *
	 * @return				The cached sentence
	 * @throws SentenceNotCachedException	if no sentence is cached
	 */
	public String getCachedSentence()
	{
		logger.logTrace("getCachedSentence: Getting cached sentence");

		if(cachedString == null || cachedString.isEmpty())
		{
			logger.logError("getCachedSentence: No sentence cached");
			throw new SentenceNotCachedException();
		}

		logger.logDebug("getCachedSentence: Retrieved cached sentence: " + cachedString);

		return cachedString;
	}

	/**
	 * Toggles verbose logging mode.
	 * <p>
	 * Verbose mode:
	 * <ul>
	 *	<li>Provides detailed operation logs</li>
	 *	<li>Useful for debugging</li>
	 *	<li>State can be checked with {@code isVerbose()}</li>
	 * </ul>
	 * </p>
	 */
	public void switchVerbosity()
	{
		logger.logTrace("switchVerbosity: Toggling verbosity");
		logger.switchVerboseMode();
		logger.logDebug("switchVerbosity: Verbose mode set to: " + logger.getVerbose());
	}

	/**
	 * Checks if verbose logging is enabled.
	 *
	 * @return	{@code true} if verbose mode is active, {@code false} otherwise
	 */
	public boolean isVerbose()
	{
		logger.logTrace("isVerbose: Checking verbosity");
		boolean verbose = logger.getVerbose();
		logger.logDebug("isVerbose: Verbose mode is " + verbose);

		return verbose;
	}

	/**
	 * Cleans up resources when processor is no longer needed.
	 * <p>
	 * Closes:
	 * <ul>
	 *	<li>{@code SentenceAnalyzer} instance</li>
	 *	<li>{@code ToxicityValidator} instance</li>
	 *	<li>Other managed resources</li>
	 * </ul>
	 * </p>
	 */
	@Override
	public void close()
	{
		logger.logTrace("close: Closing resources");

		try
		{
			if(analyzer != null)
				analyzer.close();

			if(validator != null)
				validator.close();

			logger.logTrace("close: Resources closed successfully");
		}
		catch(Exception e)
		{
			logger.logError("close: Error closing resources", e);
		}
	}
}
