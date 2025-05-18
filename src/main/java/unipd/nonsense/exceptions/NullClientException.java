package unipd.nonsense.exceptions;

public class NullClientException extends IllegalArgumentException
{
	public NullClientException()
	{
		super("Client cannot be null");
	}

	public NullClientException(String msg)
	{
		super(msg);
	}

	public NullClientException(Throwable cause)
	{
		super(cause);
	}

	public NullClientException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
