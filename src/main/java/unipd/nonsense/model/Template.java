package unipd.nonsense.model;

public class Template
{
	// Enum for template types
	public enum TemplateType
	{
		SINGULAR,
		PLURAL
	}

	private String pattern;
	private TemplateType type;

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
	public boolean containsPlaceholder(String placeholder)
	{
		return pattern.contains("[" + placeholder + "]");
	}

	// Count occurrences of a specific placeholder
	public int countPlaceholders(String placeholder)
	{
		String target = "[" + placeholder + "]";
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
	public String replacePlaceholder(String placeholder, String replacement)
	{
		return pattern.replace("[" + placeholder + "]", replacement);
	}

	// Create a new Template instance with a placeholder replaced
	public Template withReplacement(String placeholder, String replacement)
	{
		return new Template(replacePlaceholder(placeholder, replacement), this.type);
	}

	@Override
	public String toString()
	{
		return pattern;
	}
}