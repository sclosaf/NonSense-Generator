package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

public class Adjective
{
	private final String adjective;

	public Adjective(String adjective)
	{
		if(adjective == null || adjective.isEmpty())
			throw new InvalidGrammaticalElementException();

		this.adjective = adjective;
	}

	public String getAdjective()
	{
		return adjective;
	}
}
