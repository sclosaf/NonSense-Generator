package unipd.nonsense.analyzer;

import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.ModerateTextRequest;
import com.google.cloud.language.v1.ModerateTextResponse;
import com.google.cloud.language.v1.ClassificationCategory;
import unipd.nonsense.util.GoogleApiClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ToxicityValidator implements AutoCloseable
{
	private final GoogleApiClient apiClient;
	private static final float DEFAULT_TOXICITY_THRESHOLD = 0.7f;

	public ToxicityValidator(GoogleApiClient apiClient)
	{
		if(apiClient == null)
			throw new IllegalArgumentException("GoogleApiClient cannot be null");

		this.apiClient = apiClient;
	}

	public Map<String, Float> getToxicityScores(String text) throws IOException
	{
		ModerateTextResponse response = moderateText(text);
		Map<String, Float> scores = new HashMap<>();

		for(ClassificationCategory category : response.getModerationCategoriesList())
			scores.put(category.getName(), category.getConfidence());

		return scores;
	}

	public String getToxicityReport(String text) throws IOException
	{
		Map<String, Float> scores = getToxicityScores(text);
		StringBuilder report = new StringBuilder();

		report.append("Toxicity Analysis Report:\n");
		report.append("------------------------\n");

		for (Map.Entry<String, Float> entry : scores.entrySet())
			report.append(String.format("%-20s: %.1f%%\n", entry.getKey(), entry.getValue() * 100));

		return report.toString();
	}

	public ModerateTextResponse moderateText(String text) throws IOException
	{
		if(text == null || text.trim().isEmpty())
			throw new IllegalArgumentException("Text cannot be null or empty");

		LanguageServiceClient languageClient = apiClient.getClient();

		Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();

		ModerateTextRequest request = ModerateTextRequest.newBuilder().setDocument(doc).build();

		return languageClient.moderateText(request);
	}

	public boolean isTextToxic(String text) throws IOException
	{
		return isTextToxic(text, DEFAULT_TOXICITY_THRESHOLD);
	}

	public boolean isTextToxic(String text, float threshold) throws IOException
	{
		if(threshold < 0 || threshold > 1)
			throw new IllegalArgumentException("Threshold must be between 0 and 1");

		ModerateTextResponse response = moderateText(text);
		for(ClassificationCategory category : response.getModerationCategoriesList())
			if(category.getConfidence() > threshold)
				return true;

		return false;
	}

	@Override
	public void close()
	{
		if(apiClient != null)
			apiClient.close();
	}
}
