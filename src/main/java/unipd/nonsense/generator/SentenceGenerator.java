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

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

public class SentenceGenerator
{
	private RandomNounGenerator nounGenerator;
	private RandomAdjectiveGenerator adjectiveGenerator;
	private RandomVerbGenerator verbGenerator;
	private RandomTemplateGenerator templateGenerator;
	private LoggerManager logger = new LoggerManager(SentenceGenerator.class);
	private static Random random;

	public SentenceGenerator() throws IOException
	{
		logger.logInfo("Initializing SentenceGenerator");

		try
		{
			this.nounGenerator = new RandomNounGenerator();
			this.adjectiveGenerator = new RandomAdjectiveGenerator();
			this.verbGenerator = new RandomVerbGenerator();
			this.templateGenerator = new RandomTemplateGenerator();

			this.random = new Random();

			logger.logInfo("SentenceGenerator initialized successfully");
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize SentenceGenerator", e);
			throw e;
		}
	}

	public Template generateRandomSentence()
	{
		logger.logInfo("generateRandomSentence: Generating sentence with random tense and number");
		Number[] numbers = Number.values();
		Tense[] tenses = Tense.values();

		Number randomNumber = numbers[random.nextInt(numbers.length)];
		Tense randomTense = tenses[random.nextInt(tenses.length)];

		logger.logDebug("generateRandomSentence: Selected random number: " + randomNumber + ", tense: " + randomTense);
		return generateSentenceWithTenseAndNumber(randomTense, randomNumber);
	}

