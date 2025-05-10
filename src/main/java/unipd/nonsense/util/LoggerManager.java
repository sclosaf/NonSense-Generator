package unipd.nonsense.util;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class LoggerManager{

	private final Logger logger;

	public LoggerManager(Class <?> myClass){
		this.logger = LogManager.getLogger(myClass);
	}

	public void logTrace(String entry){
		logger.trace(entry);
	}

	public void logDebug(String entry){
		logger.debug(entry);
	}
	
	public void logWarn(String entry){
		logger.warn(entry);
	}

	public void logWarn(String entry, Throwable eccept){
		logger.warn(entry, eccept);
	}

	public void logError(String entry){
		logger.error(entry);
	}

	public void logError(String entry, Throwable eccept){
		logger.error(entry, eccept);
	}

	public void logFatal(String entry){
		logger.fatal(entry);
	}

}
