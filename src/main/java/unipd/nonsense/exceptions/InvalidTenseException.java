package unipd.nonsense.exceptions;

public class InvalidTenseException extends IllegalArgumentException
{
	public InvalidTenseException()
	{
		super("Invalid tense argument");
	}

	public InvalidTenseException(String msg)
	{
		super(msg);
	}

	public InvalidTenseException(Throwable cause)
	{
		super(cause);
	}

	public InvalidTenseException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
