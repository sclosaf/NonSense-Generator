package unipd.nonsense.model;

public class Noun
{
	public static enum Number
	{
		SINGULAR, PLURAL
	}

	private final String noun;
	private final Number number;

	public Noun(String noun, Number number)
	{
		this.noun = noun;
		this.number = number;
	}

	public String getNoun()
	{
		return noun;
	}

	public Number getNumber()
	{
		return number;
	}
}
