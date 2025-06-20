package unipd.nonsense.model;

import unipd.nonsense.model.Number;
import unipd.nonsense.model.Tense;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import unipd.nonsense.util.LoggerManager;

public class Verb
{
	private final String verb;
	private final Tense tense;
	private final Number number;

	private static final LoggerManager logger = new LoggerManager(Verb.class);

	public Verb(String verb, Number number, Tense tense)
	{
		logger.logTrace("Creating Verb instance");

		if(verb == null || verb.isEmpty())
		{
			logger.logError("Invalid verb provided");
			throw new InvalidGrammaticalElementException();
		}

		this.verb = verb;
		this.tense = tense;
		this.number = number;

		logger.logDebug("Successfully created Verb with value: " + verb + ", number: " + number + " and tense: " + tense);
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

	public Number getNumber()
	{
		logger.logTrace("getNumber: Returning number");
		return number;
	}
}
