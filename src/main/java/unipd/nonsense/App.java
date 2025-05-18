package unipd.nonsense;

import unipd.nonsense.util.CLI;
import unipd.nonsense.util.LoggerManager;

public class App
{
	private static final LoggerManager logger = new LoggerManager(App.class);

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
