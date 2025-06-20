package unipd.nonsense.model;

import unipd.nonsense.model.Number;
import unipd.nonsense.model.Placeholder;

import unipd.nonsense.exceptions.InvalidTemplateException;

import unipd.nonsense.util.LoggerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Template
{
	private String pattern;
	private final Number number;
	private static final LoggerManager logger = new LoggerManager(Template.class);

	public Template(String pattern, Number number)
	{
		logger.logTrace("Creating Template instance");

		if(pattern == null || pattern.isEmpty())
		{
			logger.logError("Invalid pattern provided");
			throw new InvalidTemplateException();
		}

		this.pattern = pattern;
		this.number = number;

		logger.logDebug("Successfully created Template with pattern: " + pattern + " and number: " + number);
	}

	public String getPattern()
	{
		logger.logTrace("getPattern: Returning pattern");
		return pattern;
	}

	public Number getNumber()
	{
		logger.logTrace("getNumber: Returning number");
		return number;
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

		return new Template(newPattern, this.number);
	}

	@Override
	public String toString()
	{
		logger.logTrace("toString: Converting Template to string");
		return pattern;
	}
}
