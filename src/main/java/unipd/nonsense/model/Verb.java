package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

public class Verb
{
	public static enum Tense
	{
		PAST, PRESENT, FUTURE
	}

	private final String verb;
	private final Tense tense;

	public Verb(String verb, Tense tense)
	{
		if(verb == null || verb.isEmpty())
			throw new InvalidGrammaticalElementException();

		this.verb = verb;
		this.tense = tense;
	}

	public String getVerb()
	{
		return verb;
	}

	public Tense getTense()
	{
		return tense;
	}

}
