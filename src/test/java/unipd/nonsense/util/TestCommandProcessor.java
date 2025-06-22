package unipd.nonsense.util;

import unipd.nonsense.generator.SyntaxTreeBuilder;
import unipd.nonsense.model.SyntaxToken;
import unipd.nonsense.analyzer.SentenceAnalyzer;
import unipd.nonsense.analyzer.ToxicityValidator;
import unipd.nonsense.generator.SentenceGenerator;
import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Tense;
import unipd.nonsense.model.Template;

import unipd.nonsense.exceptions.SentenceNotCachedException;
import unipd.nonsense.exceptions.IllegalToleranceException;
import unipd.nonsense.exceptions.InvalidNumberException;
import unipd.nonsense.exceptions.InvalidTenseException;
import unipd.nonsense.exceptions.InvalidTextException;
import unipd.nonsense.exceptions.InvalidTemplateException;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

import com.google.cloud.language.v1.PartOfSpeech;
import com.google.cloud.language.v1.DependencyEdge;
import com.google.cloud.language.v1.DependencyEdge.Label;
import com.google.cloud.language.v1.PartOfSpeech;
import com.google.cloud.language.v1.PartOfSpeech.Tag;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testing CommandProcessor.")
class TestCommandProcessor
{
	@Mock
	private SyntaxTreeBuilder mockTreeBuilder;

	@Mock
	private SentenceAnalyzer mockAnalyzer;

	@Mock
	private ToxicityValidator mockValidator;

	@Mock
	private SentenceGenerator mockGenerator;

	@Mock
	private Template mockTemplate;

	private CommandProcessor commandProcessor;

	private MockedStatic<JsonUpdater> mockedJsonUpdater;

	@BeforeEach
	@DisplayName("Setup mock environment.")
	void setup() throws IOException
	{
		mockedJsonUpdater = mockStatic(JsonUpdater.class);

		commandProcessor = spy(new CommandProcessor());

		setPrivateField(commandProcessor, "treeBuilder", mockTreeBuilder);
		setPrivateField(commandProcessor, "analyzer", mockAnalyzer);
		setPrivateField(commandProcessor, "validator", mockValidator);
		setPrivateField(commandProcessor, "generator", mockGenerator);
	}

	@AfterEach
	@DisplayName("Removing mock environment used for testing.")
	void tearDown()
	{
		if(commandProcessor != null)
			commandProcessor.close();

		mockedJsonUpdater.close();
	}

	private void setPrivateField(Object target, String fieldName, Object value)
	{
		try
		{
			java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Failed to set private field: " + fieldName, e);
		}
	}

	@Test
	@DisplayName("Test successful CommandProcessor creation.")
	void testConstructor_Success()
	{
		assertNotNull(commandProcessor);
		assertEquals(0.7f, commandProcessor.getTolerance(), 0.001f, "Default tolerance should be 0.7");
		assertFalse(commandProcessor.isSentenceCached(), "No sentence should be cached initially");
	}

	@Test
	@DisplayName("Test generateRandom success.")
	void testGenerateRandom_Success()
	{
		Template mockResult = mock(Template.class);
		when(mockResult.getPattern()).thenReturn("Random sentence generated");
		when(mockGenerator.generateRandomSentence()).thenReturn(mockResult);

		String result = commandProcessor.generateRandom();

		assertEquals("Random sentence generated", result, "Should return the generated random sentence");
		assertTrue(commandProcessor.isSentenceCached(), "Sentence should be cached after generation");
		assertEquals("Random sentence generated", commandProcessor.getCachedSentence(), "Cached sentence should match generated sentence");
		verify(mockGenerator, times(1)).generateRandomSentence();
	}

	@Test
	@DisplayName("Test generateFrom with valid input.")
	void testGenerateFrom_Success() throws IOException
	{
		String inputText = "The beautiful cat runs quickly";
		List<SyntaxToken> mockTokens = createMockTokens();
		Template mockResult = mock(Template.class);
		when(mockResult.getPattern()).thenReturn("Generated sentence from input");

		when(mockAnalyzer.getSyntaxTokensAsync(inputText)).thenReturn(CompletableFuture.completedFuture(mockTokens));
		when(mockGenerator.generateSentenceWith(any(List.class), any(List.class), any(List.class))).thenReturn(mockResult);

		String result = commandProcessor.generateFrom(inputText);

		assertEquals("Generated sentence from input", result, "Should return generated sentence from input");
		assertTrue(commandProcessor.isSentenceCached(), "Sentence should be cached");
		verify(mockAnalyzer, times(1)).getSyntaxTokensAsync(inputText);
		verify(mockGenerator, times(1)).generateSentenceWith(any(List.class), any(List.class), any(List.class));
	}

