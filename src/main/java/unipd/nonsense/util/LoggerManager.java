package unipd.nonsense.util;
import org.apache.logging.log4j.*;

public class LoggerManager{

	private static final Logger logger = LogManager.getLogger(LoggerManager.class);

	public LoggerManager(){
	}

	public void logTrace(String entry){
		logger.trace(entry);
	}

	public void logDebug(String entry){
		logger.debug(entry);
	}

	public void logError(String entry){
		logger.error(entry);
	}

}
