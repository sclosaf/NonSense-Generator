package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidTemplateException;

import unipd.nonsense.util.LoggerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Template
{
	public static enum TemplateType
	{
		SINGULAR,
		PLURAL
	}

	public static enum Placeholder
	{
		NOUN,
		VERB,
		ADJECTIVE
	}

	private String pattern;
	private final TemplateType type;
	private static final LoggerManager logger = new LoggerManager(Template.class);

	public Template(String pattern, TemplateType type)
	{
		logger.logTrace("Creating Template instance");

		if(pattern == null || pattern.isEmpty())
		{
			logger.logError("Invalid pattern provided");
			throw new InvalidTemplateException();
		}

		this.pattern = pattern;
		this.type = type;

		logger.logDebug("Successfully created Template with pattern: " + pattern + " and type: " + type);
	}

	public String getPattern()
	{
		logger.logTrace("getPattern: Returning pattern");
		return pattern;
	}

	public TemplateType getType()
	{
		logger.logTrace("getType: Returning type");
		return type;
	}

	public boolean containsPlaceholder(Placeholder placeholder)
	{
		logger.logDebug("containsPlaceholder: Checking for placeholder: " + placeholder);
		boolean result = pattern.contains("[" + placeholder.name().toLowerCase() + "]");
		logger.logDebug("containsPlaceholder: Result: " + result);

		return result;
	}

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

		return new Template(newPattern, this.type);
	}

	@Override
	public String toString()
	{
		logger.logTrace("toString: Converting Template to string");
		return pattern;
	}
}
