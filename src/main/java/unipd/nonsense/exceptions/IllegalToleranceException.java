package unipd.nonsense.exceptions;

public class IllegalToleranceException extends IllegalArgumentException
{
	public IllegalToleranceException()
	{
		super("Illegal tollerance, it must be between 0.0 and 1.0, inclusive");
	}

	public IllegalToleranceException(String msg)
	{
		super(msg);
	}

	public IllegalToleranceException(Throwable cause)
	{
		super(cause);
	}

	public IllegalToleranceException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
