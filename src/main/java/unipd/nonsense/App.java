package unipd.nonsense;

import unipd.nonsense.util.CLI;

public class App
{
	public static void main(String[] args)
	{
		CLI cli = new CLI();

		try
		{
			while(cli.inputCatcher());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			cli.closeResources();
		}
	}
}
