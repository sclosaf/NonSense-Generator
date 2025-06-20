package unipd.nonsense.util;

import unipd.nonsense.exceptions.MissingInternetConnectionException;
import unipd.nonsense.exceptions.IllegalToleranceException;
import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Template;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Tense;

import java.io.PrintWriter;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Collections;

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

import java.net.InetAddress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Testing CLI class")
class TestCLI
{
	@Mock
	private Terminal mockTerminal;

	@Mock
	private LineReader mockCommandReader;

	@Mock
	private LineReader mockPlainReader;

	@Mock
	private PrintWriter mockPrintWriter;

	@Mock
	private CommandProcessor mockProcessor;

	private MockedStatic<TerminalBuilder> mockedTerminalBuilder;
	private MockedStatic<LineReaderBuilder> mockedLineReaderBuilder;
	private MockedStatic<InetAddress> mockedInetAddress;

	private CLI cli;

	@BeforeEach
	@DisplayName("Setup mock environment")
	void setup() throws IOException
	{
		mockedTerminalBuilder = mockStatic(TerminalBuilder.class);
		mockedLineReaderBuilder = mockStatic(LineReaderBuilder.class);
		mockedInetAddress = mockStatic(InetAddress.class);

		TerminalBuilder terminalBuilder = mock(TerminalBuilder.class);
		when(TerminalBuilder.builder()).thenReturn(terminalBuilder);
		when(terminalBuilder.system(anyBoolean())).thenReturn(terminalBuilder);
		when(terminalBuilder.build()).thenReturn(mockTerminal);

		LineReaderBuilder lineReaderBuilder = mock(LineReaderBuilder.class);
		when(LineReaderBuilder.builder()).thenReturn(lineReaderBuilder);
		when(lineReaderBuilder.terminal(any())).thenReturn(lineReaderBuilder);
		when(lineReaderBuilder.completer(any())).thenReturn(lineReaderBuilder);
		when(lineReaderBuilder.option(any(), anyBoolean())).thenReturn(lineReaderBuilder);
		when(lineReaderBuilder.variable(anyString(), any())).thenReturn(lineReaderBuilder);
		when(lineReaderBuilder.highlighter(any())).thenReturn(lineReaderBuilder);
		when(lineReaderBuilder.build()).thenReturn(mockCommandReader, mockPlainReader);

		when(mockTerminal.writer()).thenReturn(mockPrintWriter);

		InetAddress mockInetAddress = mock(InetAddress.class);
		when(InetAddress.getByName(anyString())).thenReturn(mockInetAddress);
		when(mockInetAddress.isReachable(anyInt())).thenReturn(true);

		cli = new CLI();

		try
		{
			java.lang.reflect.Field field = CLI.class.getDeclaredField("processor");
			field.setAccessible(true);
			field.set(cli, mockProcessor);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Failed to inject mock processor", e);
		}
	}

	@AfterEach
	@DisplayName("Cleanup mock environment")
	void tearDown()
	{
		if(cli != null)
			cli.closeResources();

		mockedTerminalBuilder.close();
		mockedLineReaderBuilder.close();
		mockedInetAddress.close();
	}

	@Test
	@DisplayName("Test successful CLI initialization")
	void testConstructor_Success()
	{
		assertNotNull(cli, "CLI instance should be created");
		when(mockCommandReader.readLine(anyString())).thenReturn("help");
		assertTrue(cli.inputCatcher(), "CLI should be running after initialization");
	}

