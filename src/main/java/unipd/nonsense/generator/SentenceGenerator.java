package unipd.nonsense.generator;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Noun.Number;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Verb.Tense;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Template;
import unipd.nonsense.model.Template.TemplateType;
import unipd.nonsense.model.Template.Placeholder;

import unipd.nonsense.generator.RandomNounGenerator;
import unipd.nonsense.generator.RandomVerbGenerator;
import unipd.nonsense.generator.RandomAdjectiveGenerator;
import unipd.nonsense.generator.RandomTemplateGenerator;

import unipd.nonsense.util.JsonUpdater;
import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.exceptions.InvalidNumberException;
import unipd.nonsense.exceptions.InvalidTemplateTypeException;
import unipd.nonsense.exceptions.InvalidTemplateException;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

public class SentenceGenerator implements AutoCloseable
{
	private final RandomNounGenerator nounGenerator;
	private final RandomAdjectiveGenerator adjectiveGenerator;
	private final RandomVerbGenerator verbGenerator;
	private final RandomTemplateGenerator templateGenerator;
	private final LoggerManager logger = new LoggerManager(SentenceGenerator.class);
	private static final Random random = new Random();

	public SentenceGenerator() throws IOException
	{
		logger.logTrace("Starting initialization");

		try
		{
			logger.logTrace("Initializing component generators");
			this.nounGenerator = new RandomNounGenerator();
			this.adjectiveGenerator = new RandomAdjectiveGenerator();
			this.verbGenerator = new RandomVerbGenerator();
			this.templateGenerator = new RandomTemplateGenerator();
			logger.logTrace("Successfully initialized");
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize", e);
			throw e;
		}
	}

	public Template generateRandomSentence()
	{
		logger.logTrace("generateRandomSentence: Starting sentence generation");
		Number[] numbers = Number.values();
		Tense[] tenses = Tense.values();

		Number randomNumber = numbers[random.nextInt(numbers.length)];
		Tense randomTense = tenses[random.nextInt(tenses.length)];

		logger.logDebug("generateRandomSentence: Selected number: " + randomNumber + ", tense: " + randomTense);

		Template result = generateSentenceWithTenseAndNumber(randomTense, randomNumber);
		logger.logTrace("generateRandomSentence: Completed sentence generation");
		return result;
	}

	public Template generateSentenceWith(List<Noun> nounList, List<Adjective> adjectiveList, List<Verb> verbList) throws IOException
	{
		logger.logTrace("generateSentenceWith: Starting custom sentence generation");

		List<Noun> nouns = nounList != null ? new ArrayList<>(nounList) : new ArrayList<>();
		List<Adjective> adjectives = adjectiveList != null ? new ArrayList<>(adjectiveList) : new ArrayList<>();
		List<Verb> verbs = verbList != null ? new ArrayList<>(verbList) : new ArrayList<>();

		logger.logDebug("generateSentenceWith: Received " + nouns.size() + " nouns, " + adjectives.size() + " adjectives, " + verbs.size() + " verbs");

		Number number = getRandomNumber();
		TemplateType templateType = convertNumberToTemplateType(number);
		Template template = templateGenerator.getRandomTemplate(templateType);

		try
		{
			logger.logTrace("generateSentenceWith: Loading words into JsonUpdater");
			loadWordsIntoJsonUpdater(nouns, adjectives, verbs);
		}
		catch(IOException e)
		{
			logger.logError("generateSentenceWith: Failed to load new words into JsonUpdater", e);
			throw e;
		}

		logger.logTrace("generateSentenceWith: Replacing placeholders");
		replacePlaceholders(template, nouns, adjectives, verbs);

		logger.logTrace("generateSentenceWith: Completed custom sentence generation");
		return template;
	}

	private void loadWordsIntoJsonUpdater(List<Noun> nouns, List<Adjective> adjectives, List<Verb> verbs) throws IOException
	{
		logger.logTrace("loadWordsIntoJsonUpdater: Starting to load words");

		for(Noun noun : nouns)
		{
			logger.logDebug("loadWordsIntoJsonUpdater: Loading noun: " + noun.getNoun());
			JsonUpdater.loadNoun(noun);
		}

		for(Adjective adjective : adjectives)
		{
			logger.logDebug("loadWordsIntoJsonUpdater: Loading adjective: " + adjective.getAdjective());
			JsonUpdater.loadAdjective(adjective);
		}

		for(Verb verb : verbs)
		{
			logger.logDebug("loadWordsIntoJsonUpdater: Loading verb: " + verb.getVerb());
			JsonUpdater.loadVerb(verb);
		}

		logger.logTrace("loadWordsIntoJsonUpdater: Completed loading words");
	}