	@Test
	@DisplayName("Test generateFrom with null input throws exception.")
	void testGenerateFrom_NullInput()
	{
		assertThrows(InvalidTextException.class, () -> commandProcessor.generateFrom(null),
			"Should throw InvalidTextException for null input");
	}

	@Test
	@DisplayName("Test generateWithNumber success.")
	void testGenerateWithNumber_Success()
	{
		Template mockResult = mock(Template.class);
		when(mockResult.getPattern()).thenReturn("Singular sentence generated");
		when(mockGenerator.generateSentenceWithNumber(Number.SINGULAR)).thenReturn(mockResult);

		String result = commandProcessor.generateWithNumber(Number.SINGULAR);

		assertEquals("Singular sentence generated", result, "Should return sentence with specified number");
		assertTrue(commandProcessor.isSentenceCached(), "Sentence should be cached");
		verify(mockGenerator, times(1)).generateSentenceWithNumber(Number.SINGULAR);
	}

	@Test
	@DisplayName("Test generateWithTense success.")
	void testGenerateWithTense_Success()
	{
		Template mockResult = mock(Template.class);
		when(mockResult.getPattern()).thenReturn("Past tense sentence generated");
		when(mockGenerator.generateSentenceWithTense(Tense.PAST)).thenReturn(mockResult);

		String result = commandProcessor.generateWithTense(Tense.PAST);

		assertEquals("Past tense sentence generated", result, "Should return sentence with specified tense");
		assertTrue(commandProcessor.isSentenceCached(), "Sentence should be cached");
		verify(mockGenerator, times(1)).generateSentenceWithTense(Tense.PAST);
	}

	@Test
	@DisplayName("Test generateWithBoth success.")
	void testGenerateWithBoth_Success()
	{
		Template mockResult = mock(Template.class);
		when(mockResult.getPattern()).thenReturn("Past singular sentence generated");
		when(mockGenerator.generateSentenceWithTenseAndNumber(Tense.PAST, Number.SINGULAR)).thenReturn(mockResult);

		String result = commandProcessor.generateWithBoth(Number.SINGULAR, Tense.PAST);

		assertEquals("Past singular sentence generated", result, "Should return sentence with both number and tense");
		assertTrue(commandProcessor.isSentenceCached(), "Sentence should be cached");
		verify(mockGenerator, times(1)).generateSentenceWithTenseAndNumber(Tense.PAST, Number.SINGULAR);
	}

	@Test
	@DisplayName("Test generateWithTemplate success.")
	void testGenerateWithTemplate_Success()
	{
		Template mockResult = mock(Template.class);
		when(mockResult.getPattern()).thenReturn("Template sentence generated");
		when(mockGenerator.generateSentenceFromTemplate(mockTemplate)).thenReturn(mockResult);

		String result = commandProcessor.generateWithTemplate(mockTemplate);

		assertEquals("Template sentence generated", result, "Should return sentence from template");
		assertTrue(commandProcessor.isSentenceCached(), "Sentence should be cached");
		verify(mockGenerator, times(1)).generateSentenceFromTemplate(mockTemplate);
	}

	@Test
	@DisplayName("Test generateWithTemplate with null template throws exception.")
	void testGenerateWithTemplate_NullTemplate()
	{
		assertThrows(InvalidTemplateException.class, () -> commandProcessor.generateWithTemplate(null),
			"Should throw InvalidTemplateException for null template");
	}

	@Test
	@DisplayName("Test getRandomTemplates success.")
	void testGetRandomTemplates_Success()
	{
		List<Template> mockTemplates = List.of(mockTemplate, mockTemplate);
		when(mockGenerator.getRandomTemplates()).thenReturn(mockTemplates);

		List<Template> result = commandProcessor.getRandomTemplates();

		assertEquals(2, result.size(), "Should return correct number of templates");
		verify(mockGenerator, times(1)).getRandomTemplates();
	}