	public Template generateSentenceWith(List<Noun> nounList, List<Adjective> adjectiveList, List<Verb> verbList) throws IOException
	{
		logger.logInfo("generateSentenceWith: Generating sentence with provided word lists");

		logger.logDebug("generateSentenceWith: Received " + nounList.size() + " nouns, " + adjectiveList.size() + " adjectives, " + verbList.size() + " verbs");

		Number number = getRandomNumber();
		TemplateType templateType = convertNumberToTemplateType(number);
		Template template = templateGenerator.getRandomTemplate(templateType);

		logger.logDebug("generateSentenceWith: Selected template type: " + templateType);

		try
		{
			for(Noun noun : nounList)
				JsonUpdater.loadNoun(noun);

			for(Adjective adjective : adjectiveList)
				JsonUpdater.loadAdjective(adjective);

			for(Verb verb : verbList)
				JsonUpdater.loadVerb(verb);

			logger.logDebug("generateSentenceWith: Loaded all new words into JsonUpdater");
		}
		catch(IOException e)
		{
			logger.logError("generateSentenceWith: Failed to load new words into JsonUpdater", e);
			throw e;
		}

		while(!nounList.isEmpty() && template.countPlaceholders(Placeholder.NOUN) != 0)
		{
			String noun = nounList.removeFirst().getNoun();
			template.replacePlaceholder(Placeholder.NOUN, noun);
			logger.logInfo("generateSentenceWith: Replaced NOUN placeholder with: " + noun);
		}

		while(template.countPlaceholders(Placeholder.NOUN) != 0)
		{
			String noun = nounGenerator.getRandomNoun().getNoun();
			template.replacePlaceholder(Placeholder.NOUN, noun);
			logger.logInfo("generateSentenceWith: Replaced remaining NOUN placeholder with random noun: " + noun);
		}

		while(!adjectiveList.isEmpty() && template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
		{
			String adjective = adjectiveList.removeFirst().getAdjective();
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
			logger.logInfo("generateSentenceWith: Replaced ADJECTIVE placeholder with: " + adjective);
		}

		while(template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
		{
			String adjective = adjectiveGenerator.getRandomAdjective().getAdjective();
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
			logger.logInfo("generateSentenceWith: Replaced remaining ADJECTIVE placeholder with random adjective: " + adjective);
		}

		while(!verbList.isEmpty() && template.countPlaceholders(Placeholder.VERB) != 0)
		{
			String verb = verbList.removeFirst().getVerb();
			template.replacePlaceholder(Placeholder.VERB, verb);
			logger.logInfo("generateSentenceWith: Replaced VERB placeholder with: " + verb);
		}

		while(template.countPlaceholders(Placeholder.VERB) != 0)
		{
			String verb = verbGenerator.getRandomVerb().getVerb();
			template.replacePlaceholder(Placeholder.VERB, verb);
			logger.logInfo("generateSentenceWith: Replaced remaining VERB placeholder with random verb: " + verb);
		}

		logger.logInfo("generateSentenceWith: Sentence generation completed");

		return template;
	}

	public Template generateSentenceWithTense(Tense tense)
	{
		logger.logDebug("generateSentenceWithTense: Generating sentence with tense: " + tense);
		return generateSentenceWithTenseAndNumber(tense, getRandomNumber());
	}

	public Template generateSentenceWithNumber(Number number)
	{
		logger.logDebug("generateSentenceWithNumber: Generating sentence with number: " + number);
		return generateSentenceWithTenseAndNumber(getRandomTense(), number);
	}

	public Template generateSentenceWithTenseAndNumber(Tense tense, Number number)
	{
		logger.logDebug("generateSentenceWithTenseAndNumber: Generating sentence with tense: " + tense + ", number: " + number);

		TemplateType templateType = convertNumberToTemplateType(number);
		Template template = templateGenerator.getRandomTemplate(templateType);

		logger.logDebug("generateSentenceWithTenseAndNumber: Selected template type: " + templateType);

		while(template.countPlaceholders(Placeholder.NOUN) != 0)
		{
			String noun = nounGenerator.getRandomNoun(number).getNoun();
			template.replacePlaceholder(Placeholder.NOUN, noun);
			logger.logInfo("generateSentenceWithTenseAndNumber: Replaced NOUN placeholder with: " + noun);
		}

		while(template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
		{
			String adjective = adjectiveGenerator.getRandomAdjective().getAdjective();
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
			logger.logInfo("generateSentenceWithTenseAndNumber: Replaced ADJECTIVE placeholder with: " + adjective);
		}

		while(template.countPlaceholders(Placeholder.VERB) != 0)
		{
			String verb = verbGenerator.getRandomVerb(tense).getVerb();
			template.replacePlaceholder(Placeholder.VERB, verb);
			logger.logInfo("generateSentenceWithTenseAndNumber: Replaced VERB placeholder with: " + verb);
		}

		logger.logInfo("generateSentenceWithTenseAndNumber: Sentence generation completed");

		return template;
	}

	private Number getRandomNumber()
	{
		logger.logInfo("getRandomNumber: Generating random number");
		Number[] numbers = Number.values();
		Number randomNumber = numbers[random.nextInt(numbers.length)];

		logger.logDebug("getRandomNumber: Selected random number: " + randomNumber);

		return randomNumber;
	}

	private Tense getRandomTense()
	{
		logger.logInfo("getRandomTense: Generating random tense");
		Tense[] tenses = Tense.values();
		Tense randomTense = tenses[random.nextInt(tenses.length)];

		logger.logDebug("getRandomTense: Selected random tense: " + randomTense);

		return randomTense;
	}

	private TemplateType convertNumberToTemplateType(Number number)
	{
		logger.logDebug("convertNumberToTemplateType: Converting number " + number + " to corrispondent template type");

		try
		{
			switch(number)
			{
				case SINGULAR: return TemplateType.SINGULAR;
				case PLURAL: return TemplateType.PLURAL;
				default: throw new InvalidNumberException();
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
		logger.logDebug("convertTemplateTypeToNumber: Converting template type " + type + " to number");

		try
		{
			switch(type)
			{
				case SINGULAR: return Number.SINGULAR;
				case PLURAL: return Number.PLURAL;
				default: throw new InvalidTemplateTypeException();
			}
		}
		catch(InvalidTemplateTypeException e)
		{
			logger.logError("convertTemplateTypeToNumber: Invalid template type: " + type, e);
			throw e;
		}
	}
}
