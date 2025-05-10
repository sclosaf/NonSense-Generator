package unipd.nonsense;

import unipd.nonsense.util.CLI;

public class App
{
	public static void main(String[] args)
	{
		try
		{
			CLI cli = new CLI();
			while(cli.inputCatcher());
			cli.closeResources();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