	@Test
	@DisplayName("Test generateSyntaxTree success.")
	void testGenerateSyntaxTree_Success() throws IOException
	{
		String inputText = "Test sentence";
		List<SyntaxToken> mockTokens = createMockTokens();
		String expectedTree = "Syntax tree representation";

		try(MockedStatic<SyntaxTreeBuilder> mockedTreeBuilder = mockStatic(SyntaxTreeBuilder.class))
		{
			mockedTreeBuilder.when(() -> SyntaxTreeBuilder.getSyntaxTree(mockTokens))
				.thenReturn(expectedTree);

			when(mockAnalyzer.getSyntaxTokensAsync(inputText))
				.thenReturn(CompletableFuture.completedFuture(mockTokens));

			String result = commandProcessor.generateSyntaxTree(inputText);

			assertEquals(expectedTree, result);
			assertEquals(inputText, commandProcessor.getCachedSentence());
			verify(mockAnalyzer, times(1)).getSyntaxTokensAsync(inputText);
			mockedTreeBuilder.verify(() -> SyntaxTreeBuilder.getSyntaxTree(mockTokens));
		}
	}

	@Test
	@DisplayName("Test generateSyntaxTree with null input throws exception.")
	void testGenerateSyntaxTree_NullInput()
	{
		assertThrows(InvalidTextException.class, () -> commandProcessor.generateSyntaxTree(null),
			"Should throw InvalidTextException for null input");
	}

	@Test
	@DisplayName("Test analyzeSyntax success.")
	void testAnalyzeSyntax_Success()
	{
		String inputText = "Test sentence";
		String expectedAnalysis = "Syntax analysis result";

		when(mockAnalyzer.analyzeSyntaxAsync(inputText)).thenReturn(CompletableFuture.completedFuture(expectedAnalysis));

		String result = commandProcessor.analyzeSyntax(inputText);

		assertEquals(expectedAnalysis, result, "Should return syntax analysis result");
		assertEquals(inputText, commandProcessor.getCachedSentence(), "Input text should be cached");
		verify(mockAnalyzer, times(1)).analyzeSyntaxAsync(inputText);
	}

	@Test
	@DisplayName("Test analyzeSyntax with null input throws exception.")
	void testAnalyzeSyntax_NullInput()
	{
		assertThrows(InvalidTextException.class, () -> commandProcessor.analyzeSyntax(null),
			"Should throw InvalidTextException for null input");
	}

	@Test
	@DisplayName("Test analyzeSentiment success.")
	void testAnalyzeSentiment_Success()
	{
		String inputText = "Test sentence";
		String expectedSentiment = "Positive sentiment";

		when(mockAnalyzer.analyzeSentimentAsync(inputText)).thenReturn(CompletableFuture.completedFuture(expectedSentiment));

		String result = commandProcessor.analyzeSentiment(inputText);

		assertEquals(expectedSentiment, result, "Should return sentiment analysis result");
		assertEquals(inputText, commandProcessor.getCachedSentence(), "Input text should be cached");
		verify(mockAnalyzer, times(1)).analyzeSentimentAsync(inputText);
	}

	@Test
	@DisplayName("Test analyzeSentiment with null input throws exception.")
	void testAnalyzeSentiment_NullInput()
	{
		assertThrows(InvalidTextException.class, () -> commandProcessor.analyzeSentiment(null),
			"Should throw InvalidTextException for null input");
	}

	@Test
	@DisplayName("Test analyzeEntity success.")
	void testAnalyzeEntity_Success()
	{
		String inputText = "Test sentence";
		String expectedEntities = "Entity analysis result";

		when(mockAnalyzer.analyzeEntitiesAsync(inputText)).thenReturn(CompletableFuture.completedFuture(expectedEntities));

		String result = commandProcessor.analyzeEntity(inputText);

		assertEquals(expectedEntities, result, "Should return entity analysis result");
		assertEquals(inputText, commandProcessor.getCachedSentence(), "Input text should be cached");
		verify(mockAnalyzer, times(1)).analyzeEntitiesAsync(inputText);
	}

