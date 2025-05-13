package unipd.nonsense.exceptions;

import java.io.IOException;

public final class UnwritableFileException extends IOException
{
	public UnwritableFileException()
	{
		super("Unable to write file");
	}

	public UnwritableFileException(String msg)
	{
		super(msg);
	}

	public UnwritableFileException(Throwable cause)
	{
		super(cause);
	}

	public UnwritableFileException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
