package unipd.nonsense.exceptions;

public final class NullFilePathException extends IllegalArgumentException
{
	public NullFilePathException()
	{
		super("Invalid null file path argument.");
	}

	public NullFilePathException(String msg)
	{
		super(msg);
	}

	public NullFilePathException(Throwable cause)
	{
		super(cause);
	}

	public NullFilePathException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
