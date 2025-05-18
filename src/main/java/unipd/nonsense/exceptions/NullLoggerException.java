package unipd.nonsense.exceptions;

public class NullLoggerException extends IllegalArgumentException
{
	public NullLoggerException()
	{
		super("Logger cannot be null");
	}

	public NullLoggerException(String msg)
	{
		super(msg);
	}

	public NullLoggerException(Throwable cause)
	{
		super(cause);
	}

	public NullLoggerException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
