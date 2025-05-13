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

import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class SentenceAnalyzer implements AutoCloseable
{
	private LanguageServiceClient languageClient;
	private String credentialsPath = "/credentials.json";

	public SentenceAnalyzer()
	{
		try
		{
			GoogleApiClient manager = new GoogleApiClient(credentialsPath);
			this.languageClient = manager.getClient();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to initialize LanguageServiceClient: " + e.getMessage(), e);
		}
	}

	public String analyzeSyntaxInput(String text) throws IOException
	{
		Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();

		AnalyzeSyntaxRequest request = AnalyzeSyntaxRequest.newBuilder().setDocument(doc).setEncodingType(EncodingType.UTF16).build();

		AnalyzeSyntaxResponse response = languageClient.analyzeSyntax(request);

		StringBuilder report = new StringBuilder();

		int tokenIndex = 1;
		for(Token token : response.getTokensList())
		{
			report.append("Token ").append(tokenIndex).append(": ").append(token.getText().getContent()).append("\n");
			report.append("  - Lemma: ").append(token.getLemma()).append("\n");
			report.append("  - Part of Speech: ").append(token.getPartOfSpeech().getTag()).append("\n");

			if(token.getPartOfSpeech().getAspect() != PartOfSpeech.Aspect.ASPECT_UNKNOWN)
				report.append("  - Aspect: ").append(token.getPartOfSpeech().getAspect()).append("\n");

			if(token.getPartOfSpeech().getCase() != PartOfSpeech.Case.CASE_UNKNOWN)
				report.append("  - Case: ").append(token.getPartOfSpeech().getCase()).append("\n");

			if(token.getPartOfSpeech().getForm() != PartOfSpeech.Form.FORM_UNKNOWN)
				report.append("  - Form: ").append(token.getPartOfSpeech().getForm()).append("\n");

			if(token.getPartOfSpeech().getGender() != PartOfSpeech.Gender.GENDER_UNKNOWN)
				report.append("  - Gender: ").append(token.getPartOfSpeech().getGender()).append("\n");

			if(token.getPartOfSpeech().getMood() != PartOfSpeech.Mood.MOOD_UNKNOWN)
				report.append("  - Mood: ").append(token.getPartOfSpeech().getMood()).append("\n");

			if(token.getPartOfSpeech().getNumber() != PartOfSpeech.Number.NUMBER_UNKNOWN)
				report.append("  - Number: ").append(token.getPartOfSpeech().getNumber()).append("\n");

			if(token.getPartOfSpeech().getPerson() != PartOfSpeech.Person.PERSON_UNKNOWN)
				report.append("  - Person: ").append(token.getPartOfSpeech().getPerson()).append("\n");

			if(token.getPartOfSpeech().getProper() != PartOfSpeech.Proper.PROPER_UNKNOWN)
				report.append("  - Proper: ").append(token.getPartOfSpeech().getProper()).append("\n");

			if(token.getPartOfSpeech().getReciprocity() != PartOfSpeech.Reciprocity.RECIPROCITY_UNKNOWN)
				report.append("  - Reciprocity: ").append(token.getPartOfSpeech().getReciprocity()).append("\n");

			if(token.getPartOfSpeech().getTense() != PartOfSpeech.Tense.TENSE_UNKNOWN)
				report.append("  - Tense: ").append(token.getPartOfSpeech().getTense()).append("\n");

			if(token.getPartOfSpeech().getVoice() != PartOfSpeech.Voice.VOICE_UNKNOWN)
				report.append("  - Voice: ").append(token.getPartOfSpeech().getVoice()).append("\n");

			report.append("  - Dependency\n");
			report.append("      Head Token Index: ").append(token.getDependencyEdge().getHeadTokenIndex()).append("\n");
			report.append("      Label: ").append(token.getDependencyEdge().getLabel()).append("\n");

			report.append("\n");

			++tokenIndex;
		}

		return report.toString();
	}

	public String analyzeSentimentInput(String text) throws IOException
	{
		Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();

		AnalyzeSentimentResponse response = languageClient.analyzeSentiment(doc);
		Sentiment sentiment = response.getDocumentSentiment();

		StringBuilder report = new StringBuilder();

		if(sentiment == null)
		{
			report.append("No sentiment detected in the provided text.\n\n");
			return report.toString();
		}

		report.append("Sentiment Score: ").append(sentiment.getScore()).append("\n");
		report.append("Sentiment Magnitude: ").append(sentiment.getMagnitude()).append("\n\n");

		return report.toString();
	}

	public String analyzeEntitiesInput(String text) throws IOException
	{
		Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();

		AnalyzeEntitiesRequest request = AnalyzeEntitiesRequest.newBuilder().setDocument(doc).setEncodingType(EncodingType.UTF16).build();

		AnalyzeEntitiesResponse response = languageClient.analyzeEntities(request);

		StringBuilder report = new StringBuilder();
		report.append("Analyzed Text: ").append(text).append("\n\n");

		List<Entity> entities = response.getEntitiesList();

		if(entities.isEmpty())
		{
			report.append("No entities detected in the provided text.\n\n");
			return report.toString();
		}

		int entityIndex = 1;
		for(Entity entity : entities)
		{
			report.append("Entity ").append(entityIndex).append(": ").append(entity.getName()).append("\n");
			report.append("  - Type: ").append(entity.getType()).append("\n");
			report.append("  - Salience: ").append(String.format("%.3f", entity.getSalience())).append("\n");

			if(!entity.getMetadataMap().isEmpty())
			{
				report.append("  - Metadata:\n");
				for (Map.Entry<String, String> entry : entity.getMetadataMap().entrySet())
					report.append("      ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
			}

			if(!entity.getMentionsList().isEmpty())
			{
				report.append("  - Mentions:\n");
				for(EntityMention mention : entity.getMentionsList())
				{
					report.append("      Mention Content: ").append(mention.getText().getContent()).append("\n");
					report.append("      Begin Offset: ").append(mention.getText().getBeginOffset()).append("\n");
					report.append("      Mention Type: ").append(mention.getType()).append("\n");
				}
			}

			report.append("\n");
			++entityIndex;
		}

		return report.toString();
	}

	public List<SyntaxToken> getSyntaxTokens(String text) throws IOException
	{
		Document doc = Document.newBuilder().setContent(text).setType(Document.Type.PLAIN_TEXT).build();

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

	@Override
	public void close()
	{
		if(languageClient != null)
			languageClient.close();
	}
}