	private void replacePlaceholders(Template template, List<Noun> nouns, List<Adjective> adjectives, List<Verb> verbs)
	{
		logger.logTrace("replacePlaceholders: Starting placeholder replacement");

		replaceNounPlaceholders(template, nouns);
		replaceAdjectivePlaceholders(template, adjectives);
		replaceVerbPlaceholders(template, verbs);

		logger.logTrace("replacePlaceholders: Completed placeholder replacement");
	}

	private void replaceNounPlaceholders(Template template, List<Noun> nouns)
	{
		logger.logTrace("replaceNounPlaceholders: Starting noun replacement");
		Number number = convertTemplateTypeToNumber(template.getType());

		while(template.countPlaceholders(Placeholder.NOUN) > 0)
		{
			String noun = !nouns.isEmpty() ? nouns.remove(0).getNoun() : nounGenerator.getRandomNoun(number).getNoun();
			logger.logDebug("replaceNounPlaceholders: Replacing with noun: " + noun);
			template.replacePlaceholder(Placeholder.NOUN, noun);
		}

		logger.logTrace("replaceNounPlaceholders: Completed noun replacement");
	}

	private void replaceAdjectivePlaceholders(Template template, List<Adjective> adjectives)
	{
		logger.logTrace("replaceAdjectivePlaceholders: Starting adjective replacement");
		while(template.countPlaceholders(Placeholder.ADJECTIVE) > 0)
		{
			String adjective = !adjectives.isEmpty() ? adjectives.remove(0).getAdjective() : adjectiveGenerator.getRandomAdjective().getAdjective();
			logger.logDebug("replaceAdjectivePlaceholders: Replacing with adjective: " + adjective);
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
		}

		logger.logTrace("replaceAdjectivePlaceholders: Completed adjective replacement");
	}

	private void replaceVerbPlaceholders(Template template, List<Verb> verbs)
	{
		logger.logTrace("replaceVerbPlaceholders: Starting verb replacement");
		Tense tense = getRandomTense();

		while(template.countPlaceholders(Placeholder.VERB) > 0)
		{
			String verb = !verbs.isEmpty() ? verbs.remove(0).getVerb() : verbGenerator.getRandomVerb(tense).getVerb();
			logger.logDebug("replaceVerbPlaceholders: Replacing with verb: " + verb + " (tense: " + tense + ")");
			template.replacePlaceholder(Placeholder.VERB, verb);
		}

		logger.logTrace("replaceVerbPlaceholders: Completed verb replacement");
	}

	public Template generateSentenceWithTense(Tense tense)
	{
		logger.logDebug("generateSentenceWithTense: Starting generation with tense: " + tense);
		Template result = generateSentenceWithTenseAndNumber(tense, getRandomNumber());
		logger.logTrace("generateSentenceWithTense: Completed generation");
		return result;
	}

	public Template generateSentenceWithNumber(Number number)
	{
		logger.logDebug("generateSentenceWithNumber: Starting generation with number: " + number);
		Template result = generateSentenceWithTenseAndNumber(getRandomTense(), number);
		logger.logTrace("generateSentenceWithNumber: Completed generation");
		return result;
	}

