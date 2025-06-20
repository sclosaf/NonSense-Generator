package unipd.nonsense.util;

import unipd.nonsense.util.CommandProcessor;
import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Tense;
import unipd.nonsense.model.Template;

import unipd.nonsense.exceptions.MissingInternetConnectionException;
import unipd.nonsense.exceptions.IllegalToleranceException;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Random;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.EndOfFileException;
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
	public static enum Command
	{
		DEFAULT, PERSONALIZED, GENERATE, ANALYZE, TREE, EXTEND, SETTOLERANCE, INFO, VERBOSE, HELP, CLEAR, QUIT
	}

	public static enum GenerateOptions
	{
		RANDOM, NUMBER, TENSE, BOTH
	}

	public static enum AnalyzeOptions
	{
		RANDOM, ALL, SYNTAX, SENTIMENT, TOXICITY, ENTITY, COMBINED
	}

	public static class Option
	{
		private final String mainCommand;
		private final String description;
		private final String alias;

		public Option(String mainCommand, String description, String alias)
		{
			this.mainCommand = mainCommand;
			this.description = description;
			this.alias = alias;
		}

		public String getMainCommand()
		{
			return mainCommand;
		}

		public String getDescription()
		{
			return description;
		}

		public String getAlias()
		{
			return alias;
		}

		public boolean matches(String input)
		{
			if(input == null || input.trim().isEmpty())
				return false;

			return mainCommand.equals(input.toLowerCase()) || alias.equals(input.toLowerCase());
		}

		public String getDisplayName()
		{
			return mainCommand.substring(0, 1).toUpperCase() + mainCommand.substring(1).toLowerCase();
		}
	}

	private static final AttributedStyle RED_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
	private static final AttributedStyle BLUE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);
	private static final AttributedStyle GREEN_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
	private static final AttributedStyle YELLOW_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
	private static final AttributedStyle MAGENTA_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
	private static final AttributedStyle WHITE_STYLE = AttributedStyle.DEFAULT;

	private static final AttributedStyle BOLD_RED_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED);
	private static final AttributedStyle BOLD_BLUE_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE);
	private static final AttributedStyle BOLD_GREEN_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN);
	private static final AttributedStyle BOLD_YELLOW_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW);
	private static final AttributedStyle BOLD_MAGENTA_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.MAGENTA);
	private static final AttributedStyle BOLD_WHITE_STYLE = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.WHITE);

	private static final List<Option> INPUT_MODE_OPTIONS = Arrays.asList
		(
			new Option("generate", "Generate a random sentence", "g"),
			new Option("input", "Enter a sentence manually", "i"),
			new Option("cached", "Use the cached sentence", "ca"),
			new Option("choose", "Choose among 5 given templates", "ch")
	);

	private static final List<Option> ELEMENT_OPTIONS = Arrays.asList
		(
			new Option("noun", "Enter a noun", "n"),
			new Option("adjective", "Enter an adjective", "a"),
			new Option("verb", "Enter a verb", "v")
		);

	private static final List<Option> GENERATION_MODE_OPTIONS = Arrays.asList
		(
			new Option("random", "Number and tense used in the generated sentence are selected randomly", "r"),
			new Option("number", "User can choose the number used in the generated sentence (tense is random)", "n"),
			new Option("tense", "User can choose the tense used in the generated sentence (number is random)", "t"),
			new Option("both", "User can choose both the tense and the number", "b")
		);

	private static final List<Option> ANALYZE_MODE_OPTIONS = Arrays.asList
		(
			new Option("random", "Performs one random option", "r"),
			new Option("all", "Performs all the analysis available", "a"),
			new Option("syntax", "Performs syntactic analysis", "sy"),
			new Option("sentiment", "Performs sentiment analysis", "se"),
			new Option("toxicity", "Performs toxicity analysis", "t"),
			new Option("entity", "Performs entity analysis", "e"),
			new Option("combined", "Allows to choose a combination of options", "c")
		);

	private static final List<Option> COMBINED_ANALYZE_MODE_OPTIONS = Arrays.asList
		(
			new Option("syntax", "Performs syntactic analysis", "sy"),
			new Option("sentiment", "Performs sentiment analysis", "se"),
			new Option("toxicity", "Performs toxicity analysis", "t"),
			new Option("entity", "Performs entity analysis", "e")
		);

	private static final List<Option> NUMBER_OPTIONS = Arrays.asList
		(
			new Option("singular", "Use singular nouns", "s"),
			new Option("plural", "Use plural nouns", "p")
		);

	private static final List<Option> TENSE_OPTIONS = Arrays.asList
		(
			new Option("past", "Use simple past tense", "pa"),
			new Option("present", "Use simple present tense", "pr"),
			new Option("future", "Use will future tense", "f")
		);

	private final Map<String, Command> commands = new HashMap<>();
	private final Map<String, GenerateOptions> generateOptions = new HashMap<>();
	private final Map<String, AnalyzeOptions> analyzeOptions = new HashMap<>();

	private static final int HISTORY_SIZE = 10;
	private static final int MAX_ATTEMPTS = 4;
	private static final int MAX_WIDTH = 106;

	private boolean running;
	private final String initialOutput;

	private final LoggerManager logger = new LoggerManager(CLI.class);
	private final CommandProcessor processor;
	private final Terminal terminal;

	private LineReader commandReader;
	private LineReader plainReader;

	private class CommandHighlighter implements Highlighter
	{
		@Override
		public AttributedString highlight(LineReader reader, String buffer)
		{

			String trimmedBuffer = buffer.trim().toLowerCase();

			if(trimmedBuffer.isEmpty())
				return new AttributedString(buffer, WHITE_STYLE);

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
		logger.logTrace("Initializing CLI");

		try
		{
			terminal = TerminalBuilder.builder().system(true).build();
			logger.logTrace("Terminal initialized successfully");

			commandReader = LineReaderBuilder.builder()
				.terminal(terminal)
				.completer(new StringsCompleter(commands.keySet()))
				.option(LineReader.Option.HISTORY_BEEP, false)
				.option(LineReader.Option.AUTO_LIST, true)
				.option(LineReader.Option.AUTO_FRESH_LINE, true)
				.variable(LineReader.HISTORY_SIZE, HISTORY_SIZE)
				.highlighter(new CommandHighlighter())
				.build();
			logger.logDebug("Command reader initialized with history size: " + HISTORY_SIZE);

			plainReader = LineReaderBuilder.builder()
				.terminal(terminal)
				.option(LineReader.Option.HISTORY_BEEP, false)
				.option(LineReader.Option.AUTO_FRESH_LINE, true)
				.variable(LineReader.HISTORY_SIZE, HISTORY_SIZE)
				.build();
			logger.logDebug("Plain reader initialized with history size: " + HISTORY_SIZE);

			this.processor = new CommandProcessor();
			running = true;
			logger.logTrace("CommandProcessor initialized and running set to " + running);

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
			commands.put("extend", Command.EXTEND);
			commands.put("e", Command.EXTEND);
			commands.put("set tolerance", Command.SETTOLERANCE);
			commands.put("st", Command.SETTOLERANCE);
			commands.put("help", Command.HELP);
			commands.put("h", Command.HELP);
			commands.put("info", Command.INFO);
			commands.put("i", Command.INFO);
			commands.put("verbose", Command.VERBOSE);
			commands.put("v", Command.VERBOSE);
			commands.put("clear", Command.CLEAR);
			commands.put("c", Command.CLEAR);
			commands.put("quit", Command.QUIT);
			commands.put("q", Command.QUIT);
			logger.logDebug("Commands map initialized with " + commands.size() + " entries");

			generateOptions.put("random", GenerateOptions.RANDOM);
			generateOptions.put("r", GenerateOptions.RANDOM);
			generateOptions.put("number", GenerateOptions.NUMBER);
			generateOptions.put("n", GenerateOptions.NUMBER);
			generateOptions.put("tense", GenerateOptions.TENSE);
			generateOptions.put("t", GenerateOptions.TENSE);
			generateOptions.put("both", GenerateOptions.BOTH);
			generateOptions.put("b", GenerateOptions.BOTH);
			logger.logDebug("Generate options map initialized with " + generateOptions.size() + " entries");

			analyzeOptions.put("random", AnalyzeOptions.RANDOM);
			analyzeOptions.put("r", AnalyzeOptions.RANDOM);
			analyzeOptions.put("all", AnalyzeOptions.ALL);
			analyzeOptions.put("a", AnalyzeOptions.ALL);
			analyzeOptions.put("syntax", AnalyzeOptions.SYNTAX);
			analyzeOptions.put("sy", AnalyzeOptions.SYNTAX);
			analyzeOptions.put("sentiment", AnalyzeOptions.SENTIMENT);
			analyzeOptions.put("se", AnalyzeOptions.SENTIMENT);
			analyzeOptions.put("toxicity", AnalyzeOptions.TOXICITY);
			analyzeOptions.put("t", AnalyzeOptions.TOXICITY);
			analyzeOptions.put("entity", AnalyzeOptions.ENTITY);
			analyzeOptions.put("e", AnalyzeOptions.ENTITY);
			analyzeOptions.put("combined", AnalyzeOptions.COMBINED);
			analyzeOptions.put("c", AnalyzeOptions.COMBINED);
			logger.logDebug("Analyze options map initialized with " + analyzeOptions.size() + " entries");

			StringWriter stringWriter = new StringWriter();
			PrintWriter tempWriter = new PrintWriter(stringWriter);

			welcome(tempWriter);
			usage(tempWriter);
			tempWriter.flush();

			initialOutput = stringWriter.toString();

			logger.logTrace("Initial output prepared for welcome and usage messages");

			welcome(terminal.writer());

			logger.logTrace("Welcome message displayed");

			if(!checkInternetConnection())
			{
				logger.logError("No internet connection detected", new MissingInternetConnectionException());
				throw new MissingInternetConnectionException();
			}
			else
				logger.logTrace("Device connected to internet");

			usage(terminal.writer());
			logger.logTrace("Usage message displayed");
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize CLI", e);
			throw e;
		}
	}

	private synchronized void welcome(PrintWriter writer) throws IOException
	{
		logger.logTrace("welcome: Starting welcome message display");

		String title = "Welcome to";
		logger.logDebug("welcome: Title set to: " + title);

		int totalPadding = MAX_WIDTH - title.length() - 4;
		int leftPadding = totalPadding / 2;
		int rightPadding = totalPadding - leftPadding;

		String topBorder = "=".repeat(leftPadding) + "< " + title + " >" + "=".repeat(rightPadding);
		logger.logDebug("welcome: Top border constructed with length: " + topBorder.length());

		writer.println(new AttributedString(topBorder, BOLD_WHITE_STYLE).toAnsi(terminal));
		logger.logTrace("welcome: Top border printed");

		InputStream stream = getClass().getResourceAsStream("/asciiArt.txt");

		if(stream == null)
		{
			logger.logError("welcome: ASCII art file not found");
			writer.println(new AttributedString("ASCII art not available", RED_STYLE).toAnsi(terminal));
			writer.println(new AttributedString("=".repeat(MAX_WIDTH), BOLD_WHITE_STYLE).toAnsi(terminal));
			writer.flush();
			return;
		}

		logger.logTrace("welcome: ASCII art file loaded successfully");

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream)))
		{
			String line;

			while((line = reader.readLine()) != null)
				writer.println(new AttributedString(line, BOLD_WHITE_STYLE).toAnsi(terminal));

			writer.println(new AttributedString("=".repeat(MAX_WIDTH), BOLD_WHITE_STYLE).toAnsi(terminal));
			writer.flush();
		}
		catch(IOException e)
		{
			logger.logError("welcome: Error reading ASCII art file", e);
			writer.println(new AttributedString("Error reading ASCII art", RED_STYLE).toAnsi(terminal));
			writer.println(new AttributedString("=".repeat(MAX_WIDTH), BOLD_WHITE_STYLE).toAnsi(terminal));
			writer.flush();
			return;
		}

		logger.logTrace("welcome: ASCII art printed");
	}

	public synchronized boolean inputCatcher()
	{
		logger.logTrace("inputCatcher: Starting command input capture");

		try
		{
			printWhite("Enter a command or type 'help'", true);
			logger.logTrace("inputCatcher: Prompt for command displayed");

			String cmd = commandReader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();

			logger.logDebug("inputCatcher: Received command " + (cmd.isEmpty() ? " NONE" : cmd));

			if(!cmd.isEmpty() && !commands.containsKey(cmd))
			{
				printRed("Invalid command: " + cmd, true);
				printRed("Type 'help' for available commands.", true);

				logger.logWarn("inputCatcher: Invalid command entered: " + cmd);
				return running;
			}
			else
			{
				commandExecuter(cmd);
				logger.logDebug("inputCatcher: Command passed to commandExecuter: " + cmd);
			}
		}
		catch(UserInterruptException | EndOfFileException e)
		{
			printYellow("Program ended.", true);
			logger.logWarn("inputCatcher: Program interrupted by user", e);
			running = false;
		}
		catch(Exception e)
		{
			printRed("Unexpected error: " + e.getMessage(), true);
			logger.logError("inputCatcher: Unexpected error during command capture", e);
			running = false;
		}

		logger.logDebug("inputCatcher: Completed with running status: " + running);
		return running;
	}

	private synchronized void commandExecuter(String cmd) throws IOException
	{
		logger.logTrace("commandExecuter: Starting command execution");

		if(cmd.isEmpty())
		{
			printYellow("Please enter a command.", true);

			logger.logWarn("commandExecuter: Empty command received");
			logger.logTrace("commandExecuter: Completed due to empty command");

			return;
		}

		logger.logDebug("commandExecuter: Processing command: " + cmd);
		logger.logDebug("commandExecuter: Matched command enum: " + commands.get(cmd));

		switch(commands.get(cmd))
		{
			case DEFAULT: defaultHandler(); break;
			case PERSONALIZED: personalizedHandler(); break;
			case GENERATE: generateHandler(); break;
			case ANALYZE: analyzeHandler(); break;
			case TREE: treeHandler(); break;
			case EXTEND: extendHandler(); break;
			case SETTOLERANCE: setToleranceHandler(); break;
			case HELP: usage(terminal.writer()); break;
			case INFO: extendedUsage(); break;
			case VERBOSE: verboseHandler(); break;
			case CLEAR: clearTerminal(); break;
			case QUIT: quit(); break;
		}

		if(commands.get(cmd) != Command.HELP && commands.get(cmd) != Command.CLEAR && commands.get(cmd) != Command.QUIT)
			printSeparator(BOLD_BLUE_STYLE);

		logger.logDebug("commandExecuter: Completed successfully for command: " + cmd);
	}

	private synchronized void defaultHandler() throws IOException
	{
		logger.logTrace("defaultHandler: Starting default procedure");

		printTitleSeparator("Default procedure", BOLD_BLUE_STYLE);

		printWhite("Proceeding with the default process.", true);
		printWhite("Enter a sentence to analyze (or press Enter to generate one automatically and skip the input elabotation):", true);

		logger.logTrace("defaultHandler: Prompt for sentence input displayed");
		String userInput = read(false);
		logger.logDebug("defaultHandler: Received user input: " + (userInput.isEmpty() ? "NONE" : userInput));

		if(userInput.isEmpty())
		{
			printWhite("Sentence input skipped, proceeding by generating one from scratch", true);
			userInput = processor.generateRandom();
			printWhite("Generated sentence: '" + userInput + "'", true);
			logger.logDebug("defaultHandler: Generated random sentence: " + userInput);

			printWhite("Proceeding with a standard analysis of the generated sentence", true);
			printWhite("This passage includes syntactic and toxicity analysis of the generated sentence", true);
			printWhite("Followed by the generation of the syntactic tree of the generated sentence", true);
			logger.logTrace("defaultHandler: Starting analysis of generated sentence");

			analyzeSyntax(userInput);
			logger.logTrace("defaultHandler: Completed syntactic analysis for generated sentence");

			analyzeToxicity(userInput);
			logger.logTrace("defaultHandler: Completed toxicity analysis for generated sentence");

			printTree(userInput);
			logger.logTrace("defaultHandler: Completed syntactic tree generation for generated sentence");
		}
		else
		{
			printWhite("Proceeding with the standard synctactic analysis of the given sentence", true);
			logger.logTrace("defaultHandler: Starting analysis of user-provided sentence");
			analyzeSyntax(userInput);
			logger.logTrace("defaultHandler: Completed syntactic analysis for user-provided sentence");

			printSeparator(BOLD_BLUE_STYLE);
			printWhite("Generating a new sentence by using nouns, adjectives and verbs of the inserted one", true);
			String generated = processor.generateFrom(userInput);

			printWhite("Generated sentence: '" + generated + "'", true);
			logger.logDebug("defaultHandler: Generated sentence from user input: " + generated);

			printWhite("Proceeding with a standard analysis of the generated sentence", true);
			printWhite("This passage includes the syntactic and toxicity analysis of the generated sentence", true);
			printWhite("Followed by the creation of its synctactic tree", true);
			logger.logTrace("defaultHandler: Starting analysis of sentence generated from user input");

			analyzeSyntax(generated);
			logger.logTrace("defaultHandler: Completed syntactic analysis for sentence generated from user input");

			analyzeToxicity(generated);
			logger.logTrace("defaultHandler: Completed toxicity analysis for sentence generated from user input");
			printTree(generated);
			logger.logTrace("defaultHandler: Completed syntactic tree generation for sentence generated from user input");
		}

		logger.logTrace("defaultHandler: Completed default procedure successfully");
	}

	private synchronized void personalizedHandler() throws IOException
	{
		logger.logTrace("personalizedHandler: Starting personalized procedure");

		printTitleSeparator("Personalized procedure", BOLD_BLUE_STYLE);
		printWhite("Proceeding with the personalized process.", true);
		printWhite("This procedure lets the user go through several options that this program offers", true);

		logger.logTrace("personalizedHandler: Displayed procedure introduction");

		printWhite("As an option the user can add new nouns, adjectives and verbs to the dictionaries", true);
		logger.logTrace("personalizedHandler: Prompting for dictionary extension");
		extendHandler();
		logger.logTrace("personalizedHandler: Completed dictionary extension");

		printWhite("Enter a sentence to process (or press Enter to generate one automatically)", true);

		String userInput = read(false);
		logger.logDebug("personalizedHandler: Received user input: " + (userInput.isEmpty() ? "NONE" : userInput));

		if(userInput.isEmpty())
		{
			printWhite("Generation process chosen", true);
			logger.logTrace("personalizedHandler: User chose to generate a sentence");
			generateHandler();
			logger.logTrace("personalizedHandler: Completed sentence generation");
		}
		else
			logger.logDebug("personalizedHandler: User provided sentence for processing: " + userInput);

		printWhite("Proceeding with the analysis", true);

		logger.logTrace("personalizedHandler: Starting sentence analysis");
		analyzeHandler();
		logger.logTrace("personalizedHandler: Completed sentence analysis");

		printWhite("Proceeding by building the synctactic tree", true);
		logger.logTrace("personalizedHandler: Starting syntactic tree generation");
		treeHandler();

		logger.logTrace("personalizedHandler: Completed syntactic tree generation");
		logger.logTrace("personalizedHandler: Completed personalized procedure successfully");
	}

	private synchronized void generateHandler() throws IOException
	{
		logger.logTrace("generateHandler: Starting sentence generation procedure");
		printTitleSeparator("Generation procedure", BOLD_BLUE_STYLE);
		String userInput = validateInput("Proceeding to generate a random sentence, select one of the following options:", GENERATION_MODE_OPTIONS, false);

		logger.logDebug("generateHandler: Selected generation option: " + (userInput.isEmpty() ? "CANCELLED" : userInput));

		if(userInput.isEmpty())
		{
			logger.logWarn("generateHandler: Generation cancelled due to invalid input");
			logger.logTrace("generateHandler: Completed due to cancellation");
			return;
		}

		switch(generateOptions.get(userInput))
		{
			case RANDOM: generateRandom(); break;
			case NUMBER: generateNumber(); break;
			case TENSE: generateTense(); break;
			case BOTH: generateBoth(); break;
		}

		logger.logTrace("generateHandler: Completed sentence generation procedure successfully");
	}

	private synchronized void generateRandom()
	{
		logger.logTrace("generateRandom: Starting random sentence generation");
		printTitleSeparator("Random generation", BOLD_BLUE_STYLE);

		String sentence = processor.generateRandom();
		logger.logDebug("generateRandom: Generated sentence: " + sentence);

		printWhite("Generated sentence: '" + sentence + "'", true);
		logger.logTrace("generateRandom: Printed generated sentence");

		logger.logTrace("generateRandom: Completed random sentence generation successfully");
	}

	private synchronized void generateNumber() throws IOException
	{
		logger.logTrace("generateNumber: Starting sentence generation with specific number");

		printTitleSeparator("Random generation (with number)", BOLD_BLUE_STYLE);
		String userInput = validateInput("Specify the desired number among the available:", NUMBER_OPTIONS, false);
		logger.logTrace("generateNumber: Prompt for number input displayed");
		logger.logDebug("generateNumber: Received number input: " + (userInput.isEmpty() ? "NONE" : userInput));

		if(userInput.isEmpty())
		{
			logger.logWarn("generateNumber: Generation cancelled due to invalid input");
			logger.logTrace("generateNumber: Completed due to cancellation");
			return;
		}

		if(userInput.equals("singular") || userInput.equals("s"))
		{
			logger.logTrace("generateNumber: Selected number: singular");
			printWhite("Proceeding to generate a sentence with singular nouns", true);

			String sentence = processor.generateWithNumber(Number.SINGULAR);
			logger.logDebug("generateNumber: Generated sentence: " + sentence);

			printWhite("Generated sentence: '" + sentence + "'", true);
			logger.logTrace("generateNumber: Printed generated sentence");
		}
		else if(userInput.equals("plural") || userInput.equals("p"))
		{
			logger.logTrace("generateNumber: Selected number: plural");
			printWhite("Proceeding to generate a sentence with plural nouns", true);

			String sentence = processor.generateWithNumber(Number.PLURAL);
			logger.logDebug("generateNumber: Generated sentence: " + sentence);

			printWhite("Generated sentence: '" + sentence + "'", true);
			logger.logTrace("generateNumber: Printed generated sentence");
		}

		logger.logTrace("generateNumber: Completed sentence generation successfully");
	}

	private synchronized void generateTense() throws IOException
	{
		logger.logTrace("generateTense: Starting sentence generation with specific tense");
		printTitleSeparator("Random generation (with tense)", BOLD_BLUE_STYLE);

		String userInput = validateInput("Specify the desired tense among the available:", TENSE_OPTIONS, false);

		logger.logTrace("generateTense: Prompt for tense input displayed");
		logger.logDebug("generateTense: Received tense input: " + (userInput.isEmpty() ? "NONE" : userInput));

		if(userInput.isEmpty())
		{
			logger.logWarn("generateTense: Generation cancelled due to invalid input");
			logger.logTrace("generateTense: Completed due to cancellation");
			return;
		}

		if(userInput.equals("past") || userInput.equals("pa"))
		{
			logger.logTrace("generateTense: Selected tense: past");
			printWhite("Proceeding to generate a sentence with past tense", true);

			String sentence = processor.generateWithTense(Tense.PAST);
			logger.logDebug("generateTense: Generated sentence: " + sentence);

			printWhite("Generated sentence: '" + sentence + "'", true);
			logger.logTrace("generateTense: Printed generated sentence");
		}
		else if(userInput.equals("present") || userInput.equals("pr"))
		{
			logger.logTrace("generateTense: Selected tense: present");
			printWhite("Proceeding to generate a sentence with present tense", true);

			String sentence = processor.generateWithTense(Tense.PRESENT);
			logger.logDebug("generateTense: Generated sentence: " + sentence);

			printWhite("Generated sentence: '" + sentence + "'", true);
			logger.logTrace("generateTense: Printed generated sentence");
		}
		else if(userInput.equals("future") || userInput.equals("f"))
		{
			logger.logTrace("generateTense: Selected tense: future");
			printWhite("Proceeding to generate a sentence with future tense", true);

			String sentence = processor.generateWithTense(Tense.FUTURE);
			logger.logDebug("generateTense: Generated sentence: " + sentence);

			printWhite("Generated sentence: '" + sentence + "'", true);
			logger.logTrace("generateTense: Printed generated sentence");
		}

		logger.logTrace("generateTense: Completed sentence generation successfully");
	}

	private synchronized void generateBoth() throws IOException
	{
		logger.logTrace("generateBoth: Starting sentence generation with specific number and tense");
		printTitleSeparator("Random generation (with number and tense)", BOLD_BLUE_STYLE);

		String userInput = validateInput("Specify the desired number among the available:", NUMBER_OPTIONS, false);

		logger.logTrace("generateBoth: Prompt for number input displayed");
		logger.logDebug("generateBoth: Received number input: " + (userInput.isEmpty() ? "NONE" : userInput));

		if(userInput.isEmpty())
		{
			logger.logWarn("generateBoth: Generation cancelled due to invalid input");
			logger.logTrace("generateBoth: Completed due to cancellation");
			return;
		}

		Number number;

		if(userInput.equals("singular") || userInput.equals("s"))
		{
			number = Number.SINGULAR;
			logger.logTrace("generateBoth: Selected number: singular");
		}
		else
		{
			number = Number.PLURAL;
			logger.logTrace("generateBoth: Selected number: plural");
		}

		userInput = validateInput("Specify the desired tense among the available:", TENSE_OPTIONS, false);

		logger.logTrace("generateBoth: Prompt for tense input displayed");
		logger.logDebug("generateBoth: Received tense input: " + (userInput.isEmpty() ? "NONE" : userInput));


		if(userInput.isEmpty())
		{
			logger.logWarn("generateBoth: Generation cancelled due to invalid input");
			logger.logTrace("generateBoth: Completed due to cancellation");
			return;
		}

		Tense tense;

		if(userInput.equals("past") || userInput.equals("pa"))
		{
			tense = Tense.PAST;
			logger.logTrace("generateBoth: Selected tense: past");
		}
		else if(userInput.equals("present") || userInput.equals("pr"))
		{
			tense = Tense.PRESENT;
			logger.logTrace("generateBoth: Selected tense: present");
		}
		else
		{
			tense = Tense.FUTURE;
			logger.logTrace("generateBoth: Selected tense: future");
		}

		logger.logDebug("generateBoth: Proceeding with number: " + number.name().toLowerCase() + ", tense: " + tense.name().toLowerCase());
		printWhite("Proceeding generating a sentence with " + number.name().toLowerCase() + " nouns and " + tense.name().toLowerCase() + " tense", true);
		String sentence = processor.generateWithBoth(number, tense);

		logger.logDebug("generateBoth: Generated sentence: " + sentence);
		printWhite("Generated sentence: '" + sentence + "'", true);
		logger.logTrace("generateBoth: Printed generated sentence");

		logger.logTrace("generateBoth: Completed sentence generation successfully");
	}

	private synchronized void analyzeHandler() throws IOException
	{
		logger.logTrace("analyzeHandler: Starting sentence analysis procedure");

		printTitleSeparator("Analyze procedure", BOLD_BLUE_STYLE);
		String analysis = validateInput("Proceeding to analyze select one of the following options:", ANALYZE_MODE_OPTIONS, false);

		logger.logTrace("analyzeHandler: Prompt for analysis option displayed");
		logger.logDebug("analyzeHandler: Received analysis option: " + (analysis.isEmpty() ? "NONE" : analysis));

		if(analysis.isEmpty())
		{
			logger.logWarn("analyzeHandler: Analysis cancelled due to invalid input");
			logger.logTrace("analyzeHandler: Completed due to cancellation");
			return;
		}

		String mode = validateInput("Select which sentence you want to analyze:", INPUT_MODE_OPTIONS, false);
		logger.logTrace("analyzeHandler: Prompt for input mode displayed");
		logger.logDebug("analyzeHandler: Received input mode: " + (mode.isEmpty() ? "NONE" : mode));


		if(mode.isEmpty())
		{
			logger.logWarn("analyzeHandler: Analysis cancelled due to invalid input");
			logger.logTrace("analyzeHandler: Completed due to cancellation");
			return;
		}

		String userInput = new String();

		if(mode.equals("generate") || mode.equals("g"))
		{
			logger.logTrace("analyzeHandler: Selected input mode: generate");
			printWhite("Proceeding to generate a random sentence", true);

			userInput = processor.generateRandom();
			logger.logDebug("analyzeHandler: Generated sentence: " + userInput);

			printWhite("Generated sentence: '" + userInput + "'", true);
			logger.logTrace("analyzeHandler: Printed generated sentence");
		}
		else if(mode.equals("input") || mode.equals("i"))
		{
			logger.logTrace("analyzeHandler: Selected input mode: input");
			printWhite("Insert the desired sentence", true);

			for(int i = MAX_ATTEMPTS; i >= 0; --i)
			{
				userInput = read(false);
				logger.logDebug("analyzeHandler: Received sentence input: " + (userInput.isEmpty() ? "<empty>" : userInput));

				if(userInput.isEmpty() && i > 1)
				{
					printYellow("Please enter a valid sentence. Remaining attempts " + (i - 1), true);
					logger.logWarn("analyzeHandler: Empty sentence entered, attempts remaining: " + (i - 1));
				}
				else if(i == 1)
				{
					printYellow("Maximum attempts reached. Operation cancelled.", true);
					logger.logWarn("analyzeHandler: Maximum attempts reached, operation cancelled");

					logger.logTrace("analyzeHandler: Completed due to cancellation");
					return;
				}
				else
				{
					logger.logTrace("analyzeHandler: Valid sentence input received");
					break;
				}
			}
		}
		else if(mode.equals("cached") || mode.equals("ca"))
		{
			logger.logTrace("analyzeHandler: Selected input mode: cached");

			if(!processor.isSentenceCached())
			{
				printYellow("No sentence is cached, try generating one first", true);

				logger.logWarn("analyzeHandler: No cached sentence available");
				logger.logTrace("analyzeHandler: Completed due to no cached sentence");
				return;
			}

			printWhite("Proceeding to analyze the cached sentence: '" + processor.getCachedSentence() + "'", true);
			userInput = processor.getCachedSentence();

			logger.logDebug("analyzeHandler: Using cached sentence: " + userInput);
			logger.logTrace("analyzeHandler: Printed cached sentence");
		}
		else if(mode.equals("choose") || mode.equals("ch"))
		{
			logger.logTrace("analyzeHandler: Selected input mode: choose");
			printWhite("Choose one template among the five presented:", true);
			logger.logTrace("analyzeHandler: Prompt for template selection displayed");

			List<Template> templateList = processor.getRandomTemplates();
			logger.logDebug("analyzeHandler: Retrieved " + templateList.size() + " random templates");

			int i = 1;
			for(Template template : templateList)
			{
				printWhite((i++) + ": " + template.getPattern(), true);
				logger.logDebug("analyzeHandler: Displayed template " + (i - 1) + ": " + template.getPattern());
			}

			String choice = new String();

			for(i = MAX_ATTEMPTS; i >= 0; --i)
			{
				choice = read(true);
				logger.logDebug("analyzeHandler: Received template choice: " + (choice.isEmpty() ? "<empty>" : choice));

				if(choice.matches("[1-5]") && i > 1)
				{
					Template template = templateList.get(Integer.parseInt(choice) - 1);
					printWhite("Template chosen: " + template.getPattern(), true);

					logger.logDebug("analyzeHandler: Selected template: " + template.getPattern());
					userInput = processor.generateWithTemplate(templateList.get(Integer.parseInt(choice) - 1));

					printWhite("Sentence generated from the chosen template: " + userInput, true);
					logger.logDebug("analyzeHandler: Generated sentence from template: " + userInput);

					logger.logTrace("analyzeHandler: Printed generated sentence from template");
					break;
				}
				else if(i == 1)
				{
					printYellow("Maximum attempts reached, operation cancelled", true);
					logger.logWarn("analyzeHandler: Maximum attempts reached for template selection, operation cancelled");
					logger.logTrace("analyzeHandler: Completed due to cancellation");
					return;
				}
				else
				{
					printYellow("Please enter a valid value. Remaining attempts " + (i - 1), true);
					logger.logWarn("analyzeHandler: Invalid template choice, attempts remaining: " + (i - 1));
				}
			}
		}

		logger.logDebug("analyzeHandler: Proceeding with analysis option: " + analysis);
		switch(analyzeOptions.get(analysis))
		{
			case RANDOM: analyzeRandom(userInput); break;
			case ALL: analyzeAll(userInput); break;
			case SYNTAX: analyzeSyntax(userInput); break;
			case SENTIMENT: analyzeSentiment(userInput); break;
			case TOXICITY: analyzeToxicity(userInput); break;
			case ENTITY: analyzeEntity(userInput); break;
			case COMBINED: analyzeCombined(userInput); break;
		}

		logger.logTrace("analyzeHandler: Completed sentence analysis procedure successfully");
	}

	private synchronized void analyzeRandom(String input)
	{
		logger.logTrace("analyzeRandom: Starting random analysis procedure");

		printTitleSeparator("Random analysis", BOLD_BLUE_STYLE);
		Random random = new Random();

		AnalyzeOptions[] opts = { AnalyzeOptions.SYNTAX, AnalyzeOptions.SENTIMENT, AnalyzeOptions.TOXICITY, AnalyzeOptions.ENTITY };

		AnalyzeOptions opt = opts[random.nextInt(opts.length)];
		logger.logDebug("analyzeRandom: Selected random analysis option: " + opt.name().toLowerCase());

		printWhite("Random analysis chosen is " + opt.name().toLowerCase(), true);
		logger.logTrace("analyzeRandom: Printed selected analysis option");

		switch(opt)
		{
			case SYNTAX: analyzeSyntax(input); break;
			case SENTIMENT: analyzeSentiment(input); break;
			case TOXICITY: analyzeToxicity(input); break;
			case ENTITY: analyzeEntity(input); break;
		}

		logger.logTrace("analyzeRandom: Completed random analysis procedure successfully");
	}

	private synchronized void analyzeAll(String input)
	{
		logger.logTrace("analyzeAll: Starting complete analysis procedure");

		printTitleSeparator("Complete analysis", BOLD_BLUE_STYLE);
		printWhite("Proceeding with all the analysis", true);

		logger.logTrace("analyzeAll: Executing syntax analysis");
		analyzeSyntax(input);
		logger.logTrace("analyzeAll: Completed syntax analysis");

		logger.logTrace("analyzeAll: Executing sentiment analysis");
		analyzeSentiment(input);
		logger.logTrace("analyzeAll: Completed sentiment analysis");

		logger.logTrace("analyzeAll: Executing toxicity analysis");
		analyzeToxicity(input);
		logger.logTrace("analyzeAll: Completed toxicity analysis");

		logger.logTrace("analyzeAll: Executing entity analysis");
		analyzeEntity(input);
		logger.logTrace("analyzeAll: Completed entity analysis");

		logger.logTrace("analyzeAll: Completed complete analysis procedure successfully");
	}

	private synchronized void analyzeSyntax(String input)
	{
		logger.logTrace("analyzeSyntax: Starting syntax analysis procedure");

		printTitleSeparator("Analyze syntax", BOLD_BLUE_STYLE);
		logger.logDebug("analyzeSyntax: Analyzing input: " + input);

		String result = processor.analyzeSyntax(input);
		logger.logDebug("analyzeSyntax: Syntax analysis result: " + result);

		printWhite(result, true);
		logger.logTrace("analyzeSyntax: Printed analysis result");

		logger.logTrace("analyzeSyntax: Completed syntax analysis procedure successfully");
	}

	private synchronized void analyzeSentiment(String input)
	{
		logger.logTrace("analyzeSentiment: Starting sentiment analysis procedure");

		printTitleSeparator("Analyze sentiment", BOLD_BLUE_STYLE);

		logger.logDebug("analyzeSentiment: Analyzing input: " + input);
		String result = processor.analyzeSentiment(input);
		logger.logDebug("analyzeSentiment: Sentiment analysis result: " + result);

		printWhite(result, true);
		logger.logTrace("analyzeSentiment: Printed analysis result");

		logger.logTrace("analyzeSentiment: Completed sentiment analysis procedure successfully");
	}

	private synchronized void analyzeToxicity(String input)
	{
		logger.logTrace("analyzeToxicity: Starting toxicity analysis procedure");

		printTitleSeparator("Analyze toxicity", BOLD_BLUE_STYLE);

		logger.logDebug("analyzeToxicity: Analyzing input: " + input);
		String result = processor.analyzeToxicity(input);
		logger.logDebug("analyzeToxicity: Toxicity analysis result: " + result);

		printWhite(result, true);
		logger.logTrace("analyzeToxicity: Printed analysis result");

		logger.logTrace("analyzeToxicity: Completed toxicity analysis procedure successfully");
	}

	private synchronized void analyzeEntity(String input)
	{
		logger.logTrace("analyzeEntity: Starting entity analysis procedure");

		printTitleSeparator("Analyze entity", BOLD_BLUE_STYLE);

		logger.logDebug("analyzeEntity: Analyzing input: " + input);
		String result = processor.analyzeEntity(input);
		logger.logDebug("analyzeEntity: Entity analysis result: " + result);

		printWhite(result, true);
		logger.logTrace("analyzeEntity: Printed analysis result");

		logger.logTrace("analyzeEntity: Completed entity analysis procedure successfully");
	}

	private synchronized void analyzeCombined(String input) throws IOException
	{
		logger.logTrace("analyzeCombined: Starting combined analysis procedure");

		printTitleSeparator("Combined analysis", BOLD_BLUE_STYLE);
		String mode = validateInput("Select the desired analysis (press enter to confirm the combination choice):", COMBINED_ANALYZE_MODE_OPTIONS, true);

		logger.logTrace("analyzeCombined: Prompt for analysis mode displayed");
		logger.logDebug("analyzeCombined: Received analysis mode: " + (mode.isEmpty() ? "NONE" : mode));

		List<AnalyzeOptions> opts = new ArrayList<>();

		while(!mode.isEmpty())
		{
			if(mode.equals("syntax") || mode.equals("sy"))
			{
				if(opts.contains(AnalyzeOptions.SYNTAX))
				{
					printYellow("Option already chosen", true);
					logger.logWarn("analyzeCombined: Syntax option already chosen");
				}
				else
				{
					opts.add(AnalyzeOptions.SYNTAX);
					printWhite("Option added: syntax", true);
					logger.logTrace("analyzeCombined: Added syntax option");
				}
			}
			else if(mode.equals("sentiment") || mode.equals("se"))
			{
				if(opts.contains(AnalyzeOptions.SENTIMENT))
				{
					printYellow("Option already chosen", true);
					logger.logWarn("analyzeCombined: Sentiment option already chosen");
				}
				else
				{
					opts.add(AnalyzeOptions.SENTIMENT);
					printWhite("Option added: sentiment", true);
					logger.logTrace("analyzeCombined: Added sentiment option");
				}
			}
			else if(mode.equals("toxicity") || mode.equals("t"))
			{
				if(opts.contains(AnalyzeOptions.TOXICITY))
				{
					printYellow("Option already chosen", true);
					logger.logWarn("analyzeCombined: Toxicity option already chosen");
				}
				else
				{
					opts.add(AnalyzeOptions.TOXICITY);
					printWhite("Option added: toxicity", true);
					logger.logTrace("analyzeCombined: Added toxicity option");
				}
			}
			else if(mode.equals("entity") || mode.equals("e"))
			{
				if(opts.contains(AnalyzeOptions.ENTITY))
				{
					printYellow("Option already chosen", true);
					logger.logWarn("analyzeCombined: Entity option already chosen");
				}
				else
				{
					opts.add(AnalyzeOptions.ENTITY);
					printWhite("Option added: entity", true);
					logger.logTrace("analyzeCombined: Added entity option");
				}
			}

			mode = validateInput("Select the next desired mode:", COMBINED_ANALYZE_MODE_OPTIONS, true);
			logger.logTrace("analyzeCombined: Prompt for next analysis mode displayed");
			logger.logDebug("analyzeCombined: Received next analysis mode: " + (mode.isEmpty() ? "NONE" : mode));
		}

		if(opts.isEmpty())
		{
			printYellow("No options specified.", true);
			logger.logWarn("analyzeCombined: No analysis options selected, operation cancelled");
			logger.logTrace("analyzeCombined: Completed due to no options selected");
			return;
		}

		for(AnalyzeOptions opt : opts)
		{
			if(opts.indexOf(opt) != 0)
				printSeparator(BOLD_BLUE_STYLE);

			printWhite("Proceeding with " + opt, true);

			logger.logDebug("analyzeCombined: Executing analysis: " + opt.name().toLowerCase());
			switch(opt)
			{
				case SYNTAX: analyzeSyntax(input); break;
				case SENTIMENT: analyzeSentiment(input); break;
				case TOXICITY: analyzeToxicity(input); break;
				case ENTITY: analyzeEntity(input); break;
			}
		}

		logger.logTrace("analyzeCombined: Completed combined analysis procedure successfully");
	}

	private synchronized void treeHandler() throws IOException
	{
		logger.logTrace("treeHandler: Starting syntax tree generation procedure");

		printTitleSeparator("Syntax tree procedure", BOLD_BLUE_STYLE);
		String mode = validateInput("Select the sentence you want to print the syntactic tree of:", INPUT_MODE_OPTIONS, false);

		logger.logTrace("treeHandler: Prompt for input mode displayed");
		logger.logDebug("treeHandler: Received input mode: " + (mode.isEmpty() ? "NONE" : mode));


		if(mode.isEmpty())
		{
			logger.logWarn("treeHandler: Operation cancelled due to invalid input");
			logger.logTrace("treeHandler: Completed due to cancellation");
			return;
		}

		String userInput = new String();

		if(mode.equals("generate") || mode.equals("g"))
		{
			logger.logTrace("treeHandler: Selected input mode: generate");
			printWhite("Proceeding to generate a random sentence", true);

			userInput = processor.generateRandom();
			logger.logDebug("treeHandler: Generated sentence: " + userInput);

			printWhite("Generated sentence: '" + userInput + "'", true);
			logger.logTrace("treeHandler: Printed generated sentence");
		}
		else if(mode.equals("input") || mode.equals("i"))
		{
			logger.logTrace("treeHandler: Selected input mode: input");
			printWhite("Insert the desired sentence", true);

			for(int i = MAX_ATTEMPTS; i >= 0; --i)
			{
				userInput = read(false);
				logger.logDebug("treeHandler: Received sentence input: " + (userInput.isEmpty() ? "NONE" : userInput));

				if(userInput.isEmpty() && i > 1)
				{
					printYellow("Please enter a valid sentence. Remaining attempts " + (i - 1), true);
					logger.logWarn("treeHandler: Empty sentence entered, attempts remaining: " + (i - 1));
				}
				else if(i == 1)
				{
					printYellow("Maximum attempts reached, operation cancelled", true);
					logger.logWarn("treeHandler: Maximum attempts reached, operation cancelled");
					logger.logTrace("treeHandler: Completed due to cancellation");
					return;
				}
				else
				{
					logger.logTrace("treeHandler: Valid sentence input received");
					break;
				}
			}
		}
		else if(mode.equals("cached") || mode.equals("c"))
		{
			logger.logTrace("treeHandler: Selected input mode: cached");

			if(!processor.isSentenceCached())
			{
				printYellow("No sentence is cached, try generating one first", true);
				logger.logWarn("treeHandler: No cached sentence available");
				logger.logTrace("treeHandler: Completed due to no cached sentence");
				return;
			}

			printWhite("Proceding with the cached sentence", true);
			userInput = processor.getCachedSentence();

			logger.logDebug("treeHandler: Using cached sentence: " + userInput);
			logger.logTrace("treeHandler: Printed cached sentence notification");
		}
		else if(mode.equals("choose") || mode.equals("ch"))
		{
			logger.logTrace("treeHandler: Selected input mode: choose");
			printWhite("Choose one template among the five presented:", true);

			List<Template> templateList = processor.getRandomTemplates();
			logger.logDebug("treeHandler: Retrieved " + templateList.size() + " random templates");

			int i = 1;
			for(Template template : templateList)
			{
				printWhite((i++) + ": " + template.getPattern(), true);
				logger.logDebug("treeHandler: Displayed template " + (i - 1) + ": " + template.getPattern());
			}

			String choice = new String();

			for(i = MAX_ATTEMPTS; i >= 0; --i)
			{
				choice = read(true);
				logger.logDebug("treeHandler: Received template choice: " + (choice.isEmpty() ? "<empty>" : choice));

				if(choice.matches("[1-5]") && i > 1)
				{
					Template template = templateList.get(Integer.parseInt(choice) - 1);
					printWhite("Template chosen: " + template.getPattern(), true);

					logger.logDebug("treeHandler: Selected template: " + template.getPattern());
					userInput = processor.generateWithTemplate(templateList.get(Integer.parseInt(choice) - 1));

					printWhite("Sentence generated from the chosen template: " + userInput, true);
					logger.logDebug("treeHandler: Generated sentence from template: " + userInput);

					logger.logTrace("treeHandler: Printed generated sentence from template");
					break;
				}
				else if(i == 1)
				{
					printYellow("Maximum attempts reached, operation cancelled", true);
					logger.logWarn("treeHandler: Maximum attempts reached for template selection, operation cancelled");
					logger.logTrace("treeHandler: Completed due to cancellation");
					return;
				}
				else
				{
					printYellow("Please enter a valid value. Remaining attempts " + (i - 1), true);
					logger.logWarn("treeHandler: Invalid template choice, attempts remaining: " + (i - 1));
				}
			}
		}

		logger.logDebug("treeHandler: Generating syntax tree for sentence: " + userInput);
		printTree(userInput);
		logger.logTrace("treeHandler: Completed syntax tree generation");

		logger.logTrace("treeHandler: Completed syntax tree generation procedure successfully");
	}

	private synchronized void printTree(String input) throws IOException
	{
		logger.logTrace("printTree: Starting syntax tree printing procedure");

		printTitleSeparator("Syntax tree", BOLD_BLUE_STYLE);

		logger.logDebug("printTree: Printing syntax tree for input: " + input);
		printWhite(processor.generateSyntaxTree(input), true);
		logger.logTrace("printTree: Completed syntax tree printing");

		logger.logTrace("printTree: Completed syntax tree printing procedure successfully");
	}

	private synchronized void extendHandler() throws IOException
	{
		logger.logTrace("extendHandler: Starting sentence extension procedure");

		printTitleSeparator("Extension procedure", BOLD_BLUE_STYLE);
		String partOfSpeech = validateInput("Enter the part of speech that you want to add (press Enter to confirm the new terms):", ELEMENT_OPTIONS, true);

		logger.logTrace("extendHandler: Prompt for part of speech displayed");
		logger.logDebug("extendHandler: Received part of speech: " + (partOfSpeech.isEmpty() ? "<empty>" : partOfSpeech));

		List<Noun> nounList = new ArrayList<>();
		List<Adjective> adjectiveList = new ArrayList<>();
		List<Verb> verbList = new ArrayList<>();

		while(!partOfSpeech.isEmpty())
		{
			if(partOfSpeech.equals("noun") || partOfSpeech.equals("n"))
			{
				logger.logTrace("extendHandler: Selected part of speech: noun");
				String num = validateInput("Enter the number for the noun: ", NUMBER_OPTIONS, false);
				logger.logDebug("extendHandler: Received noun number: " + (num.isEmpty() ? "NONE" : num));

				if(num.isEmpty())
				{
					logger.logWarn("extendHandler: Operation cancelled due to invalid input");
					logger.logTrace("extendHandler: Completed due to cancellation");
					return;
				}

				Number number;
				if(num.equals("singular") || num.equals("s"))
				{
					number = Number.SINGULAR;
					logger.logTrace("extendHandler: Selected noun number: singular");
				}
				else
				{
					number = Number.PLURAL;
					logger.logTrace("extendHandler: Selected noun number: plural");
				}

				printWhite("Insert the new noun:", true);

				String text = new String();

				for(int i = MAX_ATTEMPTS; i >= 0; --i)
				{
					text = read(true);
					logger.logDebug("extendHandler: Received noun input: " + (text.isEmpty() ? "NONE" : text));

					if((text.isEmpty() || !text.matches("[a-zA-Z]+")) && i > 1)
					{
						printYellow("Please enter a valid value. Remaining attempts " + (i - 1), true);
						logger.logWarn("extendHandler: Invalid noun input, attempts remaining: " + (i - 1));
					}
					else if(i == 1)
					{
						printYellow("Maximum attempts reached, operation cancelled", true);
						logger.logWarn("extendHandler: Maximum attempts reached for noun input, operation cancelled");
						logger.logTrace("extendHandler: Completed due to cancellation");
						break;
					}
					else
					{
						logger.logTrace("extendHandler: Valid noun input received");
						break;
					}
				}

				nounList.add(new Noun(text, number));
				logger.logDebug("extendHandler: Added noun: " + text + " (" + number.name().toLowerCase() + ")");
			}
			else if(partOfSpeech.equals("adjective") || partOfSpeech.equals("a"))
			{
				logger.logTrace("extendHandler: Selected part of speech: adjective");
				printWhite("Insert the new adjective:", true);

				String text = new String();

				for(int i = MAX_ATTEMPTS; i >= 0; --i)
				{
					text = read(true);
					logger.logDebug("extendHandler: Received adjective input: " + (text.isEmpty() ? "<empty>" : text));

					if((text.isEmpty() || !text.matches("[a-zA-Z]+")) && i > 1)
					{
						printYellow("Please enter a valid value. Remaining attempts " + (i - 1), true);
						logger.logWarn("extendHandler: Invalid adjective input, attempts remaining: " + (i - 1));
					}
					else if(i == 1)
					{
						printYellow("Maximum attempts reached, operation cancelled", true);
						logger.logWarn("extendHandler: Maximum attempts reached for adjective input, operation cancelled");
						logger.logTrace("extendHandler: Completed due to cancellation");
						return;
					}
					else
					{
						logger.logTrace("extendHandler: Valid adjective input received");
						break;
					}
				}

				adjectiveList.add(new Adjective(text));
				logger.logDebug("extendHandler: Added adjective: " + text);
			}
			else if(partOfSpeech.equals("verb") || partOfSpeech.equals("v"))
			{
				logger.logTrace("extendHandler: Selected part of speech: verb");
				String textTense = validateInput("Enter the tense for the verb: ", TENSE_OPTIONS, false);
				logger.logDebug("extendHandler: Received verb tense: " + (textTense.isEmpty() ? "<empty>" : textTense));

				if(textTense.isEmpty())
				{
					logger.logWarn("extendHandler: Operation cancelled due to invalid input");
					logger.logTrace("extendHandler: Completed due to cancellation");
					return;
				}

				Tense tense;

				if(textTense.equals("past") || textTense.equals("pa"))
				{
					tense = Tense.PAST;
					logger.logTrace("extendHandler: Selected verb tense: past");
				}
				else if(textTense.equals("present") || textTense.equals("pr"))
				{
					tense = Tense.PRESENT;
					logger.logTrace("extendHandler: Selected verb tense: present");
				}
				else
				{
					tense = Tense.FUTURE;
					logger.logTrace("extendHandler: Selected verb tense: future");
				}

				String textNumber = validateInput("Enter the number for the verb: ", NUMBER_OPTIONS, false);
				logger.logDebug("extendHandler: Received verb number: " + (textNumber.isEmpty() ? "<empty>" : textNumber));

				if(textNumber.isEmpty())
				{
					logger.logWarn("entendHandler: Operation cancelled due to invalid input");
					logger.logTrace("extendHandler: Completed due to cancellation");
					return;
				}

				Number number;

				if(textNumber.equals("singular") || textNumber.equals("s"))
				{
					number = Number.SINGULAR;
					logger.logTrace("extendHandler: Selected verb number: singular");
				}
				else
				{
					number = Number.PLURAL;
					logger.logTrace("extendHandler: Selected verb number: plural");
				}
				printWhite("Insert the new verb:", true);

				String text = new String();

				for(int i = MAX_ATTEMPTS; i >= 0; --i)
				{
					text = read(true);
					logger.logDebug("extendHandler: Received verb input: " + (text.isEmpty() ? "<empty>" : text));

					if((text.isEmpty() || !text.matches("[a-zA-Z]+")) && i > 1)
					{
						printYellow("Please enter a valid value. Remaining attempts " + (i - 1), true);
						logger.logWarn("extendHandler: Invalid verb input, attempts remaining: " + (i - 1));
					}
					else if(i == 1)
					{
						printYellow("Maximum attempts reached, operation cancelled", true);
						logger.logWarn("extendHandler: Maximum attempts reached for verb input, operation cancelled");
						logger.logTrace("extendHandler: Completed due to cancellation");
						return;
					}
					else
					{
						logger.logTrace("extendHandler: Valid verb input received");
						break;
					}
				}

				verbList.add(new Verb(text, number, tense));
				logger.logDebug("extendHandler: Added verb: " + text + " (" + tense.name().toLowerCase() + ")");
			}

			partOfSpeech = validateInput("Enter your next choice:", ELEMENT_OPTIONS, true);
			logger.logTrace("extendHandler: Prompt for next part of speech displayed");
			logger.logDebug("extendHandler: Received next part of speech: " + (partOfSpeech.isEmpty() ? "NONE" : partOfSpeech));

		}

		if(nounList.isEmpty() && adjectiveList.isEmpty() && verbList.isEmpty())
		{
			printWhite("No parts of speech added", true);
			logger.logWarn("extendHandler: No parts of speech added");
		}
		else
		{
			printWhite("Proceeding to add the new elements", true);
			logger.logTrace("extendHandler: Proceeding to add new elements");
			processor.append(nounList, adjectiveList, verbList);
			logger.logDebug("extendHandler: Adding " + nounList.size() + " nouns, " + adjectiveList.size() + " adjectives, " + verbList.size() + " verbs");
		}

		logger.logTrace("extendHandler: Completed adding new elements");
		logger.logTrace("extendHandler: Completed sentence extension procedure successfully");
	}

	private synchronized void setToleranceHandler()
	{
		logger.logTrace("setToleranceHandler: Starting tolerance setting procedure");

		printTitleSeparator("Tolerance procedure", BOLD_BLUE_STYLE);
		printWhite("Current tolerance value is " + processor.getTolerance(), true);
		logger.logDebug("setToleranceHandler: Current tolerance value: " + processor.getTolerance());

		printWhite("The tolerance parameter specifies which is the value over which an analyzed sentence is considered toxic", true);
		printWhite("Enter a new tolerance value (0.0 - 1.0): ", true);

		String newTolerance = new String();

		for(int i = MAX_ATTEMPTS; i >= 0; --i)
		{
			newTolerance = read(true);
			logger.logDebug("setToleranceHandler: Received tolerance input: " + (newTolerance.isEmpty() ? "NONE" : newTolerance));

			try
			{
				if((newTolerance.isEmpty() || ((Float.parseFloat(newTolerance) < 0.0f || (Float.parseFloat(newTolerance) > 1.0f)))) && i > 1)
				{
					printYellow("Please enter a valid value. Remaining attempts " + (i - 1), true);
					logger.logWarn("setToleranceHandler: Invalid tolerance input, attempts remaining: " + (i - 1));
				}
				else if(i == 1)
				{
					printYellow("Maximum attempts reached, operation cancelled", true);
					logger.logWarn("setToleranceHandler: Maximum attempts reached, operation cancelled");
					logger.logTrace("setToleranceHandler: Completed due to cancellation");
					return;
				}
				else
				{
					logger.logTrace("setToleranceHandler: Valid tolerance input received");
					break;
				}
			}
			catch(NumberFormatException e)
			{
				printYellow("Please enter a valid value. Remaining attempts " + i, true);
				logger.logWarn("setToleranceHandler: Invalid number format for tolerance input, attempts remaining: " + (i - 1), e);
			}
		}

		printWhite("Setting the new tolerance value", true);
		logger.logTrace("setToleranceHandler: Proceeding to set new tolerance");

		float tolerance = Float.parseFloat(newTolerance);
		processor.setTolerance(tolerance);

		logger.logDebug("setToleranceHandler: Set new tolerance value: " + tolerance);
		logger.logTrace("setToleranceHandler: Completed tolerance setting procedure successfully");
	}

	private synchronized void extendedUsage()
	{
		logger.logTrace("extendedUsage: Starting extended usage display procedure");
		printTitleSeparator("Extended commands help", BOLD_BLUE_STYLE);

		String[][] commandsInfo =
		{
			{
				"Default (d)",
					"Performs a basic but complete procedure of the functionalities offered,\n" +
					"this procedure offers the possibility to generate and/or analyze a sentence\n" +
					"and ends with the syntactic tree of the sentence being displayed.\n" +
					"This is a default combination of commands, for a more specific combination\n" +
					"use 'personalized'; the other entries perform singular functionalities."
			},

			{
				"Personalized (p)",
					"Performs, in a specific order, all the functionalities offered by the program.\n" +
					"In each step a full personalization of the commands is available,\n" +
					"the user can choose every modality and functionality for each process."
			},

			{
				"Generate (g)",
					"Generates a random nonsense sentence, based on a pool of various\n" +
					"templates, nouns, adjectives and verbs, which are randomly combined.\n" +
					"The sentence, although grammatically correct, totally lacks\n" +
					"logical sense, as the combination is completely random.\n" +
					"The sentence is printed and cached for future analysis, only the\n" +
					"last generated sentence is cached, any previous is overwritten;\n"+
					"the program begins without any cached sentence."
			},

			{
				"Analyze (a)",
					"Offers a number of analysis procedures which can be used\n" +
					"in various combinations, as the analysis can be\n" +
					"chosen by the user, be random, costumized or all of the above.\n" +
					"The types of analysis offered are ones regarding the syntax, the sentiment,\n" +
					"the toxicity or the entities that are contained in the sentence."
			},

			{
				"Tree (t)",
					"Proceeds to print the syntactic tree of the chosen sentence.\n" +
					"Shows the hierarchical structure of the sentence and the relationship among its components.\n" +
					"This functionality can support the analysis of more than one sentence.\n" +
					"This function requires the execution of the syntax analysis of the sentence in background.\n" +
					"It is implicit that this process requires the sentence to be at least grammatically correct."
			},

			{
				"Extend (e)",
					"Proceeds to let the user extend the dictionaries used by the program to generate the sentence;\n" +
					"in particular the user can input nouns, adjectives or verbs at will.\n" +
					"This update is immediately applied to the program, but doesn't last among different sessions."
			},

			{
				"Set tolerance (st)",
					"Allows the user to change the program's tolerance when considering\n" +
					"an analyzed sentence via toxicity analysis, setting the upper bound\n" +
					"over which a sentence is considered to be toxic by the program.\n" +
					"The default value is setted to 0.7 (it ranges from 0.0 to 1.0, inclusive)"
			},

			{
				"Help (h)",
					"Displays basic help information, for commands and their purpose."
			},

			{
				"Info (i)",
					"Shows detailed information about commands and their shortcuts.\n" +
					"Provides extended help for each available command (including hidden ones).\n" +
					"Fuction, use and purpose are explained in detail for each command."
			},

			{
				"Verbose (v)",
					"Hidden command that toggles verbose output mode. When enabled, it provides a more \n" +
					"detailed feedback about the background activity performed by the program.\n" +
					"Used for debugging purposes, its use is not recommended for a basic user experience, as\n" +
					"it can cause much 'noise' on the terminal. The default setting for this function is off."
			},

			{
				"Clear (c)",
					"Clears the terminal screen.\n" +
					"Resets the display and shows the title and the initial menu.\n" +
					"Helpful when there is too much output on the terminal."
			},

			{
				"Quit (q)",
					"Exits the program.\n" +
					"Terminates the application safely."
			}
		};

		logger.logDebug("extendedUsage: Displaying information for " + commandsInfo.length + " commands");

		for(String[] cmdInfo : commandsInfo)
		{
			String command = cmdInfo[0];
			String description = cmdInfo[1];

			printGreen(command, true);
			logger.logDebug("extendedUsage: Printed command: " + command);

			String[] lines = description.split("\n");
			for (String line : lines)
				printWhite("    " + line, true);
		}

		logger.logTrace("extendedUsage: Completed extended usage display procedure successfully");
	}

	private synchronized void verboseHandler()
	{
		logger.logTrace("verboseHandler: Starting verbosity switching procedure");

		printTitleSeparator("Verbosity procedure", BOLD_BLUE_STYLE);
		printWhite("Currently verbosity is set to " + processor.isVerbose(), true);

		logger.logDebug("verboseHandler: Current verbosity state: " + processor.isVerbose());

		printWhite("Switching verbosity", true);
		logger.logTrace("verboseHandler: Proceeding to switch verbosity");

		processor.switchVerbosity();
		logger.logDebug("verboseHandler: Switched verbosity to: " + processor.isVerbose());

		logger.logTrace("verboseHandler: Completed verbosity switching procedure successfully");
	}

	private synchronized void clearTerminal()
	{
		logger.logTrace("clearTerminal: Starting terminal clearing procedure");

		terminal.puts(InfoCmp.Capability.clear_screen);

		print("\033[3J");
		logger.logTrace("clearTerminal: Cleared terminal scrollback");

		print(initialOutput);
		logger.logTrace("clearTerminal: Printed initial output");

		logger.logTrace("clearTerminal: Completed terminal clearing procedure successfully");
	}

	private synchronized void usage(PrintWriter writer)
	{
		logger.logTrace("usage: Starting usage display procedure");
		String title = "Available Commands";

		int titlePadding = (MAX_WIDTH - title.length() - 4) / 2;
		String titleLine = "=".repeat(titlePadding) + "< " + title + " >" + "=".repeat(titlePadding);
		writer.println(new AttributedString(titleLine, BOLD_MAGENTA_STYLE).toAnsi(terminal));

		String[] commands =
		{
			"Default", "Performs a basic combination of generation and analysis",
			"Personalized", "Performs all the functionalities, with each step personalizable",
			"Generate", "Generates a random nonsense sentence",
			"Analyze", "Analyzes a sentence via several functions",
			"Tree", "Prints the syntactic tree of a sentence",
			"Extend", "Allows insertion of a noun, adjective or verb to the dicionaries",
			"Set tolerance", "Change tolerance level for toxicity analysis",
			"Help", "Shows this help menu",
			"Info", "Shows more detailed info about the commands",
			"Clear", "Clears the terminal and shows initial menu",
			"Quit", "Exits the program"
		};

		logger.logDebug("usage: Displaying information for " + commands.length + " commands");

		for(int i = 0; i < commands.length; i += 2)
		{
			String command = String.format("%-22s", commands[i]);
			String description = String.format("%-33s", commands[i + 1]);

			AttributedString styledCommand = new AttributedString(command, BOLD_GREEN_STYLE);
			AttributedString styledDescription = new AttributedString(description, BOLD_WHITE_STYLE);

			writer.println(styledCommand.toAnsi(terminal) + styledDescription.toAnsi(terminal));
		}

		writer.println(new AttributedString("=".repeat(MAX_WIDTH), BOLD_MAGENTA_STYLE).toAnsi(terminal));
		writer.flush();

		logger.logTrace("usage: Completed usage display procedure successfully");
	}

	private synchronized void quit()
	{
		logger.logTrace("quit: Starting quit procedure");
		printTitleSeparator("Terminating", BOLD_BLUE_STYLE);

		closeResources();
		logger.logTrace("quit: Completed quit procedure successfully");
	}

	private synchronized boolean checkInternetConnection()
	{
		logger.logTrace("checkInternetConnection: Starting internet connection check");
		printTitleSeparator("Checking internet connection", BOLD_BLUE_STYLE);

		try
		{
			logger.logTrace("checkInternetConnection: Trying to reach google.com");
			InetAddress.getByName("google.com").isReachable(1000);
			printTitleSeparator("Success", BOLD_GREEN_STYLE);

			logger.logTrace("checkInternetConnection: Successfully reached google.com");
			return true;

		}
		catch(Exception e)
		{
			logger.logTrace("checkInternetConnection: Failed to reach google.com, trying cloudflare.com");

			try
			{
				InetAddress.getByName("cloudflare.com").isReachable(1000);
				printTitleSeparator("Success", BOLD_GREEN_STYLE);

				logger.logTrace("checkInternetConnection: Successfully reached cloudflare.com");
				return true;
			}
			catch (Exception ex)
			{
				printTitleSeparator("Failed", BOLD_RED_STYLE);
				logger.logError("checkInternetConnection: Failed to establish internet connection", ex);
				return false;
			}
		}
	}

	private synchronized void print(String text)
	{
		terminal.writer().print(text);
		terminal.flush();
	}

	private synchronized void prinln(String text)
	{
		terminal.writer().println(text);
		terminal.flush();
	}

	private synchronized void printMagenta(String text, boolean bold)
	{
		if(bold)
			terminal.writer().println(new AttributedString(text, BOLD_MAGENTA_STYLE).toAnsi(terminal));
		else
			terminal.writer().println(new AttributedString(text, MAGENTA_STYLE).toAnsi(terminal));

		terminal.flush();
	}

	private synchronized void printGreen(String text, boolean bold)
	{
		if(bold)
			terminal.writer().println(new AttributedString(text, BOLD_GREEN_STYLE).toAnsi(terminal));
		else
			terminal.writer().println(new AttributedString(text, GREEN_STYLE).toAnsi(terminal));

		terminal.flush();
	}

	private synchronized void printWhite(String text, boolean bold)
	{
		if(bold)
			terminal.writer().println(new AttributedString(text, BOLD_WHITE_STYLE).toAnsi(terminal));
		else
			terminal.writer().println(new AttributedString(text, WHITE_STYLE).toAnsi(terminal));

		terminal.flush();
	}

	private synchronized void printYellow(String text, boolean bold)
	{
		if(bold)
			terminal.writer().println(new AttributedString(text, BOLD_YELLOW_STYLE).toAnsi(terminal));
		else
			terminal.writer().println(new AttributedString(text, YELLOW_STYLE).toAnsi(terminal));

		terminal.flush();
	}

	private synchronized void printRed(String text, boolean bold)
	{
		if(bold)
			terminal.writer().println(new AttributedString(text, BOLD_RED_STYLE).toAnsi(terminal));
		else
			terminal.writer().println(new AttributedString(text, RED_STYLE).toAnsi(terminal));
		terminal.flush();
	}

	private synchronized void printBlue(String text, boolean bold)
	{
		if(bold)
			terminal.writer().println(new AttributedString(text, BOLD_BLUE_STYLE).toAnsi(terminal));
		else
			terminal.writer().println(new AttributedString(text, BLUE_STYLE).toAnsi(terminal));

		terminal.flush();
	}

	private synchronized String read(boolean demangled)
	{
		if(demangled)
			return plainReader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
		else
			return plainReader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ");
	}

	private synchronized String validateInput(String prompt, List<Option> options, boolean skippable) throws IOException
	{
		logger.logTrace("validateInput: Starting input validation");
		logger.logDebug("validateInput: Prompt: " + prompt + ", skippable: " + skippable);

		printWhite(prompt, true);

		for(Option option : options)
		{
			logger.logDebug("validateInput: Displaying option - " + option.getDisplayName());
			printWhite(" - " + option.getDisplayName() + ": " + option.getDescription(), true);
		}

		for(int i = MAX_ATTEMPTS; i > 0; --i)
		{
			logger.logTrace("validateInput: Attempt " + (MAX_ATTEMPTS - i + 1) + "/" + MAX_ATTEMPTS);
			String input = read(true);

			if(input.isEmpty() && skippable)
			{
				logger.logTrace("validateInput: Empty input allowed, returning empty string");
				return "";
			}

			Optional<Option> matchedOption = options.stream().filter(opt -> opt.matches(input)).findFirst();

			if(matchedOption.isPresent())
			{
				logger.logDebug("validateInput: Valid input received: " + input);
				return matchedOption.get().getMainCommand();
			}

			if(i > 1)
			{
				String validOptions = options.stream().map(opt -> opt.getDisplayName() + " (" + String.join("/", opt.getAlias()) + ")").collect(Collectors.joining(", "));

				logger.logDebug("validateInput: Invalid input: " + input + ", remaining attempts: " + (i - 1));
				printYellow("Invalid input. Please enter one of: " + validOptions + ". Remaining attempts: " + (i - 1), true);
			}
			else
			{
				logger.logDebug("validateInput: Maximum attempts reached");
				printYellow("Maximum attempts reached. Operation cancelled.", true);
			}
		}

		logger.logDebug("validateInput: Returning empty string after all attempts");

		return "";
	}

	private synchronized void printSeparator(AttributedStyle style)
	{
		terminal.writer().println(new AttributedString("=".repeat(MAX_WIDTH), style).toAnsi(terminal));
		terminal.flush();
	}

	private synchronized void printTitleSeparator(String title, AttributedStyle style)
	{
		int availableSpace = MAX_WIDTH - title.length() - 4;

		int rightPadding = availableSpace / 2;
		int leftPadding = availableSpace - rightPadding;

		String separator = "=".repeat(leftPadding) + "< " + title + " >" + "=".repeat(rightPadding);

		terminal.writer().println(new AttributedString(separator, style).toAnsi(terminal));
		terminal.flush();
	}

	public synchronized void closeResources()
	{
		logger.logTrace("closeResources: Starting resource cleanup");

		if(running)
		{
			logger.logDebug("closeResources: Setting running flag to false");
			running = false;
		}

		try
		{
			logger.logTrace("closeResources: Attempting to close terminal");
			terminal.close();
			logger.logTrace("closeResources: Terminal closed successfully");
		}
		catch(IOException e)
		{
			logger.logError("closeResources: Failed to close terminal", e);
			printRed("Failed to close terminal: " + e.getMessage(), true);
		}

		logger.logTrace("closeResources: Resource cleanup completed");
	}
}
