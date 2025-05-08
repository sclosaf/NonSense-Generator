package unipd.nonsense.exceptions;

public final class InvalidJsonIndexException extends IndexOutOfBoundsException
{
	int invalidIndex;

	public InvalidJsonIndexException(int index)
	{
		super("Invalid index used to access json value. Value: '" + index + "'.");
		invalidIndex = index;
	}

	public InvalidJsonIndexException(int index, String msg)
	{
		super(msg + " Value: '" + index + "'.");
		invalidIndex = index;
	}

	public int getInvalidIndex()
	{
		return invalidIndex;
	}
}