	public Template generateSentenceWithTenseAndNumber(Tense tense, Number number)
	{
		logger.logDebug("generateSentenceWithTenseAndNumber: Starting generation with tense: " + tense + ", number: " + number);
		TemplateType templateType = convertNumberToTemplateType(number);
		Template template = templateGenerator.getRandomTemplate(templateType);

		while(template.countPlaceholders(Placeholder.NOUN) != 0)
		{
			String noun = nounGenerator.getRandomNoun(number).getNoun();
			logger.logDebug("generateSentenceWithTenseAndNumber: Replacing noun with: " + noun);
			template.replacePlaceholder(Placeholder.NOUN, noun);
		}

		while(template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
		{
			String adjective = adjectiveGenerator.getRandomAdjective().getAdjective();
			logger.logDebug("generateSentenceWithTenseAndNumber: Replacing adjective with: " + adjective);
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
		}

		while(template.countPlaceholders(Placeholder.VERB) != 0)
		{
			String verb = verbGenerator.getRandomVerb(tense).getVerb();
			logger.logDebug("generateSentenceWithTenseAndNumber: Replacing verb with: " + verb);
			template.replacePlaceholder(Placeholder.VERB, verb);
		}

		logger.logTrace("generateSentenceWithTenseAndNumber: Completed generation");
		return template;
	}

	public Template generateSentenceFromTemplate(Template template)
	{
		logger.logTrace("generateSentenceFromTemplate: Starting generation from template");

		if(template == null || template.getPattern().isEmpty())
		{
			logger.logError("generateSentenceFromTemplate: Invalid template provided");
			throw new InvalidTemplateException();
		}

		Number number = convertTemplateTypeToNumber(template.getType());

		while(template.countPlaceholders(Placeholder.NOUN) != 0)
		{
			String noun = nounGenerator.getRandomNoun(number).getNoun();
			logger.logDebug("generateSentenceFromTemplate: Replacing noun with: " + noun);
			template.replacePlaceholder(Placeholder.NOUN, noun);
		}

		while(template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
		{
			String adjective = adjectiveGenerator.getRandomAdjective().getAdjective();
			logger.logDebug("generateSentenceFromTemplate: Replacing adjective with: " + adjective);
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
		}

		while(template.countPlaceholders(Placeholder.VERB) != 0)
		{
			String verb = verbGenerator.getRandomVerb(getRandomTense()).getVerb();
			logger.logDebug("generateSentenceFromTemplate: Replacing verb with: " + verb);
			template.replacePlaceholder(Placeholder.VERB, verb);
		}

		logger.logTrace("generateSentenceFromTemplate: Completed generation");
		return template;
	}

	public List<Template> getRandomTemplates()
	{
		logger.logTrace("getRandomTemplates: Starting to get random templates");
		List<Template> templateList = new ArrayList<>();

		for(int i = 0; i < 5; ++i)
		{
			Template template = templateGenerator.getRandomTemplate();
			templateList.add(template);
			logger.logDebug("getRandomTemplates: Added template: " + template.getPattern());
		}

		logger.logTrace("getRandomTemplates: Completed getting templates");

		return templateList;
	}

	private Number getRandomNumber()
	{
		logger.logTrace("getRandomNumber: Getting random number");
		Number[] numbers = Number.values();
		Number randomNumber = numbers[random.nextInt(numbers.length)];
		logger.logDebug("getRandomNumber: Selected number: " + randomNumber);

		return randomNumber;
	}

	private Tense getRandomTense()
	{
		logger.logTrace("getRandomTense: Getting random tense");
		Tense[] tenses = Tense.values();
		Tense randomTense = tenses[random.nextInt(tenses.length)];
		logger.logDebug("getRandomTense: Selected tense: " + randomTense);

		return randomTense;
	}

	private TemplateType convertNumberToTemplateType(Number number)
	{
		logger.logTrace("convertNumberToTemplateType: Converting number to template type");
		try
		{
			switch(number)
			{
				case SINGULAR:
					logger.logDebug("convertNumberToTemplateType: Converted to SINGULAR");
					return TemplateType.SINGULAR;

				case PLURAL:
					logger.logDebug("convertNumberToTemplateType: Converted to PLURAL");
					return TemplateType.PLURAL;

				default:
					logger.logError("convertNumberToTemplateType: Invalid number value: " + number);
					throw new InvalidNumberException();
			}
		}
		catch(InvalidNumberException e)
		{
			logger.logError("convertNumberToTemplateType: Invalid number value: " + number, e);
			throw e;
		}
	}

	private Number convertTemplateTypeToNumber(TemplateType type)
	{
		logger.logTrace("convertTemplateTypeToNumber: Converting template type to number");
		try
		{
			switch(type)
			{
				case SINGULAR:
					logger.logDebug("convertTemplateTypeToNumber: Converted to SINGULAR");
					return Number.SINGULAR;

				case PLURAL:
					logger.logDebug("convertTemplateTypeToNumber: Converted to PLURAL");
					return Number.PLURAL;

				default:
					logger.logError("convertTemplateTypeToNumber: Invalid template type: " + type);
					throw new InvalidTemplateTypeException();
			}
		}
		catch(InvalidTemplateTypeException e)
		{
			logger.logError("convertTemplateTypeToNumber: Invalid template type: " + type, e);
			throw e;
		}
	}

	@Override
	public void close()
	{
		logger.logTrace("close: Starting cleanup");
		nounGenerator.cleanup();
		adjectiveGenerator.cleanup();
		verbGenerator.cleanup();
		templateGenerator.cleanup();
		logger.logTrace("close: Completed cleanup");
	}
}
