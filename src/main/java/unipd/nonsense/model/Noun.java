package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

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
		if(noun == null || noun.isEmpty())
			throw new InvalidGrammaticalElementException();

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
