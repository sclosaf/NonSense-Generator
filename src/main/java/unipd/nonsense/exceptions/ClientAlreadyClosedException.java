package unipd.nonsense.exceptions;

public final class ClientAlreadyClosedException extends IllegalStateException
{
	public ClientAlreadyClosedException()
	{
		super("Client is already closed.");
	}

	public ClientAlreadyClosedException(String msg)
	{
		super(msg);
	}

	public ClientAlreadyClosedException(Throwable cause)
	{
		super(cause);
	}

	public ClientAlreadyClosedException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
