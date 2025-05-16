package unipd.nonsense.util;

import unipd.nonsense.generator.SyntaxTreeBuilder;
import unipd.nonsense.model.SyntaxToken;

import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.analyzer.SentenceAnalyzer;
import unipd.nonsense.analyzer.ToxicityValidator;

import unipd.nonsense.generator.SentenceGenerator;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Noun.Number;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Verb.Tense;
import unipd.nonsense.model.Template;

import unipd.nonsense.exceptions.SentenceNotCachedException;
import unipd.nonsense.exceptions.IllegalToleranceException;
import unipd.nonsense.exceptions.InvalidNumberException;
import unipd.nonsense.exceptions.InvalidTenseException;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

import com.google.cloud.language.v1.PartOfSpeech;

import static com.google.cloud.language.v1.PartOfSpeech.Number.SINGULAR;
import static com.google.cloud.language.v1.PartOfSpeech.Number.PLURAL;

import static com.google.cloud.language.v1.PartOfSpeech.Tense.PAST;
import static com.google.cloud.language.v1.PartOfSpeech.Tense.PRESENT;
import static com.google.cloud.language.v1.PartOfSpeech.Tense.FUTURE;

public class CommandProcessor implements AutoCloseable
{
	private static SyntaxTreeBuilder treeBuilder;
	private static SentenceAnalyzer analyzer;
	private static ToxicityValidator validator;
	private static SentenceGenerator generator;
	private LoggerManager logger = new LoggerManager(CommandProcessor.class);

	private static String cachedString;

	private float toxicityTolerance;

	public CommandProcessor() throws IOException
	{
		this.treeBuilder = new SyntaxTreeBuilder();
		this.analyzer = new SentenceAnalyzer();
		this.validator = new ToxicityValidator();
		this.generator = new SentenceGenerator();

		this.cachedString = "";
		toxicityTolerance = 0.7f;
	}

	public String generateRandom()
	{
		return cachedString = generator.generateRandomSentence().getPattern();
	}

	public String generateFrom(String str) throws IOException
	{
		List<SyntaxToken> analysis = analyzer.getSyntaxTokens(str);

		List<Noun> nounList = new ArrayList<>();
		List<Adjective> adjectiveList = new ArrayList<>();
		List<Verb> verbList = new ArrayList<>();

		for(SyntaxToken token : analysis)
		{
			String posTag = token.getPosTag();
			if(posTag.equals("NOUN"))
			{
				PartOfSpeech.Number number = token.getPartOfSpeech().getNumber();
				if(number == PartOfSpeech.Number.SINGULAR || number == PartOfSpeech.Number.PLURAL)
					nounList.add(new Noun(token.getText(), fromPartOfSpeechNumberToNumber(number)));
			}
			else if(posTag.equals("ADJ"))
				adjectiveList.add(new Adjective(token.getText()));
			else if(posTag.equals("VERB"))
			{
				PartOfSpeech.Tense tense = token.getPartOfSpeech().getTense();
				if(tense == PartOfSpeech.Tense.PAST || tense == PartOfSpeech.Tense.PRESENT || tense == PartOfSpeech.Tense.FUTURE)
					verbList.add(new Verb(token.getText(), fromPartOfSpeechTenseToTense(tense)));
			}
		}

		return cachedString = generator.generateSentenceWith(nounList, adjectiveList, verbList).getPattern();
	}

	public String generateWithNumber(Number num)
	{
		return cachedString = generator.generateSentenceWithNumber(num).getPattern();
	}

	public String generateWithTense(Tense tense)
	{
		return cachedString = generator.generateSentenceWithTense(tense).getPattern();
	}

	public String generateWithBoth(Number num, Tense tense)
	{
		return cachedString = generator.generateSentenceWithTenseAndNumber(tense, num).getPattern();
	}

	private Number fromPartOfSpeechNumberToNumber(PartOfSpeech.Number num)
	{
		switch(num)
		{
			case SINGULAR: return Number.SINGULAR;
			case PLURAL: return Number.PLURAL;
			default: throw new InvalidNumberException();
		}
	}

	private Tense fromPartOfSpeechTenseToTense(PartOfSpeech.Tense tense)
	{
		switch(tense)
		{
			case PAST: return Tense.PAST;
			case PRESENT: return Tense.PRESENT;
			case FUTURE: return Tense.FUTURE;
			default: throw new InvalidTenseException();
		}
	}

	public String generateSyntaxTree(String str) throws IOException
	{
		cachedString = str;

		return treeBuilder.getSyntaxTree(analyzer.getSyntaxTokens(str));
	}

	public String analyzeSyntax(String str)
	{
		cachedString = str;

		try
		{
			return analyzer.analyzeSyntaxInput(str);
		}
		catch(IOException e)
		{
			return "Error during syntax analysis: " + e.getMessage();
		}
	}

	public String analyzeSentiment(String str)
	{
		cachedString = str;

		try
		{
			return analyzer.analyzeSentimentInput(str);
		}
		catch(IOException e)
		{
			return "Error during sentiment analysis: " + e.getMessage();
		}
	}

	public String analyzeEntity(String str)
	{
		cachedString = str;

		try
		{
			return analyzer.analyzeEntitiesInput(str);
		}
		catch(IOException e)
		{
			return "Error during entity analysis: " + e.getMessage();
		}
	}

	public String analyzeToxicity(String str)
	{
		cachedString = str;

		String report = validator.getToxicityReport(str);

		boolean isToxic = validator.isTextToxic(cachedString, toxicityTolerance);

		StringBuilder result = new StringBuilder();
		result.append(report);

		result.append("\nTolerance threshold set to: ").append(toxicityTolerance).append("\n");
		result.append("Overall Assessment: ");
		result.append(isToxic ? "TEXT FLAGGED AS POTENTIALLY INAPPROPRIATE" : "Text within acceptable parameters");
		return result.toString();
	}

	public void append(List<Noun> nounList, List<Adjective> adjectiveList, List<Verb> verbList) throws IOException
	{
		for(Noun noun : nounList)
			JsonUpdater.loadNoun(noun);

		for(Adjective adjective : adjectiveList)
			JsonUpdater.loadAdjective(adjective);

		for(Verb verb : verbList)
			JsonUpdater.loadVerb(verb);
	}

	public float getTolerance()
	{
		return toxicityTolerance;
	}

	public void setTolerance(float newTolerance)
	{
		if(newTolerance < 0.0f || newTolerance > 1.0f)
			throw new IllegalToleranceException();

		toxicityTolerance = newTolerance;
	}

	public boolean isSentenceCached()
	{
		return cachedString != null && !cachedString.isEmpty();
	}

	public String getCachedSentence()
	{
		if(cachedString == null || cachedString.isEmpty())
			throw new SentenceNotCachedException();

		return cachedString;
	}

	public void switchVerbosity()
	{
		logger.switchVerboseMode();
	}

	public boolean isVerbose()
	{
		return logger.getVerbose();
	}

	@Override
	public void close()
	{
		try
		{
			if(analyzer != null)
				analyzer.close();

			if(validator != null)
				validator.close();
		}
		catch(Exception e)
		{
			logger.logError("Error closing resources", e);
		}
	}
}