	@Test
	@DisplayName("Test analyzeEntity with null input throws exception.")
	void testAnalyzeEntity_NullInput()
	{
		assertThrows(InvalidTextException.class, () -> commandProcessor.analyzeEntity(null),
			"Should throw InvalidTextException for null input");
	}

	@Test
	@DisplayName("Test analyzeToxicity success with non-toxic text.")
	void testAnalyzeToxicity_NonToxic()
	{
		String inputText = "This is a nice sentence";
		String toxicityReport = "Toxicity score: 0.1";

		when(mockValidator.getToxicityReportAsync(inputText)).thenReturn(CompletableFuture.completedFuture(toxicityReport));
		when(mockValidator.isTextToxicAsync(inputText, 0.7f)).thenReturn(CompletableFuture.completedFuture(false));

		String result = commandProcessor.analyzeToxicity(inputText);

		assertTrue(result.contains(toxicityReport), "Should contain toxicity report");
		assertTrue(result.contains("Text within acceptable parameters"), "Should indicate text is acceptable");
		assertTrue(result.contains("0.7"), "Should show tolerance threshold");
		assertEquals(inputText, commandProcessor.getCachedSentence(), "Input text should be cached");
		verify(mockValidator, times(1)).getToxicityReportAsync(inputText);
		verify(mockValidator, times(1)).isTextToxicAsync(inputText, 0.7f);
	}

	@Test
	@DisplayName("Test analyzeToxicity success with toxic text.")
	void testAnalyzeToxicity_Toxic()
	{
		String inputText = "This is toxic content";
		String toxicityReport = "Toxicity score: 0.9";

		when(mockValidator.getToxicityReportAsync(inputText)).thenReturn(CompletableFuture.completedFuture(toxicityReport));
		when(mockValidator.isTextToxicAsync(inputText, 0.7f)).thenReturn(CompletableFuture.completedFuture(true));

		String result = commandProcessor.analyzeToxicity(inputText);

		assertTrue(result.contains(toxicityReport), "Should contain toxicity report");
		assertTrue(result.contains("TEXT FLAGGED AS POTENTIALLY INAPPROPRIATE"), "Should flag as inappropriate");
		assertTrue(result.contains("0.7"), "Should show tolerance threshold");
		verify(mockValidator, times(1)).getToxicityReportAsync(inputText);
		verify(mockValidator, times(1)).isTextToxicAsync(inputText, 0.7f);
	}

	@Test
	@DisplayName("Test analyzeToxicity with null input throws exception.")
	void testAnalyzeToxicity_NullInput()
	{
		assertThrows(InvalidTextException.class, () -> commandProcessor.analyzeToxicity(null),
			"Should throw InvalidTextException for null input");
	}

	@Test
	@DisplayName("Test append words to dictionary.")
	void testAppend_Success() throws IOException
	{
		List<Noun> nouns = List.of(new Noun("cat", Number.SINGULAR));
		List<Adjective> adjectives = List.of(new Adjective("beautiful"));
		List<Verb> verbs = List.of(new Verb("run", Number.SINGULAR, Tense.PRESENT));

		commandProcessor.append(nouns, adjectives, verbs);

		mockedJsonUpdater.verify(() -> JsonUpdater.loadNoun(any(Noun.class)), times(1));
		mockedJsonUpdater.verify(() -> JsonUpdater.loadAdjective(any(Adjective.class)), times(1));
		mockedJsonUpdater.verify(() -> JsonUpdater.loadVerb(any(Verb.class)), times(1));
	}

	@Test
	@DisplayName("Test append with empty lists.")
	void testAppend_EmptyLists() throws IOException
	{
		List<Noun> emptyNouns = new ArrayList<>();
		List<Adjective> emptyAdjectives = new ArrayList<>();
		List<Verb> emptyVerbs = new ArrayList<>();

		commandProcessor.append(emptyNouns, emptyAdjectives, emptyVerbs);

		mockedJsonUpdater.verify(() -> JsonUpdater.loadNoun(any(Noun.class)), never());
		mockedJsonUpdater.verify(() -> JsonUpdater.loadAdjective(any(Adjective.class)), never());
		mockedJsonUpdater.verify(() -> JsonUpdater.loadVerb(any(Verb.class)), never());
	}

