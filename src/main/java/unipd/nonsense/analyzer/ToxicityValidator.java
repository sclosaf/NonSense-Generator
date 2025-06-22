package unipd.nonsense.analyzer;

import com.google.cloud.language.v1.ClassificationCategory;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.ModerateTextRequest;
import com.google.cloud.language.v1.ModerateTextResponse;

import unipd.nonsense.exceptions.InvalidThresholdException;
import unipd.nonsense.exceptions.InvalidTextException;
import unipd.nonsense.exceptions.NullLoggerException;
import unipd.nonsense.exceptions.NullClientException;

import com.google.api.gax.rpc.ApiException;

import unipd.nonsense.util.GoogleApiClient;
import unipd.nonsense.util.LoggerManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A comprehensive toxicity analysis validator that detects and evaluates toxic content in text
 * using Google Cloud Natural Language API's moderation features. This class implements
 * {@code AutoCloseable} to ensure proper resource cleanup of API clients and thread pools.
 *
 * <p>The validator provides both synchronous and asynchronous methods for:
 * <ul>
 *	<li>Retrieving toxicity category scores</li>
 *	<li>Generating detailed toxicity reports</li>
 *	<li>Performing binary toxicity checks against configurable thresholds</li>
 * </ul>
 * </p>
 *
 * <p>Example usage:
 * <pre>{@code
 * try (ToxicityValidator validator = new ToxicityValidator())
 * {
 *		Map<String, Float> scores = validator.getToxicityScoresAsync("offensive text").get();
 *		boolean isToxic = validator.isTextToxicAsync("hate speech", 0.8f).get();
 * }
 * }</pre>
 * </p>
 *
 * @see com.google.cloud.language.v1.LanguageServiceClient
 * @see com.google.cloud.language.v1.ModerateTextResponse
 */
public class ToxicityValidator implements AutoCloseable
{
	/**
	 * Thread pool executor for handling asynchronous operations.
	 * <p>Uses a cached thread pool that:
	 * <ul>
	 *	<li>Creates new threads as needed</li>
	 *	<li>Reuses idle threads</li>
	 *	<li>Terminates threads that remain idle for 60 seconds</li>
	 * </ul>
	 * </p>
	 */
	private final ExecutorService executor = Executors.newCachedThreadPool();


	/**
	 * The API client manager for Google Cloud services.
	 * <p>Manages:
	 * <ul>
	 *	<li>Authentication credentials</li>
	 *	<li>Client lifecycle</li>
	 *	<li>Connection pooling</li>
	 * </ul>
	 * </p>
	 */
	private final GoogleApiClient apiClient;

	/**
	 * Logger instance for tracking operations and errors.
	 * <p>Configured to log messages from {@code ToxicityValidator} class.</p>
	 */
	private final LoggerManager logger = new LoggerManager(ToxicityValidator.class);

	/**
	 * Default path to the Google Cloud credentials file.
	 * <p>Located at {@code /credentials.json} in the classpath.</p>
	 */
	private static final String credentialsPath = "/credentials.json";

	/**
	 * Default threshold for toxicity classification.
	 * <p>A text is considered toxic if any category confidence exceeds this value (0.7 = 70%).</p>
	 */
	private static final float DEFAULT_TOXICITY_THRESHOLD = 0.7f;


	/**
	 * Constructs a validator with a pre-configured API client.
	 *
	 * @param apiClient		Pre-initialized {@code GoogleApiClient} instance
	 * @throws NullClientException	if {@code apiClient} is {@code null}
	 */
	public ToxicityValidator(GoogleApiClient apiClient)
	{
		logger.logTrace("Starting initialization with provided client and logger");

		if(apiClient == null)
		{
			logger.logError("Provided API client is null");
			throw new NullClientException();
		}

		this.apiClient = apiClient;
		logger.logTrace("Successfully initialized with provided client and logger");
	}

	/**
	 * Default constructor that initializes with default credentials.
	 *
	 * @throws IOException	if credential loading fails
	 */
	public ToxicityValidator() throws IOException
	{
		this(new GoogleApiClient(credentialsPath));
		logger.logTrace("Completed default initialization");
	}


