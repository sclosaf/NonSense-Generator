package unipd.nonsense.exceptions;

public final class ClientNonExistentException extends IllegalStateException
{
	public ClientNonExistentException()
	{
		super("Client not found");
	}

	public ClientNonExistentException(String msg)
	{
		super(msg);
	}

	public ClientNonExistentException(Throwable cause)
	{
		super(cause);
	}

	public ClientNonExistentException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
