package unipd.nonsense.exceptions;

public class MissingInternetConnectionException extends RuntimeException
{
	public MissingInternetConnectionException()
	{
		super("The device is not connected.");
	}

	public MissingInternetConnectionException(String msg)
	{
		super(msg);
	}

	public MissingInternetConnectionException(Throwable cause)
	{
		super(cause);
	}

	public MissingInternetConnectionException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
