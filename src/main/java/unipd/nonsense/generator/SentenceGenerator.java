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

import unipd.nonsense.exception.TemplateLoadException;
import unipd.nonsense.exception.TemplateNotFoundException;

import java.util.Random;
import java.io.IOException;

public class SentenceGenerator
{
	private RandomNounGenerator nounGenerator;
	private RandomAdjectiveGenerator adjectiveGenerator;
	private RandomVerbGenerator verbGenerator;
	private RandomTemplateGenerator templateGenerator;

	private static Random random;

	public SentenceGenerator() throws IOException, TemplateLoadException
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

	public Template generateSentenceWithTense(Tense tense)
	{
		Number[] numbers = Number.values();
		Number randomNumber = numbers[random.nextInt(numbers.length)];

		return generateSentenceWithTenseAndNumber(tense, randomNumber);
	}

	public Template generateSentenceWithNumber(Number number)
	{
		Tense[] tenses = Tense.values();
		Tense randomTense = tenses[random.nextInt(tenses.length)];

		return generateSentenceWithTenseAndNumber(randomTense, number);
	}

	public Template generateSentenceWithTenseAndNumber(Tense tense, Number number)
	{
		TemplateType templateType = convertNumberToTemplateType(number);
		Template template = templateGenerator.getRandomTemplate(templateType);

		for(int i = template.countPlaceholders(Placeholder.NOUN); i > 0; --i)
			template.replacePlaceholder(Placeholder.NOUN, nounGenerator.getRandomNoun(number).getNoun());

		for(int i = template.countPlaceholders(Placeholder.ADJECTIVE); i > 0; --i)
			template.replacePlaceholder(Placeholder.ADJECTIVE, adjectiveGenerator.getRandomAdjective().getAdjective());

		for(int i = template.countPlaceholders(Placeholder.VERB); i > 0; --i)
			template.replacePlaceholder(Placeholder.VERB, verbGenerator.getRandomVerb(tense).getVerb());

		return template;
	}

	private TemplateType convertNumberToTemplateType(Number number)
	{
		switch(number)
		{
			case SINGULAR: return TemplateType.SINGULAR;
			case PLURAL: return TemplateType.PLURAL;
			default: throw new IllegalArgumentException("Unsupported number: " + number);
		}
	}

	private Number convertTemplateTypeToNumber(TemplateType type)
	{
		switch(type)
		{
			case SINGULAR: return Number.SINGULAR;
			case PLURAL: return Number.PLURAL;
			default: throw new IllegalArgumentException("Unsupported template type: " + type);
		}
	}
}