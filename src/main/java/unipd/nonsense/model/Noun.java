package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import unipd.nonsense.util.LoggerManager;

public class Noun
{
	public static enum Number
	{
		SINGULAR, PLURAL
	}

	private final String noun;
	private final Number number;
	private static final LoggerManager logger = new LoggerManager(Noun.class);

	public Noun(String noun, Number number)
	{
		logger.logTrace("Creating Noun instance");

		if(noun == null || noun.isEmpty())
		{
			logger.logError("Invalid noun provided");
			throw new InvalidGrammaticalElementException();
		}

		this.noun = noun;
		this.number = number;
		logger.logDebug("Successfully created Noun with value: " + noun + " and number: " + number);
	}

	public String getNoun()
	{
		logger.logTrace("getNoun: Returning noun");
		return noun;
	}

	public Number getNumber()
	{
		logger.logTrace("getNumber: Returning number");
		return number;
	}
}
