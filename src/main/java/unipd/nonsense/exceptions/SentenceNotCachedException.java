package unipd.nonsense.exceptions;

public class SentenceNotCachedException extends IllegalStateException
{
	public SentenceNotCachedException()
	{
		super("No sentence was cached");
	}

	public SentenceNotCachedException(String msg)
	{
		super(msg);
	}

	public SentenceNotCachedException(Throwable cause)
	{
		super(cause);
	}

	public SentenceNotCachedException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
