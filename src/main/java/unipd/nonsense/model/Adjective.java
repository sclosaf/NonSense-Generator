package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import unipd.nonsense.util.LoggerManager;

public class Adjective
{
	private final String adjective;
	private static final LoggerManager logger = new LoggerManager(Adjective.class);

	public Adjective(String adjective)
	{
		logger.logTrace("Creating Adjective instance");

		if(adjective == null || adjective.trim().isEmpty())
		{
			logger.logError("Invalid adjective provided");
			throw new InvalidGrammaticalElementException();
		}

		this.adjective = adjective.trim();
		logger.logDebug("Successfully created Adjective with value: " + adjective);
	}

	public String getAdjective()
	{
		logger.logTrace("getAdjective: Returning adjective");
		return adjective;
	}
}
