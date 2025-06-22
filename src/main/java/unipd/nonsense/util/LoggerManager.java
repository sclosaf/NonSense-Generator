package unipd.nonsense.util;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * A dual-logging manager that handles both file and console logging with configurable verbosity.
 * <p>
 * This class provides synchronized logging methods that write to both file and console loggers,
 * with console output being controllable via a verbose mode switch. The class maintains two
 * separate log4j loggers:
 * <ul>
 *	<li>File logger - Always active at ALL level</li>
 *	<li>Console logger - Defaults to FATAL level unless verbose mode is enabled</li>
 * </ul>
 * </p>
 *
 * <p>Key features:
 * <ul>
 *	<li>Thread-safe logging operations</li>
 *	<li>Dynamic verbosity control</li>
 *	<li>Class-contextual logging</li>
 *	<li>Exception logging support</li>
 *	<li>Dual output channels (file + console)</li>
 * </ul>
 * </p>
 */
public class LoggerManager
{
	/**
	 * File logger instance that writes to log files.
	 * <p>
	 * Configured to log at {@code Level.ALL} by default, capturing all log events.
	 * </p>
	 */
	private final Logger fileLogger;

	/**
	 * Console logger instance that writes to standard output.
	 * <p>
	 * Defaults to {@code Level.FATAL} but can be elevated to {@code Level.DEBUG}
	 * when verbose mode is enabled.
	 * </p>
	 */
	private final Logger consoleLogger;

	/**
	 * Simple name of the class being logged, used for contextual logging.
	 */
	private final String logClass;

	/**
	 * Verbose mode flag controlling console output granularity.
	 * <p>
	 * When {@code true}, console logger outputs DEBUG level and above.
	 * When {@code false}, console logger outputs only FATAL level.
	 * </p>
	 */
	private static boolean verbose;

	/**
	 * Constructs a new {@code LoggerManager} for the specified class.
	 * <p>
	 * Initializes both file and console loggers and sets their default levels:
	 * <ul>
	 *	<li>File logger: {@code Level.ALL}</li>
	 *	<li>Console logger: {@code Level.FATAL}</li>
	 * </ul>
	 * </p>
	 *
	 * @param loggingClass	the {@code Class} to associate with log messages
	 */
	public LoggerManager(Class <?> loggingClass)
	{
		this.fileLogger = LogManager.getLogger("FileLogger");
		this.consoleLogger = LogManager.getLogger("ConsoleLogger");

		Configurator.setLevel("FileLogger", Level.ALL);
		Configurator.setLevel("ConsoleLogger", Level.FATAL);

		this.logClass = loggingClass.getSimpleName();
		this.verbose = false;
	}

	/**
	 * Toggles verbose mode for console output.
	 * <p>
	 * When enabled, console logger level is set to {@code Level.DEBUG}.
	 * When disabled, console logger level reverts to {@code Level.FATAL}.
	 * This operation is thread-safe.
	 * </p>
	 */
	public synchronized void switchVerboseMode()
	{
		verbose = !verbose;

		if(verbose)
			Configurator.setLevel("ConsoleLogger", Level.DEBUG);
		else
			Configurator.setLevel("ConsoleLogger", Level.FATAL);
	}

	/**
	 * Returns the current verbose mode status.
	 *
	 * @return	{@code true} if verbose mode is active, {@code false} otherwise
	 */
	public boolean getVerbose()
	{
		return verbose;
	}

	/**
	 * Logs a TRACE level message.
	 * <p>
	 * TRACE level is used for very detailed debugging information.
	 * Messages are only written to the file logger, regardless of verbose mode.
	 * </p>
	 *
	 * @param entry	the message to log
	 */
	public synchronized void logTrace(String entry)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.trace(msg);
	}

	/**
	 * Logs a DEBUG level message.
	 * <p>
	 * DEBUG level is used for general debugging information.
	 * Messages are written to file logger and, if verbose mode is active,
	 * to console logger as well.
	 * </p>
	 *
	 * @param entry	the message to log
	 */
	public synchronized void logDebug(String entry)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.debug(msg);
		if(verbose)
			consoleLogger.debug(msg);
	}

	/**
	 * Logs an INFO level message.
	 * <p>
	 * INFO level is used for informational messages about application progress.
	 * Messages are written to file logger and, if verbose mode is active,
	 * to console logger as well.
	 * </p>
	 *
	 * @param entry	the message to log
	 */
	public synchronized void logInfo(String entry)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.info(msg);
		if(verbose)
			consoleLogger.info(msg);
	}

	/**
	 * Logs a WARN level message without exception.
	 * <p>
	 * WARN level is used for potentially harmful situations.
	 * Messages are written to file logger and, if verbose mode is active,
	 * to console logger as well.
	 * </p>
	 *
	 * @param entry	the warning message to log
	 */
	public synchronized void logWarn(String entry)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.warn(msg);
		if(verbose)
			consoleLogger.warn(msg);
	}

	/**
	 * Logs a WARN level message with associated exception.
	 * <p>
	 * WARN level is used for potentially harmful situations.
	 * Both message and exception stack trace are written to file logger
	 * and, if verbose mode is active, to console logger as well.
	 * </p>
	 *
	 * @param entry		the warning message to log
	 * @param eccept	the {@code Throwable} to log with the message
	 */
	public synchronized void logWarn(String entry, Throwable eccept)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.warn(msg, eccept);
		if(verbose)
			consoleLogger.warn(msg, eccept);
	}

	/**
	 * Logs an ERROR level message without exception.
	 * <p>
	 * ERROR level is used for error events that might still allow the
	 * application to continue running.
	 * Messages are written to file logger and, if verbose mode is active,
	 * to console logger as well.
	 * </p>
	 *
	 * @param entry	the error message to log
	 */
	public synchronized void logError(String entry)
	{
		String msg = "[" +logClass + "] " + entry;

		fileLogger.error(msg);
		if(verbose)
			consoleLogger.error(msg);
	}

	/**
	 * Logs an ERROR level message with associated exception.
	 * <p>
	 * ERROR level is used for error events that might still allow the
	 * application to continue running.
	 * Both message and exception stack trace are written to file logger
	 * and, if verbose mode is active, to console logger as well.
	 * </p>
	 *
	 * @param entry		the error message to log
	 * @param eccept	the {@code Throwable} to log with the message
	 */
	public synchronized void logError(String entry, Throwable eccept)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.error(msg, eccept);
		if(verbose)
			consoleLogger.error(msg, eccept);
	}

	/**
	 * Logs a FATAL level message without exception.
	 * <p>
	 * FATAL level is used for severe error events that will presumably
	 * lead the application to abort.
	 * Messages are written to both file and console loggers regardless
	 * of verbose mode.
	 * </p>
	 *
	 * @param entry	the fatal error message to log
	 */
	public synchronized void logFatal(String entry)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.fatal(msg);
		consoleLogger.fatal(msg);
	}

	/**
	 * Logs a FATAL level message with associated exception.
	 * <p>
	 * FATAL level is used for severe error events that will presumably
	 * lead the application to abort.
	 * Both message and exception stack trace are written to both file
	 * and console loggers regardless of verbose mode.
	 * </p>
	 *
	 * @param entry		the fatal error message to log
	 * @param eccept	the {@code Throwable} to log with the message
	 */
	public synchronized void logFatal(String entry, Throwable eccept)
	{
		String msg = "[" + logClass + "] " + entry;

		fileLogger.fatal(msg, eccept);
		consoleLogger.fatal(msg, eccept);
	}
}
