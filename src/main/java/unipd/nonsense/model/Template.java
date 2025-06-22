package unipd.nonsense.model;

import unipd.nonsense.model.Number;
import unipd.nonsense.model.Placeholder;

import unipd.nonsense.exceptions.InvalidTemplateException;

import unipd.nonsense.util.LoggerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a sentence template with placeholders for dynamic content generation.
 * <p>
 * This class manages text patterns containing special placeholders (marked with square brackets)
 * that can be replaced with specific content. The template maintains grammatical number information
 * and provides various operations for placeholder manipulation while preserving immutability through
 * the {@code withReplacement} method.
 *
 * <p>Example usage:
 * <pre>{@code
 * Template template = new Template("The [noun] [verb] over the [noun]", Number.SINGULAR);
 * if(template.containsPlaceholder(Placeholder.ANIMAL))
 *     Template newTemplate = template.withReplacement(Placeholder.ANIMAL, "dog");
 *
 * }</pre>
 * </p>
 *
 * @see Placeholder
 * @see Number
 */
public class Template
{
	/**
	 * The text pattern containing placeholders for dynamic content.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Stored in trimmed form</li>
	 *	<li>Placeholders are in format {@code [type]} where type matches {@code Placeholder} enum</li>
	 *	<li>Modified only through replacement operations</li>
	 * </ul>
	 */
	private String pattern;

	/**
	 * The grammatical number associated with the template.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Final to ensure immutability</li>
	 *	<li>Must be either {@code SINGULAR} or {@code PLURAL}</li>
	 *	<li>Affects placeholder replacement strategies</li>
	 * </ul>
	 */
	private final Number number;

	/**
	 * Logger instance for tracking operations.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Static to share across instances</li>
	 *	<li>Configured for {@code Template} class</li>
	 *	<li>Logs creation, modification and access events</li>
	 * </ul>
	 */
	private static final LoggerManager logger = new LoggerManager(Template.class);

	/**
	 * Constructs a new {@code Template} with validation.
	 * <p>
	 * The constructor performs the following validations:
	 * <ul>
	 *	<li>Pattern cannot be {@code null} or empty/whitespace-only</li>
	 *	<li>Number cannot be {@code null}</li>
	 *	<li>Input pattern is automatically trimmed</li>
	 * </ul>
	 *
	 * @param pattern	The text pattern with placeholders
	 * @param number	The grammatical number of the template
	 * @throws InvalidTemplateException	if input fails validation
	 */
	public Template(String pattern, Number number)
	{
		logger.logTrace("Creating Template instance");

		if(pattern == null || pattern.trim().isEmpty() || number == null)
		{
			logger.logError("Invalid pattern provided");
			throw new InvalidTemplateException();
		}

		this.pattern = pattern.trim();
		this.number = number;

		logger.logDebug("Successfully created Template with pattern: " + pattern + " and number: " + number);
	}

	/**
	 * Retrieves the current pattern string.
	 * <p>
	 * The returned value:
	 * <ul>
	 *	<li>Is guaranteed to be non-null</li>
	 *	<li>Is guaranteed to be non-empty</li>
	 *	<li>Is in trimmed form</li>
	 *	<li>May contain unprocessed placeholders</li>
	 * </ul>
	 *
	 * @return	The current template pattern
	 */
	public String getPattern()
	{
		logger.logTrace("getPattern: Returning pattern");
		return pattern;
	}

	/**
	 * Retrieves the grammatical number of the template.
	 * <p>
	 * The returned value:
	 * <ul>
	 *	<li>Is guaranteed to be non-null</li>
	 *	<li>Will be either {@code Number.SINGULAR} or {@code Number.PLURAL}</li>
	 *	<li>Matches the number used during construction</li>
	 * </ul>
	 *
	 * @return	The grammatical number of the template
	 */
	public Number getNumber()
	{
		logger.logTrace("getNumber: Returning number");
		return number;
	}

	/**
	 * Checks if the template contains a specific placeholder type.
	 * <p>
	 * The check is case-insensitive and looks for placeholders in the format
	 * {@code [placeholder]} where {@code placeholder} matches the enum name.
	 * Logs both the check operation and its result at debug level.
	 *
	 * @param placeholder	The placeholder type to search for
	 * @return				{@code true} if the pattern contains the placeholder,
	 *						{@code false} otherwise
	 */
	public boolean containsPlaceholder(Placeholder placeholder)
	{
		logger.logDebug("containsPlaceholder: Checking for placeholder: " + placeholder);
		boolean result = pattern.contains("[" + placeholder.name().toLowerCase() + "]");
		logger.logDebug("containsPlaceholder: Result: " + result);

		return result;
	}

