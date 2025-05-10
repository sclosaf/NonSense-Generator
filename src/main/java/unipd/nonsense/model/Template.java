package unipd.nonsense.model;

public class Template
{
	// Enum for template types
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

	// Constructor with pattern and type
	public Template(String pattern, TemplateType type)
	{
		this.pattern = pattern;
		this.type = type;
	}

	// Get the template pattern
	public String getPattern()
	{
		return pattern;
	}

	// Get the template type
	public TemplateType getType()
	{
		return type;
	}

	// Check if template contains a specific placeholder
	public boolean containsPlaceholder(Placeholder placeholder)
	{
		return pattern.contains("[" + placeholder.name().toLowerCase() + "]");
	}

	// Count occurrences of a specific placeholder
	public int countPlaceholders(Placeholder placeholder)
	{
		String target = "[" + placeholder.name().toLowerCase() + "]";

		int count = 0;
		int index = pattern.indexOf(target);

		while (index != -1)
		{
			count++;
			index = pattern.indexOf(target, index + target.length());
		}

		return count;
	}

	// Replace a placeholder with actual content
	public String replacePlaceholder(Placeholder placeholder, String replacement)
	{
		return pattern.replace("[" + placeholder.name().toLowerCase() + "]", replacement);
	}

	// Create a new Template instance with a placeholder replaced
	public Template withReplacement(Placeholder placeholder, String replacement)
	{
		return new Template(replacePlaceholder(placeholder, replacement), this.type);
	}

	@Override
	public String toString()
	{
		return pattern;
	}
}
