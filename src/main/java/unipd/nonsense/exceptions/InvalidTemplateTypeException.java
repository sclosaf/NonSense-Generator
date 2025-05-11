package unipd.nonsense.exceptions;

public class InvalidTemplateTypeException extends IllegalArgumentException
{
	public InvalidTemplateTypeException()
	{
		super("Invalid template type.");
	}

	public InvalidTemplateTypeException(String msg)
	{
		super(msg);
	}

	public InvalidTemplateTypeException(Throwable cause)
	{
		super(cause);
	}

	public InvalidTemplateTypeException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
