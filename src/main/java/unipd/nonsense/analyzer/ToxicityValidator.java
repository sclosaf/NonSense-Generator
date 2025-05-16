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
			this.logger.logError("GoogleApiClient cannot be null.");
			throw new IllegalArgumentException("GoogleApiClient cannot be null.");
		}

		this.apiClient = apiClient;
		this.logger.logInfo("ToxicityValidator initialized with provided dependencies.");
	}

	public ToxicityValidator() throws IOException
	{
		this(new GoogleApiClient("/credentials.json"), new LoggerManager(ToxicityValidator.class));
		this.logger.logInfo("ToxicityValidator initialized using default constructor.");
	}

	public Map<String, Float> getToxicityScores(String text)
	{
		logger.logDebug("getToxicityScores: Analyzing text of length " + (text != null ? text.length() : 0));

		ModerateTextResponse response = moderateText(text);
		Map<String, Float> scores = new HashMap<>();

		for(ClassificationCategory category : response.getModerationCategoriesList())
			scores.put(category.getName(), category.getConfidence());

		logger.logDebug("getToxicityScores: Analysis completed. Categories found: " + scores.size());

		if(!scores.isEmpty())
		{
			StringBuilder details = new StringBuilder("Scores: ");
			scores.forEach((cat, score) -> details.append(cat).append("=").append(String.format("%.2f", score)).append(" "));
			logger.logDebug(details.toString().trim());
		}

		return scores;
	}

	public String getToxicityReport(String text)
	{
		logger.logInfo("getToxicityReport: Generating report.");

		Map<String, Float> scores = getToxicityScores(text);
		StringBuilder report = new StringBuilder();

		if(scores.isEmpty())
			report.append("No toxicity categories found or scores available.\n");
		else
			for(Map.Entry<String, Float> entry : scores.entrySet())
				report.append(String.format("%-25s: %.1f%%\n", entry.getKey(), entry.getValue() * 100));

		logger.logInfo("getToxicityReport: Report generated.");
		return report.toString();
	}

	public ModerateTextResponse moderateText(String text)
	{
		logger.logInfo("moderateText: Preparing to moderate text.");

		if(text == null || text.trim().isEmpty())
		{
			String errorMsg = "Input text cannot be null or empty.";
			logger.logError("moderateText: " + errorMsg);
			throw new InvalidTextException(errorMsg);
		}

		LanguageServiceClient languageClient = apiClient.getClient();
		Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();
		ModerateTextRequest request = ModerateTextRequest.newBuilder().setDocument(doc).build();

		logger.logDebug("moderateText: Sending request to Google Natural Language API.");
		ModerateTextResponse response = languageClient.moderateText(request);
		logger.logDebug("moderateText: Response received. Categories count: " + response.getModerationCategoriesCount());
		return response;
	}

	public boolean isTextToxic(String text)
	{
		logger.logDebug("isTextToxic: Checking toxicity with default threshold " + DEFAULT_TOXICITY_THRESHOLD);
		return isTextToxic(text, DEFAULT_TOXICITY_THRESHOLD);
	}

	public boolean isTextToxic(String text, float threshold)
	{
		logger.logDebug("isTextToxic: Verifying toxicity with threshold " + threshold);

		if(threshold < 0.0f || threshold > 1.0f)
		{
			String errorMsg = "Threshold must be between 0.0 and 1.0, inclusive. Received: " + threshold;
			logger.logError("isTextToxic: " + errorMsg);
			throw new InvalidThresholdException(threshold, errorMsg);
		}

		ModerateTextResponse response = moderateText(text);

		for(ClassificationCategory category : response.getModerationCategoriesList())
		{
			if(category.getConfidence() > threshold)
			{
				logger.logDebug("isTextToxic: Text identified as toxic. Category: " + category.getName() +
								", Confidence: " + String.format("%.2f", category.getConfidence()) +
								", Threshold: " + String.format("%.2f", threshold));
				return true;
			}
		}

		logger.logDebug("isTextToxic: Text not considered toxic for threshold " + threshold);
		return false;
	}

	@Override
	public void close()
	{
		logger.logInfo("close: Closing ToxicityValidator resources.");

		if (apiClient != null)
		{
			try
			{
				apiClient.close();
				logger.logInfo("close: GoogleApiClient closed successfully.");
			}
			catch (Exception e)
			{
				logger.logError("close: Error encountered while closing GoogleApiClient.", e);
			}
		}
	}
}
