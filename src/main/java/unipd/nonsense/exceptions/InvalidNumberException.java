package unipd.nonsense.exceptions;

public class InvalidNumberException extends IllegalArgumentException
{
	public InvalidNumberException()
	{
		super("Unsupported number.");
	}

	public InvalidNumberException(String msg)
	{
		super(msg);
	}

	public InvalidNumberException(Throwable cause)
	{
		super(cause);
	}
	public InvalidNumberException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
