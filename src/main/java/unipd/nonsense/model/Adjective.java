package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import unipd.nonsense.util.LoggerManager;

/**
 * Represents an adjective in the sentence generation system.
 * <p>
 * This class encapsulates an adjective string and provides validation to ensure
 * grammatical correctness. It implements immutable pattern to prevent modification
 * after creation.
 * </p>
 *
 * <p>Example usage:
 * <pre>{@code
 * try
 * {
 *     Adjective adj = new Adjective("happy");
 *     String adjectiveText = adj.getAdjective();
 * }
 * catch(InvalidGrammaticalElementException e)
 * {
 *     // Handle invalid adjective
 * }
 * }</pre>
 * </p>
 *
 * @see InvalidGrammaticalElementException
 */
public class Adjective
{
	/**
	 * The adjective string value.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Stored in trimmed form</li>
	 *	<li>Final to ensure immutability</li>
	 *	<li>Cannot be null or empty</li>
	 * </ul>
	 */
	private final String adjective;

	/**
	 * Logger instance for tracking operations.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Static to share across instances</li>
	 *	<li>Configured for {@code Adjective} class</li>
	 *	<li>Used for trace and error logging</li>
	 * </ul>
	 */
	private static final LoggerManager logger = new LoggerManager(Adjective.class);

	/**
	 * Constructs an {@code Adjective} instance with validation.
	 * <p>
	 * Validation rules:
	 * <ul>
	 *	<li>Input cannot be null</li>
	 *	<li>Input cannot be empty or whitespace-only</li>
	 *	<li>Input is automatically trimmed</li>
	 * </ul>
	 *
	 * @param adjective		The adjective string to encapsulate
	 * @throws InvalidGrammaticalElementException	if input fails validation
	 */
	public Adjective(String adjective)
	{
		logger.logTrace("Creating Adjective instance");

		if(adjective == null || adjective.trim().isEmpty())
		{
			logger.logError("Invalid adjective provided");
			throw new InvalidGrammaticalElementException();
		}

		this.adjective = adjective.trim();
		logger.logDebug("Successfully created Adjective with value: " + adjective);
	}

	/**
	 * Retrieves the adjective string value.
	 * <p>
	 * The returned value:
	 * <ul>
	 *	<li>Is guaranteed to be non-null</li>
	 *	<li>Is guaranteed to be non-empty</li>
	 *	<li>Is in trimmed form</li>
	 * </ul>
	 *
	 * @return	The validated adjective string
	 */
	public String getAdjective()
	{
		logger.logTrace("getAdjective: Returning adjective");
		return adjective;
	}
}
