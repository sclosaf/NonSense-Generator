package unipd.nonsense.exceptions;

public final class InvalidJsonKeyException extends IllegalArgumentException
{
	private String invalidKey;

	public InvalidJsonKeyException(String key)
	{
		super("Used a json key: '" + key + "' that was't part of the json file.");
		invalidKey = key;
	}

	public InvalidJsonKeyException(String key, String msg)
	{
		super(msg + " Key: '" + key + "'.");
		invalidKey = key;
	}

	public InvalidJsonKeyException(String key, Throwable cause)
	{
		super("Used a json key: '" + key + "' that wasn't part of the json file.", cause);
		invalidKey = key;
	}

	public InvalidJsonKeyException(String key, String msg, Throwable cause)
	{
		super(msg + " Key: '" + key + "'.", cause);
		invalidKey = key;
	}

	public String getInvalidKey()
	{
		return invalidKey;
	}
}
