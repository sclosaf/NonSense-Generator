package unipd.nonsense.exceptions;

public class InvalidGrammaticalElementException extends IllegalArgumentException
{
	public InvalidGrammaticalElementException()
	{
		super("Attempted to create and invalid grammatical element");
	}

	public InvalidGrammaticalElementException(String msg)
	{
		super(msg);
	}

	public InvalidGrammaticalElementException(Throwable cause)
	{
		super(cause);
	}

	public InvalidGrammaticalElementException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
