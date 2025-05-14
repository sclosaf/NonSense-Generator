package unipd.nonsense.util;

import unipd.nonsense.util.CommandProcessor;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Noun.Number;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Verb.Tense;

import unipd.nonsense.exceptions.MissingInternetConnectionException;
import unipd.nonsense.exceptions.IllegalToleranceException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Random;

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
		DEFAULT, PERSONALIZED, GENERATE, ANALYZE, TREE, EXTEND, SETTOLERANCE, INFO, VERBOSE, HELP, CLEAR, QUIT
	}

	private enum GenerateOptions
	{
		RANDOM, NUMBER, TENSE, BOTH
	}

	private enum AnalyzeOptions
	{
		RANDOM, ALL, SYNTAX, SENTIMENT, TOXICITY, COMBINED
	}

	private static final AttributedStyle RED_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
	private static final AttributedStyle BLUE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);
	private static final AttributedStyle GREEN_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
	private static final AttributedStyle YELLOW_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
	private static final AttributedStyle PURPLE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
	private static final AttributedStyle DEFAULT_STYLE = AttributedStyle.DEFAULT;

	private static final AttributedStyle BOLD_RED_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED);
	private static final AttributedStyle BOLD_BLUE_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE);
	private static final AttributedStyle BOLD_GREEN_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN);
	private static final AttributedStyle BOLD_YELLOW_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW);
	private static final AttributedStyle BOLD_MAGENTA_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.MAGENTA);
	private static final AttributedStyle BOLD_WHITE_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.WHITE);


	private static Map<String, Command> commands = new HashMap<>();
	private static Map<String, GenerateOptions> generateOptions = new HashMap<>();
	private static Map<String, AnalyzeOptions> analyzeOptions = new HashMap<>();
	private static final int HISTORY_SIZE = 20;
	private static final int MAX_ATTEMPTS = 5;
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

		commands.put("default", Command.DEFAULT);
		commands.put("d", Command.DEFAULT);
		commands.put("personalized", Command.PERSONALIZED);
		commands.put("p", Command.PERSONALIZED);
		commands.put("generate", Command.GENERATE);
		commands.put("g", Command.GENERATE);
		commands.put("analyze", Command.ANALYZE);
		commands.put("a", Command.ANALYZE);
		commands.put("tree", Command.TREE);
		commands.put("t", Command.TREE);
		commands.put("extends", Command.EXTEND);
		commands.put("e", Command.EXTEND);
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

		generateOptions.put("random", GenerateOptions.RANDOM);
		generateOptions.put("r", GenerateOptions.RANDOM);
		generateOptions.put("number", GenerateOptions.NUMBER);
		generateOptions.put("n", GenerateOptions.NUMBER);
		generateOptions.put("tense", GenerateOptions.TENSE);
		generateOptions.put("t", GenerateOptions.TENSE);
		generateOptions.put("both", GenerateOptions.BOTH);
		generateOptions.put("b", GenerateOptions.BOTH);

		analyzeOptions.put("random", AnalyzeOptions.RANDOM);
		analyzeOptions.put("r", AnalyzeOptions.RANDOM);
		analyzeOptions.put("all", AnalyzeOptions.RANDOM);
		analyzeOptions.put("a", AnalyzeOptions.RANDOM);
		analyzeOptions.put("syntax", AnalyzeOptions.RANDOM);
		analyzeOptions.put("sy", AnalyzeOptions.RANDOM);
		analyzeOptions.put("sentiment", AnalyzeOptions.RANDOM);
		analyzeOptions.put("se", AnalyzeOptions.RANDOM);
		analyzeOptions.put("toxicity", AnalyzeOptions.RANDOM);
		analyzeOptions.put("t", AnalyzeOptions.RANDOM);
		analyzeOptions.put("combined", AnalyzeOptions.RANDOM);
		analyzeOptions.put("c", AnalyzeOptions.RANDOM);

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
			"Default", "Performs a basic combination of generation and analysis",
			"Personalized", "Performs the full process, with each step personalizable",
			"Generate", "Generates a random nonsense sentence",
			"Analyze", "Validates sentence structure and syntax",
			"Tree", "Prints the syntactic tree",
			"Extend", "User can insert a noun, adjective or verb to the dicionaries",
			"Set tolerance", "Change tolerance level for the analysis",
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
				"Default (d)",
					"Performs both procedures of generation and analysis in one step.\n" +
					"This is a default combination of commands, for a more specific settings\n" +
					"use the other commands."
			},
			{
				"Personalized (p)",
					"Performs the whole process, but every part of it is personalizable\n" +
					"acordingly to the user choice."
			},
			{
				"Generate (g)",
					"Generates a random nonsense sentence.\n" +
					"The sentece even if it has grammatical sense,\n" +
					"it's missing all the logical sense.\n" +
					"The sentence is printed and buffered."
			},
			{
				"Analyze (a)",
					"Validates the buffered sentence structure and syntax.\n" +
					"Via different settings can be analyzed accordingly to its:\n" +
					"'toxicity', 'sentiment' or 'syntax'.\n" +
					"If no sentence is buffered, no analysis is performed."
			},
			{
				"Tree (t)",
					"Prints the syntactic tree of the buffered sentence.\n" +
					"Shows the hierarchical structure of the sentence\n" +
					"components for better understanding.\n" +
					"If no sentence is buffered, no analysis is performed.\n" +
					"This function requires to analyze the sentence."
			},
			{
				"Extend",
					"Gives the opportunity to the user to input a Noun, an Adjective or a Verb\n" +
					"to the data dictionaries used by the program."
			},
			{
				"Set tolerance (st)",
					"Changes the tolerance level for the analysis.\n" +
					"Default 0.7 for toxicity (ranges from 0.0 to 1.0),\n" +
					"  Defines the level over which a text is considered offensive.\n"
			},
			{
				"Info (i)",
					"Shows detailed information about commands.\n" +
					"Provides extended help for each available command (even hidden ones)."
			},
			{
				"Verbose (v)",
					"Toggles verbose output mode.\n" +
					"When enabled, provides more detailed feedback\n" +
					"during command execution (for debugging).\n" +
					"Default is off."
			},
			{
				"Clear (c)",
					"Clears the terminal screen.\n" +
					"Resets the display and shows the initial menu."
			},
			{
				"Help (h)",
					"Displays basic help information."
			},
			{
				"Quit (q)",
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

	public boolean inputCatcher() throws IOException
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

	private void commandExecuter(String cmd) throws IOException
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
			case DEFAULT:

			break;

			case PERSONALIZED:
				personalizedHandler();
			break;

			case GENERATE:
				generateHandler();
			break;

			case ANALYZE:
				analyzeHandler();
			break;

			case TREE:
				treeHandler();
			break;

			case EXTEND:
				extendHandler();
			break;

			case SETTOLERANCE:
				setToleranceHandler();
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
				quit();
			break;
		}
	}

	private void defaultHandler()
	{
		try
		{
			terminal.writer().println(new AttributedString("Proceding with default process.", BOLD_WHITE_STYLE).toAnsi(terminal));
			terminal.writer().println(new AttributedString("Enter a sentence to process (or press Enter to generate one automatically and skip the elabotation process):", BOLD_WHITE_STYLE).toAnsi(terminal));
			terminal.flush();

			String userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim();
			String syntax = "";
			String toxicity = "";

			if(userInput.isEmpty())
			{
				userInput = processor.generateRandom();
				terminal.writer().println(new AttributedString("Generated sentence: " + userInput, GREEN_STYLE).toAnsi(terminal));

				terminal.writer().println(new AttributedString("Proceding with the standard analyze of the generated sentence.", BOLD_WHITE_STYLE).toAnsi(terminal));

				syntax = processor.analyzeSyntax(userInput);
				terminal.writer().println(new AttributedString(syntax, BOLD_WHITE_STYLE).toAnsi(terminal));

				toxicity = processor.analyzeToxicity(userInput);
				terminal.writer().println(new AttributedString(toxicity, BOLD_WHITE_STYLE).toAnsi(terminal));

				return;
			}

			terminal.writer().println(new AttributedString("Proceding with the standard analyze of the sentence.", BOLD_WHITE_STYLE).toAnsi(terminal));
			syntax = processor.analyzeSyntax(userInput);
			terminal.writer().println(new AttributedString("Analysis result:\n" + syntax, DEFAULT_STYLE).toAnsi(terminal));

			String generated = processor.generateFrom(userInput);

			terminal.writer().println(new AttributedString("Proceding with the standard analyze of the generated sentence.", BOLD_WHITE_STYLE).toAnsi(terminal));

			syntax = processor.analyzeSyntax(generated);
			terminal.writer().println(new AttributedString(syntax, BOLD_WHITE_STYLE).toAnsi(terminal));

			toxicity = processor.analyzeToxicity(generated);
			terminal.writer().println(new AttributedString(toxicity, BOLD_WHITE_STYLE).toAnsi(terminal));

			String syntaxTree = processor.generateSyntaxTree(generated);
			terminal.writer().println(new AttributedString("\nSyntax tree:\n" + syntaxTree, BOLD_WHITE_STYLE).toAnsi(terminal));
		}
		catch(IOException e)
		{
			terminal.writer().println(new AttributedString("Error processing input: " + e.getMessage(), BOLD_RED_STYLE).toAnsi(terminal));
		}

		terminal.flush();
	}

	private void personalizedHandler() throws IOException
	{
		terminal.writer().println(new AttributedString("Proceding with the personalized process.", BOLD_WHITE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("Enter a sentence to process (or press Enter to generate one automatically and skip the elabotation process):", BOLD_WHITE_STYLE).toAnsi(terminal));
		terminal.flush();

		String userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim();

		if(userInput.isEmpty())
			generateHandler();

		analyzeHandler();
		treeHandler();
	}

	private void generateHandler() throws IOException
	{
		terminal.writer().println(new AttributedString("Proceeding generating a random sentence, select one of the following options:", BOLD_BLUE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Randomized", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("      Each parameter (number and tense) used in the generated sentence, are selected randomly", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Number", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("      User can choose the number used into the generated sentence, the tense is selected randomly", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Tense", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("      User can choose the tense used into the generated sentence, the number is selected randomly", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Both", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("      User can choose both tense and number used in the generated sentence", DEFAULT_STYLE).toAnsi(terminal));
		terminal.flush();

		String userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();

		for(int i = MAX_ATTEMPTS; i >= 0; --i)
		{
			if(i == 0)
				return;

			if(userInput.isEmpty() || !generateOptions.containsKey(userInput))
			{
				terminal.writer().println(new AttributedString("Please enter a valid option. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
				terminal.flush();
				userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
			}
			else
				break;
		}

		switch(generateOptions.get(userInput))
		{
			case RANDOM: generateRandom(); break;
			case NUMBER: generateNumber(); break;
			case TENSE: generateTense(); break;
			case BOTH: generateBoth(); break;
		}
	}

	private void generateRandom()
	{
		String generated = processor.generateRandom();
		terminal.writer().println(new AttributedString("Generated sentence: " + generated, GREEN_STYLE).toAnsi(terminal));
	}

	private void generateNumber()
	{
		terminal.writer().println(new AttributedString("Specify the number among the available:", BOLD_BLUE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Singular", BOLD_WHITE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Pluralar", BOLD_WHITE_STYLE).toAnsi(terminal));

		String userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();

		for(int i = MAX_ATTEMPTS; i >= 0; --i)
		{
			if(i == 0)
				return;

			if(userInput.isEmpty() || (!userInput.equals("singular") && !userInput.equals("s") && !userInput.equals("plural") && !userInput.equals("p")))
			{
				terminal.writer().println(new AttributedString("Please enter a valid option. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
				terminal.flush();
				userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
			}
			else
				break;
		}

		if(userInput.equals("singular") || userInput.equals("s"))
		{
			String generated = processor.generateWithNumber(Number.SINGULAR);
			terminal.writer().println(new AttributedString("Generated sentence: " + generated, GREEN_STYLE).toAnsi(terminal));
		}
		else if(userInput.equals("plural") || userInput.equals("p"))
		{
			String generated = processor.generateWithNumber(Number.PLURAL);
			terminal.writer().println(new AttributedString("Generated sentence: " + generated, GREEN_STYLE).toAnsi(terminal));
		}
	}

	private void generateTense()
	{
		terminal.writer().println(new AttributedString("Specify the tense among the available:", BOLD_BLUE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Past", BOLD_WHITE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Present", BOLD_WHITE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Future", BOLD_WHITE_STYLE).toAnsi(terminal));

		String userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();

		for(int i = 0; i <= MAX_ATTEMPTS; ++i)
		{
			if(i == MAX_ATTEMPTS)
				return;

			if(userInput.isEmpty() || (!userInput.equals("past") && !userInput.equals("pa") && !userInput.equals("present") && !userInput.equals("pr") && !userInput.equals("future") && !userInput.equals("f")))
			{
				terminal.writer().println(new AttributedString("Please enter a valid option. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
				terminal.flush();
				userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
			}
			else
				break;
		}

		if(userInput.equals("past") || userInput.equals("pa"))
		{
			String generated = processor.generateWithTense(Tense.PAST);
		terminal.writer().println(new AttributedString("Generated sentence: " + generated, GREEN_STYLE).toAnsi(terminal));
		}
		else if(userInput.equals("present") || userInput.equals("pr"))
		{
			String generated = processor.generateWithTense(Tense.PRESENT);
			terminal.writer().println(new AttributedString("Generated sentence: " + generated, GREEN_STYLE).toAnsi(terminal));
		}
		else if(userInput.equals("future") || userInput.equals("f"))
		{
			String generated = processor.generateWithTense(Tense.FUTURE);
			terminal.writer().println(new AttributedString("Generated sentence: " + generated, GREEN_STYLE).toAnsi(terminal));
		}
	}

	private void generateBoth()
	{
		terminal.writer().println(new AttributedString("Specify the number among the available:", BOLD_BLUE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Singular", BOLD_WHITE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Pluralar", BOLD_WHITE_STYLE).toAnsi(terminal));

		String userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();

		for(int i = MAX_ATTEMPTS; i >= 0; --i)
		{
			if(i == 0)
				return;

			if(userInput.isEmpty() || (!userInput.equals("singular") && !userInput.equals("s") && !userInput.equals("plural") && !userInput.equals("p")))
			{
				terminal.writer().println(new AttributedString("Please enter a valid option. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
				terminal.flush();
				userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
			}
			else
				break;
		}

		Number number;
		if(userInput.equals("singular") && userInput.equals("s"))
			number = Number.SINGULAR;
		else
			number = Number.PLURAL;

		terminal.writer().println(new AttributedString("Specify the tense among the available:", BOLD_BLUE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Past", BOLD_WHITE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Present", BOLD_WHITE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Future", BOLD_WHITE_STYLE).toAnsi(terminal));

		userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();

		for(int i = 0; i <= MAX_ATTEMPTS; ++i)
		{
			if(i == MAX_ATTEMPTS)
				return;

			if(userInput.isEmpty() || (!userInput.equals("past") && !userInput.equals("pa") && !userInput.equals("present") && !userInput.equals("pr") && !userInput.equals("future") && !userInput.equals("f")))
			{
				terminal.writer().println(new AttributedString("Please enter a valid option. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
				terminal.flush();
				userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
			}
			else
				break;
		}

		Tense tense;
		if(userInput.equals("past") || userInput.equals("pa"))
			tense = Tense.PAST;
		else if(userInput.equals("present") || userInput.equals("pr"))
			tense = Tense.PRESENT;
		else
			tense = Tense.FUTURE;

		String generated = processor.generateWithBoth(number, tense);
		terminal.writer().println(new AttributedString("Generated sentence: " + generated, GREEN_STYLE).toAnsi(terminal));
	}

	private void analyzeHandler()
	{
		terminal.writer().println(new AttributedString("Proceeding to analyze select one of the following options:", BOLD_BLUE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Random", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("      Performs one random of the available options", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    All", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("      Performs all the analysis available", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Syntax", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("      Performs the syntactic analysis", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Sentiment", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("      Performs the sentiment analysis", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Toxicity", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("      Performs the toxicity analysis", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("    Combined", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("      Allows to choose a combination of the options", DEFAULT_STYLE).toAnsi(terminal));
		terminal.flush();

		String analysis = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();

		for(int i = MAX_ATTEMPTS; i >= 0; --i)
		{
			if(i == 0)
				return;

			if(analysis.isEmpty() || !analyzeOptions.containsKey(analysis))
			{
				terminal.writer().println(new AttributedString("Please enter a valid option. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
				terminal.flush();
				analysis = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
			}
			else
				break;
		}

		terminal.writer().println(new AttributedString("Select if do you want to analyze a generate sentence or input a new one", BOLD_BLUE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("(Generate/Input)", DEFAULT_STYLE).toAnsi(terminal));

		String mode = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();

		for(int i = MAX_ATTEMPTS; i >= 0; --i)
		{
			if(i == 0)
				return;

			if(mode.isEmpty() || (!mode.equals("generate") && !mode.equals("g") && !mode.equals("input") && !mode.equals("i")))
			{
				terminal.writer().println(new AttributedString("Please enter a valid option. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
				terminal.flush();
				mode = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
			}
			else
				break;
		}

		String userInput = new String();

		if(mode.equals("generate") || mode.equals("g"))
		{
			userInput = processor.generateRandom();
			terminal.writer().println(new AttributedString("Generated sentence: " + userInput, GREEN_STYLE).toAnsi(terminal));
		}
		else if(mode.equals("input") || mode.equals("i"))
		{
			userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim();

			for(int i = MAX_ATTEMPTS; i >= 0; --i)
			{
				if(i == 0)
					return;

				if(userInput.isEmpty())
				{
					terminal.writer().println(new AttributedString("Please enter a valid Sentence. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
					terminal.flush();
					userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim();
				}
				else
					break;
			}
		}

		switch(analyzeOptions.get(analysis))
		{
			case RANDOM: analyzeRandom(userInput); break;
			case ALL: analyzeAll(userInput); break;
			case SYNTAX: analyzeSyntax(userInput); break;
			case SENTIMENT: analyzeSentiment(userInput); break;
			case TOXICITY: analyzeToxicity(userInput); break;
			case COMBINED: analyzeCombined(userInput); break;
		}
	}

	private void analyzeRandom(String input)
	{
		Random random = new Random();

		AnalyzeOptions[] opts = { AnalyzeOptions.SYNTAX, AnalyzeOptions.SENTIMENT, AnalyzeOptions.TOXICITY };

		AnalyzeOptions opt = opts[random.nextInt(opts.length)];

		terminal.writer().println(new AttributedString("Random analysis chosen is " + opt, DEFAULT_STYLE).toAnsi(terminal));

		switch(opt)
		{
			case SYNTAX: analyzeSyntax(input); break;
			case SENTIMENT: analyzeSentiment(input); break;
			case TOXICITY: analyzeToxicity(input); break;
		}
	}

	private void analyzeAll(String input)
	{
		terminal.writer().println(new AttributedString("Proceding analyzing all the analysis", DEFAULT_STYLE).toAnsi(terminal));

		analyzeSyntax(input);
		analyzeSentiment(input);
		analyzeToxicity(input);
	}

	private void analyzeSyntax(String input)
	{
		terminal.writer().println(new AttributedString("Syntax analysis: ", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString(processor.analyzeSyntax(input), DEFAULT_STYLE).toAnsi(terminal));
	}

	private void analyzeSentiment(String input)
	{
		terminal.writer().println(new AttributedString("Sentiment analysis: ", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString(processor.analyzeSentiment(input), DEFAULT_STYLE).toAnsi(terminal));
	}

	private void analyzeToxicity(String input)
	{
		terminal.writer().println(new AttributedString("Toxicity analysis: ", DEFAULT_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString(processor.analyzeToxicity(input), DEFAULT_STYLE).toAnsi(terminal));
	}

	private void analyzeCombined(String input)
	{
		terminal.writer().println(new AttributedString("Select the desired analysis (press enter to confirm the combination choice):", BOLD_BLUE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("Syntax, sentiment, toxicity", DEFAULT_STYLE).toAnsi(terminal));

		String mode = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
		List<AnalyzeOptions> opts = new ArrayList<>();

		for(int i = MAX_ATTEMPTS; i >= 0; --i)
		{
			if(i == 0)
				return;

			if(mode.isEmpty())
				break;
			else if((!mode.equals("syntax") && !mode.equals("sy") && !mode.equals("sentiment") && !mode.equals("se") && !mode.equals("toxicity") && !mode.equals("t")))
			{
				terminal.writer().println(new AttributedString("Please enter a valid option. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
				terminal.flush();
				mode = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
			}
			else if(mode.equals("syntax") || mode.equals("sy"))
				opts.add(AnalyzeOptions.SYNTAX);
			else if(mode.equals("sentiment") || mode.equals("se"))
				opts.add(AnalyzeOptions.SENTIMENT);
			else if(mode.equals("toxicity") || mode.equals("t"))
				opts.add(AnalyzeOptions.TOXICITY);
		}

		for(AnalyzeOptions opt : opts)
		{
			terminal.writer().println(new AttributedString("Proceeding with " + opt, BOLD_BLUE_STYLE).toAnsi(terminal));

			switch(opt)
			{
				case SYNTAX: analyzeSyntax(input); break;
				case SENTIMENT: analyzeSentiment(input); break;
				case TOXICITY: analyzeToxicity(input); break;
			}
		}
	}

	private void treeHandler() throws IOException
	{
		terminal.writer().println(new AttributedString("Select if do you want to print the syntactic tree of a generate sentence or input a new one", BOLD_BLUE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("(Generate/Input)", DEFAULT_STYLE).toAnsi(terminal));

		String mode = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();

		for(int i = MAX_ATTEMPTS; i >= 0; --i)
		{
			if(i == 0)
				return;

			if(mode.isEmpty() || (!mode.equals("generate") && !mode.equals("g") && !mode.equals("input") && !mode.equals("i")))
			{
				terminal.writer().println(new AttributedString("Please enter a valid option. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
				terminal.flush();
				mode = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
			}
			else
				break;
		}

		String userInput = new String();

		if(mode.equals("generate") || mode.equals("g"))
		{
			userInput = processor.generateRandom();
			terminal.writer().println(new AttributedString("Generated sentence: " + userInput, GREEN_STYLE).toAnsi(terminal));
		}
		else if(mode.equals("input") || mode.equals("i"))
		{
			userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim();

			for(int i = MAX_ATTEMPTS; i >= 0; --i)
			{
				if(i == 0)
					return;

				if(userInput.isEmpty())
				{
					terminal.writer().println(new AttributedString("Please enter a valid Sentence. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
					terminal.flush();
					userInput = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim();
				}
				else
					break;
			}
		}

		terminal.writer().println(new AttributedString("Proceeding printing the syntactic tree of the chosen sentence", BOLD_BLUE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString(processor.generateSyntaxTree(userInput), DEFAULT_STYLE).toAnsi(terminal));
	}

	private void extendHandler() throws IOException
	{
		terminal.writer().println(new AttributedString("Enter the part of speech taht you want to add (press Enter to confirm the new terms):", BOLD_BLUE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("Noun, adjective or verb", BOLD_BLUE_STYLE).toAnsi(terminal));

		String partOfSpeech = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().toLowerCase();

		List<Noun> nounList = new ArrayList<>();
		List<Adjective> adjectiveList = new ArrayList<>();
		List<Verb> verbList = new ArrayList<>();

		while(!partOfSpeech.isEmpty())
		{
			for(int i = MAX_ATTEMPTS; i >= 0; --i)
			{
				if(i == 0)
					return;

				if((!partOfSpeech.equals("noun") && !partOfSpeech.equals("n") && !partOfSpeech.equals("adjective") && !partOfSpeech.equals("a") && !partOfSpeech.equals("verb") && !partOfSpeech.equals("v")))
				{
					terminal.writer().println(new AttributedString("Please enter a valid value. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
					terminal.flush();
					partOfSpeech = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().toLowerCase();
				}
				else
					break;
			}

			if(partOfSpeech.equals("noun") || partOfSpeech.equals("n"))
			{
				terminal.writer().println(new AttributedString("Enter the number for the noun: ", BOLD_BLUE_STYLE).toAnsi(terminal));
				terminal.writer().println(new AttributedString("Singular or plural ", BOLD_BLUE_STYLE).toAnsi(terminal));
				String num = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().toLowerCase();

				for(int i = MAX_ATTEMPTS; i >= 0; --i)
				{
					if(i == 0)
						return;

					if(num.isEmpty() || (!num.equals("singular") && !num.equals("s") && !num.equals("plural") && !num.equals("p")))
					{
						terminal.writer().println(new AttributedString("Please enter a valid value. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
						terminal.flush();
						num = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
					}
					else
						break;
				}

				Number number;
				if(num.equals("singular") || num.equals("s"))
					number = Number.SINGULAR;
				else
					number = Number.PLURAL;

				terminal.writer().println(new AttributedString("Insert the new noun:", BOLD_BLUE_STYLE).toAnsi(terminal));
				String text = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().toLowerCase();

				nounList.add(new Noun(text, number));
			}
			else if(partOfSpeech.equals("adjective") || partOfSpeech.equals("a"))
			{
				terminal.writer().println(new AttributedString("Insert the new adjective:", BOLD_BLUE_STYLE).toAnsi(terminal));
				String text = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().toLowerCase();

				adjectiveList.add(new Adjective(text));
			}
			else if(partOfSpeech.equals("verb") || partOfSpeech.equals("v"))
			{
				terminal.writer().println(new AttributedString("Enter the tense for the verb: ", BOLD_BLUE_STYLE).toAnsi(terminal));
				terminal.writer().println(new AttributedString("past, present or future ", BOLD_BLUE_STYLE).toAnsi(terminal));
				String textTense = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().toLowerCase();

				for(int i = MAX_ATTEMPTS; i >= 0; --i)
				{
					if(i == 0)
						return;

					if(textTense.isEmpty() || (!textTense.equals("present") && !textTense.equals("pr") && !textTense.equals("past") && !textTense.equals("pa") && !textTense.equals("future") && !textTense.equals("f")))
					{
						terminal.writer().println(new AttributedString("Please enter a valid tense. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
						terminal.flush();
						textTense = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
					}
					else
						break;
				}

				Tense tense;
				if(textTense.equals("past") || textTense.equals("pa"))
					tense = Tense.PAST;
				else if(textTense.equals("present") || textTense.equals("pr"))
					tense = Tense.PRESENT;
				else
					tense = Tense.FUTURE;

				terminal.writer().println(new AttributedString("Insert the new verb:", BOLD_BLUE_STYLE).toAnsi(terminal));
				String text = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().toLowerCase();

				verbList.add(new Verb(text, tense));
			}
		}

		terminal.writer().println(new AttributedString("Adding the new elements", BOLD_BLUE_STYLE).toAnsi(terminal));
		processor.append(nounList, adjectiveList, verbList);
	}

	private void setToleranceHandler()
	{
		terminal.writer().println(new AttributedString("Enter tolerance value (0.0-1.0): ", BOLD_WHITE_STYLE).toAnsi(terminal));
		terminal.writer().println(new AttributedString("Current tolerance value is " + processor.getTolerance(), BOLD_WHITE_STYLE).toAnsi(terminal));

		String newTolerance = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();

		for(int i = MAX_ATTEMPTS; i >= 0; --i)
		{
			if(i == 0)
				return;

			try
			{
				if(newTolerance.isEmpty() || ((Float.parseFloat(newTolerance) < 0.0f && (Float.parseFloat(newTolerance) > 1.0f))))
				{
					terminal.writer().println(new AttributedString("Please enter a valid value. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
					terminal.flush();
					newTolerance = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
				}
				else
					break;
			}
			catch(NumberFormatException e)
			{
				terminal.writer().println(new AttributedString("Please enter a valid value. Remaining attempts " + i, BOLD_YELLOW_STYLE).toAnsi(terminal));
				terminal.flush();
				newTolerance = reader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
			}
		}

		float tolerance = Float.parseFloat(newTolerance);
		processor.setTolerance(tolerance);
	}

	private void quit()
	{
		terminal.writer().println(new AttributedString("Exiting.", DEFAULT_STYLE).toAnsi(terminal));
		terminal.flush();
		running = false;
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
