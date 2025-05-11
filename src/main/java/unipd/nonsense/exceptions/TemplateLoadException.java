package unipd.nonsense.exception;

import java.io.IOException;

public class TemplateLoadException extends IOException
{
	public TemplateLoadException()
	{
		super("Unable to load template.");
	}

	public TemplateLoadException(String msg)
	{
		super(msg);
	}

	public TemplateLoadException(Throwable cause)
	{
		super(cause);
	}

	public TemplateLoadException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