	@Test
	@DisplayName("Test getTolerance returns correct value.")
	void testGetTolerance_Success()
	{
		float tolerance = commandProcessor.getTolerance();
		assertEquals(0.7f, tolerance, 0.001f, "Should return default tolerance of 0.7");
	}

	@Test
	@DisplayName("Test setTolerance with valid value.")
	void testSetTolerance_ValidValue()
	{
		commandProcessor.setTolerance(0.5f);
		assertEquals(0.5f, commandProcessor.getTolerance(), 0.001f, "Should set tolerance to 0.5");
	}

	@Test
	@DisplayName("Test setTolerance with invalid low value throws exception.")
	void testSetTolerance_InvalidLowValue()
	{
		assertThrows(IllegalToleranceException.class, () -> commandProcessor.setTolerance(-0.1f),
			"Should throw IllegalToleranceException for negative tolerance");
	}

	@Test
	@DisplayName("Test setTolerance with invalid high value throws exception.")
	void testSetTolerance_InvalidHighValue()
	{
		assertThrows(IllegalToleranceException.class, () -> commandProcessor.setTolerance(1.1f),
			"Should throw IllegalToleranceException for tolerance above 1.0");
	}

	@Test
	@DisplayName("Test isSentenceCached initially false.")
	void testIsSentenceCached_InitiallyFalse()
	{
		assertFalse(commandProcessor.isSentenceCached(), "Should initially return false");
	}

	@Test
	@DisplayName("Test getCachedSentence throws exception when no sentence cached.")
	void testGetCachedSentence_NotCached()
	{
		assertThrows(SentenceNotCachedException.class, () -> commandProcessor.getCachedSentence(),
			"Should throw SentenceNotCachedException when no sentence is cached");
	}

	@Test
	@DisplayName("Test getCachedSentence returns cached sentence.")
	void testGetCachedSentence_Success()
	{
		Template mockResult = mock(Template.class);
		when(mockResult.getPattern()).thenReturn("Cached sentence");
		when(mockGenerator.generateRandomSentence()).thenReturn(mockResult);

		commandProcessor.generateRandom();
		String cached = commandProcessor.getCachedSentence();

		assertEquals("Cached sentence", cached, "Should return the cached sentence");
	}

	@Test
	@DisplayName("Test switchVerbosity functionality.")
	void testSwitchVerbosity_Success()
	{
		assertDoesNotThrow(() -> commandProcessor.switchVerbosity(), "Should not throw exception when switching verbosity");
	}

	@Test
	@DisplayName("Test isVerbose functionality.")
	void testIsVerbose_Success()
	{
		assertDoesNotThrow(() -> commandProcessor.isVerbose(), "Should not throw exception when checking verbosity");
	}

	private List<SyntaxToken> createMockTokens()
	{
		List<SyntaxToken> tokens = new ArrayList<>();

		SyntaxToken nounToken = new SyntaxToken("cat", 0, "cat",
			PartOfSpeech.newBuilder()
			.setTag(PartOfSpeech.Tag.NOUN)
			.setNumber(PartOfSpeech.Number.SINGULAR)
			.build(),
			0, DependencyEdge.Label.UNKNOWN);

		tokens.add(nounToken);

		SyntaxToken adjToken = new SyntaxToken("beautiful", 0, "beautiful",
			PartOfSpeech.newBuilder()
			.setTag(PartOfSpeech.Tag.ADJ)
			.build(),
			1, DependencyEdge.Label.AMOD);


		tokens.add(adjToken);

		SyntaxToken verbToken = new SyntaxToken("runs", 0, "run",
			PartOfSpeech.newBuilder()
			.setTag(PartOfSpeech.Tag.VERB)
			.setTense(PartOfSpeech.Tense.PRESENT)
			.setNumber(PartOfSpeech.Number.SINGULAR)
			.build(),
			0, DependencyEdge.Label.ROOT);

		tokens.add(verbToken);

		return tokens;
	}

	@Test
	@DisplayName("Test generateFrom with empty input string.")
	void testGenerateFrom_EmptyInput()
	{
		assertThrows(InvalidTextException.class, () -> commandProcessor.generateFrom(""),
			"Should throw InvalidTextException for empty input string");
	}

