package unipd.nonsense;

import unipd.nonsense.util.CLI;
import unipd.nonsense.util.LoggerManager;

public class App
{
	private static final LoggerManager logger = new LoggerManager(App.class);

	public static void main(String[] args)
	{
		logger.logInfo("Application starting");

		try
		{
			CLI cli = new CLI();

			logger.logInfo("CLI initialized successfully");

			while(cli.inputCatcher());

			cli.closeResources();
			logger.logInfo("Application shutdown completed successfully");
		}
		catch(Exception e)
		{
			logger.logFatal("Terminating due to fatal error", e);
			System.exit(1);
		}
	}
}
