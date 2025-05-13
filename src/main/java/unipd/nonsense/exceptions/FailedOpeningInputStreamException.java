package unipd.nonsense.exceptions;

import java.io.IOException;

public final class FailedOpeningInputStreamException extends IOException
{
	public FailedOpeningInputStreamException()
	{
		super("Failed to open input stream");
	}

	public FailedOpeningInputStreamException(String msg)
	{
		super(msg);
	}

	public FailedOpeningInputStreamException(Throwable cause)
	{
		super(cause);
	}

	public FailedOpeningInputStreamException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
