package unipd.nonsense.exceptions;

import java.io.IOException;

public final class InaccessibleFileException extends IOException
{
	public InaccessibleFileException()
	{
		super("Unable to open file");
	}

	public InaccessibleFileException(String msg)
	{
		super(msg);
	}

	public InaccessibleFileException(Throwable cause)
	{
		super(cause);
	}

	public InaccessibleFileException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
