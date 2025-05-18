package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import unipd.nonsense.util.LoggerManager;

public class Verb
{
	public static enum Tense
	{
		PAST, PRESENT, FUTURE
	}

	private final String verb;
	private final Tense tense;

	private static final LoggerManager logger = new LoggerManager(Verb.class);

	public Verb(String verb, Tense tense)
	{
		logger.logTrace("Creating Verb instance");

		if(verb == null || verb.isEmpty())
		{
			logger.logError("Invalid verb provided");
			throw new InvalidGrammaticalElementException();
		}

		this.verb = verb;
		this.tense = tense;

		logger.logDebug("Successfully created Verb with value: " + verb + " and tense: " + tense);
	}

	public String getVerb()
	{
		logger.logTrace("getVerb: Returning verb");
		return verb;
	}

	public Tense getTense()
	{
		logger.logTrace("getTense: Returning tense");
		return tense;
	}

}
