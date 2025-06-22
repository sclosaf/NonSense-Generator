package unipd.nonsense.model;

import unipd.nonsense.model.Number;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import unipd.nonsense.util.LoggerManager;

/**
 * Represents a noun in the sentence generation system.
 * <p>
 * This class encapsulates a noun string and its grammatical number (singular or plural),
 * providing validation to ensure grammatical correctness. It follows the immutable pattern
 * to prevent modification after creation.
 * </p>
 *
 * <p>Example usage:
 * <pre>{@code
 * try
 * {
 *     Noun noun = new Noun("cat", Number.SINGULAR);
 *     String nounText = noun.getNoun();
 *     Number nounNumber = noun.getNumber();
 * }
 * catch(InvalidGrammaticalElementException e)
 * {
 *     // Handle invalid noun
 * }
 * }</pre>
 * </p>
 *
 * @see Number
 */
public class Noun
{
	/**
	 * The noun string value.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Stored in trimmed form</li>
	 *	<li>Final to ensure immutability</li>
	 *	<li>Cannot be null or empty</li>
	 * </ul>
	 */
	private final String noun;

	/**
	 * The grammatical number of the noun (singular or plural).
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Final to ensure immutability</li>
	 *	<li>Cannot be null</li>
	 *	<li>Must be a valid {@code Number} enum value</li>
	 * </ul>
	 */
	private final Number number;

	/**
	 * Logger instance for tracking operations.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Static to share across instances</li>
	 *	<li>Configured for {@code Noun} class</li>
	 *	<li>Used for trace and error logging</li>
	 * </ul>
	 */
	private static final LoggerManager logger = new LoggerManager(Noun.class);

	/**
	 * Constructs a {@code Noun} instance with validation.
	 * <p>
	 * Validation rules:
	 * <ul>
	 *	<li>{@code noun} cannot be null or empty/whitespace-only</li>
	 *	<li>{@code number} cannot be null</li>
	 *	<li>Input strings are automatically trimmed</li>
	 * </ul>
	 *
	 * @param noun		The noun string to encapsulate
	 * @param number	The grammatical number of the noun (singular or plural)
	 * @throws InvalidGrammaticalElementException	if input fails validation
	 */
	public Noun(String noun, Number number)
	{
		logger.logTrace("Creating Noun instance");

		if(noun == null || noun.trim().isEmpty() || number == null)
		{
			logger.logError("Invalid noun provided");
			throw new InvalidGrammaticalElementException();
		}

		this.noun = noun.trim();
		this.number = number;
		logger.logDebug("Successfully created Noun with value: " + noun + " and number: " + number);
	}

	/**
	 * Retrieves the noun string value.
	 * <p>
	 * The returned value:
	 * <ul>
	 *	<li>Is guaranteed to be non-null</li>
	 *	<li>Is guaranteed to be non-empty</li>
	 *	<li>Is in trimmed form</li>
	 * </ul>
	 *
	 * @return	The validated noun string
	 */
	public String getNoun()
	{
		logger.logTrace("getNoun: Returning noun");
		return noun;
	}

	/**
	 * Retrieves the grammatical number of the noun.
	 * <p>
	 * The returned value:
	 * <ul>
	 *	<li>Is guaranteed to be non-null</li>
	 *	<li>Will be either {@code Number.SINGULAR} or {@code Number.PLURAL}</li>
	 * </ul>
	 *
	 * @return	The grammatical number of the noun
	 */
	public Number getNumber()
	{
		logger.logTrace("getNumber: Returning number");
		return number;
	}
}
