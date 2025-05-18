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

public class SentenceAnalyzer implements AutoCloseable
{
	private final LanguageServiceClient languageClient;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private static final String credentialsPath = "/credentials.json";

	public SentenceAnalyzer()
	{
		try
		{
			GoogleApiClient manager = new GoogleApiClient(credentialsPath);
			this.languageClient = manager.getClient();
		}
		catch (Exception e)
		{
			throw new FailedClientInitializationException();
		}
	}

	public CompletableFuture<String> analyzeSyntaxAsync(String text)
	{
		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					return analyzeSyntax(text);
				}
				catch(IOException e)
				{
					throw new FailedAnalysisException();
				}
			}, executor);
	}

	private String analyzeSyntax(String text) throws IOException
	{
		validateInput(text);
		Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();

		AnalyzeSyntaxRequest request = AnalyzeSyntaxRequest.newBuilder().setDocument(doc).setEncodingType(EncodingType.UTF16).build();
		AnalyzeSyntaxResponse response = languageClient.analyzeSyntax(request);

		StringBuilder report = new StringBuilder();
		int tokenIndex = 1;

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

		return report.toString().trim();
	}

	private void addIfNotDefault(StringBuilder sb, String prefix, Object value, Object defaultValue)
	{
		if(!value.equals(defaultValue))
			sb.append(prefix).append(value).append("\n");
	}

	public CompletableFuture<String> analyzeSentimentAsync(String text)
	{
		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					return analyzeSentiment(text);
				}
				catch(IOException e)
				{
					throw new FailedAnalysisException();
				}
			}, executor);
	}

	private String analyzeSentiment(String text) throws IOException
	{
		validateInput(text);
		Document doc = buildDocument(text);
		AnalyzeSentimentResponse response = languageClient.analyzeSentiment(doc);
		Sentiment sentiment = response.getDocumentSentiment();

		if (sentiment == null)
			return "No sentiment detected.";

		return String.format
			(
				"Sentiment Score: %.2f (Magnitude: %.2f)",
				sentiment.getScore(),
				sentiment.getMagnitude()
			);
	}

	public CompletableFuture<String> analyzeEntitiesAsync(String text)
	{
		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					return analyzeEntities(text);
				}
				catch(IOException e)
				{
					throw new FailedAnalysisException();
				}
			}, executor);
	}

	private String analyzeEntities(String text) throws IOException
	{
		validateInput(text);
		Document doc = buildDocument(text);
		AnalyzeEntitiesRequest request = AnalyzeEntitiesRequest.newBuilder().setDocument(doc).setEncodingType(EncodingType.UTF16).build();
		AnalyzeEntitiesResponse response = languageClient.analyzeEntities(request);

		StringBuilder report = new StringBuilder();
		List<Entity> entities = response.getEntitiesList();

		if(entities.isEmpty())
			return "No entities detected.";

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

		return report.toString().trim();
	}

	public CompletableFuture<List<SyntaxToken>> getSyntaxTokensAsync(String text)
	{
		return CompletableFuture.supplyAsync(() ->
			{
				try
				{
					return getSyntaxTokens(text);
				}
				catch(IOException e)
				{
					throw new FailedAnalysisException();
				}
			}, executor);
	}

	private List<SyntaxToken> getSyntaxTokens(String text) throws IOException
	{
		validateInput(text);
		Document doc = buildDocument(text);

		AnalyzeSyntaxRequest request = AnalyzeSyntaxRequest.newBuilder().setDocument(doc).setEncodingType(EncodingType.UTF16).build();

		AnalyzeSyntaxResponse response = languageClient.analyzeSyntax(request);

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
		return tokens;
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
			if(languageClient != null)
				languageClient.close();
		}
		catch(Exception e)
		{
			throw e;
		}
		finally
		{
			executor.shutdownNow();
		}
	}
}
