package unipd.nonsense.util;

import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class CLI
{
	private enum Command
	{
		GENERATESENTENCE, ANALYZESENTENCE, GENERATEANDANALYZESENTENCE, SETTOLERANCE, HELP, QUIT
	}

	private static final String RESET = "\033[0m";
	private static final String BOLD = "\033[1m";
	private static final String RED = "\033[31m";
	private static final String GREEN = "\033[32m";
	private static final String YELLOW = "\033[33m";
	private static final String BLUE = "\033[34m";
	private static final String PURPLE = "\033[35m";
	private static final String CYAN = "\033[36m";

	private static Map<String, Command> commands = new HashMap<>();
	private Scanner scanner;
	private boolean running;

	public CLI()
	{
		running = true;
		scanner = new Scanner(System.in);

		commands.put("generate", Command.GENERATESENTENCE);
		commands.put("g", Command.GENERATESENTENCE);
		commands.put("analyze", Command.ANALYZESENTENCE);
		commands.put("a", Command.ANALYZESENTENCE);
		commands.put("generate and analyze", Command.GENERATEANDANALYZESENTENCE);
		commands.put("ga", Command.GENERATEANDANALYZESENTENCE);
		commands.put("set tolerance", Command.SETTOLERANCE);
		commands.put("st", Command.SETTOLERANCE);
		commands.put("help", Command.HELP);
		commands.put("h", Command.HELP);
		commands.put("quit", Command.QUIT);
		commands.put("q", Command.QUIT);

		welcome();
		usage();
	}

	private void welcome()
	{
		int asciiArtWidth = 58;
		String title = "Welcome to";

		int totalPadding = asciiArtWidth - title.length() - 2;
		int leftPadding = totalPadding / 2;
		int rightPadding = totalPadding - leftPadding;

		String topBorder = BOLD + "=".repeat(leftPadding) + "< " + title + " >" + "=".repeat(rightPadding) + RESET;

		System.out.println(topBorder);

		try(InputStream stream = getClass().getResourceAsStream("/asciiArt.txt"))
		{
			if(stream == null)
				return;

			try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream)))
			{
				String line;

				while((line = reader.readLine()) != null)
					System.out.println(BOLD + line + RESET);
			}
		}
		catch (IOException e)
		{
			System.out.println(RED + "[ERROR] Failed to load ASCII art." + RESET);
		}

		System.out.println(BOLD + "=".repeat(asciiArtWidth + 2) + RESET);
	}

	private void usage()
	{
		int totalWidth = 58;
		String title = "Available Commands";

		int titlePadding = (totalWidth - title.length() - 2) / 2;
		String titleLine = BOLD + PURPLE + "=".repeat(titlePadding) + "< " + title + " >" + "=".repeat(titlePadding) + RESET;

		System.out.println(titleLine);

		String format = GREEN + BOLD + "%-22s" + RESET + " %-33s" + RESET;

		System.out.printf(format + "\n", "Generate", "Generates a random nonsense sentence");
		System.out.printf(format + "\n", "Analyze", "Validates sentence structure and syntax");
		System.out.printf(format + "\n", "Generate and analyze", "Does both operations in one step");
		System.out.printf(format + "\n", "Set tolerance", "Change tollerance level (default: X)");
		System.out.printf(format + "\n", "Help", "Shows this help menu");
		System.out.printf(format + "\n", "Quit", "Exits the program");
		System.out.println(BOLD + PURPLE + "=".repeat(totalWidth + 2) + RESET);

		System.out.println(BOLD + PURPLE + "Enter a command or type 'Help' to see this again:" + RESET);
	}

	public boolean inputCatcher()
	{
		System.out.print(BOLD + ">> " + RESET);

		String cmd = scanner.nextLine();

		commandExecuter(cmd.trim().replaceAll("\\s+", " ").toLowerCase());

		return running;
	}

	public void closeResources()
	{
		if(scanner != null)
			scanner.close();

		if(running)
			running = false;
	}

	private void commandExecuter(String cmd)
	{
		if(cmd.isEmpty())
		{
			System.out.println(YELLOW + "Please enter a command." + RESET);
			return;
		}

		if(!commands.containsKey(cmd))
		{
			System.out.println(RED + "Invalid command. Type 'Help' for options." + RESET);
			return;
		}

		switch(commands.get(cmd))
		{
			case GENERATESENTENCE:
				System.out.println("Sentence generated");
			break;

			case ANALYZESENTENCE:
				System.out.println("Sentence analyzed");
			break;

			case GENERATEANDANALYZESENTENCE:
				System.out.println("Sentence generated and analyzed");
			break;

			case SETTOLERANCE:
				System.out.println("Setted new tolerance.");
			break;

			case HELP:
				usage();
			break;

			case QUIT:
				System.out.println("See you soon!");
				running = false;
			break;

			default:
				throw new IllegalArgumentException("Please insert a valid command");
		}
	}
}
