package unipd.nonsense.exceptions;

public final class NullJsonKeyException extends IllegalArgumentException
{
	public NullJsonKeyException()
	{
		super("Invalid null key argument.");
	}

	public NullJsonKeyException(String msg)
	{
		super(msg);
	}

	public NullJsonKeyException(Throwable cause)
	{
		super(cause);
	}

	public NullJsonKeyException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
