package unipd.nonsense.analyzer;

import com.google.cloud.language.v1.ClassificationCategory;
import com.google.cloud.language.v1.Document;
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

public class ToxicityValidator implements AutoCloseable
{
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

	public Map<String, Float> getToxicityScores(String text)
	{

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

	public String getToxicityReport(String text)
	{
		Map<String, Float> scores = getToxicityScores(text);
		StringBuilder report = new StringBuilder();

		if(scores.isEmpty())
			report.append("No toxicity categories found or scores available.\n");
		else
			for(Map.Entry<String, Float> entry : scores.entrySet())
				report.append(String.format("%-25s: %.1f%%\n", entry.getKey(), entry.getValue() * 100));

		return report.toString();
	}

	public ModerateTextResponse moderateText(String text)
	{

		if(text == null || text.trim().isEmpty())
		{
			String errorMsg = "Input text cannot be null or empty.";
			throw new InvalidTextException(errorMsg);
		}

		LanguageServiceClient languageClient = apiClient.getClient();
		Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();
		ModerateTextRequest request = ModerateTextRequest.newBuilder().setDocument(doc).build();

		ModerateTextResponse response = languageClient.moderateText(request);
		return response;
	}

	public boolean isTextToxic(String text)
	{
		return isTextToxic(text, DEFAULT_TOXICITY_THRESHOLD);
	}

	public boolean isTextToxic(String text, float threshold)
	{

		if(threshold < 0.0f || threshold > 1.0f)
		{
			String errorMsg = "Threshold must be between 0.0 and 1.0, inclusive. Received: " + threshold;
			throw new InvalidThresholdException(threshold, errorMsg);
		}

		ModerateTextResponse response = moderateText(text);

		for(ClassificationCategory category : response.getModerationCategoriesList())
		{
			if(category.getConfidence() > threshold)
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public void close()
	{
		if (apiClient != null)
		{
			try
			{
				apiClient.close();
			}
			catch (Exception e)
			{
				logger.logError("close: Error encountered while closing GoogleApiClient.", e);
			}
		}
	}
}
