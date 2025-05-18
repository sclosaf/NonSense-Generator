package unipd.nonsense.exceptions;

public class FailedClientInitializationException extends RuntimeException
{
	public FailedClientInitializationException()
	{
		super("Failed to initialize google client");
	}

	public FailedClientInitializationException(String msg)
	{
		super(msg);
	}

	public FailedClientInitializationException(Throwable cause)
	{
		super(cause);
	}

	public FailedClientInitializationException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
