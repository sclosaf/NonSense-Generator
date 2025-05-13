package unipd.nonsense.exceptions;

public class InvalidThresholdException extends IllegalArgumentException
{
	private float invalidThreshold;

	public InvalidThresholdException(float threshold)
	{
		super("Threshold must be between 0 and 1. Threshold: '" + threshold + "'");
	}

	public InvalidThresholdException(float threshold, String msg)
	{
		super(msg + "Threshold: '" + threshold + "'");
	}

	public InvalidThresholdException(float threshold, Throwable cause)
	{
		super(cause);
	}

	public InvalidThresholdException(float threshold, String msg, Throwable cause)
	{
		super(msg + "Threshold: '" + threshold + "'", cause);
	}

	public float getInvalidThreshold()
	{
		return invalidThreshold;
	}
}
