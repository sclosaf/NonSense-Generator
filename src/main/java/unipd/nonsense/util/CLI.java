package unipd.nonsense.util;

import unipd.nonsense.util.CommandProcessor;

import unipd.nonsense.exceptions.MissingInternetConnectionException;

import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.jline.reader.*;
import org.jline.reader.Highlighter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;

import java.net.InetAddress;

public class CLI
{
	private enum Command
	{
		GENERATE, ANALYZE, GENERATEANALYZE, TREE, SETTOLERANCE, INFO, VERBOSE, HELP, CLEAR, QUIT
	}

	private static final AttributedStyle RED_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
	private static final AttributedStyle GREEN_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
	private static final AttributedStyle YELLOW_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
	private static final AttributedStyle PURPLE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
	private static final AttributedStyle DEFAULT_STYLE = AttributedStyle.DEFAULT;

	private static final AttributedStyle BOLD_RED_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED);
	private static final AttributedStyle BOLD_GREEN_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN);
	private static final AttributedStyle BOLD_YELLOW_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW);
	private static final AttributedStyle BOLD_MAGENTA_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.MAGENTA);
	private static final AttributedStyle BOLD_WHITE_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.WHITE);


	private static Map<String, Command> commands = new HashMap<>();
	private static final int HISTORY_SIZE = 20;
	private String initialOutput;
	private boolean running;
	private CommandProcessor processor;

	private final Terminal terminal;
	private final LineReader reader;

	private static class CommandHighlighter implements Highlighter
	{
		@Override
		public AttributedString highlight(LineReader reader, String buffer)
		{

			String trimmedBuffer = buffer.trim().toLowerCase();

			if(trimmedBuffer.isEmpty())
				return new AttributedString(buffer, DEFAULT_STYLE);

			boolean isValid = commands.keySet().stream().anyMatch(cmd -> cmd.startsWith(trimmedBuffer));

			if(isValid)
				return new AttributedString(buffer, GREEN_STYLE);

			return new AttributedString(buffer, RED_STYLE);
		}

		@Override
		public void setErrorPattern(java.util.regex.Pattern pattern)
		{}

		@Override
		public void setErrorIndex(int index)
		{}
	}

	public CLI() throws IOException
	{
		if(!checkInternetConnection())
			throw new MissingInternetConnectionException();

		terminal = TerminalBuilder.builder().system(true).build();
		reader = LineReaderBuilder.builder()
			.terminal(terminal)
			.completer(new StringsCompleter(commands.keySet()))
			.option(LineReader.Option.HISTORY_BEEP, false)
			.option(LineReader.Option.AUTO_LIST, true)
			.option(LineReader.Option.AUTO_FRESH_LINE, true)
			.variable(LineReader.HISTORY_SIZE, HISTORY_SIZE)
			.highlighter(new CommandHighlighter())
			.build();

		this.processor = new CommandProcessor();
		running = true;

		commands.put("generate", Command.GENERATE);
		commands.put("g", Command.GENERATE);
		commands.put("analyze", Command.ANALYZE);
		commands.put("a", Command.ANALYZE);
		commands.put("generate and analyze", Command.GENERATEANALYZE);
		commands.put("ga", Command.GENERATEANALYZE);
		commands.put("tree", Command.TREE);
		commands.put("t", Command.TREE);
		commands.put("set tolerance", Command.SETTOLERANCE);
		commands.put("st", Command.SETTOLERANCE);
		commands.put("info", Command.INFO);
		commands.put("i", Command.INFO);
		commands.put("verbose", Command.VERBOSE);
		commands.put("v", Command.VERBOSE);
		commands.put("help", Command.HELP);
		commands.put("h", Command.HELP);
		commands.put("clear", Command.CLEAR);
		commands.put("c", Command.CLEAR);
		commands.put("quit", Command.QUIT);
		commands.put("q", Command.QUIT);

		StringWriter stringWriter = new StringWriter();
		PrintWriter tempWriter = new PrintWriter(stringWriter);

		welcome(tempWriter);
		usage(tempWriter);
		tempWriter.flush();

		initialOutput = stringWriter.toString();

		welcome(terminal.writer());
		usage(terminal.writer());
	}

	private void welcome(PrintWriter writer) throws IOException
	{
		int asciiArtWidth = 58;
		String title = "Welcome to";

		int totalPadding = asciiArtWidth - title.length() - 2;
		int leftPadding = totalPadding / 2;
		int rightPadding = totalPadding - leftPadding;

		String topBorder = "=".repeat(leftPadding) + "< " + title + " >" + "=".repeat(rightPadding);
		writer.println(new AttributedString(topBorder, BOLD_WHITE_STYLE).toAnsi(terminal));

		InputStream stream = getClass().getResourceAsStream("/asciiArt.txt");

		if(stream == null)
			return;

		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

		String line;

		while((line = reader.readLine()) != null)
			writer.println(new AttributedString(line, BOLD_WHITE_STYLE).toAnsi(terminal));


		writer.println(new AttributedString("=".repeat(asciiArtWidth + 2), BOLD_WHITE_STYLE).toAnsi(terminal));
		terminal.flush();
	}

	private void usage(PrintWriter writer)
	{
		int totalWidth = 58;
		String title = "Available Commands";

		int titlePadding = (totalWidth - title.length() - 2) / 2;
		String titleLine = "=".repeat(titlePadding) + "< " + title + " >" + "=".repeat(titlePadding);
		writer.println(new AttributedString(titleLine, BOLD_MAGENTA_STYLE).toAnsi(terminal));

		String[] commands =
		{
			"Generate", "Generates a random nonsense sentence",
			"Analyze", "Validates sentence structure and syntax",
			"Generate and analyze", "Does both operations in one step",
			"Tree", "Prints the syntactic tree",
			"Set tolerance", "Change tolerance level (default: X)",
			"Info", "Shows more detailed infos about the commands",
			"Clear", "Clears the terminal and shows initial menu",
			"Help", "Shows this help menu",
			"Quit", "Exits the program"
		};

		for(int i = 0; i < commands.length; i += 2)
		{
			String command = String.format("%-22s", commands[i]);
			String description = String.format("%-33s", commands[i + 1]);

			AttributedString styledCommand = new AttributedString(command, BOLD_GREEN_STYLE);
			AttributedString styledDescription = new AttributedString(description, BOLD_WHITE_STYLE);

			writer.println(styledCommand.toAnsi(terminal) + styledDescription.toAnsi(terminal));
		}

		writer.println(new AttributedString("=".repeat(totalWidth + 2), BOLD_MAGENTA_STYLE).toAnsi(terminal));
		writer.println(new AttributedString("Enter a command or type 'Help' to see this again:", BOLD_MAGENTA_STYLE).toAnsi(terminal));
		writer.flush();
	}

	private void extendedUsage(PrintWriter writer)
	{
		int totalWidth = 58;
		String title = "Extended Commands help";
		int titlePadding = (totalWidth - title.length() - 1) / 2;

		String titleLine = "=".repeat(titlePadding) + "< " + title + " >" + "=".repeat(titlePadding);
		writer.println(new AttributedString(titleLine, BOLD_MAGENTA_STYLE).toAnsi(terminal));

		String[][] commandsInfo =
		{
			{
				"generate (g)",
					"Generates a random nonsense sentence.\n" +
					"The sentece even if it has grammatical sense,\n" +
					"it's missing all the logical sense.\n" +
					"The sentence is printed and buffered."
			},
			{
				"analyze (a)",
					"Validates the buffered sentence structure and syntax.\n" +
					"Via different settings can be analyzed accordingly to its:\n" +
					"'toxicity', 'sentiment' or 'syntax'.\n" +
					"If no sentence is buffered, no analysis is performed."
			},
			{
				"generate and analyze (ga)",
					"Performs both generation and analysis\n" +
					"in one step. First generates a sentence, then\n" +
					"analyzes its structure.\n" +
					"Generated sentence is buffered."
			},
			{
				"tree (t)",
					"Prints the syntactic tree of the buffered sentence.\n" +
					"Shows the hierarchical structure of the sentence\n" +
					"components for better understanding.\n" +
					"If no sentence is buffered, no analysis is performed.\n" +
					"This function requires to analyze the sentence."
			},
			{
				"set tolerance (st)",
					"Changes the tolerance level for the analysis.\n" +
					"Default: X for toxicity;\n" +
					"Default: Y for sentiment;\n" +
					"Default: Z for syntax.\n" +
					"Higher values analyzed sentences will be blocked.\n" +
					"A confirmation message is showed asking if the user wants to proceed."
			},
			{
				"info (i)",
					"Shows detailed information about commands.\n" +
					"Provides extended help for each available command (even hidden ones)."
			},
			{
				"verbose (v)",
					"Toggles verbose output mode.\n" +
					"When enabled, provides more detailed feedback\n" +
					"during command execution (for debugging).\n" +
					"Default is off."
			},
			{
				"clear (c)",
					"Clears the terminal screen.\n" +
					"Resets the display and shows the initial menu."
			},
			{
				"help (h)",
					"Displays basic help information."
			},
			{
				"quit (q)",
					"Exits the program.\n" +
					"Terminates the application safely."
			},
		};

		for(String[] cmdInfo : commandsInfo)
		{
			String command = cmdInfo[0];
			String description = cmdInfo[1];

			writer.println(new AttributedString(command, BOLD_GREEN_STYLE).toAnsi(terminal));

			String[] lines = description.split("\n");
			for (String line : lines)
				writer.println(new AttributedString("    " + line, BOLD_WHITE_STYLE).toAnsi(terminal));

			writer.println();
		}

		writer.println(new AttributedString("=".repeat(totalWidth + 2), BOLD_MAGENTA_STYLE).toAnsi(terminal));
		writer.flush();
	}

	private void clearTerminal()
	{
		terminal.puts(InfoCmp.Capability.clear_screen);
		terminal.writer().print("\033[3J");
		terminal.flush();

		terminal.writer().print(initialOutput);
		terminal.flush();
	}

	public boolean inputCatcher()
	{
		try
		{
			String cmd = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();

			if(!cmd.isEmpty() && !commands.containsKey(cmd))
			{
				terminal.writer().println(new AttributedString("Invalid command: " + cmd, BOLD_RED_STYLE).toAnsi(terminal));
				terminal.flush();
			}

			commandExecuter(cmd);
			}
			catch(UserInterruptException | EndOfFileException e)
			{
				terminal.writer().println(new AttributedString("Program ended.", BOLD_YELLOW_STYLE).toAnsi(terminal));
				terminal.flush();
				running = false;
			}

		return running;
	}

	public void closeResources()
	{
		if(running)
			running = false;

		try
		{
			terminal.close();
		}
		catch(IOException e)
		{
			terminal.writer().println(new AttributedString("Failed to close terminal: " + e.getMessage(), BOLD_RED_STYLE).toAnsi(terminal));
			e.printStackTrace();
		}
	}

	private void commandExecuter(String cmd)
	{
		if(cmd.isEmpty())
		{
			terminal.writer().println(new AttributedString("Please enter a command.", BOLD_YELLOW_STYLE).toAnsi(terminal));
			terminal.flush();
			return;
		}

		if(!commands.containsKey(cmd))
		{
			terminal.writer().println(new AttributedString("Type 'Help' for available commands.", BOLD_RED_STYLE).toAnsi(terminal));
			terminal.flush();
			return;
		}

		switch(commands.get(cmd))
		{
			case GENERATE:
				terminal.writer().println(new AttributedString("Sentence generated", DEFAULT_STYLE).toAnsi(terminal));
				terminal.flush();
			break;

			case ANALYZE:
				terminal.writer().println(new AttributedString("Sentence analyzed", DEFAULT_STYLE).toAnsi(terminal));
				terminal.flush();
			break;

			case GENERATEANALYZE:
				terminal.writer().println(new AttributedString("Sentence generated and analyzed", DEFAULT_STYLE).toAnsi(terminal));
				terminal.flush();
			break;

			case TREE:
				terminal.writer().println(new AttributedString("Tree printed", DEFAULT_STYLE).toAnsi(terminal));
				terminal.flush();
			break;

			case SETTOLERANCE:
				terminal.writer().println(new AttributedString("Set new tolerance.", DEFAULT_STYLE).toAnsi(terminal));
				terminal.flush();
			break;

			case INFO:
				extendedUsage(terminal.writer());
			break;

			case VERBOSE:
				processor.switchVerbosity();
			break;

			case CLEAR:
				clearTerminal();
				break;

			case HELP:
				usage(terminal.writer());
			break;

			case QUIT:
				terminal.writer().println(new AttributedString("See you soon!", DEFAULT_STYLE).toAnsi(terminal));
				terminal.flush();
				running = false;
			break;
		}
	}

	private boolean checkInternetConnection()
	{
		try
		{
			InetAddress.getByName("google.com").isReachable(1000);
			return true;

		}
		catch(Exception e)
		{
			try
			{
				InetAddress.getByName("cloudflare.com").isReachable(1000);
				return true;
			}
			catch (Exception ex)
			{
				return false;
			}
		}
	}
}
