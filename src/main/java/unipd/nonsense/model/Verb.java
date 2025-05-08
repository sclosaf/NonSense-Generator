package unipd.nonsense.model;

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
