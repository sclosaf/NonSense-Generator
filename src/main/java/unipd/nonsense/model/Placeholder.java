package unipd.nonsense.model;

/**
 * Enumerates the types of grammatical placeholders used in the templates
 * <p>
 * These placeholders serve as markers in sentence templates that will be
 * replaced with actual grammatical elements during sentence construction.
 * Each enum constant represents a different part of speech that the system
 * can generate and substitute.
 * </p>
 */
public enum Placeholder
{
	/**
	 * Represents a noun placeholder in sentence templates.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Will be replaced by a noun</li>
	 * </ul>
	 *
	 * @see Noun
	 */
	NOUN,

	/**
	 * Represents a verb placeholder in sentence templates.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Will be replaced by a conjugated verb form</li>
	 * </ul>
	 */
	VERB,

	/**
	 * Represents an adjective placeholder in sentence templates.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Will be replaced by a descriptive adjective</li>
	 * </ul>
	 */
	ADJECTIVE
}