	/**
	 * Asynchronously retrieves toxicity confidence scores for all moderation categories.
	 *
	 * @param text	Input text to analyze (1-1000 characters)
	 * @return		{@code CompletableFuture} containing a map of:
	 *				<ul>
	 *					<li>Keys: Toxicity category names</li>
	 *					<li>Values: Confidence scores (0.0 to 1.0)</li>
	 *				</ul>
	 * @see #getToxicityScores(String)
	 */
	public CompletableFuture<Map<String, Float>> getToxicityScoresAsync(String text)
	{
		logger.logTrace("getToxicityScoresAsync: Starting async toxicity scores retrieval");

		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					logger.logTrace("getToxicityScoresAsync: Processing toxicity scores retrieval");
					return getToxicityScores(text);
				}
				catch(Exception e)
				{
					logger.logError("getToxicityScoresAsync: Error during toxicity scores retrieval", e);
					throw e;
				}
			}, executor);
	}

	/**
	 * Synchronously calculates toxicity scores for all moderation categories.
	 *
	 * @param text	Input text to analyze (1-1000 characters)
	 * @return		Map of category names to confidence scores
	 * @throws InvalidTextException	if input validation fails
	 * @throws ApiException			if API communication fails
	 */
	private Map<String, Float> getToxicityScores(String text)
	{
		logger.logTrace("getToxicityScores: Starting toxicity scores calculation");
		validateInput(text);

		ModerateTextResponse response = moderateText(text);
		Map<String, Float> scores = new HashMap<>();

		for(ClassificationCategory category : response.getModerationCategoriesList())
		{
			scores.put(category.getName(), category.getConfidence());
			logger.logDebug("getToxicityScores: Found category " + category.getName() + " with confidence: " + category.getConfidence());
		}

		if(!scores.isEmpty())
		{
			StringBuilder details = new StringBuilder("Scores: ");
			scores.forEach((cat, score) -> details.append(cat).append("=").append(String.format("%.2f", score)).append(" "));
			logger.logDebug("getToxicityScores: " + details.toString());
		}
		else
			logger.logWarn("getToxicityScores: No toxicity categories found");

		logger.logTrace("getToxicityScores: Completed toxicity scores calculation");

		return scores;
	}

	/**
	 * Asynchronously generates a formatted toxicity analysis report.
	 *
	 * @param text	Input text to analyze (1-1000 characters)
	 * @return		{@code CompletableFuture} containing a multi-line report with:
	 *				<ul>
	 *					<li>All detected toxicity categories</li>
	 *					<li>Formatted confidence percentages</li>
	 *				</ul>
	 * @see #getToxicityReport(String)
	 */
	public CompletableFuture<String> getToxicityReportAsync(String text)
	{
		logger.logTrace("getToxicityReportAsync: Starting async toxicity report generation");

		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					logger.logTrace("getToxicityReportAsync: Processing toxicity report generation");
					return getToxicityReport(text);
				}
				catch(Exception e)
				{
					logger.logError("getToxicityReportAsync: Error during toxicity report generation", e);
					throw e;
				}
			}, executor);
	}

	/**
	 * Synchronously generates a human-readable toxicity report.
	 *
	 * @param text	Input text to analyze (1-1000 characters)
	 * @return		Formatted string report with aligned columns
	 * @throws InvalidTextException	if input validation fails
	 * @throws ApiException			if API communication fails
	 */
	private String getToxicityReport(String text)
	{
		logger.logTrace("getToxicityReport: Starting toxicity report generation");
		validateInput(text);

		Map<String, Float> scores = getToxicityScores(text);
		StringBuilder report = new StringBuilder();

		if(scores.isEmpty())
		{
			logger.logWarn("getToxicityReport: No toxicity categories found");
			report.append("No toxicity categories found or scores available.\n");
		}
		else
		{
			logger.logDebug("getToxicityReport: Generating report for " + scores.size() + " categories");

			for(Map.Entry<String, Float> entry : scores.entrySet())
				report.append(String.format("%-25s: %.1f%%\n", entry.getKey(), entry.getValue() * 100));
		}

		logger.logTrace("getToxicityReport: Completed toxicity report generation");

		return report.toString();
	}

	/**
	 * Asynchronously performs raw text moderation via Google API.
	 *
	 * @param text	Input text to analyze (1-1000 characters)
	 * @return		{@code CompletableFuture} containing the complete {@code ModerateTextResponse}
	 * @see #moderateText(String)
	 */
	public CompletableFuture<ModerateTextResponse> moderateTextAsync(String text)
	{
		logger.logTrace("moderateTextAsync: Starting async text moderation");

		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					logger.logTrace("moderateTextAsync: Processing text moderation");
					return moderateText(text);
				}
				catch(Exception e)
				{
					logger.logError("moderateTextAsync: Error during text moderation", e);
					throw e;
				}
			}, executor);
	}

	/**
	 * Synchronously sends text to Google's moderation API.
	 *
	 * @param text	Input text to analyze (1-1000 characters)
	 * @return		Raw API response with all moderation data
	 * @throws InvalidTextException	if input validation fails
	 * @throws ApiException			if API communication fails
	 */
	private ModerateTextResponse moderateText(String text)
	{
		logger.logTrace("moderateText: Starting text moderation");
		validateInput(text);

		if(text == null || text.trim().isEmpty())
		{
			logger.logError("moderateText: Input text cannot be null or empty");
			throw new InvalidTextException();
		}

		LanguageServiceClient languageClient = apiClient.getClient();
		Document doc = buildDocument(text);
		ModerateTextRequest request = ModerateTextRequest.newBuilder().setDocument(doc).build();

		logger.logDebug("moderateText: Sending moderation request for text with length: " + text.length());

		ModerateTextResponse response = languageClient.moderateText(request);

		logger.logTrace("moderateText: Completed text moderation");
		return response;
	}

	/**
	 * Asynchronously checks if text exceeds default toxicity threshold (0.7).
	 *
	 * @param text	Input text to analyze (1-1000 characters)
	 * @return		{@code CompletableFuture} containing:
	 *				<ul>
	 *					<li>{@code true} if any category exceeds threshold</li>
	 *					<li>{@code false} otherwise</li>
	 *				</ul>
	 * @see #isTextToxic(String)
	 */
	public CompletableFuture<Boolean> isTextToxicAsync(String text)
	{
		logger.logTrace("isTextToxicAsync: Starting async toxicity check with default threshold");

		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					logger.logTrace("isTextToxicAsync: Processing toxicity check");
					return isTextToxic(text);
				}
				catch(Exception e)
				{
					logger.logError("isTextToxicAsync: Error during toxicity check", e);
					throw e;
				}
			}, executor);
	}

	/**
	 * Synchronously checks toxicity against default threshold.
	 *
	 * @param text	Input text to analyze (1-1000 characters)
	 * @return		Toxicity determination using {@code DEFAULT_TOXICITY_THRESHOLD}
	 * @throws InvalidTextException	if input validation fails
	 * @throws ApiException			if API communication fails
	 */
	private boolean isTextToxic(String text)
	{
		logger.logTrace("isTextToxic: Starting toxicity check with default threshold");
		return isTextToxic(text, DEFAULT_TOXICITY_THRESHOLD);
	}

	/**
	 * Asynchronously checks if text exceeds custom toxicity threshold.
	 *
	 * @param text		Input text to analyze (1-1000 characters)
	 * @param threshold	Custom threshold (0.0 to 1.0)
	 * @return			{@code CompletableFuture} containing the toxicity determination
	 * @see #isTextToxic(String, float)
	 */
	public CompletableFuture<Boolean> isTextToxicAsync(String text, float threshold)
	{
		logger.logTrace("isTextToxicAsync: Starting async toxicity check with custom threshold");

		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					logger.logDebug("isTextToxicAsync: Processing toxicity check with threshold: " + threshold);
					return isTextToxic(text, threshold);
				}
				catch(Exception e)
				{
					logger.logError("isTextToxicAsync: Error during toxicity check", e);
					throw e;
				}
			}, executor);
	}

	/**
	 * Synchronously checks toxicity against custom threshold.
	 *
	 * @param text		Input text to analyze (1-1000 characters)
	 * @param threshold	Custom threshold value between 0.0 and 1.0 inclusive
	 * @return			Toxicity determination
	 * @throws InvalidTextException		if input validation fails
	 * @throws InvalidThresholdException	if threshold is invalid
	 * @throws ApiException				if API communication fails
	 */
	private boolean isTextToxic(String text, float threshold)
	{
		logger.logDebug("isTextToxic: Starting toxicity check with threshold: " + threshold);
		validateInput(text);

		if(threshold < 0.0f || threshold > 1.0f)
		{
			logger.logError("isTextToxic: Threshold value not in between 0.0 and 1.0, inclusive");
			throw new InvalidThresholdException(threshold);
		}

		ModerateTextResponse response = moderateText(text);

		for(ClassificationCategory category : response.getModerationCategoriesList())
		{
			if(category.getConfidence() > threshold)
			{
				logger.logDebug("isTextToxic: Found toxic category " + category.getName() +
								" with confidence " + category.getConfidence() +
								" exceeding threshold " + threshold);

				logger.logTrace("isTextToxic: Completed toxicity check - text is toxic");
				return true;
			}
		}

		logger.logTrace("isTextToxic: No categories exceeded toxicity threshold");
		logger.logTrace("isTextToxic: Completed toxicity check - text is not toxic");

		return false;
	}

	/**
	 * Validates input text meets analysis requirements.
	 *
	 * @param text	Input text to validate
	 * @throws InvalidTextException	if text is:
	 *	<ul>
	 *		<li>{@code null}</li>
	 *		<li>Empty/whitespace</li>
	 *		<li>Exceeds 1000 characters</li>
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
	 * Constructs a Google Cloud Language API document.
	 *
	 * @param text	Input text to wrap
	 * @return		Configured {@code Document} instance with:
	 *				<ul>
	 *					<li>PLAIN_TEXT type</li>
	 *					<li>UTF-8 encoding</li>
	 *				</ul>
	 */
	private Document buildDocument(String text)
	{
		logger.logTrace("buildDocument: Building document for analysis");
		return Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).build();
	}

	/**
	 * Cleans up resources including:
	 * <ul>
	 *	<li>API client connections</li>
	 *	<li>Thread pool executor</li>
	 * </ul>
	 *
	 * <p>Automatically called in try-with-resources blocks.</p>
	 *
	 * @throws Exception	if resource cleanup fails
	 */
	@Override
	public void close()
	{
		logger.logTrace("close: Closing ToxicityValidator resources");

		try
		{
			if(apiClient != null)
			{
				logger.logTrace("close: Closing API client");
				apiClient.close();
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
