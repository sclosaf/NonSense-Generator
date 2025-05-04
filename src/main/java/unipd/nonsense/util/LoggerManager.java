package unipd.nonsense.util;
import org.apache.logging.log4j.*;

public class LoggerManager
{
	private static final Logger logger = LogManager.getLogger(LoggerManager.class);

	public LoggerManager()
	{
		logger.debug("messaggio DEBUG");
		logger.fatal("messaggio FATAL");
		logger.debug("messaggio DEBUG");
	}
}
