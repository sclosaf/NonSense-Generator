package unipd.nonsense.exceptions;

public class InvalidListException extends IllegalStateException
{
	public InvalidListException()
	{
		super("Unable to load elements on the list.");
	}

	public InvalidListException(String msg)
	{
		super(msg);
	}

	public InvalidListException(Throwable cause)
	{
		super(cause);
	}

	public InvalidListException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