	/**
	 * Counts occurrences of a specific placeholder type in the pattern.
	 * <p>
	 * The count includes all instances of the placeholder regardless of their
	 * position in the pattern. The search is case-insensitive and matches the
	 * standard placeholder format. Logs both the operation and result count.
	 *
	 * @param placeholder	The placeholder type to count
	 * @return				The number of occurrences (0 if none found)
	 */
	public int countPlaceholders(Placeholder placeholder)
	{
		logger.logDebug("countPlaceholders: Counting placeholders of type: " + placeholder);

		String target = "[" + placeholder.name().toLowerCase() + "]";

		int count = 0;
		int index = pattern.indexOf(target);

		while(index != -1)
		{
			count++;
			index = pattern.indexOf(target, index + target.length());
		}

		logger.logDebug("countPlaceholders: Count: " + count);
		return count;
	}

	/**
	 * Replaces a random occurrence of a placeholder with specified content.
	 * <p>
	 * This method modifies the current template instance by:
	 * <ul>
	 *	<li>Finding all occurrences of the placeholder</li>
	 *	<li>Randomly selecting one occurrence</li>
	 *	<li>Replacing it with the provided string</li>
	 * </ul>
	 * If no matching placeholder is found, logs a warning but continues execution.
	 *
	 * @param placeholder	The placeholder type to replace
	 * @param replacement	The content to insert in place of the placeholder
	 */
	public void replacePlaceholder(Placeholder placeholder, String replacement)
	{
		logger.logDebug("replacePlaceholder: Replacing placeholder: " + placeholder + " with: " + replacement);
		String target = "[" + placeholder.name().toLowerCase() + "]";
		List<Integer> indices = new ArrayList<>();

		int index = pattern.indexOf(target);

		while(index != -1)
		{
			indices.add(index);
			index = pattern.indexOf(target, index + target.length());
		}

		if(!indices.isEmpty())
		{
			Random random = new Random();
			int randomIndex = indices.get(random.nextInt(indices.size()));

			StringBuilder sb = new StringBuilder(pattern);
			sb.replace(randomIndex, randomIndex + target.length(), replacement);
			pattern = sb.toString();
			logger.logTrace("replacePlaceholder: Successfully replaced placeholder");
		}
		else
			logger.logWarn("replacePlaceholder: No placeholders found to replace");
	}

	/**
	 * Creates a new template with a random placeholder replaced.
	 * <p>
	 * This immutable variant:
	 * <ul>
	 *	<li>Preserves the original template</li>
	 *	<li>Returns a new instance with the replacement</li>
	 *	<li>Maintains the same grammatical number</li>
	 *	<li>Uses the same random selection logic as {@code replacePlaceholder}</li>
	 * </ul>
	 *
	 * @param placeholder	The placeholder type to replace
	 * @param replacement	The content to insert in place of the placeholder
	 * @return				A new {@code Template} instance with the replacement,
	 *						or the original if no placeholder was found
	 */
	public Template withReplacement(Placeholder placeholder, String replacement)
	{
		logger.logDebug("withReplacement: Creating new Template with replacement for: " + placeholder);

		String target = "[" + placeholder.name().toLowerCase() + "]";
		List<Integer> indices = new ArrayList<>();

		int index = pattern.indexOf(target);
		String newPattern = pattern;

		while(index != -1)
		{
			indices.add(index);
			index = newPattern.indexOf(target, index + target.length());
		}

		if(!indices.isEmpty())
		{
			Random random = new Random();
			int randomIndexToReplace = indices.get(random.nextInt(indices.size()));
			StringBuilder sb = new StringBuilder(newPattern);

			sb.replace(randomIndexToReplace, randomIndexToReplace + target.length(), replacement);
			newPattern = sb.toString();
			logger.logInfo("withReplacement: Successfully created new Template with replacement");
		}
		else
			logger.logWarn("withReplacement: No placeholders found to replace");

		return new Template(newPattern, this.number);
	}

	/**
	 * Returns the pattern string representation of the template.
	 * <p>
	 * The returned string:
	 * <ul>
	 *	<li>Exactly matches the current pattern</li>
	 *	<li>May contain unprocessed placeholders</li>
	 *	<li>Is suitable for display or further processing</li>
	 * </ul>
	 *
	 * @return	The pattern string with any replacements applied
	 */
	@Override
	public String toString()
	{
		logger.logTrace("toString: Converting Template to string");
		return pattern;
	}
}
