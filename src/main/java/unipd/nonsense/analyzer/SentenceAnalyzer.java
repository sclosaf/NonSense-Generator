package unipd.nonsense.analyzer;

import com.google.cloud.language.v1.AnalyzeSyntaxRequest;
import com.google.cloud.language.v1.AnalyzeSyntaxResponse;
import com.google.cloud.language.v1.PartOfSpeech;
import com.google.cloud.language.v1.Token;

import com.google.cloud.language.v1.AnalyzeEntitiesRequest;
import com.google.cloud.language.v1.AnalyzeEntitiesResponse;
import com.google.cloud.language.v1.Entity;
import com.google.cloud.language.v1.EntityMention;
import com.google.cloud.language.v1.EncodingType;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;

import com.google.cloud.language.v1.Sentiment;
import com.google.cloud.language.v1.AnalyzeSentimentResponse;

import com.google.cloud.language.v1.LanguageServiceClient;

import unipd.nonsense.util.GoogleApiClient;
import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.model.SyntaxToken;

import unipd.nonsense.exceptions.InvalidTextException;
import unipd.nonsense.exceptions.FailedAnalysisException;
import unipd.nonsense.exceptions.FailedClientInitializationException;

import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A comprehensive natural language processing analyzer that provides syntactic,
 * sentiment, and entity analysis capabilities using Google Cloud Natural Language API.
 * This class implements {@code AutoCloseable} to ensure proper resource cleanup.
 *
 * <p>The analyzer supports both synchronous and asynchronous operations through
 * {@code CompletableFuture} for better performance in concurrent environments.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * try (SentenceAnalyzer analyzer = new SentenceAnalyzer())
 * {
 *     String result = analyzer.analyzeSyntaxAsync("Sample text").get();
 *     System.out.println(result);
 * }
 * }</pre>
 * </p>
 *
 * @see com.google.cloud.language.v1.LanguageServiceClient
 * @see unipd.nonsense.util.GoogleApiClient
 */
public class SentenceAnalyzer implements AutoCloseable
{
	/**
	 * The API client manager for Google Cloud services.
	 * <p>This field holds the reference to the {@code GoogleApiClient} instance
	 * that manages authentication and client creation.</p>
	 */
	private final GoogleApiClient apiClient;

	/**
	 * The Language Service client instance for making API calls.
	 * <p>This is the primary client used for all natural language processing operations.</p>
	 */
	private final LanguageServiceClient languageClient;

	/**
	 * Thread pool executor for handling asynchronous operations.
	 * <p>Uses a cached thread pool that creates new threads as needed and reuses previously
	 * constructed threads when available.</p>
	 */
	private final ExecutorService executor = Executors.newCachedThreadPool();

	/**
	 * Logger instance for tracking operations and debugging.
	 */
	private final LoggerManager logger = new LoggerManager(SentenceAnalyzer.class);

	/**
	 * Path to the credentials file required for Google Cloud API authentication.
	 * <p>The default path is {@code /credentials.json} in the classpath.</p>
	 */
	private static final String credentialsPath = "/credentials.json";

	/**
	 * Default constructor that initializes the Google Cloud Language client.
	 * <p>This constructor:
	 * <ul>
	 *	<li>Creates a new {@code GoogleApiClient} instance</li>
	 *	<li>Initializes the {@code LanguageServiceClient}</li>
	 *	<li>Handles authentication using the default credentials file</li>
	 * </ul>
	 * </p>
	 *
	 * @throws FailedClientInitializationException if client creation fails
	 */
	public SentenceAnalyzer()
	{
		logger.logTrace("Starting initialization");

		try
		{
			logger.logTrace("Creating GoogleApiClient");
			GoogleApiClient manager = new GoogleApiClient(credentialsPath);
			this.apiClient = manager;
			this.languageClient = manager.getClient();
			logger.logTrace("Successfully initialized language client");
		}
		catch (Exception e)
		{
			logger.logError("Failed to initialize", e);
			throw new FailedClientInitializationException();
		}
	}

