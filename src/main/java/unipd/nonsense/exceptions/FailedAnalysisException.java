package unipd.nonsense.exceptions;

public class FailedAnalysisException extends RuntimeException
{
	public FailedAnalysisException()
	{
		super("Failed sentence analysis");
	}

	public FailedAnalysisException(String msg)
	{
		super(msg);
	}

	public FailedAnalysisException(Throwable cause)
	{
		super(cause);
	}

	public FailedAnalysisException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
