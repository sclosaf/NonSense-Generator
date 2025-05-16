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
		RANDOM, ALL, SYNTAX, SENTIMENT, TOXICITY, ENTITY, COMBINED
	}

	private static class Option
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
			new Option("cached", "Use the cached sentence", "c")
		);

	private static final List<Option> ELEMENT_OPTIONS = Arrays.asList
		(
			new Option("noun", "Enter a noun", "n"),
			new Option("adjective", "Enter a new adjective", "a"),
			new Option("verb", "Enter a new verb", "v")
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
			new Option("future", "Use simple future tense", "f")
		);

	private static Map<String, Command> commands = new HashMap<>();
	private static Map<String, GenerateOptions> generateOptions = new HashMap<>();
	private static Map<String, AnalyzeOptions> analyzeOptions = new HashMap<>();

	private static final int HISTORY_SIZE = 20;
	private static final int MAX_ATTEMPTS = 4;
	private static final int MAX_WIDTH = 106;

	private boolean running;
	private final String initialOutput;

	private final CommandProcessor processor;
	private final Terminal terminal;

	private LineReader commandReader;
	private LineReader plainReader;

	private static class CommandHighlighter implements Highlighter
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

		terminal = TerminalBuilder.builder().system(true).build();

		commandReader = LineReaderBuilder.builder()
			.terminal(terminal)
			.completer(new StringsCompleter(commands.keySet()))
			.option(LineReader.Option.HISTORY_BEEP, false)
			.option(LineReader.Option.AUTO_LIST, true)
			.option(LineReader.Option.AUTO_FRESH_LINE, true)
			.variable(LineReader.HISTORY_SIZE, HISTORY_SIZE)
			.highlighter(new CommandHighlighter())
			.build();

		plainReader = LineReaderBuilder.builder()
			.terminal(terminal)
			.option(LineReader.Option.HISTORY_BEEP, false)
			.option(LineReader.Option.AUTO_FRESH_LINE, true)
			.variable(LineReader.HISTORY_SIZE, HISTORY_SIZE)
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

		StringWriter stringWriter = new StringWriter();
		PrintWriter tempWriter = new PrintWriter(stringWriter);

		welcome(tempWriter);
		usage(tempWriter);
		tempWriter.flush();

		initialOutput = stringWriter.toString();

		welcome(terminal.writer());

		if(!checkInternetConnection())
			throw new MissingInternetConnectionException();

		usage(terminal.writer());
	}

	private void welcome(PrintWriter writer) throws IOException
	{
		String title = "Welcome to";

		int totalPadding = MAX_WIDTH - title.length() - 4;
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

		writer.println(new AttributedString("=".repeat(MAX_WIDTH), BOLD_WHITE_STYLE).toAnsi(terminal));
		writer.flush();
	}

	public boolean inputCatcher() throws IOException
	{
		try
		{
			printWhite("Enter a command or type 'help'", true);
			String cmd = commandReader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();

			if(!cmd.isEmpty() && !commands.containsKey(cmd))
			{
				printRed("Invalid command: " + cmd, true);
				printRed("Type 'help' for available commands.", true);
				return running;
			}
			else
				commandExecuter(cmd);
		}
		catch(UserInterruptException | EndOfFileException e)
		{
			printYellow("Program ended.", true);
			running = false;
		}

		return running;
	}

	private void commandExecuter(String cmd) throws IOException
	{
		if(cmd.isEmpty())
		{
			printYellow("Please enter a command.", true);
			return;
		}

		switch(commands.get(cmd))
		{
			case DEFAULT: defaultHandler(); break;
			case PERSONALIZED: personalizedHandler(); break;
			case GENERATE: generateHandler(); break;
			case ANALYZE: analyzeHandler(); break;
			case TREE: treeHandler(); break;
			case EXTEND: extendHandler(); break;
			case SETTOLERANCE: setToleranceHandler(); break;
			case INFO: extendedUsage(); break;
			case VERBOSE: verboseHandler(); break;
			case CLEAR: clearTerminal(); break;
			case HELP: usage(terminal.writer()); break;
			case QUIT: quit(); break;
		}

		if(commands.get(cmd) != Command.HELP && commands.get(cmd) != Command.CLEAR && commands.get(cmd) != Command.QUIT)
			printSeparator(BOLD_BLUE_STYLE);
	}

	private void defaultHandler()
	{
		try
		{
			printTitleSeparator("Default procedure", BOLD_BLUE_STYLE);

			printWhite("Proceding with the default process.", true);
			printWhite("Enter a sentence to analyze (or press Enter to generate one automatically and skip the input elabotation):", true);

			String userInput = read(false);

			if(userInput.isEmpty())
			{
				printWhite("Sentence input skipped, proceding by generating one from scratch", true);
				userInput = processor.generateRandom();
				printWhite("Generated sentence: '" + userInput + "'", true);

				printWhite("Proceding with a standard analysis of the generated sentence", true);
				printWhite("This passage includes syntactic and toxicity analysis of the generated sentence", true);
				printWhite("Followed by the generation of the syntactic tree of the generated sentence", true);

				analyzeSyntax(userInput);

				analyzeToxicity(userInput);

				printTree(userInput);
			}
			else
			{
				printWhite("Proceding with the standard synctactic analysis of the given sentence", true);
				analyzeSyntax(userInput);

				printSeparator(BOLD_BLUE_STYLE);
				printWhite("Generating a new sentence by using, nouns, adjectives and verbs of the inserted one", true);
				String generated = processor.generateFrom(userInput);

				printWhite("Generated sentence: '" + generated + "'", true);

				printWhite("Proceding with a standard analysis of the generated sentence", true);
				printWhite("This passage includes the syntactic and toxicity analysis of the generated sentence", true);
				printWhite("Followed by the creation of its synctactic tree", true);

				analyzeSyntax(generated);

				analyzeToxicity(generated);

				printTree(generated);
			}
		}
		catch(IOException e)
		{
			printRed("Error processing input: " + e.getMessage(), true);
		}
	}

	private void personalizedHandler() throws IOException
	{
		printTitleSeparator("Personalized procedure", BOLD_BLUE_STYLE);
		printWhite("Proceding with the personalized process.", true);
		printWhite("This procedure lets the user go through several options that this program offers", true);

		printWhite("As option the user can add new nouns, adjectives and verbs to the dictionaries", true);
		extendHandler();

		printWhite("Enter a sentence to process (or press Enter to generate one automatically)", true);

		String userInput = read(false);

		if(userInput.isEmpty())
		{
			printWhite("Generation process chosen", true);
			generateHandler();
		}

		printWhite("Proceding with the analysis", true);
		analyzeHandler();

		printWhite("Proceding by building the synctactic tree", true);
		treeHandler();
	}

	private void generateHandler() throws IOException
	{
		printTitleSeparator("Generation procedure", BOLD_BLUE_STYLE);
		String userInput = validateInput("Proceding generating a random sentence, select one of the following options:", GENERATION_MODE_OPTIONS, false);

		if(userInput.isEmpty())
			return;

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
		printTitleSeparator("Random generation", BOLD_BLUE_STYLE);
		printWhite("Generated sentence: '" + processor.generateRandom() + "'", true);
	}

	private void generateNumber() throws IOException
	{
		printTitleSeparator("Random generation (with number)", BOLD_BLUE_STYLE);
		String userInput = validateInput("Specify the desired number among the available:", NUMBER_OPTIONS, false);

		if(userInput.isEmpty())
			return;

		if(userInput.equals("singular") || userInput.equals("s"))
		{
			printWhite("Proceding generating a sentence with singular nouns", true);
			printWhite("Generated sentence: '" + processor.generateWithNumber(Number.SINGULAR) + "'", true);
		}
		else if(userInput.equals("plural") || userInput.equals("p"))
		{
			printWhite("Proceding generating a sentence with plural nouns", true);
			printWhite("Generated sentence: '" + processor.generateWithNumber(Number.PLURAL) + "'", true);
		}
	}

	private void generateTense() throws IOException
	{
		printTitleSeparator("Random generation (with tense)", BOLD_BLUE_STYLE);
		String userInput = validateInput("Specify the desired tense among the available:", TENSE_OPTIONS, false);

		if(userInput.isEmpty())
			return;

		if(userInput.equals("past") || userInput.equals("pa"))
		{
			printWhite("Proceding generating a sentence with past tense", true);
			printWhite("Generated sentence: '" + processor.generateWithTense(Tense.PAST) + "'", true);
		}
		else if(userInput.equals("present") || userInput.equals("pr"))
		{
			printWhite("Proceding generating a sentence with present tense", true);
			printWhite("Generated sentence: '" + processor.generateWithTense(Tense.PRESENT) + "'", true);
		}
		else if(userInput.equals("future") || userInput.equals("f"))
		{
			printWhite("Proceding generating a sentence with future tense", true);
			printWhite("Generated sentence: '" + processor.generateWithTense(Tense.FUTURE) + "'", true);
		}
	}

	private void generateBoth() throws IOException
	{
		printTitleSeparator("Random generation (with number and tense)", BOLD_BLUE_STYLE);

		String userInput = validateInput("Specify the desired number among the available:", NUMBER_OPTIONS, false);

		if(userInput.isEmpty())
			return;

		Number number;

		if(userInput.equals("singular") || userInput.equals("s"))
			number = Number.SINGULAR;
		else
			number = Number.PLURAL;

		userInput = validateInput("Specify the desired tense among the available:", TENSE_OPTIONS, false);

		if(userInput.isEmpty())
			return;

		Tense tense;

		if(userInput.equals("past") || userInput.equals("pa"))
			tense = Tense.PAST;
		else if(userInput.equals("present") || userInput.equals("pr"))
			tense = Tense.PRESENT;
		else
			tense = Tense.FUTURE;

		printWhite("Proceding generating a sentence with " + number.name().toLowerCase() + " nouns and " + tense.name().toLowerCase() + " tense", true);
		printWhite("Generated sentence: '" + processor.generateWithBoth(number, tense) + "'", true);
	}

	private void analyzeHandler() throws IOException
	{
		printTitleSeparator("Analyze procedure", BOLD_BLUE_STYLE);
		String analysis = validateInput("Proceeding to analyze select one of the following options:", ANALYZE_MODE_OPTIONS, false);

		if(analysis.isEmpty())
			return;

		String mode = validateInput("Select what sentence do you want to analyze:", INPUT_MODE_OPTIONS, false);

		if(mode.isEmpty())
			return;

		String userInput = new String();

		if(mode.equals("generate") || mode.equals("g"))
		{
			printWhite("Proceding generating a random sentence", true);
			userInput = processor.generateRandom();
			printWhite("Generated sentence: '" + userInput + "'", true);
		}
		else if(mode.equals("input") || mode.equals("i"))
		{
			printWhite("Insert the desired sentence", true);
			userInput = read(false);

			for(int i = MAX_ATTEMPTS - 1; i >= 0; --i)
			{
				if(i == 0)
					return;

				if(userInput.isEmpty())
				{
					printYellow("Please enter a valid sentence. Remaining attempts " + i, true);
					userInput = read(false);
				}
				else
					break;
			}
		}
		else if(mode.equals("cached") || mode.equals("c"))
		{
			if(!processor.isSentenceCached())
			{
				printYellow("No sentence is cached, try generating one first", true);
				return;
			}

			printWhite("Proceding analyzing the cached sentence: '" + processor.getCachedSentence() + "'", true);
			userInput = processor.getCachedSentence();
		}

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
	}

	private void analyzeRandom(String input)
	{
		printTitleSeparator("Random analysis", BOLD_BLUE_STYLE);
		Random random = new Random();

		AnalyzeOptions[] opts = { AnalyzeOptions.SYNTAX, AnalyzeOptions.SENTIMENT, AnalyzeOptions.TOXICITY, AnalyzeOptions.ENTITY };

		AnalyzeOptions opt = opts[random.nextInt(opts.length)];

		printWhite("Random analysis chosen is " + opt.name().toLowerCase(), true);

		switch(opt)
		{
			case SYNTAX: analyzeSyntax(input); break;
			case SENTIMENT: analyzeSentiment(input); break;
			case TOXICITY: analyzeToxicity(input); break;
			case ENTITY: analyzeEntity(input); break;
		}
	}

	private void analyzeAll(String input)
	{
		printTitleSeparator("Complete analysis", BOLD_BLUE_STYLE);
		printWhite("Proceding with all the analysis", true);

		analyzeSyntax(input);
		analyzeSentiment(input);
		analyzeToxicity(input);
		analyzeEntity(input);
	}

	private void analyzeSyntax(String input)
	{
		printTitleSeparator("Analyze syntax", BOLD_BLUE_STYLE);
		printWhite(processor.analyzeSyntax(input), true);
	}

	private void analyzeSentiment(String input)
	{
		printTitleSeparator("Analyze sentiment", BOLD_BLUE_STYLE);
		printWhite(processor.analyzeSentiment(input), true);
	}

	private void analyzeToxicity(String input)
	{
		printTitleSeparator("Analyze toxicity", BOLD_BLUE_STYLE);
		printWhite(processor.analyzeToxicity(input), true);
	}

	private void analyzeEntity(String input)
	{
		printTitleSeparator("Analyze entity", BOLD_BLUE_STYLE);
		printWhite(processor.analyzeEntity(input), true);
	}

	private void analyzeCombined(String input) throws IOException
	{
		printTitleSeparator("Combined analysis", BOLD_BLUE_STYLE);
		String mode = validateInput("Select the desired analysis (press enter to confirm the combination choice):", COMBINED_ANALYZE_MODE_OPTIONS, true);

		List<AnalyzeOptions> opts = new ArrayList<>();

		while(!mode.isEmpty())
		{
			if(mode.equals("syntax") || mode.equals("sy"))
			{
				if(opts.contains(AnalyzeOptions.SYNTAX))
					printYellow("Option already chosen", true);
				else
				{
					opts.add(AnalyzeOptions.SYNTAX);
					printWhite("Option added: syntax", true);
				}
			}
			else if(mode.equals("sentiment") || mode.equals("se"))
			{
				if(opts.contains(AnalyzeOptions.SENTIMENT))
					printYellow("Option already chosen", true);
				else
				{
					opts.add(AnalyzeOptions.SENTIMENT);
					printWhite("Option added: sentiment", true);
				}
			}
			else if(mode.equals("toxicity") || mode.equals("t"))
			{
				if(opts.contains(AnalyzeOptions.TOXICITY))
					printYellow("Option already chosen", true);
				else
				{
					opts.add(AnalyzeOptions.TOXICITY);
					printWhite("Option added: toxicity", true);
				}
			}
			else if(mode.equals("entity") || mode.equals("e"))
			{
				if(opts.contains(AnalyzeOptions.ENTITY))
					printYellow("Option already chosen", true);
				else
				{
					opts.add(AnalyzeOptions.ENTITY);
					printWhite("Option added: entity", true);
				}
			}

			mode = validateInput("Select the next desired mode:", COMBINED_ANALYZE_MODE_OPTIONS, true);
		}

		if(opts.isEmpty())
		{
			printYellow("No options specified.", true);
			return;
		}

		for(AnalyzeOptions opt : opts)
		{
			if(opts.indexOf(opt) != 0)
				printSeparator(BOLD_BLUE_STYLE);

			printWhite("Proceeding with " + opt, true);

			switch(opt)
			{
				case SYNTAX: analyzeSyntax(input); break;
				case SENTIMENT: analyzeSentiment(input); break;
				case TOXICITY: analyzeToxicity(input); break;
				case ENTITY: analyzeEntity(input); break;
			}
		}
	}

	private void treeHandler() throws IOException
	{
		printTitleSeparator("Syntax tree procedure", BOLD_BLUE_STYLE);
		String mode = validateInput("Select what sentence do you want to print the syntactic tree:", INPUT_MODE_OPTIONS, false);

		if(mode.isEmpty())
			return;

		String userInput = new String();

		if(mode.equals("generate") || mode.equals("g"))
		{
			printWhite("Proceding generating a random sentence", true);
			userInput = processor.generateRandom();
			printWhite("Generated sentence: '" + userInput + "'", true);
		}
		else if(mode.equals("input") || mode.equals("i"))
		{
			printWhite("Insert the desired sentence", true);

			userInput = read(false);

			for(int i = MAX_ATTEMPTS - 1; i >= 0; --i)
			{
				if(i == 0)
					return;

				if(userInput.isEmpty())
				{
					printYellow("Please enter a valid Sentence. Remaining attempts " + i, true);
					userInput = read(false);
				}
				else
					break;
			}
		}
		else if(mode.equals("cached") || mode.equals("c"))
		{
			if(!processor.isSentenceCached())
			{
				printYellow("No sentence is cached, try generating one first", true);
				return;
			}

			printWhite("Proceding with the cached sentence", true);
			userInput = processor.getCachedSentence();
		}

		printTree(userInput);
	}

	private void printTree(String input) throws IOException
	{
		printTitleSeparator("Syntax tree", BOLD_BLUE_STYLE);
		printWhite(processor.generateSyntaxTree(input), true);
	}

	private void extendHandler() throws IOException
	{
		printTitleSeparator("Extension procedure", BOLD_BLUE_STYLE);
		String partOfSpeech = validateInput("Enter the part of speech that you want to add (press Enter to confirm the new terms):", ELEMENT_OPTIONS, true);

		List<Noun> nounList = new ArrayList<>();
		List<Adjective> adjectiveList = new ArrayList<>();
		List<Verb> verbList = new ArrayList<>();

		while(!partOfSpeech.isEmpty())
		{
			if(partOfSpeech.equals("noun") || partOfSpeech.equals("n"))
			{
				String num = validateInput("Enter the number for the noun: ", NUMBER_OPTIONS, false);

				if(num.isEmpty())
					return;

				Number number;
				if(num.equals("singular") || num.equals("s"))
					number = Number.SINGULAR;
				else
					number = Number.PLURAL;

				printWhite("Insert the new noun:", true);
				String text = read(true);

				for(int i = MAX_ATTEMPTS - 1; i >= 0; --i)
				{
					if(i == 0)
						return;

					if(text.isEmpty() || !text.matches("[a-zA-Z]+"))
					{
						printYellow("Please enter a valid value. Remaining attempts " + i, true);
						text = read(true);
					}
					else
						break;
				}

				nounList.add(new Noun(text, number));
			}
			else if(partOfSpeech.equals("adjective") || partOfSpeech.equals("a"))
			{
				printWhite("Insert the new adjective:", true);
				String text = read(true);

				for(int i = MAX_ATTEMPTS - 1; i >= 0; --i)
				{
					if(i == 0)
						return;

					if(text.isEmpty() || !text.matches("[a-zA-Z]+"))
					{
						printYellow("Please enter a valid value. Remaining attempts " + i, true);
						text = read(true);
					}
					else
						break;
				}

				adjectiveList.add(new Adjective(text));
			}
			else if(partOfSpeech.equals("verb") || partOfSpeech.equals("v"))
			{
				String textTense = validateInput("Enter the tense for the verb: ", TENSE_OPTIONS, false);

				if(textTense.isEmpty())
					return;

				Tense tense;

				if(textTense.equals("past") || textTense.equals("pa"))
					tense = Tense.PAST;
				else if(textTense.equals("present") || textTense.equals("pr"))
					tense = Tense.PRESENT;
				else
					tense = Tense.FUTURE;

				printWhite("Insert the new verb:", true);
				String text = read(true);

				for(int i = MAX_ATTEMPTS - 1; i >= 0; --i)
				{
					if(i == 0)
						return;

					if(text.isEmpty() || !text.matches("[a-zA-Z]+"))
					{
						printYellow("Please enter a valid value. Remaining attempts " + i, true);
						text = read(true);
					}
					else
						break;
				}

				verbList.add(new Verb(text, tense));
			}

			partOfSpeech = validateInput("Enter your next choice:", ELEMENT_OPTIONS, true);
		}

		if(nounList.isEmpty() && adjectiveList.isEmpty() && verbList.isEmpty())
			printWhite("No parts of speech added", true);
		else
			printWhite("Proceding adding the new elements", true);

		processor.append(nounList, adjectiveList, verbList);
	}

	private void setToleranceHandler()
	{
		printTitleSeparator("Tolerance procedure", BOLD_BLUE_STYLE);
		printWhite("Current tolerance value is " + processor.getTolerance(), true);
		printWhite("The tolerance parameter specifies which is the value over which an analyzed sentence is considered toxic", true);
		printWhite("Enter a new tolerance value (0.0 - 1.0): ", true);

		String newTolerance = read(true);

		for(int i = MAX_ATTEMPTS - 1; i >= 0; --i)
		{
			if(i == 0)
				return;

			try
			{
				if(newTolerance.isEmpty() || ((Float.parseFloat(newTolerance) < 0.0f || (Float.parseFloat(newTolerance) > 1.0f))))
				{
					printYellow("Please enter a valid value. Remaining attempts " + i, true);
					newTolerance = read(true);
				}
				else
					break;
			}
			catch(NumberFormatException e)
			{
				printYellow("Please enter a valid value. Remaining attempts " + i, true);
				newTolerance = read(true);
			}
		}

		printWhite("Setting the new tolerance value", true);
		float tolerance = Float.parseFloat(newTolerance);
		processor.setTolerance(tolerance);
	}

	private void extendedUsage()
	{
		printTitleSeparator("Extended commands help", BOLD_BLUE_STYLE);

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
				"Extend (e)",
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

			printGreen(command, true);

			String[] lines = description.split("\n");
			for (String line : lines)
				printWhite("    " + line, true);
		}
	}

	private void verboseHandler()
	{
		printTitleSeparator("Verbosity procedure", BOLD_BLUE_STYLE);
		printWhite("Currently verbosity is set to " + processor.isVerbose(), true);
		printWhite("Switching verbosity", true);

		processor.switchVerbosity();
	}

	private void clearTerminal()
	{
		terminal.puts(InfoCmp.Capability.clear_screen);
		print("\033[3J");

		print(initialOutput);
	}

	private void usage(PrintWriter writer)
	{
		String title = "Available Commands";

		int titlePadding = (MAX_WIDTH - title.length() - 4) / 2;
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

		writer.println(new AttributedString("=".repeat(MAX_WIDTH), BOLD_MAGENTA_STYLE).toAnsi(terminal));
		writer.flush();
	}

	private void quit()
	{
		printTitleSeparator("Terminating", BOLD_BLUE_STYLE);
		running = false;
	}

	private boolean checkInternetConnection()
	{
		printTitleSeparator("Checking internet connection", BOLD_BLUE_STYLE);
		try
		{
			InetAddress.getByName("google.com").isReachable(1000);
			printTitleSeparator("Success", BOLD_GREEN_STYLE);
			return true;

		}
		catch(Exception e)
		{
			try
			{
				InetAddress.getByName("cloudflare.com").isReachable(1000);
				printTitleSeparator("Success", BOLD_GREEN_STYLE);
				return true;
			}
			catch (Exception ex)
			{
				printTitleSeparator("Failed", BOLD_RED_STYLE);
				return false;
			}
		}
	}

	private void print(String text)
	{
		terminal.writer().print(text);
		terminal.flush();
	}

	private void prinln(String text)
	{
		terminal.writer().println(text);
		terminal.flush();
	}

	private void printMagenta(String text, boolean bold)
	{
		if(bold)
			terminal.writer().println(new AttributedString(text, BOLD_MAGENTA_STYLE).toAnsi(terminal));
		else
			terminal.writer().println(new AttributedString(text, MAGENTA_STYLE).toAnsi(terminal));

		terminal.flush();
	}

	private void printGreen(String text, boolean bold)
	{
		if(bold)
			terminal.writer().println(new AttributedString(text, BOLD_GREEN_STYLE).toAnsi(terminal));
		else
			terminal.writer().println(new AttributedString(text, GREEN_STYLE).toAnsi(terminal));

		terminal.flush();
	}

	private void printWhite(String text, boolean bold)
	{
		if(bold)
			terminal.writer().println(new AttributedString(text, BOLD_WHITE_STYLE).toAnsi(terminal));
		else
			terminal.writer().println(new AttributedString(text, WHITE_STYLE).toAnsi(terminal));

		terminal.flush();
	}

	private void printYellow(String text, boolean bold)
	{
		if(bold)
			terminal.writer().println(new AttributedString(text, BOLD_YELLOW_STYLE).toAnsi(terminal));
		else
			terminal.writer().println(new AttributedString(text, YELLOW_STYLE).toAnsi(terminal));

		terminal.flush();
	}

	private void printRed(String text, boolean bold)
	{
		if(bold)
			terminal.writer().println(new AttributedString(text, BOLD_RED_STYLE).toAnsi(terminal));
		else
			terminal.writer().println(new AttributedString(text, RED_STYLE).toAnsi(terminal));
		terminal.flush();
	}

	private void printBlue(String text, boolean bold)
	{
		if(bold)
			terminal.writer().println(new AttributedString(text, BOLD_BLUE_STYLE).toAnsi(terminal));
		else
			terminal.writer().println(new AttributedString(text, BLUE_STYLE).toAnsi(terminal));

		terminal.flush();
	}

	private String read(boolean demangled)
	{
		if(demangled)
			return plainReader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ").toLowerCase();
		else
			return plainReader.readLine(new AttributedString(">> ", BOLD_WHITE_STYLE).toAnsi(terminal)).trim().replaceAll("\\s+", " ");
	}

	private String validateInput(String prompt, List<Option> options, boolean skippable) throws IOException
	{
		printWhite(prompt, true);

		for(Option option : options)
			printWhite(" - " + option.getDisplayName() + ": " + option.getDescription(), true);

		for(int i = MAX_ATTEMPTS; i > 0; --i)
		{
			String input = read(true);

			if(input.isEmpty() && skippable)
				return "";

			Optional<Option> matchedOption = options.stream().filter(opt -> opt.matches(input)).findFirst();

			if(matchedOption.isPresent())
				return matchedOption.get().getMainCommand();

			if(i > 1)
			{
				String validOptions = options.stream().map(opt -> opt.getDisplayName() + " (" + String.join("/", opt.getAlias()) + ")").collect(Collectors.joining(", "));
				printYellow("Invalid input. Please enter one of: " + validOptions + ". Remaining attempts: " + (i - 1), true);
			}
			else
				printYellow("Maximum attempts reached. Operation cancelled.", true);
		}

		return "";
	}

	private void printSeparator(AttributedStyle style)
	{
		terminal.writer().println(new AttributedString("=".repeat(MAX_WIDTH), style).toAnsi(terminal));
		terminal.flush();
	}

	private void printTitleSeparator(String title, AttributedStyle style)
	{
		int availableSpace = MAX_WIDTH - title.length() - 4;

		int rightPadding = availableSpace / 2;
		int leftPadding = availableSpace - rightPadding;

		String separator = "=".repeat(leftPadding) + "< " + title + " >" + "=".repeat(rightPadding);

		terminal.writer().println(new AttributedString(separator, style).toAnsi(terminal));
		terminal.flush();
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
			printRed("Failed to close terminal: " + e.getMessage(), true);
			e.printStackTrace();
		}
	}
}
