package unipd.nonsense.exceptions;

import java.io.IOException;

public final class UnreadableFileException extends IOException
{
	public UnreadableFileException()
	{
		super("Unable to read file.");
	}

	public UnreadableFileException(String msg)
	{
		super(msg);
	}

	public UnreadableFileException(Throwable cause)
	{
		super(cause);
	}

	public UnreadableFileException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
