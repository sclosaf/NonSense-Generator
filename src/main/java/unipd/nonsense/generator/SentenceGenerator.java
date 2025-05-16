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
		try
		{
			this.nounGenerator = new RandomNounGenerator();
			this.adjectiveGenerator = new RandomAdjectiveGenerator();
			this.verbGenerator = new RandomVerbGenerator();
			this.templateGenerator = new RandomTemplateGenerator();

			this.random = new Random();

		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize SentenceGenerator", e);
			throw e;
		}
	}

	public Template generateRandomSentence()
	{
		Number[] numbers = Number.values();
		Tense[] tenses = Tense.values();

		Number randomNumber = numbers[random.nextInt(numbers.length)];
		Tense randomTense = tenses[random.nextInt(tenses.length)];

		return generateSentenceWithTenseAndNumber(randomTense, randomNumber);
	}

	public Template generateSentenceWith(List<Noun> nounList, List<Adjective> adjectiveList, List<Verb> verbList) throws IOException
	{
		Number number = getRandomNumber();
		TemplateType templateType = convertNumberToTemplateType(number);
		Template template = templateGenerator.getRandomTemplate(templateType);

		try
		{
			for(Noun noun : nounList)
				JsonUpdater.loadNoun(noun);

			for(Adjective adjective : adjectiveList)
				JsonUpdater.loadAdjective(adjective);

			for(Verb verb : verbList)
				JsonUpdater.loadVerb(verb);

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
		}

		while(template.countPlaceholders(Placeholder.NOUN) != 0)
		{
			String noun = nounGenerator.getRandomNoun().getNoun();
			template.replacePlaceholder(Placeholder.NOUN, noun);
		}

		while(!adjectiveList.isEmpty() && template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
		{
			String adjective = adjectiveList.removeFirst().getAdjective();
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
		}

		while(template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
		{
			String adjective = adjectiveGenerator.getRandomAdjective().getAdjective();
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
		}

		while(!verbList.isEmpty() && template.countPlaceholders(Placeholder.VERB) != 0)
		{
			String verb = verbList.removeFirst().getVerb();
			template.replacePlaceholder(Placeholder.VERB, verb);
		}

		while(template.countPlaceholders(Placeholder.VERB) != 0)
		{
			String verb = verbGenerator.getRandomVerb().getVerb();
			template.replacePlaceholder(Placeholder.VERB, verb);
		}

		return template;
	}

	public Template generateSentenceWithTense(Tense tense)
	{
		return generateSentenceWithTenseAndNumber(tense, getRandomNumber());
	}

	public Template generateSentenceWithNumber(Number number)
	{
		return generateSentenceWithTenseAndNumber(getRandomTense(), number);
	}

	public Template generateSentenceWithTenseAndNumber(Tense tense, Number number)
	{

		TemplateType templateType = convertNumberToTemplateType(number);
		Template template = templateGenerator.getRandomTemplate(templateType);

		while(template.countPlaceholders(Placeholder.NOUN) != 0)
		{
			String noun = nounGenerator.getRandomNoun(number).getNoun();
			template.replacePlaceholder(Placeholder.NOUN, noun);
		}

		while(template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
		{
			String adjective = adjectiveGenerator.getRandomAdjective().getAdjective();
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
		}

		while(template.countPlaceholders(Placeholder.VERB) != 0)
		{
			String verb = verbGenerator.getRandomVerb(tense).getVerb();
			template.replacePlaceholder(Placeholder.VERB, verb);
		}

		return template;
	}

	public Template generateSentenceFromTemplate(Template template)
	{
		if(template == null || template.getPattern().isEmpty())
			throw new InvalidTemplateException();

		Number number = convertTemplateTypeToNumber(template.getType());

		while(template.countPlaceholders(Placeholder.NOUN) != 0)
		{
			String noun = nounGenerator.getRandomNoun(number).getNoun();
			template.replacePlaceholder(Placeholder.NOUN, noun);
		}

		while(template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
		{
			String adjective = adjectiveGenerator.getRandomAdjective().getAdjective();
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjective);
		}

		while(template.countPlaceholders(Placeholder.VERB) != 0)
		{
			String verb = verbGenerator.getRandomVerb(getRandomTense()).getVerb();
			template.replacePlaceholder(Placeholder.VERB, verb);
		}

		return template;
	}

	public List<Template> getRandomTemplates()
	{
		List<Template> templateList = new ArrayList<>();

		for(int i = 0; i < 5; ++i)
			templateList.add(templateGenerator.getRandomTemplate());

		return templateList;
	}

	private Number getRandomNumber()
	{
		Number[] numbers = Number.values();
		Number randomNumber = numbers[random.nextInt(numbers.length)];

		return randomNumber;
	}

	private Tense getRandomTense()
	{
		Tense[] tenses = Tense.values();
		Tense randomTense = tenses[random.nextInt(tenses.length)];

		return randomTense;
	}

	private TemplateType convertNumberToTemplateType(Number number)
	{
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
