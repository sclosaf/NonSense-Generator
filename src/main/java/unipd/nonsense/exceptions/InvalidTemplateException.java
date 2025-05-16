package unipd.nonsense.exceptions;

public class InvalidTemplateException extends IllegalArgumentException
{
	public InvalidTemplateException()
	{
		super("Invalid template used");
	}

	public InvalidTemplateException(String msg)
	{
		super(msg);
	}

	public InvalidTemplateException(Throwable cause)
	{
		super(cause);
	}

	public InvalidTemplateException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
