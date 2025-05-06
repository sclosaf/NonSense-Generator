package unipd.nonsense.exceptions;

public final class InvalidJsonStateException extends IllegalStateException
{
	public InvalidJsonStateException()
	{
		super("Json file is in a invalid state.");
	}

	public InvalidJsonStateException(String msg)
	{
		super(msg);
	}

	public InvalidJsonStateException(Throwable cause)
	{
		super(cause);
	}

	public InvalidJsonStateException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