	@Test
	@DisplayName("Test generateFrom with input containing only punctuation.")
	void testGenerateFrom_PunctuationOnly() throws IOException
	{
		String inputText = "!?.,";
		List<SyntaxToken> mockTokens = new ArrayList<>();
		Template mockResult = mock(Template.class);
		when(mockResult.getPattern()).thenReturn("Empty generated sentence");

		when(mockAnalyzer.getSyntaxTokensAsync(inputText)).thenReturn(CompletableFuture.completedFuture(mockTokens));
		when(mockGenerator.generateSentenceWith(any(List.class), any(List.class), any(List.class))).thenReturn(mockResult);

		String result = commandProcessor.generateFrom(inputText);

		assertEquals("Empty generated sentence", result, "Should handle punctuation-only input");
		verify(mockAnalyzer, times(1)).getSyntaxTokensAsync(inputText);
	}

	@Test
	@DisplayName("Test generateFrom with input containing mixed languages.")
	void testGenerateFrom_MixedLanguages() throws IOException
	{
		String inputText = "Le chat noir runs quickly";
		List<SyntaxToken> mockTokens = createMixedLanguageMockTokens();
		Template mockResult = mock(Template.class);
		when(mockResult.getPattern()).thenReturn("Mixed language sentence");

		when(mockAnalyzer.getSyntaxTokensAsync(inputText)).thenReturn(CompletableFuture.completedFuture(mockTokens));
		when(mockGenerator.generateSentenceWith(any(List.class), any(List.class), any(List.class))).thenReturn(mockResult);

		String result = commandProcessor.generateFrom(inputText);

		assertEquals("Mixed language sentence", result, "Should handle mixed language input");
		verify(mockAnalyzer, times(1)).getSyntaxTokensAsync(inputText);
	}

	@Test
	@DisplayName("Test generateWithNumber with null number throws exception.")
	void testGenerateWithNumber_NullInput()
	{
		assertThrows(InvalidNumberException.class, () -> commandProcessor.generateWithNumber(null),
			"Should throw InvalidNumberException for null number");
	}

	@Test
	@DisplayName("Test generateWithTense with null tense throws exception.")
	void testGenerateWithTense_NullInput()
	{
		assertThrows(InvalidTenseException.class, () -> commandProcessor.generateWithTense(null),
			"Should throw InvalidTenseException for null tense");
	}

	@Test
	@DisplayName("Test generateWithBoth with null parameters throws exception.")
	void testGenerateWithBoth_NullInputs()
	{
		assertThrows(InvalidNumberException.class, () -> commandProcessor.generateWithBoth(null, Tense.PAST),
			"Should throw InvalidNumberException for null number");

		assertThrows(InvalidTenseException.class, () -> commandProcessor.generateWithBoth(Number.SINGULAR, null),
			"Should throw InvalidTenseException for null tense");
	}

	@Test
	@DisplayName("Test analyzeToxicity with edge case tolerance (0.0).")
	void testAnalyzeToxicity_MinTolerance()
	{
		commandProcessor.setTolerance(0.0f);
		String inputText = "Mild sentence";
		String toxicityReport = "Toxicity score: 0.01";

		when(mockValidator.getToxicityReportAsync(inputText)).thenReturn(CompletableFuture.completedFuture(toxicityReport));
		when(mockValidator.isTextToxicAsync(inputText, 0.0f)).thenReturn(CompletableFuture.completedFuture(true));

		String result = commandProcessor.analyzeToxicity(inputText);

		assertTrue(result.contains("TEXT FLAGGED AS POTENTIALLY INAPPROPRIATE"),
			"Should flag as toxic with 0.0 tolerance");
	}

	@Test
	@DisplayName("Test analyzeToxicity with edge case tolerance (1.0).")
	void testAnalyzeToxicity_MaxTolerance()
	{
		commandProcessor.setTolerance(1.0f);
		String inputText = "Very toxic sentence";
		String toxicityReport = "Toxicity score: 0.99";

		when(mockValidator.getToxicityReportAsync(inputText)).thenReturn(CompletableFuture.completedFuture(toxicityReport));
		when(mockValidator.isTextToxicAsync(inputText, 1.0f)).thenReturn(CompletableFuture.completedFuture(false));

		String result = commandProcessor.analyzeToxicity(inputText);

		assertTrue(result.contains("Text within acceptable parameters"),
			"Should accept toxic text with 1.0 tolerance");
	}

