package unipd.nonsense.exceptions;

public final class JsonElementIsNotArrayException extends IllegalArgumentException
{
	String notArrayKey;

	public JsonElementIsNotArrayException(String key)
	{
		super("Used a json key: '" + key + "' that didn't contained an array");
		notArrayKey = key;
	}

	public JsonElementIsNotArrayException(String key, String msg)
	{
		super(msg + " Key: '" + key + "'");
		notArrayKey = key;
	}

	public JsonElementIsNotArrayException(String key, Throwable cause)
	{
		super("Used a json key: '" + key + "' that didn't contained an array", cause);
		notArrayKey = key;
	}

	public JsonElementIsNotArrayException(String key, String msg, Throwable cause)
	{
		super(msg + " Key: '" + key + "'", cause);
		notArrayKey = key;
	}

	public String getNotArrayKey()
	{
		return notArrayKey;
	}
}
