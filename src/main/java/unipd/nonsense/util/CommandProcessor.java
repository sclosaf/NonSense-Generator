package unipd.nonsense.util;

import unipd.nonsense.generator.SyntaxTreeBuilder;
import unipd.nonsense.model.SyntaxToken;

import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.analyzer.SentenceAnalyzer;
import unipd.nonsense.analyzer.ToxicityValidator;

import unipd.nonsense.generator.SentenceGenerator;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Tense;
import unipd.nonsense.model.Template;

import unipd.nonsense.exceptions.SentenceNotCachedException;
import unipd.nonsense.exceptions.IllegalToleranceException;
import unipd.nonsense.exceptions.InvalidNumberException;
import unipd.nonsense.exceptions.InvalidTenseException;
import unipd.nonsense.exceptions.InvalidTextException;
import unipd.nonsense.exceptions.InvalidTemplateException;

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

	private String cachedString;

	private float toxicityTolerance;

	public CommandProcessor() throws IOException
	{
		logger.logTrace("Initializing CommandProcessor");

		this.treeBuilder = new SyntaxTreeBuilder();
		this.analyzer = new SentenceAnalyzer();
		this.validator = new ToxicityValidator();
		this.generator = new SentenceGenerator();

		this.cachedString = "";
		toxicityTolerance = 0.7f;

		logger.logTrace("Initialized successfully");
	}

	public String generateRandom()
	{
		logger.logTrace("generateRandom: Generating random sentence");
		String result = generator.generateRandomSentence().getPattern();
		cachedString = result;
		logger.logDebug("generateRandom: Generated sentence: " + result);

		return result;
	}

	public String generateFrom(String str) throws IOException
	{
		logger.logTrace("generateFrom: Generating sentence from input");

		if(str == null)
		{
			logger.logError("generateFrom: Invalid text input - null");
			throw new InvalidTextException();
		}

		List<SyntaxToken> analysis = analyzer.getSyntaxTokensAsync(str).join();
		logger.logDebug("generateFrom: Retrieved syntax tokens: " + analysis.size());

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
				PartOfSpeech.Number number = token.getPartOfSpeech().getNumber();
				if((tense == PartOfSpeech.Tense.PAST || tense == PartOfSpeech.Tense.PRESENT || tense == PartOfSpeech.Tense.FUTURE) && (number == PartOfSpeech.Number.SINGULAR || number == PartOfSpeech.Number.PLURAL))
					verbList.add(new Verb(token.getText(), fromPartOfSpeechNumberToNumber(number), fromPartOfSpeechTenseToTense(tense)));
			}
		}

		logger.logDebug("generateFrom: Extracted - Nouns: " + nounList.size() + ", Adjectives: " + adjectiveList.size() + ", Verbs: " + verbList.size());

		String result = generator.generateSentenceWith(nounList, adjectiveList, verbList).getPattern();
		cachedString = result;

		logger.logDebug("generateFrom: Generated sentence: " + result);
		return result;
	}


	public String generateWithNumber(Number number)
	{
		logger.logTrace("generateWithNumber: Generating sentence with number");
		String result = generator.generateSentenceWithNumber(number).getPattern();
		cachedString = result;
		logger.logDebug("generateWithNumber: Generated sentence with number " + number + ": " + result);

		return result;
	}

	public String generateWithTense(Tense tense)
	{
		logger.logTrace("generateWithTense: Generating sentence with tense");
		String result = generator.generateSentenceWithTense(tense).getPattern();
		cachedString = result;
		logger.logDebug("generateWithTense: Generated sentence with tense " + tense + ": " + result);

		return result;
	}

	public String generateWithBoth(Number number, Tense tense)
	{
		logger.logTrace("generateWithBoth: Generating sentence with number and tense");
		String result = generator.generateSentenceWithTenseAndNumber(tense, number).getPattern();
		cachedString = result;
		logger.logDebug("generateWithBoth: Generated sentence with number " + number + " and tense " + tense + ": " + result);

		return result;

	}

	public String generateWithTemplate(Template template)
	{
		logger.logTrace("generateWithTemplate: Generating sentence from template");

		if(template == null)
		{
			logger.logError("generateWithTemplate: Invalid template - null");
			throw new InvalidTemplateException();
		}

		String result = generator.generateSentenceFromTemplate(template).getPattern();
		cachedString = result;
		logger.logDebug("generateWithTemplate: Generated sentence from template: " + result);

		return result;
	}

	public List<Template> getRandomTemplates()
	{
		logger.logTrace("getRandomTemplates: Getting random templates");
		List<Template> templates = generator.getRandomTemplates();
		logger.logDebug("getRandomTemplates: Retrieved " + templates.size() + " templates");

		return templates;
	}

	private Number fromPartOfSpeechNumberToNumber(PartOfSpeech.Number number)
	{
		logger.logTrace("fromPartOfSpeechNumberToNumber: Converting number");

		try
		{
			switch(number)
			{
				case SINGULAR: return Number.SINGULAR;
				case PLURAL: return Number.PLURAL;
				default: throw new InvalidNumberException();
			}
		}
		catch(InvalidNumberException e)
		{
			logger.logError("fromPartOfSpeechNumberToNumber: Invalid number conversion", e);
			throw e;
		}
	}

	private Tense fromPartOfSpeechTenseToTense(PartOfSpeech.Tense tense)
	{
		logger.logTrace("fromPartOfSpeechTenseToTense: Converting tense");

		try
		{
			switch(tense)
			{
				case PAST: return Tense.PAST;
				case PRESENT: return Tense.PRESENT;
				case FUTURE: return Tense.FUTURE;
				default: throw new InvalidTenseException();
			}
		}
		catch(InvalidTenseException e)
		{
			logger.logError("fromPartOfSpeechTenseToTense: Invalid tense conversion", e);
			throw e;
		}
	}

	public String generateSyntaxTree(String str) throws IOException
	{
		logger.logTrace("generateSyntaxTree: Generating syntax tree");

		if(str == null)
		{
			logger.logError("generateSyntaxTree: Text input null");
			throw new InvalidTextException();
		}

		cachedString = str;
		String result = treeBuilder.getSyntaxTree(analyzer.getSyntaxTokensAsync(str).join());
		logger.logDebug("generateSyntaxTree: Generated syntax tree for: " + str);

		return result;
	}

	public String analyzeSyntax(String str)
	{
		logger.logTrace("analyzeSyntax: Analyzing syntax");

		if(str == null)
		{
			logger.logError("analyzeSyntax: Text input null");
			throw new InvalidTextException();
		}

		cachedString = str;
		String result = analyzer.analyzeSyntaxAsync(str).join();
		logger.logDebug("analyzeSyntax: Analyzed syntax for: " + str);

		return result;
	}

	public String analyzeSentiment(String str)
	{
		logger.logTrace("analyzeSentiment: Analyzing sentiment");

		if(str == null)
		{
			logger.logError("analyzeSentiment: Text input null");
			throw new InvalidTextException();
		}

		cachedString = str;
		String result = analyzer.analyzeSentimentAsync(str).join();
		logger.logDebug("analyzeSentiment: Analyzed sentiment for: " + str);

		return result;
	}

	public String analyzeEntity(String str)
	{
		logger.logTrace("analyzeEntity: Analyzing entities");

		if(str == null)
		{
			logger.logError("analyzeEntity: Text input null");
			throw new InvalidTextException();
		}

		cachedString = str;
		String result = analyzer.analyzeEntitiesAsync(str).join();
		logger.logDebug("analyzeEntity: Analyzed entities for: " + str);

		return result;
	}

	public String analyzeToxicity(String str)
	{
		logger.logTrace("analyzeToxicity: Analyzing toxicity");

		if(str == null)
		{
			logger.logError("analyzeToxicity: Text input null");
			throw new InvalidTextException();
		}

		cachedString = str;

		String report = validator.getToxicityReportAsync(str).join();
		boolean isToxic = validator.isTextToxicAsync(cachedString, toxicityTolerance).join();

		StringBuilder result = new StringBuilder();
		result.append(report);

		result.append("\nTolerance threshold set to: ").append(toxicityTolerance).append("\n");
		result.append("Overall Assessment: ");
		result.append(isToxic ? "TEXT FLAGGED AS POTENTIALLY INAPPROPRIATE" : "Text within acceptable parameters");

		logger.logDebug("analyzeToxicity: Toxicity analysis completed. Toxic: " + isToxic + ", Tolerance: " + toxicityTolerance);
		return result.toString();
	}

	public void append(List<Noun> nounList, List<Adjective> adjectiveList, List<Verb> verbList) throws IOException
	{
		logger.logTrace("append: Appending words to dictionary");

		if(!nounList.isEmpty())
		{
			for(Noun noun : nounList)
				JsonUpdater.loadNoun(noun);

			logger.logDebug("append: Loaded " + nounList.size() + " nouns");
		}

		if(!adjectiveList.isEmpty())
		{
			for(Adjective adjective : adjectiveList)
				JsonUpdater.loadAdjective(adjective);

			logger.logDebug("append: Loaded " + adjectiveList.size() + " adjectives");
		}

		if(!verbList.isEmpty())
		{
			for(Verb verb : verbList)
				JsonUpdater.loadVerb(verb);

			logger.logDebug("append: Loaded " + verbList.size() + " verbs");
		}

		logger.logTrace("append: Appending completed");
	}

	public float getTolerance()
	{
		logger.logTrace("getTolerance: Getting toxicity tolerance");
		logger.logDebug("getTolerance: Current tolerance: " + toxicityTolerance);

		return toxicityTolerance;
	}

	public void setTolerance(float newTolerance)
	{
		logger.logTrace("setTolerance: Setting toxicity tolerance");

		if(newTolerance < 0.0f || newTolerance > 1.0f)
		{
			logger.logError("setTolerance: Invalid tolerance value: " + newTolerance);
			throw new IllegalToleranceException();
		}

		toxicityTolerance = newTolerance;
		logger.logDebug("setTolerance: Tolerance set to: " + newTolerance);
	}

	public boolean isSentenceCached()
	{
		logger.logTrace("isSentenceCached: Checking if sentence is cached");
		boolean isCached = cachedString != null && !cachedString.isEmpty();
		logger.logDebug("isSentenceCached: " + isCached);

		return isCached;
	}

	public String getCachedSentence()
	{
		logger.logTrace("getCachedSentence: Getting cached sentence");

		if(cachedString == null || cachedString.isEmpty())
		{
			logger.logError("getCachedSentence: No sentence cached");
			throw new SentenceNotCachedException();
		}

		logger.logDebug("getCachedSentence: Retrieved cached sentence: " + cachedString);

		return cachedString;
	}

	public void switchVerbosity()
	{
		logger.logTrace("switchVerbosity: Toggling verbosity");
		logger.switchVerboseMode();
		logger.logDebug("switchVerbosity: Verbose mode set to: " + logger.getVerbose());
	}

	public boolean isVerbose()
	{
		logger.logTrace("isVerbose: Checking verbosity");
		boolean verbose = logger.getVerbose();
		logger.logDebug("isVerbose: Verbose mode is " + verbose);

		return verbose;
	}

	@Override
	public void close()
	{
		logger.logTrace("close: Closing resources");

		try
		{
			if(analyzer != null)
				analyzer.close();

			if(validator != null)
				validator.close();

			logger.logTrace("close: Resources closed successfully");
		}
		catch(Exception e)
		{
			logger.logError("close: Error closing resources", e);
		}
	}
}
