package unipd.nonsense.analyzer;

import com.google.cloud.language.v1.ClassificationCategory;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.ModerateTextRequest;
import com.google.cloud.language.v1.ModerateTextResponse;

import unipd.nonsense.exceptions.InvalidThresholdException;
import unipd.nonsense.exceptions.InvalidTextException;

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
	private final LoggerManager logger;
	private static final float DEFAULT_TOXICITY_THRESHOLD = 0.7f;

	public ToxicityValidator(GoogleApiClient apiClient, LoggerManager logger)
	{
		if(logger == null)
			throw new IllegalArgumentException("LoggerManager cannot be null.");

		this.logger = logger;

		if (apiClient == null)
		{
			throw new IllegalArgumentException("GoogleApiClient cannot be null.");
		}

		this.apiClient = apiClient;
	}

	public ToxicityValidator() throws IOException
	{
		this(new GoogleApiClient("/credentials.json"), new LoggerManager(ToxicityValidator.class));
	}

	public CompletableFuture<Map<String, Float>> getToxicityScoresAsync(String text)
	{
		return CompletableFuture.supplyAsync(() -> getToxicityScores(text), executor);
	}

	private Map<String, Float> getToxicityScores(String text)
	{
		validateInput(text);

		ModerateTextResponse response = moderateText(text);
		Map<String, Float> scores = new HashMap<>();

		for(ClassificationCategory category : response.getModerationCategoriesList())
			scores.put(category.getName(), category.getConfidence());

		if(!scores.isEmpty())
		{
			StringBuilder details = new StringBuilder("Scores: ");
			scores.forEach((cat, score) -> details.append(cat).append("=").append(String.format("%.2f", score)).append(" "));
		}

		return scores;
	}

	public CompletableFuture<String> getToxicityReportAsync(String text)
	{
		return CompletableFuture.supplyAsync(() -> getToxicityReport(text), executor);
	}

	private String getToxicityReport(String text)
	{
		validateInput(text);

		Map<String, Float> scores = getToxicityScores(text);
		StringBuilder report = new StringBuilder();

		if(scores.isEmpty())
			report.append("No toxicity categories found or scores available.\n");
		else
			for(Map.Entry<String, Float> entry : scores.entrySet())
				report.append(String.format("%-25s: %.1f%%\n", entry.getKey(), entry.getValue() * 100));

		return report.toString();
	}

	public CompletableFuture<ModerateTextResponse> moderateTextAsync(String text)
	{
		return CompletableFuture.supplyAsync(() -> moderateText(text), executor);
	}

	private ModerateTextResponse moderateText(String text)
	{
		validateInput(text);

		if(text == null || text.trim().isEmpty())
		{
			String errorMsg = "Input text cannot be null or empty.";
			throw new InvalidTextException(errorMsg);
		}

		LanguageServiceClient languageClient = apiClient.getClient();
		Document doc = buildDocument(text);
		ModerateTextRequest request = ModerateTextRequest.newBuilder().setDocument(doc).build();

		ModerateTextResponse response = languageClient.moderateText(request);
		return response;
	}

	public CompletableFuture<Boolean> isTextToxicAsync(String text)
	{
		return CompletableFuture.supplyAsync(() -> isTextToxic(text), executor);
	}

	private boolean isTextToxic(String text)
	{
		return isTextToxic(text, DEFAULT_TOXICITY_THRESHOLD);
	}

	public CompletableFuture<Boolean> isTextToxicAsync(String text, float threshold)
	{
		return CompletableFuture.supplyAsync(() -> isTextToxic(text, threshold), executor);
	}

	private boolean isTextToxic(String text, float threshold)
	{
		validateInput(text);

		if(threshold < 0.0f || threshold > 1.0f)
		{
			String errorMsg = "Threshold must be between 0.0 and 1.0, inclusive. Received: " + threshold;
			throw new InvalidThresholdException(threshold, errorMsg);
		}

		ModerateTextResponse response = moderateText(text);

		for(ClassificationCategory category : response.getModerationCategoriesList())
			if(category.getConfidence() > threshold)
				return true;

		return false;
	}

	private void validateInput(String text)
	{
		if(text == null)
			throw new InvalidTextException("Input text cannot be null");

		if(text.trim().isEmpty())
			throw new InvalidTextException("Input text cannot be empty or whitespace-only");

		if(text.length() > 1000)
			throw new InvalidTextException("Input text exceeds maximum length (1,000 characters)");
	}

	private Document buildDocument(String text)
	{
		return Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).build();
	}

	@Override
	public void close()
	{
		try
		{
			if(apiClient != null)
				apiClient.close();
		}
		catch(Exception e)
		{
			logger.logError("Error closing GoogleApiClient", e);
		}
		finally
		{
			executor.shutdownNow();
		}
	}
}

