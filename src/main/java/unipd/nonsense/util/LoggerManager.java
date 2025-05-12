package unipd.nonsense.util;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class LoggerManager
{
	private final Logger fileLogger;
	private final Logger consoleLogger;
	private final String logClass;
	private static boolean verbose;

	public LoggerManager(Class <?> loggingClass)
	{
		this.fileLogger = LogManager.getLogger("FileLogger");
		this.consoleLogger = LogManager.getLogger("ConsoleLogger");

		this.logClass = loggingClass.getSimpleName();
		this.verbose = false;
	}

	public void switchVerboseMode()
	{
		verbose = !verbose;

		if(verbose)
			Configurator.setLevel("ConsoleLogger", Level.DEBUG);
		else
			Configurator.setLevel("ConsoleLogger", Level.INFO);
	}

	public boolean getVerbose()
	{
		return verbose;
	}

	public void logTrace(String entry)
	{
		String msg = "[" + logClass + "]" + entry;

		fileLogger.trace(msg);
		consoleLogger.trace(msg);
	}

	public void logDebug(String entry)
	{
		String msg = "[" + logClass + "]" + entry;

		fileLogger.debug(msg);
		consoleLogger.debug(msg);
	}

	public void logInfo(String entry)
	{
		String msg = "[" + logClass + "]" + entry;

		fileLogger.info(msg);
		consoleLogger.info(msg);
	}

	public void logWarn(String entry)
	{
		String msg = "[" + logClass + "]" + entry;

		fileLogger.warn(msg);
		consoleLogger.warn(msg);
	}

	public void logWarn(String entry, Throwable eccept)
	{
		String msg = "[" + logClass + "]" + entry;

		fileLogger.warn(msg, eccept);
		consoleLogger.warn(msg, eccept);
	}

	public void logError(String entry)
	{
		String msg = "[" +logClass + "]" + entry;

		fileLogger.error(msg);
		consoleLogger.error(msg);
	}

	public void logError(String entry, Throwable eccept)
	{
		String msg = "[" + logClass + "]" + entry;

		fileLogger.error(msg, eccept);
		consoleLogger.error(msg, eccept);
	}

	public void logFatal(String entry)
	{
		String msg = "[" + logClass + "]" + entry;

		fileLogger.fatal(msg);
		consoleLogger.fatal(msg);
	}

	public void logFatal(String entry, Throwable eccept)
	{
		String msg = "[" + logClass + "]" + entry;

		fileLogger.fatal(msg, eccept);
		consoleLogger.fatal(msg, eccept);
	}
}