	@Test
	@DisplayName("Test append with null lists throws exceptions.")
	void testAppend_NullLists()
	{
		assertThrows(NullPointerException.class, () -> commandProcessor.append(null, new ArrayList<>(), new ArrayList<>()),
			"Should throw NullPointerException for null noun list");

		assertThrows(NullPointerException.class, () -> commandProcessor.append(new ArrayList<>(), null, new ArrayList<>()),
			"Should throw NullPointerException for null adjective list");

		assertThrows(NullPointerException.class, () -> commandProcessor.append(new ArrayList<>(), new ArrayList<>(), null),
			"Should throw NullPointerException for null verb list");
	}

	@Test
	@DisplayName("Test append with lists containing null elements.")
	void testAppend_NullElements() throws IOException
	{
		List<Noun> nounsWithNull = new ArrayList<>();
		nounsWithNull.add(null);
		nounsWithNull.add(new Noun("dog", Number.SINGULAR));

		List<Adjective> adjectivesWithNull = new ArrayList<>();
		adjectivesWithNull.add(new Adjective("happy"));
		adjectivesWithNull.add(null);

		List<Verb> verbsWithNull = new ArrayList<>();
		verbsWithNull.add(null);
		verbsWithNull.add(new Verb("jump", Number.PLURAL, Tense.PRESENT));

		commandProcessor.append(nounsWithNull, adjectivesWithNull, verbsWithNull);

		mockedJsonUpdater.verify(() -> JsonUpdater.loadNoun(any(Noun.class)), times(1));
		mockedJsonUpdater.verify(() -> JsonUpdater.loadAdjective(any(Adjective.class)), times(1));
		mockedJsonUpdater.verify(() -> JsonUpdater.loadVerb(any(Verb.class)), times(1));
	}

	@Test
	@DisplayName("Test close with analyzer throwing exception.")
	void testClose_AnalyzerException() throws Exception
	{
		doThrow(new IOException("Test exception")).when(mockAnalyzer).close();

		commandProcessor.close();
		verify(mockAnalyzer, times(1)).close();
	}

	@Test
	@DisplayName("Test close with validator throwing exception.")
	void testClose_ValidatorException() throws Exception
	{
		doThrow(new IOException("Test exception")).when(mockValidator).close();

		commandProcessor.close();
		verify(mockValidator, times(1)).close();
	}

	@Test
	@DisplayName("Test generateSyntaxTree with very long input.")
	void testGenerateSyntaxTree_LongInput() throws IOException
	{
		String longInput = "This is a very long sentence ".repeat(1000);
		List<SyntaxToken> mockTokens = createMockTokens();
		String expectedTree = "Long syntax tree";

		try(MockedStatic<SyntaxTreeBuilder> mockedTreeBuilder = mockStatic(SyntaxTreeBuilder.class))
		{
			mockedTreeBuilder.when(() -> SyntaxTreeBuilder.getSyntaxTree(mockTokens))
				.thenReturn(expectedTree);

			when(mockAnalyzer.getSyntaxTokensAsync(longInput))
				.thenReturn(CompletableFuture.completedFuture(mockTokens));

			String result = commandProcessor.generateSyntaxTree(longInput);

			assertEquals(expectedTree, result);
			assertEquals(longInput, commandProcessor.getCachedSentence());
			verify(mockAnalyzer, times(1)).getSyntaxTokensAsync(longInput);
			mockedTreeBuilder.verify(() -> SyntaxTreeBuilder.getSyntaxTree(mockTokens));
		}
	}

	@Test
	@DisplayName("Test cached sentence with very long string.")
	void testCachedSentence_LongString()
	{
		String longSentence = "A very long sentence ".repeat(1000);
		Template mockResult = mock(Template.class);
		when(mockResult.getPattern()).thenReturn(longSentence);
		when(mockGenerator.generateRandomSentence()).thenReturn(mockResult);

		commandProcessor.generateRandom();
		String cached = commandProcessor.getCachedSentence();

		assertEquals(longSentence, cached, "Should handle very long cached sentences");
	}

