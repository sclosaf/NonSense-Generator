package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import com.google.cloud.language.v1.PartOfSpeech;
import com.google.cloud.language.v1.DependencyEdge;

import unipd.nonsense.util.LoggerManager;

/**
 * Represents a linguistic token with syntactic information extracted from text analysis.
 * <p>
 * This class encapsulates all grammatical and syntactic information about a word or punctuation
 * mark in a sentence, including its part-of-speech, dependency relation, and lemma form.
 * The class is immutable and performs strict null-checking during construction.
 *
 * <p>Example usage:
 * <pre>{@code
 * SyntaxToken token = new SyntaxToken("running", 0, "run", partOfSpeech, 2, DependencyEdge.Label.AMOD);
 * String lemma = token.getLemma();
 * }</pre>
 */
public class SyntaxToken
{
	/**
	 * The original surface form of the token as it appears in the text.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Immutable after construction</li>
	 *	<li>Cannot be {@code null}</li>
	 *	<li>Preserves original capitalization and form</li>
	 * </ul>
	 */
	private final String text;

	/**
	 * The character offset where this token begins in the original text.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Zero-based index</li>
	 *	<li>Counts Unicode code points (not bytes)</li>
	 *	<li>Negative values are not allowed</li>
	 * </ul>
	 */
	private final int beginOffset;

	/**
	 * The canonical or dictionary form of the word.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>For verbs, this is the base/infinitive form</li>
	 *	<li>For nouns, typically the singular form</li>
	 *	<li>Cannot be {@code null}</li>
	 * </ul>
	 */
	private final String lemma;

	/**
	 * The part-of-speech information for this token.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Contains detailed grammatical categorization</li>
	 *	<li>Includes features like tense, number, gender etc.</li>
	 *	<li>Cannot be {@code null}</li>
	 * </ul>
	 */
	private final PartOfSpeech partOfSpeech;

	/**
	 * The index of the head token in the sentence's dependency tree.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Zero-based index within the sentence</li>
	 *	<li>-1 indicates no head (typically for root tokens)</li>
	 *	<li>Must be consistent with the sentence structure</li>
	 * </ul>
	 */
	private final int headTokenIndex;

	/**
	 * The grammatical relation between this token and its head.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Represents syntactic function (subject, object, modifier etc.)</li>
	 *	<li>Cannot be {@code null}</li>
	 *	<li>Values defined by {@code DependencyEdge.Label} enum</li>
	 * </ul>
	 */
	private final DependencyEdge.Label dependencyLabel;

	/**
	 * Logger instance for tracking operations and errors.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Shared across all instances</li>
	 *	<li>Configured for {@code SyntaxToken} class</li>
	 *	<li>Used for debug, trace and error logging</li>
	 * </ul>
	 */
	private static final LoggerManager logger = new LoggerManager(SyntaxToken.class);

	/**
	 * Constructs a new {@code SyntaxToken} with complete grammatical information.
	 * <p>
	 * The constructor performs strict null-checking on all reference parameters and will
	 * throw {@code InvalidGrammaticalElementException} if any required field is {@code null}.
	 * Logs creation events at different severity levels.
	 *
	 * @param text				the original text of the token
	 * @param beginOffset		the starting character offset in the original text
	 * @param lemma				the canonical form of the word
	 * @param partOfSpeech		the grammatical category information
	 * @param headTokenIndex	the index of the governing token in the dependency tree
	 * @param dependencyLabel	the grammatical relation to the head token
	 * @throws InvalidGrammaticalElementException	if any non-nullable parameter is {@code null}
	 */
	public SyntaxToken(String text, int beginOffset, String lemma, PartOfSpeech partOfSpeech, int headTokenIndex, DependencyEdge.Label dependencyLabel)
	{
		logger.logTrace("Creating SyntaxToken instance");

		if(text == null || lemma == null || partOfSpeech == null || dependencyLabel == null)
		{
			logger.logError("Invalid parameters provided");
			throw new InvalidGrammaticalElementException();
		}

		this.text = text;
		this.beginOffset = beginOffset;
		this.lemma = lemma;
		this.partOfSpeech = partOfSpeech;
		this.headTokenIndex = headTokenIndex;
		this.dependencyLabel = dependencyLabel;

		logger.logDebug("Successfully created SyntaxToken with text: " + text);
	}

