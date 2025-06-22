package unipd.nonsense;

import unipd.nonsense.util.CLI;
import unipd.nonsense.util.LoggerManager;

/**
 * Main application class that serves as the entry point for the nonsense sentence generator.
 * <p>
 * This class initializes the application and manages the main execution flow, including:
 * <ul>
 *	<li>Application startup and shutdown procedures</li>
 *	<li>Error handling and logging</li>
 *	<li>Resource management</li>
 *	<li>CLI interface initialization</li>
 * </ul>
 * </p>
 * <p>
 * The application follows a structured lifecycle:
 * <ol>
 *	<li>Initialize logging system</li>
 *	<li>Create and configure CLI interface</li>
 *	<li>Process user commands until termination</li>
 *	<li>Clean up resources on exit</li>
 * </ol>
 * </p>
 */
public class App
{
	/**
	 * Logger instance for the application class.
	 * <p>
	 * Features:
	 * <ul>
	 *	<li>Class-specific logging configuration</li>
	 *	<li>Support for multiple log levels (trace, debug, info, warn, error, fatal)</li>
	 *	<li>Automatic class name inclusion in log messages</li>
	 * </ul>
	 */
	private static final LoggerManager logger = new LoggerManager(App.class);


	/**
	 * Main entry point for the application.
	 * <p>
	 * Execution flow:
	 * <ol>
	 *	<li>Logs application startup</li>
	 *	<li>Initializes CLI interface</li>
	 *	<li>Enters command processing loop</li>
	 *	<li>Handles termination (normal or error)</li>
	 * </ol>
	 * </p>
	 * <p>
	 * Error handling:
	 * <ul>
	 *	<li>Catches and logs all exceptions</li>
	 *	<li>Ensures resource cleanup in finally block</li>
	 *	<li>Provides appropriate exit codes (0 for success, 1 for error)</li>
	 * </ul>
	 * </p>
	 *
	 * @param args	Command-line arguments (not currently used)
	 */
	public static void main(String[] args)
	{
		logger.logInfo("Application starting");

		CLI cli = null;

		try
		{
			cli = new CLI();
			while(cli.inputCatcher());
		}
		catch(Exception e)
		{
			logger.logFatal("Fatal error, terminating due to ", e);

			if(cli != null)
				cli.closeResources();

			System.exit(1);
		}
		finally
		{
			if(cli != null)
				cli.closeResources();

			System.exit(0);
		}
	}
}
