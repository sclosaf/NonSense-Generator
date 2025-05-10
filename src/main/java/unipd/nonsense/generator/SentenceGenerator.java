package unipd.nonsense.generator;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Verb.Tense;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Template;

import unipd.nonsense.generator.RandomNounGenerator;
import unipd.nonsense.generator.RandomVerbGenerator;
import unipd.nonsense.generator.RandomAdjectiveGenerator;
import unipd.nonsense.generator.RandomTemplateGenerator;

import java.io.IOException;

public class SentenceGenerator
{
	private RandomNounGenerator nounGenerator;
	private RandomAdjectiveGenerator adjectiveGenerator;
	private RandomVerbGenerator verbGenerator;
	private RandomTemplateGenerator templateGenerator;

	public SentenceGenerator() throws IOException
	{
		nounGenerator = new RandomNounGenerator();
		adjectiveGenerator = new RandomAdjectiveGenerator();
		verbGenerator = new RandomVerbGenerator();
		templateGenerator = new RandomTemplateGenerator();
	}

	public String generateRandomSentence()
	{
		return "";
	}

	public String generateSentenceWithTense(Tense tense)
	{
		return "";
	}

	private void loadResources()
	{}

	private void setResourcesPaths(String templatesPath, String nounsPath, String verbsPath, String adjectivesPath)
	{}

	private <T> T getRandomElement(T element)
	{
		return element;
	}

	private <T> void addNewElementToJson(T element)
	{}

	// Helpers
}