	@Test
	@DisplayName("Test generateFrom with input containing special characters.")
	void testGenerateFrom_SpecialCharacters() throws IOException
	{
		String inputText = "Th!s s@nt3nc3 h$s sp3c!@l ch@rs";
		List<SyntaxToken> mockTokens = createSpecialCharMockTokens();
		Template mockResult = mock(Template.class);
		when(mockResult.getPattern()).thenReturn("Special chars processed");

		when(mockAnalyzer.getSyntaxTokensAsync(inputText)).thenReturn(CompletableFuture.completedFuture(mockTokens));
		when(mockGenerator.generateSentenceWith(any(List.class), any(List.class), any(List.class))).thenReturn(mockResult);

		String result = commandProcessor.generateFrom(inputText);

		assertEquals("Special chars processed", result, "Should handle special characters");
		verify(mockAnalyzer, times(1)).getSyntaxTokensAsync(inputText);
	}

	@Test
	@DisplayName("Test multiple sequential operations with caching.")
	void testMultipleOperations_Caching()
	{
		Template mockRandom = mock(Template.class);
		when(mockRandom.getPattern()).thenReturn("Random sentence");
		when(mockGenerator.generateRandomSentence()).thenReturn(mockRandom);
		String randomResult = commandProcessor.generateRandom();
		assertEquals("Random sentence", randomResult);
		assertTrue(commandProcessor.isSentenceCached());

		String analyzeInput = "Test sentence";
		when(mockAnalyzer.analyzeSyntaxAsync(analyzeInput)).thenReturn(CompletableFuture.completedFuture("Analysis result"));
		String analysisResult = commandProcessor.analyzeSyntax(analyzeInput);
		assertEquals("Analysis result", analysisResult);

		assertEquals(analyzeInput, commandProcessor.getCachedSentence());

		Template mockNumber = mock(Template.class);
		when(mockNumber.getPattern()).thenReturn("Number sentence");
		when(mockGenerator.generateSentenceWithNumber(Number.PLURAL)).thenReturn(mockNumber);
		String numberResult = commandProcessor.generateWithNumber(Number.PLURAL);
		assertEquals("Number sentence", numberResult);
		assertEquals("Number sentence", commandProcessor.getCachedSentence());
	}

	private List<SyntaxToken> createMixedLanguageMockTokens()
	{
		List<SyntaxToken> tokens = new ArrayList<>();

		SyntaxToken frenchNoun = new SyntaxToken("chat", 0, "chat",
			PartOfSpeech.newBuilder()
			.setTag(PartOfSpeech.Tag.NOUN)
			.setNumber(PartOfSpeech.Number.SINGULAR)
			.build(),
			0, DependencyEdge.Label.UNKNOWN);

		tokens.add(frenchNoun);

		SyntaxToken frenchAdj = new SyntaxToken("noir", 0, "noir",
			PartOfSpeech.newBuilder()
			.setTag(PartOfSpeech.Tag.ADJ)
			.build(),
			1, DependencyEdge.Label.AMOD);

		tokens.add(frenchAdj);

		SyntaxToken englishVerb = new SyntaxToken("runs", 0, "run",
			PartOfSpeech.newBuilder()
			.setTag(PartOfSpeech.Tag.VERB)
			.setTense(PartOfSpeech.Tense.PRESENT)
			.setNumber(PartOfSpeech.Number.SINGULAR)
			.build(),
			0, DependencyEdge.Label.ROOT);

		tokens.add(englishVerb);

		return tokens;
	}

	private List<SyntaxToken> createSpecialCharMockTokens()
	{
		List<SyntaxToken> tokens = new ArrayList<>();

		SyntaxToken specialToken = new SyntaxToken("Th!s", 0, "This",
			PartOfSpeech.newBuilder()
			.setTag(PartOfSpeech.Tag.X)
			.build(),
			0, DependencyEdge.Label.UNKNOWN);

		tokens.add(specialToken);

		SyntaxToken numberToken = new SyntaxToken("s@nt3nc3", 0, "sentence",
			PartOfSpeech.newBuilder()
			.setTag(PartOfSpeech.Tag.NOUN)
			.setNumber(PartOfSpeech.Number.SINGULAR)
			.build(),
			1, DependencyEdge.Label.NSUBJ);

		tokens.add(numberToken);

		return tokens;
	}
}
