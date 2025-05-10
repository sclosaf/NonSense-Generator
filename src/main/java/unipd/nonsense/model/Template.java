package unipd.nonsense.model;

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

	public Template(String pattern, TemplateType type)
	{
		this.pattern = pattern;
		this.type = type;
	}

	public String getPattern()
	{
		return pattern;
	}

	public TemplateType getType()
	{
		return type;
	}

	public boolean containsPlaceholder(Placeholder placeholder)
	{
		return pattern.contains("[" + placeholder.name().toLowerCase() + "]");
	}

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

	public String replacePlaceholder(Placeholder placeholder, String replacement)
	{
		return pattern.replace("[" + placeholder.name().toLowerCase() + "]", replacement);
	}

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
