package unipd.nonsense.exceptions;

public final class InvalidFilePathException extends IllegalArgumentException
{
	String invalidPath;

	public InvalidFilePathException(String path)
	{
		super("Invalid path used. Path: '" + path + "'.");
		invalidPath = path;
	}

	public InvalidFilePathException(String path, String msg)
	{
		super(msg + " Path: '" + path + "'.");
		invalidPath = path;
	}

	public InvalidFilePathException(String path, Throwable cause)
	{
		super("Invalid path used. Path: '" + path + "'.", cause);
		invalidPath = path;
	}

	public InvalidFilePathException(String path, String msg, Throwable cause)
	{
		super(msg + " Path: '" + path + "'.", cause);
		invalidPath = path;
	}

	public String getInvalidPath()
	{
		return invalidPath;
	}
}
