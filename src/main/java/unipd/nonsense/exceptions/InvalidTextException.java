package unipd.nonsense.exceptions;

public class InvalidTextException extends IllegalArgumentException
{
	public InvalidTextException()
	{
		super("Invalid text inserted.");
	}

	public InvalidTextException(String msg)
	{
		super(msg);
	}

	public InvalidTextException(Throwable cause)
	{
		super(cause);
	}

	public InvalidTextException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