	/**
	 * Dependency injection constructor for testing or custom client configuration.
	 *
	 * @param apiClient			 Pre-configured {@code GoogleApiClient} instance
	 * @param languageClient	 Pre-configured {@code LanguageServiceClient} instance
	 */
	public SentenceAnalyzer(GoogleApiClient apiClient, LanguageServiceClient languageClient)
	{
		this.apiClient = apiClient;
		this.languageClient = languageClient;
	}

	/**
	 * Asynchronously analyzes the syntactic structure of the input text.
	 * <p>Returns a {@code CompletableFuture} that will contain a detailed report including:
	 * <ul>
	 *	<li>Token text and lemma</li>
	 *	<li>Part-of-speech tags</li>
	 *	<li>Morphological features (case, gender, number, etc.)</li>
	 *	<li>Dependency tree relationships</li>
	 * </ul>
	 * </p>
	 *
	 * @param text	The text to analyze (1-1000 characters)
	 * @return		{@code CompletableFuture} containing the analysis report
	 * @see #analyzeSyntax(String)
	 */
	public CompletableFuture<String> analyzeSyntaxAsync(String text)
	{
		logger.logTrace("analyzeSyntaxAsync: Starting async syntax analysis");
		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					logger.logTrace("analyzeSyntaxAsync: Processing syntax analysis");
					return analyzeSyntax(text);
				}
				catch(IOException e)
				{
					logger.logError("analyzeSyntaxAsync: Error during syntax analysis", e);
					throw new FailedAnalysisException();
				}
			}, executor);
	}

	/**
	 * Performs synchronous syntactic analysis of the input text.
	 *
	 * @param text	The text to analyze (1-1000 characters)
	 * @return		Detailed syntactic analysis report
	 * @throws IOException						if API communication fails
	 * @throws InvalidTextException			if input validation fails
	 * @throws FailedAnalysisException		if analysis fails
	 */
	private String analyzeSyntax(String text) throws IOException
	{
		logger.logTrace("analyzeSyntax: Starting syntax analysis");
		validateInput(text);
		Document doc = buildDocument(text);

		logger.logDebug("analyzeSyntax: Analyzing text with length: " + text.length());

		AnalyzeSyntaxRequest request = AnalyzeSyntaxRequest.newBuilder().setDocument(doc).setEncodingType(EncodingType.UTF16).build();
		AnalyzeSyntaxResponse response = languageClient.analyzeSyntax(request);

		StringBuilder report = new StringBuilder();
		int tokenIndex = 1;

		logger.logDebug("analyzeSyntax: Processing " + response.getTokensList().size() + " tokens");

		for(Token token : response.getTokensList())
		{
			report.append("Token ").append(tokenIndex).append(": ").append(token.getText().getContent()).append("\n");
			report.append("  Lemma: ").append(token.getLemma()).append("\n");
			report.append("  POS: ").append(token.getPartOfSpeech().getTag()).append("\n");

			addIfNotDefault(report, "  Aspect: ", token.getPartOfSpeech().getAspect(), PartOfSpeech.Aspect.ASPECT_UNKNOWN);
			addIfNotDefault(report, "  Case: ", token.getPartOfSpeech().getCase(), PartOfSpeech.Case.CASE_UNKNOWN);
			addIfNotDefault(report, "  Form: ", token.getPartOfSpeech().getForm(), PartOfSpeech.Form.FORM_UNKNOWN);
			addIfNotDefault(report, "  Gender: ", token.getPartOfSpeech().getGender(), PartOfSpeech.Gender.GENDER_UNKNOWN);

			addIfNotDefault(report, "  Mood: ", token.getPartOfSpeech().getMood(), PartOfSpeech.Mood.MOOD_UNKNOWN);

			addIfNotDefault(report, "  Number: ", token.getPartOfSpeech().getNumber(), PartOfSpeech.Number.NUMBER_UNKNOWN);

			addIfNotDefault(report, "  Person: ", token.getPartOfSpeech().getPerson(), PartOfSpeech.Person.PERSON_UNKNOWN);

			addIfNotDefault(report, "  Proper: ", token.getPartOfSpeech().getProper(), PartOfSpeech.Proper.PROPER_UNKNOWN);
			addIfNotDefault(report, "  Reciprocity: ", token.getPartOfSpeech().getReciprocity(), PartOfSpeech.Reciprocity.RECIPROCITY_UNKNOWN);
			addIfNotDefault(report, "  Tense: ", token.getPartOfSpeech().getTense(), PartOfSpeech.Tense.TENSE_UNKNOWN);
			addIfNotDefault(report, "  Voice: ", token.getPartOfSpeech().getVoice(), PartOfSpeech.Voice.VOICE_UNKNOWN);

			report.append("  Dependency:\n");
			report.append("    Head Token Index: ").append(token.getDependencyEdge().getHeadTokenIndex()).append("\n");
			report.append("    Label: ").append(token.getDependencyEdge().getLabel()).append("\n");

			tokenIndex++;
			report.append("\n");
		}

		logger.logTrace("analyzeSyntax: Completed syntax analysis");
		return report.toString().trim();
	}

	/**
	 * Helper method to conditionally append values to a string builder.
	 * <p>Only appends the value if it differs from the specified default.</p>
	 *
	 * @param sb			StringBuilder to append to
	 * @param prefix		Line prefix to add
	 * @param value			Value to potentially append
	 * @param defaultValue	Value to compare against
	 */
	private void addIfNotDefault(StringBuilder sb, String prefix, Object value, Object defaultValue)
	{
		if(!value.equals(defaultValue))
			sb.append(prefix).append(value).append("\n");
	}

	/**
	 * Asynchronously analyzes the sentiment of the input text.
	 * <p>Returns a {@code CompletableFuture} that will contain:
	 * <ul>
	 *	<li>Sentiment score (-1.0 to 1.0)</li>
	 *	<li>Sentiment magnitude (0.0 to infinity)</li>
	 * </ul>
	 * </p>
	 *
	 * @param text	The text to analyze (1-1000 characters)
	 * @return		{@code CompletableFuture} containing the sentiment analysis
	 * @see #analyzeSentiment(String)
	 */
	public CompletableFuture<String> analyzeSentimentAsync(String text)
	{
		logger.logTrace("analyzeSentimentAsync: Starting async sentiment analysis");

		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					logger.logTrace("analyzeSentimentAsync: Processing sentiment analysis");
					return analyzeSentiment(text);
				}
				catch(IOException e)
				{
					logger.logError("analyzeSentimentAsync: Error during sentiment analysis", e);
					throw new FailedAnalysisException();
				}
			}, executor);
	}

	/**
	 * Performs synchronous sentiment analysis of the input text.
	 *
	 * @param text	The text to analyze (1-1000 characters)
	 * @return		Sentiment analysis results
	 * @throws IOException						if API communication fails
	 * @throws InvalidTextException			if input validation fails
	 * @throws FailedAnalysisException		if analysis fails
	 */
	private String analyzeSentiment(String text) throws IOException
	{
		logger.logTrace("analyzeSentiment: Starting sentiment analysis");
		validateInput(text);
		Document doc = buildDocument(text);

		logger.logDebug("analyzeSentiment: Analyzing sentiment for text with length: " + text.length());

		AnalyzeSentimentResponse response = languageClient.analyzeSentiment(doc);
		Sentiment sentiment = response.getDocumentSentiment();

		if (sentiment == null)
		{
			logger.logWarn("analyzeSentiment: No sentiment detected for text");
			return "No sentiment detected.";
		}

		logger.logDebug("analyzeSentiment: Detected sentiment score: " + sentiment.getScore() + ", magnitude: " + sentiment.getMagnitude());

		return String.format("Sentiment Score: %.2f (Magnitude: %.2f)", sentiment.getScore(), sentiment.getMagnitude());
	}

	/**
	 * Asynchronously identifies and analyzes entities in the input text.
	 * <p>Returns a {@code CompletableFuture} that will contain a report with:
	 * <ul>
	 *	<li>Entity names and types</li>
	 *	<li>Salience scores (0.0 to 1.0)</li>
	 *	<li>Metadata (when available)</li>
	 *	<li>All mentions in the text</li>
	 * </ul>
	 * </p>
	 *
	 * @param text	The text to analyze (1-1000 characters)
	 * @return		{@code CompletableFuture} containing the entity analysis
	 * @see #analyzeEntities(String)
	 */
	public CompletableFuture<String> analyzeEntitiesAsync(String text)
	{
		logger.logTrace("analyzeEntitiesAsync: Starting async entities analysis");

		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					logger.logTrace("analyzeEntitiesAsync: Processing entities analysis");
					return analyzeEntities(text);
				}
				catch(IOException e)
				{
					logger.logError("analyzeEntitiesAsync: Error during entities analysis", e);
					throw new FailedAnalysisException();
				}
			}, executor);
	}

	/**
	 * Performs synchronous entity analysis of the input text.
	 *
	 * @param text	The text to analyze (1-1000 characters)
	 * @return		Entity analysis report
	 * @throws IOException						if API communication fails
	 * @throws InvalidTextException			if input validation fails
	 * @throws FailedAnalysisException		if analysis fails
	 */
	private String analyzeEntities(String text) throws IOException
	{
		logger.logTrace("analyzeEntities: Starting entities analysis");
		validateInput(text);
		Document doc = buildDocument(text);

		logger.logDebug("analyzeEntities: Analyzing entities for text with length: " + text.length());

		AnalyzeEntitiesRequest request = AnalyzeEntitiesRequest.newBuilder().setDocument(doc).setEncodingType(EncodingType.UTF16).build();
		AnalyzeEntitiesResponse response = languageClient.analyzeEntities(request);

		StringBuilder report = new StringBuilder();
		List<Entity> entities = response.getEntitiesList();

		if(entities.isEmpty())
		{
			logger.logWarn("analyzeEntities: No entities detected in text");
			return "No entities detected.";
		}

		logger.logDebug("analyzeEntities: Found " + entities.size() + " entities");

		int entityIndex = 1;
		for(Entity entity : entities)
		{
			report.append(String.format
				(
					"Entity %d: %s (Type: %s, Salience: %.3f)\n",
					entityIndex,
					entity.getName(),
					entity.getType(),
					entity.getSalience()
				));

			if(!entity.getMetadataMap().isEmpty())
			{
				report.append("  Metadata:\n");
				for(Map.Entry<String, String> entry : entity.getMetadataMap().entrySet())
					report.append(String.format("    %s: %s\n", entry.getKey(), entry.getValue()));
			}

			if(!entity.getMentionsList().isEmpty())
			{
				report.append("  Mentions:\n");

				for(EntityMention mention : entity.getMentionsList())
					report.append(String.format
						(
							"    - %s (Type: %s, Offset: %d)\n",
							mention.getText().getContent(),
							mention.getType(),
							mention.getText().getBeginOffset()
						));
			}

			entityIndex++;
			report.append("\n");
		}

		logger.logTrace("analyzeEntities: Completed entities analysis");
		return report.toString().trim();
	}

	/**
	 * Asynchronously retrieves syntactic tokens from the input text.
	 * <p>Returns a {@code CompletableFuture} that will contain a list of
	 * {@code SyntaxToken} objects with detailed linguistic information.</p>
	 *
	 * @param text	The text to analyze (1-1000 characters)
	 * @return		{@code CompletableFuture} containing the list of tokens
	 * @see SyntaxToken
	 * @see #getSyntaxTokens(String)
	 */
	public CompletableFuture<List<SyntaxToken>> getSyntaxTokensAsync(String text)
	{
		logger.logTrace("getSyntaxTokensAsync: Starting async syntax tokens retrieval");
		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					logger.logTrace("getSyntaxTokensAsync: Processing syntax tokens retrieval");
					return getSyntaxTokens(text);
				}
				catch(IOException e)
				{
					logger.logError("getSyntaxTokensAsync: Error during syntax tokens retrieval", e);
					throw new FailedAnalysisException();
				}
			}, executor);
	}

	/**
	 * Synchronously extracts syntactic tokens from the input text.
	 *
	 * @param text	The text to analyze (1-1000 characters)
	 * @return		List of {@code SyntaxToken} objects
	 * @throws IOException						if API communication fails
	 * @throws InvalidTextException			if input validation fails
	 * @throws FailedAnalysisException		if analysis fails
	 */
	private List<SyntaxToken> getSyntaxTokens(String text) throws IOException
	{
		logger.logTrace("getSyntaxTokens: Starting syntax tokens extraction");
		validateInput(text);
		Document doc = buildDocument(text);

		logger.logDebug("getSyntaxTokens: Extracting tokens from text with length: " + text.length());

		AnalyzeSyntaxRequest request = AnalyzeSyntaxRequest.newBuilder().setDocument(doc).setEncodingType(EncodingType.UTF16).build();

		AnalyzeSyntaxResponse response = languageClient.analyzeSyntax(request);

		logger.logDebug("getSyntaxTokens: Converting " + response.getTokensList().size() + " tokens to SyntaxToken objects");

		List<SyntaxToken> tokens = new ArrayList<>();
		for (Token googleToken : response.getTokensList())
		{
			SyntaxToken token = new SyntaxToken
				(
					googleToken.getText().getContent(),
					googleToken.getText().getBeginOffset(),
					googleToken.getLemma(),
					googleToken.getPartOfSpeech(),
					googleToken.getDependencyEdge().getHeadTokenIndex(),
					googleToken.getDependencyEdge().getLabel()
				 );
			tokens.add(token);
		}

		logger.logTrace("getSyntaxTokens: Completed tokens extraction");
		return tokens;
	}

	/**
	 * Validates input text according to analysis requirements.
	 *
	 * @param text	The text to validate
	 * @throws InvalidTextException if text is:
	 *	<ul>
	 *		<li>{@code null}</li>
	 *		<li>Empty or whitespace-only</li>
	 *		<li>Longer than 1000 characters</li>
	 *	</ul>
	 */
	private void validateInput(String text)
	{
		logger.logTrace("validateInput: Validating input text");

		if(text == null)
		{
			logger.logError("validateInput: Text is null");
			throw new InvalidTextException("Input text cannot be null");
		}

		if(text.trim().isEmpty())
		{
			logger.logError("validateInput: Text is empty or whitespace-only");
			throw new InvalidTextException("Input text cannot be empty or whitespace-only");
		}

		if(text.length() > 1000)
		{
			logger.logError("validateInput: Text length " + text.length() + " exceeds maximum");
			throw new InvalidTextException("Input text exceeds maximum length (1,000 characters)");
		}
	}

	/**
	 * Creates a Google Cloud Language API document from input text.
	 *
	 * @param text	The text to convert
	 * @return		Configured {@code Document} instance
	 */
	private Document buildDocument(String text)
	{
		logger.logTrace("buildDocument: Building document for analysis");
		return Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).build();
	}

	/**
	 * Cleans up resources including:
	 * <ul>
	 *	<li>Closing the language client</li>
	 *	<li>Shutting down the executor service</li>
	 * </ul>
	 *
	 * <p>This method is automatically called when used in try-with-resources blocks.</p>
	 *
	 * @throws Exception if resource cleanup fails
	 */
	@Override
	public void close()
	{
		logger.logTrace("close: Closing SentenceAnalyzer resources");
		try
		{
			if(languageClient != null)
			{
				logger.logTrace("close: Closing language client");
				languageClient.close();
			}
		}
		catch(Exception e)
		{
			logger.logError("close: Error while closing resources", e);
			throw e;
		}
		finally
		{
			logger.logTrace("close: Shutting down executor");
			executor.shutdownNow();
		}
	}
}
