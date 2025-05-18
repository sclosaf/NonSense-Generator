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

public class ToxicityValidator implements AutoCloseable
{
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final GoogleApiClient apiClient;
	private final LoggerManager logger = new LoggerManager(ToxicityValidator.class);
	private static final String credentialsPath = "/credentials.json";
	private static final float DEFAULT_TOXICITY_THRESHOLD = 0.7f;

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

	public ToxicityValidator() throws IOException
	{
		this(new GoogleApiClient(credentialsPath));
		logger.logTrace("Completed default initialization");
	}

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

	private boolean isTextToxic(String text)
	{
		logger.logTrace("isTextToxic: Starting toxicity check with default threshold");
		return isTextToxic(text, DEFAULT_TOXICITY_THRESHOLD);
	}

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

	private Document buildDocument(String text)
	{
		logger.logTrace("buildDocument: Building document for analysis");
		return Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).build();
	}

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