	@Test
	@DisplayName("Test CLI quit command")
	void testQuitCommand()
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("quit");
		assertFalse(cli.inputCatcher(), "CLI should stop running after quit command");
	}

	@Test
	@DisplayName("Test default handler with empty input")
	void testDefaultHandler_EmptyInput() throws IOException
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("default");
		when(mockPlainReader.readLine(anyString())).thenReturn("");

		String generatedSentence = "This is a generated sentence";
		when(mockProcessor.generateRandom()).thenReturn(generatedSentence);
		when(mockProcessor.analyzeSyntax(generatedSentence)).thenReturn("Syntax analysis result");
		when(mockProcessor.analyzeToxicity(generatedSentence)).thenReturn("Toxicity analysis result");
		when(mockProcessor.generateSyntaxTree(generatedSentence)).thenReturn("Syntax tree result");
		assertTrue(cli.inputCatcher());

		verify(mockPrintWriter, atLeastOnce()).println(contains("Generated sentence"));
		verify(mockProcessor).analyzeSyntax(generatedSentence);
		verify(mockProcessor).analyzeToxicity(generatedSentence);
	}

	@Test
	@DisplayName("Test default handler with user input")
	void testDefaultHandler_WithInput() throws IOException
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("default");
		when(mockPlainReader.readLine(anyString())).thenReturn("User input sentence");

		String generatedSentence = "Generated from user input";
		when(mockProcessor.generateFrom(anyString())).thenReturn(generatedSentence);
		when(mockProcessor.analyzeSyntax(anyString())).thenReturn("Syntax analysis result");
		when(mockProcessor.analyzeToxicity(anyString())).thenReturn("Toxicity analysis result");
		when(mockProcessor.generateSyntaxTree(anyString())).thenReturn("Syntax tree result");
		assertTrue(cli.inputCatcher());

		verify(mockProcessor).generateFrom("User input sentence");
		verify(mockProcessor).analyzeSyntax("User input sentence");
		verify(mockProcessor).analyzeSyntax(generatedSentence);
		verify(mockProcessor).analyzeToxicity(generatedSentence);
	}

	@Test
	@DisplayName("Test generate handler - random option")
	void testGenerateHandler_Random() throws IOException
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("generate");
		when(mockPlainReader.readLine(anyString())).thenReturn("random");

		String generatedSentence = "Random generated sentence";
		when(mockProcessor.generateRandom()).thenReturn(generatedSentence);

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).generateRandom();
		verify(mockPrintWriter).println(contains(generatedSentence));
	}

	@Test
	@DisplayName("Test generate handler - number option")
	void testGenerateHandler_Number() throws IOException
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("generate");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("number")
			.thenReturn("singular");

		String generatedSentence = "Singular generated sentence";
		when(mockProcessor.generateWithNumber(any())).thenReturn(generatedSentence);

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).generateWithNumber(Number.SINGULAR);
		verify(mockPrintWriter).println(contains(generatedSentence));
	}

	@Test
	@DisplayName("Test analyze handler - syntax option")
	void testAnalyzeHandler_Syntax() throws IOException
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("analyze");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("syntax")
			.thenReturn("input")
			.thenReturn("Test sentence");

		when(mockProcessor.analyzeSyntax(anyString())).thenReturn("Syntax analysis result");

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).analyzeSyntax("Test sentence");
		verify(mockPrintWriter).println(contains("Syntax analysis result"));
	}

	@Test
	@DisplayName("Test analyze handler - all option")
	void testAnalyzeHandler_All() throws IOException
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("analyze");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("all")
			.thenReturn("input")
			.thenReturn("Test sentence");

		when(mockProcessor.analyzeSyntax(anyString())).thenReturn("Syntax result");
		when(mockProcessor.analyzeSentiment(anyString())).thenReturn("Sentiment result");
		when(mockProcessor.analyzeToxicity(anyString())).thenReturn("Toxicity result");
		when(mockProcessor.analyzeEntity(anyString())).thenReturn("Entity result");

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).analyzeSyntax("Test sentence");
		verify(mockProcessor).analyzeSentiment("Test sentence");
		verify(mockProcessor).analyzeToxicity("Test sentence");
		verify(mockProcessor).analyzeEntity("Test sentence");
	}

	@Test
	@DisplayName("Test tree handler")
	void testTreeHandler() throws IOException
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("tree");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("input")
			.thenReturn("Test sentence");

		when(mockProcessor.generateSyntaxTree(anyString())).thenReturn("Syntax tree");

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).generateSyntaxTree("Test sentence");
		verify(mockPrintWriter, times(3)).println(contains("Syntax tree"));
	}

	@Test
	@DisplayName("Test set tolerance handler")
	void testSetToleranceHandler() throws IOException
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("set tolerance");
		when(mockPlainReader.readLine(anyString())).thenReturn("0.5");

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).setTolerance(0.5f);
	}

	@Test
	@DisplayName("Test set tolerance handler with invalid input")
	void testSetToleranceHandler_InvalidInput() throws IOException
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("set tolerance");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("invalid")
			.thenReturn("1.5")
			.thenReturn("0.8");

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).setTolerance(0.8f);
	}

	@Test
	@DisplayName("Test verbose handler")
	void testVerboseHandler()
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("verbose");
		when(mockProcessor.isVerbose()).thenReturn(false);

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).switchVerbosity();
	}

	@Test
	@DisplayName("Test clear terminal")
	void testClearTerminal()
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("clear");

		assertTrue(cli.inputCatcher());
		verify(mockTerminal).puts(any());
		verify(mockPrintWriter, atLeast(2)).print(anyString());
	}

	@Test
	@DisplayName("Test help command")
	void testHelpCommand()
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("help");

		assertTrue(cli.inputCatcher());
		verify(mockPrintWriter, atLeastOnce()).println(contains("Available Commands"));
	}

	@Test
	@DisplayName("Test info command")
	void testInfoCommand()
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("info");

		assertTrue(cli.inputCatcher());
		verify(mockPrintWriter, atLeastOnce()).println(contains("Extended commands help"));
	}

	@Test
	@DisplayName("Test invalid command")
	void testInvalidCommand()
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("invalid");

		assertTrue(cli.inputCatcher());
		verify(mockPrintWriter).println(contains("Invalid command"));
	}

	@Test
	@DisplayName("Test user interrupt")
	void testUserInterrupt()
	{
		when(mockCommandReader.readLine(anyString())).thenThrow(new UserInterruptException("Ctrl + D"));

		assertFalse(cli.inputCatcher());
		verify(mockPrintWriter).println(contains("Program ended"));
	}

	@Test
	@DisplayName("Test end of file")
	void testEndOfFile()
	{
		when(mockCommandReader.readLine(anyString())).thenThrow(new EndOfFileException());

		assertFalse(cli.inputCatcher());
		verify(mockPrintWriter).println(contains("Program ended"));
	}

	@Test
	@DisplayName("Test analyze combined handler with multiple options")
	void testAnalyzeCombinedHandler() throws IOException {
		when(mockCommandReader.readLine(anyString())).thenReturn("analyze");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("combined")
			.thenReturn("input")
			.thenReturn("Test sentence")
			.thenReturn("syntax")
			.thenReturn("sentiment")
			.thenReturn("");

		when(mockProcessor.analyzeSyntax(anyString())).thenReturn("Syntax result");
		when(mockProcessor.analyzeSentiment(anyString())).thenReturn("Sentiment result");

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).analyzeSyntax("Test sentence");
		verify(mockProcessor).analyzeSentiment("Test sentence");
		verify(mockProcessor, never()).analyzeToxicity(anyString());
		verify(mockProcessor, never()).analyzeEntity(anyString());
	}

	@Test
	@DisplayName("Test generate handler - tense option")
	void testGenerateHandler_Tense() throws IOException {
		when(mockCommandReader.readLine(anyString())).thenReturn("generate");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("tense")
			.thenReturn("future");

		String generatedSentence = "Future generated sentence";
		when(mockProcessor.generateWithTense(any())).thenReturn(generatedSentence);

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).generateWithTense(Tense.FUTURE);
		verify(mockPrintWriter).println(contains(generatedSentence));
	}

	@Test
	@DisplayName("Test generate handler - both option")
	void testGenerateHandler_Both() throws IOException {
		when(mockCommandReader.readLine(anyString())).thenReturn("generate");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("both")
			.thenReturn("plural")
			.thenReturn("past");

		String generatedSentence = "Plural past generated sentence";
		when(mockProcessor.generateWithBoth(any(), any())).thenReturn(generatedSentence);

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).generateWithBoth(Number.PLURAL, Tense.PAST);
		verify(mockPrintWriter).println(contains(generatedSentence));
	}

	@Test
	@DisplayName("Test analyze handler with cached sentence")
	void testAnalyzeHandler_Cached() throws IOException {
		when(mockCommandReader.readLine(anyString())).thenReturn("analyze");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("syntax")
			.thenReturn("cached");

		String cachedSentence = "Cached sentence";
		when(mockProcessor.isSentenceCached()).thenReturn(true);
		when(mockProcessor.getCachedSentence()).thenReturn(cachedSentence);
		when(mockProcessor.analyzeSyntax(anyString())).thenReturn("Cached analysis");

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).analyzeSyntax(cachedSentence);
	}

	@Test
	@DisplayName("Test analyze handler with template choice")
	void testAnalyzeHandler_TemplateChoice() throws IOException {
		when(mockCommandReader.readLine(anyString())).thenReturn("analyze");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("syntax")
			.thenReturn("choose")
			.thenReturn("1");

		Template mockTemplate = mock(Template.class);
		List<Template> templates = List.of(mockTemplate);
		when(mockProcessor.getRandomTemplates()).thenReturn(templates);

		String generatedSentence = "Template generated sentence";
		when(mockProcessor.generateWithTemplate(any())).thenReturn(generatedSentence);
		when(mockProcessor.analyzeSyntax(anyString())).thenReturn("Template analysis");

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).generateWithTemplate(mockTemplate);
		verify(mockProcessor).analyzeSyntax(generatedSentence);
	}

	@Test
	@DisplayName("Test tree handler with cached sentence")
	void testTreeHandler_Cached() throws IOException {
		when(mockCommandReader.readLine(anyString())).thenReturn("tree");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("cached");

		String cachedSentence = "Cached sentence";
		when(mockProcessor.isSentenceCached()).thenReturn(true);
		when(mockProcessor.getCachedSentence()).thenReturn(cachedSentence);
		when(mockProcessor.generateSyntaxTree(anyString())).thenReturn("Cached tree");

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).generateSyntaxTree(cachedSentence);
	}

	@Test
	@DisplayName("Test input validation with empty allowed input")
	void testInputValidation_EmptyAllowed() throws IOException
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("generate");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("")
			.thenReturn("random");
		when(mockProcessor.generateRandom()).thenReturn("Test sentence");

		assertTrue(cli.inputCatcher());
		verify(mockPrintWriter).println(contains("Test sentence"));
	}

	@Test
	@DisplayName("Test input validation with invalid then valid input")
	void testInputValidation_InvalidThenValid() throws IOException
	{
		when(mockCommandReader.readLine(anyString())).thenReturn("generate");
		when(mockPlainReader.readLine(anyString()))
			.thenReturn("invalid")
			.thenReturn("number")
			.thenReturn("singular");
		when(mockProcessor.generateWithNumber(any())).thenReturn("Valid sentence");

		assertTrue(cli.inputCatcher());
		verify(mockPrintWriter).println(contains("Valid sentence"));
	}

	@Test
	@DisplayName("Test analyze random handler")
	void testAnalyzeRandomHandler() throws IOException
	{
		when(mockCommandReader.readLine(anyString()))
			.thenReturn("analyze")
			.thenReturn("quit");

		when(mockPlainReader.readLine(anyString()))
			.thenReturn("random")
			.thenReturn("input")
			.thenReturn("Test sentence");


		when(mockProcessor.analyzeSyntax(anyString())).thenReturn("result");
		when(mockProcessor.analyzeSentiment(anyString())).thenReturn("result");
		when(mockProcessor.analyzeToxicity(anyString())).thenReturn("result");
		when(mockProcessor.analyzeEntity(anyString())).thenReturn("result");

		assertTrue(cli.inputCatcher());
	}

	@Test
	@DisplayName("Test extend handler with nouns")
	void testExtendHandler_Nouns() throws IOException
	{
		when(mockCommandReader.readLine(anyString()))
			.thenReturn("extend")
			.thenReturn("quit");

		when(mockPlainReader.readLine(anyString()))
			.thenReturn("noun")
			.thenReturn("singular")
			.thenReturn("computer")
			.thenReturn("");

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).append(argThat(list -> !list.isEmpty()), argThat(List::isEmpty), argThat(List::isEmpty));
	}

	@Test
	@DisplayName("Test extend handler with adjectives")
	void testExtendHandler_Adjectives() throws IOException
	{
		when(mockCommandReader.readLine(anyString()))
			.thenReturn("extend")
			.thenReturn("quit");

		when(mockPlainReader.readLine(anyString()))
			.thenReturn("adjective")
			.thenReturn("red")
			.thenReturn("");

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).append(argThat(List::isEmpty), argThat(list -> !list.isEmpty()), argThat(List::isEmpty));
	}

	@Test
	@DisplayName("Test extend handler with verbs")
	void testExtendHandler_Verbs() throws IOException
	{
		when(mockCommandReader.readLine(anyString()))
			.thenReturn("extend")
			.thenReturn("quit");

		when(mockPlainReader.readLine(anyString()))
			.thenReturn("verb")
			.thenReturn("past")
			.thenReturn("plural")
			.thenReturn("ran")
			.thenReturn("");

		assertTrue(cli.inputCatcher());
    verify(mockProcessor).append(argThat(List::isEmpty), argThat(List::isEmpty), argThat(list -> !list.isEmpty()));
	}

	@Test
	@DisplayName("Test extend handler with multiple parts of speech")
	void testExtendHandler_MultipleParts() throws IOException
	{
		when(mockCommandReader.readLine(anyString()))
			.thenReturn("extend")
			.thenReturn("quit");

		when(mockPlainReader.readLine(anyString()))
			.thenReturn("noun")
			.thenReturn("plural")
			.thenReturn("computers")
			.thenReturn("adjective")
			.thenReturn("fast")
			.thenReturn("verb")
			.thenReturn("present")
			.thenReturn("plural")
			.thenReturn("run")
			.thenReturn("");

		assertTrue(cli.inputCatcher());
		verify(mockProcessor).append(argThat(list -> !list.isEmpty()), argThat(list -> !list.isEmpty()), argThat(list -> !list.isEmpty()));
	}

	@Test
	@DisplayName("Test input validation with max attempts")
	void testInputValidation_MaxAttempts() throws IOException
	{
		when(mockCommandReader.readLine(anyString()))
			.thenReturn("generate")
			.thenReturn("quit");

		when(mockPlainReader.readLine(anyString()))
			.thenReturn("invalid1")
			.thenReturn("invalid2")
			.thenReturn("invalid3")
			.thenReturn("invalid4")
			.thenReturn("random");

		when(mockProcessor.generateRandom()).thenReturn("Fallback sentence");

		assertTrue(cli.inputCatcher());

		verify(mockPrintWriter).println(contains("Maximum attempts reached. Operation cancelled."));
		verify(mockProcessor, never()).generateRandom();
	}
}
