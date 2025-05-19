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

		Configurator.setLevel("FileLogger", Level.ALL);
		Configurator.setLevel("ConsoleLogger", Level.FATAL);

		this.logClass = loggingClass.getSimpleName();
		this.verbose = false;
	}

	public synchronized void switchVerboseMode()
	{
		verbose = !verbose;

		if(verbose)
			Configurator.setLevel("ConsoleLogger", Level.DEBUG);
		else
			Configurator.setLevel("ConsoleLogger", Level.FATAL);
	}

	public boolean getVerbose()
	{
		return verbose;
	}

	public synchronized void logTrace(String entry)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.trace(msg);
	}

	public synchronized void logDebug(String entry)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.debug(msg);
		if(verbose)
			consoleLogger.debug(msg);
	}

	public synchronized void logInfo(String entry)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.info(msg);
		if(verbose)
			consoleLogger.info(msg);
	}

	public synchronized void logWarn(String entry)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.warn(msg);
		if(verbose)
			consoleLogger.warn(msg);
	}

	public synchronized void logWarn(String entry, Throwable eccept)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.warn(msg, eccept);
		if(verbose)
			consoleLogger.warn(msg, eccept);
	}

	public synchronized void logError(String entry)
	{
		String msg = "[" +logClass + "] " + entry;

		fileLogger.error(msg);
		if(verbose)
			consoleLogger.error(msg);
	}

	public synchronized void logError(String entry, Throwable eccept)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.error(msg, eccept);
		if(verbose)
			consoleLogger.error(msg, eccept);
	}

	public synchronized void logFatal(String entry)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.fatal(msg);
		consoleLogger.fatal(msg);
	}

	public synchronized void logFatal(String entry, Throwable eccept)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.fatal(msg, eccept);
		consoleLogger.fatal(msg, eccept);
	}
}
