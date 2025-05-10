package unipd.nonsense.util;

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

public class CLI
{
	private enum Command
	{
		GENERATESENTENCE, ANALYZESENTENCE, GENERATEANDANALYZESENTENCE, SETTOLERANCE, HELP, CLEAR, QUIT
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
	private static final int HISTORY_SIZE = 25;
	private String initialOutput;
	private boolean running;

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

		running = true;

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

	private void welcome(PrintWriter writer)
	{
		int asciiArtWidth = 58;
		String title = "Welcome to";

		int totalPadding = asciiArtWidth - title.length() - 2;
		int leftPadding = totalPadding / 2;
		int rightPadding = totalPadding - leftPadding;

		String topBorder = "=".repeat(leftPadding) + "< " + title + " >" + "=".repeat(rightPadding);
		writer.println(new AttributedString(topBorder, BOLD_WHITE_STYLE).toAnsi(terminal));

		try(InputStream stream = getClass().getResourceAsStream("/asciiArt.txt"))
		{
			if(stream == null)
				return;

			try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream)))
			{
				String line;

				while((line = reader.readLine()) != null)
					writer.println(new AttributedString(line, BOLD_WHITE_STYLE).toAnsi(terminal));
			}
		}
		catch (IOException e)
		{
			 writer.println(new AttributedString("[ERROR] Failed to load ASCII art.", RED_STYLE).toAnsi(terminal));
		}

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
			"Set tolerance", "Change tolerance level (default: X)",
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
				terminal.writer().println(new AttributedString("Invalid command: " + cmd, RED_STYLE).toAnsi(terminal));
				terminal.flush();
			}

			commandExecuter(cmd);
			}
			catch(UserInterruptException e)
			{
				terminal.writer().println(new AttributedString("Program interrupted.", YELLOW_STYLE).toAnsi(terminal));
				terminal.flush();
				running = false;
			}
			catch(EndOfFileException e)
			{
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
			System.err.println("Failed to close terminal: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void commandExecuter(String cmd)
	{
		if(cmd.isEmpty())
		{
			terminal.writer().println(new AttributedString("Please enter a command.", YELLOW_STYLE).toAnsi(terminal));
			terminal.flush();
			return;
		}

		if(!commands.containsKey(cmd))
		{
			terminal.writer().println(new AttributedString("Type 'Help' for available commands.", RED_STYLE).toAnsi(terminal));
			terminal.flush();
			return;
		}

		switch(commands.get(cmd))
		{
			case GENERATESENTENCE:
				terminal.writer().println(new AttributedString("Sentence generated", DEFAULT_STYLE).toAnsi(terminal));
				terminal.flush();
			break;

			case ANALYZESENTENCE:
				terminal.writer().println(new AttributedString("Sentence analyzed", DEFAULT_STYLE).toAnsi(terminal));
				terminal.flush();
			break;

			case GENERATEANDANALYZESENTENCE:
				terminal.writer().println(new AttributedString("Sentence generated and analyzed", DEFAULT_STYLE).toAnsi(terminal));
				terminal.flush();
			break;

			case SETTOLERANCE:
				terminal.writer().println(new AttributedString("Set new tolerance.", DEFAULT_STYLE).toAnsi(terminal));
				terminal.flush();
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
}
