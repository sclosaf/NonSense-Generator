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

	private static Random random;

	public SentenceGenerator() throws IOException
	{
		this.nounGenerator = new RandomNounGenerator();
		this.adjectiveGenerator = new RandomAdjectiveGenerator();
		this.verbGenerator = new RandomVerbGenerator();
		this.templateGenerator = new RandomTemplateGenerator();

		this.random = new Random();
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
		Template template = templateGenerator.getRandomTemplate(convertNumberToTemplateType(getRandomNumber()));

		for(Noun noun : nounList)
			JsonUpdater.loadNoun(noun);

		for(Adjective adjective : adjectiveList)
			JsonUpdater.loadAdjective(adjective);

		for(Verb verb : verbList)
			JsonUpdater.loadVerb(verb);

		while(!nounList.isEmpty() || template.countPlaceholders(Placeholder.NOUN) != 0)
			template.replacePlaceholder(Placeholder.NOUN, nounList.removeFirst().getNoun());

		while(template.countPlaceholders(Placeholder.NOUN) != 0)
			template.replacePlaceholder(Placeholder.NOUN, nounGenerator.getRandomNoun().getNoun());

		while(!adjectiveList.isEmpty() || template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjectiveList.removeFirst().getAdjective());

		while(template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjectiveGenerator.getRandomAdjective().getAdjective());

		while(!verbList.isEmpty() || template.countPlaceholders(Placeholder.VERB) != 0)
			template.replacePlaceholder(Placeholder.VERB, verbList.removeFirst().getVerb());

		while(template.countPlaceholders(Placeholder.VERB) != 0)
			template.replacePlaceholder(Placeholder.VERB, verbGenerator.getRandomVerb().getVerb());

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
			template.replacePlaceholder(Placeholder.NOUN, nounGenerator.getRandomNoun(number).getNoun());

		while(template.countPlaceholders(Placeholder.ADJECTIVE) != 0)
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjectiveGenerator.getRandomAdjective().getAdjective());

		while(template.countPlaceholders(Placeholder.VERB) != 0)
			template.replacePlaceholder(Placeholder.VERB, verbGenerator.getRandomVerb(tense).getVerb());

		return template;
	}

	private Number getRandomNumber()
	{
		Number[] numbers = Number.values();
		return numbers[random.nextInt(numbers.length)];
	}

	private Tense getRandomTense()
	{
		Tense[] tenses = Tense.values();
		return tenses[random.nextInt(tenses.length)];
	}

	private TemplateType convertNumberToTemplateType(Number number)
	{
		switch(number)
		{
			case SINGULAR: return TemplateType.SINGULAR;
			case PLURAL: return TemplateType.PLURAL;
			default: throw new InvalidNumberException();
		}
	}

	private Number convertTemplateTypeToNumber(TemplateType type)
	{
		switch(type)
		{
			case SINGULAR: return Number.SINGULAR;
			case PLURAL: return Number.PLURAL;
			default: throw new InvalidTemplateTypeException();
		}
	}
}
