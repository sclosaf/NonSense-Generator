package unipd.nonsense.analyzer;

import unipd.nonsense.exceptions.InvalidThresholdException;
import unipd.nonsense.exceptions.InvalidTextException;

import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.ModerateTextRequest;
import com.google.cloud.language.v1.ModerateTextResponse;
import com.google.cloud.language.v1.ClassificationCategory;

import unipd.nonsense.util.GoogleApiClient;
import unipd.nonsense.util.LoggerManager;

import com.google.api.gax.rpc.ApiException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ToxicityValidator implements AutoCloseable
{
	private final GoogleApiClient apiClient;
	private static final float DEFAULT_TOXICITY_THRESHOLD = 0.7f;
	private LoggerManager logger = new LoggerManager(ToxicityValidator.class);

	public ToxicityValidator() throws IOException
	{
		logger.logInfo("Initializing ToxicityValidator");

		try
		{
			this.apiClient = new GoogleApiClient("/credentials.json");
			logger.logInfo("Initialization completed successfully");
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize GoogleApiClient", e);
			throw e;
		}
	}

	public Map<String, Float> getToxicityScores(String text)
	{
		logger.logDebug("getToxicityScores: Starting text analysis - length: " + (text != null ? text.length() : 0));

		ModerateTextResponse response = moderateText(text);
		Map<String, Float> scores = new HashMap<>();

		for(ClassificationCategory category : response.getModerationCategoriesList())
			scores.put(category.getName(), category.getConfidence());

		logger.logDebug("getToxicityScores: Analysis completed - categories found: " + scores.size());

		StringBuilder details = new StringBuilder();
		scores.forEach((cat, score) -> details.append(cat).append(":").append(score).append(" "));

		logger.logDebug("getToxicityScores: Score details - " + details.toString());

		return scores;
	}

	public String getToxicityReport(String text)
	{
		logger.logInfo("getToxicityReport: Starting report generation");

		Map<String, Float> scores = getToxicityScores(text);
		StringBuilder report = new StringBuilder();

		report.append("Toxicity Analysis Report:\n");
		report.append("------------------------\n");

		for (Map.Entry<String, Float> entry : scores.entrySet())
			report.append(String.format("%-20s: %.1f%%\n", entry.getKey(), entry.getValue() * 100));

		logger.logInfo("getToxicityReport: Report generated successfully");

		return report.toString();
	}

	public ModerateTextResponse moderateText(String text)
	{
		logger.logInfo("moderateText: Sending request to Google API");

		if(text == null || text.trim().isEmpty())
		{
			logger.logError("moderateText: Invalid text (null or empty)");
			throw new InvalidTextException();
		}

		LanguageServiceClient languageClient = apiClient.getClient();

		Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();
		ModerateTextRequest request = ModerateTextRequest.newBuilder().setDocument(doc).build();

		logger.logInfo("moderateText: Request prepared, sending to API");

		ModerateTextResponse response = languageClient.moderateText(request);

		logger.logDebug("moderateText: Response received from Google API - categories: " + response.getModerationCategoriesCount());

		return response;
	}

	public boolean isTextToxic(String text) throws IOException
	{
		logger.logDebug("isTextToxic: Checking toxicity with default threshold " + DEFAULT_TOXICITY_THRESHOLD);
		return isTextToxic(text, DEFAULT_TOXICITY_THRESHOLD);
	}

	public boolean isTextToxic(String text, float threshold) throws IOException
	{
		logger.logDebug("isTextToxic: Starting toxicity verification with threshold " + threshold);

		if(threshold < 0 || threshold > 1)
		{
			logger.logError("isTextToxic: Invalid threshold value: " + threshold);
			throw new InvalidThresholdException(threshold);
		}

		ModerateTextResponse response = moderateText(text);
		boolean isToxic = false;

		for(ClassificationCategory category : response.getModerationCategoriesList())
		{
			if(category.getConfidence() > threshold)
			{
				logger.logDebug("isTextToxic: Text identified as toxic - category: " + category.getName() + ", confidence: " + category.getConfidence());
				isToxic = true;
				break;
			}
		}

		if(!isToxic)
			logger.logDebug("isTextToxic: Text not toxic (all categories below threshold " + threshold + ")");

		return isToxic;
	}

	@Override
	public void close()
	{
		logger.logInfo("close: Closing ToxicityValidator resources");

		if(apiClient != null)
		{
			apiClient.close();
			logger.logInfo("close: GoogleApiClient closed successfully");
		}
	}
}
