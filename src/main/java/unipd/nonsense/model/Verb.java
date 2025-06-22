package unipd.nonsense.model;

import unipd.nonsense.model.Number;
import unipd.nonsense.model.Tense;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import unipd.nonsense.util.LoggerManager;


/**
 * Represents a verb with its grammatical properties in the sentence generation system.
 * <p>
 * This immutable class encapsulates a verb string along with its tense and number information,
 * ensuring grammatical correctness in generated sentences. It performs strict validation
 * during construction and provides access to all verb properties.
 *
 * <p>Example usage:
 * <pre>{@code
 * try
 * {
 *     Verb verb = new Verb("running", Number.SINGULAR, Tense.PRESENT);
 *     String baseForm = verb.getVerb();
 *     Tense verbTense = verb.getTense();
 * }
 * catch(InvalidGrammaticalElementException e)
 * {
 *     // Handle invalid verb
 * }
 * }</pre>
 * </p>
 *
 * @see Tense
 * @see Number
 */
public class Verb
{
	/**
	 * The verb string value.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Stored in trimmed form</li>
	 *	<li>Final to ensure immutability</li>
	 *	<li>Cannot be null or empty</li>
	 *	<li>Represents the base form or conjugated form</li>
	 * </ul>
	 */
	private final String verb;

	/**
	 * The grammatical tense of the verb.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Final to ensure immutability</li>
	 *	<li>Cannot be null</li>
	 *	<li>Must be a valid {@code Tense} enum value</li>
	 *	<li>Affects verb conjugation in sentences</li>
	 * </ul>
	 */
	private final Tense tense;


	/**
	 * The grammatical number of the verb.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Final to ensure immutability</li>
	 *	<li>Cannot be null</li>
	 *	<li>Must be a valid {@code Number} enum value</li>
	 *	<li>Affects verb conjugation (especially in present tense)</li>
	 * </ul>
	 */
	private final Number number;

	/**
	 * Logger instance for tracking operations.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Static to share across instances</li>
	 *	<li>Configured for {@code Verb} class</li>
	 *	<li>Used for trace and error logging</li>
	 * </ul>
	 */
	private static final LoggerManager logger = new LoggerManager(Verb.class);


	/**
	 * Constructs a {@code Verb} instance with validation.
	 * <p>
	 * Validation rules:
	 * <ul>
	 *	<li>{@code verb} cannot be null or empty/whitespace-only</li>
	 *	<li>{@code number} cannot be null</li>
	 *	<li>{@code tense} cannot be null</li>
	 *	<li>Input strings are automatically trimmed</li>
	 * </ul>
	 *
	 * @param verb		The verb string to encapsulate
	 * @param number	The grammatical number of the verb
	 * @param tense		The grammatical tense of the verb
	 * @throws InvalidGrammaticalElementException	if input fails validation
	 */
	public Verb(String verb, Number number, Tense tense)
	{
		logger.logTrace("Creating Verb instance");

		if(verb == null || verb.trim().isEmpty() || number == null || tense == null)
		{
			logger.logError("Invalid verb provided");
			throw new InvalidGrammaticalElementException();
		}

		this.verb = verb.trim();
		this.tense = tense;
		this.number = number;

		logger.logDebug("Successfully created Verb with value: " + verb + ", number: " + number + " and tense: " + tense);
	}

	/**
	 * Retrieves the verb string value.
	 * <p>
	 * The returned value:
	 * <ul>
	 *	<li>Is guaranteed to be non-null</li>
	 *	<li>Is guaranteed to be non-empty</li>
	 *	<li>Is in trimmed form</li>
	 *	<li>May be in base or conjugated form</li>
	 * </ul>
	 *
	 * @return	The validated verb string
	 */
	public String getVerb()
	{
		logger.logTrace("getVerb: Returning verb");
		return verb;
	}


	/**
	 * Retrieves the grammatical tense of the verb.
	 * <p>
	 * The returned value:
	 * <ul>
	 *	<li>Is guaranteed to be non-null</li>
	 *	<li>Will be one of {@code Tense.PAST}, {@code Tense.PRESENT}, or {@code Tense.FUTURE}</li>
	 *	<li>Matches the tense used during construction</li>
	 * </ul>
	 *
	 * @return	The grammatical tense of the verb
	 */
	public Tense getTense()
	{
		logger.logTrace("getTense: Returning tense");
		return tense;
	}

	/**
	 * Retrieves the grammatical number of the verb.
	 * <p>
	 * The returned value:
	 * <ul>
	 *	<li>Is guaranteed to be non-null</li>
	 *	<li>Will be either {@code Number.SINGULAR} or {@code Number.PLURAL}</li>
	 *	<li>Matches the number used during construction</li>
	 * </ul>
	 *
	 * @return	The grammatical number of the verb
	 */
	public Number getNumber()
	{
		logger.logTrace("getNumber: Returning number");
		return number;
	}
}