	/**
	 * Retrieves the original text form of the token.
	 * <p>
	 * The returned string exactly matches how the token appeared in the source text,
	 * including any capitalization or punctuation. Logs the access at trace level.
	 *
	 * @return	the original text form, never {@code null}
	 */
	public String getText()
	{
		logger.logTrace("getText: Returning text");
		return text;
	}

	/**
	 * Retrieves the starting character offset of the token.
	 * <p>
	 * The offset indicates where this token begins in the original text string,
	 * measured in Unicode code points. Logs the access at trace level.
	 *
	 * @return	the zero-based starting position in the original text
	 */
	public int getBeginOffset()
	{
		logger.logTrace("getBeginOffset: Returning beginOffset");
		return beginOffset;
	}

	/**
	 * Retrieves the lemma (dictionary form) of the token.
	 * <p>
	 * For inflected words, this returns the base form (e.g., "run" for "running").
	 * For uninflected words, typically matches the original text. Logs the access
	 * at trace level.
	 *
	 * @return	the canonical form of the word, never {@code null}
	 */
	public String getLemma()
	{
		logger.logTrace("getLemma: Returning lemma");
		return lemma;
	}

	/**
	 * Retrieves the complete part-of-speech information.
	 * <p>
	 * The returned object contains detailed grammatical categorization including:
	 * <ul>
	 *	<li>Broad category (noun, verb etc.)</li>
	 *	<li>Grammatical features (tense, number, case etc.)</li>
	 *	<li>Morphological information</li>
	 * </ul>
	 * Logs the access at trace level.
	 *
	 * @return	the complete part-of-speech data, never {@code null}
	 */
	public PartOfSpeech getPartOfSpeech()
	{
		logger.logTrace("getPartOfSpeech: Returning partOfSpeech");
		return partOfSpeech;
	}

	/**
	 * Retrieves the index of this token's head in the dependency tree.
	 * <p>
	 * The head token is the governing word in a syntactic relationship.
	 * Special values:
	 * <ul>
	 *	<li>{@code -1} indicates no head (typically root of sentence)</li>
	 *	<li>Self-reference indicates a loop in the dependency tree</li>
	 * </ul>
	 * Logs the access at trace level.
	 *
	 * @return	the zero-based index of the head token
	 */
	public int getHeadTokenIndex()
	{
		logger.logTrace("getHeadTokenIndex: Returning headTokenIndex");
		return headTokenIndex;
	}

	/**
	 * Retrieves the grammatical relation to the head token.
	 * <p>
	 * The label describes the syntactic function of this token relative to its head,
	 * such as subject, object, or modifier. The possible values are defined by the
	 * Natural Language API's dependency grammar. Logs the access at trace level.
	 *
	 * @return	the dependency relation label, never {@code null}
	 */
	public DependencyEdge.Label getDependencyLabel()
	{
		logger.logTrace("getDependencyLabel: Returning dependencyLabel");
		return dependencyLabel;
	}


	/**
	 * Retrieves the basic part-of-speech tag as a string.
	 * <p>
	 * This convenience method extracts just the primary grammatical category
	 * (e.g., "VERB", "NOUN") from the detailed part-of-speech information.
	 * The returned values match the Natural Language API's tag set. Logs the
	 * access at trace level.
	 *
	 * @return	the coarse part-of-speech category as a string, never {@code null}
	 */
	public String getPosTag()
	{
		logger.logTrace("getPosTag: Returning POS tag");
		return partOfSpeech.getTag().toString();
	}
}

