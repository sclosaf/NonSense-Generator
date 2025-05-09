package unipd.nonsense.util;

import java.io.IOException;

import unipd.nonsense.util.JsonFileHandler;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Noun.Number;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Verb.Tense;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Template;
import unipd.nonsense.model.Template.TemplateType;

public class JsonUpdater
{
	private static JsonFileHandler jsonHandler =  JsonFileHandler.getInstance();

	private static String nounsPath = "nouns.json";
	private static String verbsPath = "verbs.json";
	private static String adjectivesPath = "adjectives.json";
	private static String templatesPath = "templates.json";

	private JsonUpdater()
	{
		throw new UnsupportedOperationException("Utility class: cannot be instantiated.");
	}

	public static void loadNoun(Noun noun) throws IOException
	{
		loadNoun(noun.getNoun(), noun.getNumber());
	}

	public static void loadNoun(String noun, Number num) throws IOException
	{
		String key;

		switch(num)
		{
			case Number.SINGULAR: key = "singularNouns"; break;
			case Number.PLURAL: key = "pluralNouns"; break;
			default: throw new IllegalArgumentException();
		}

		jsonHandler.appendItemToJson(nounsPath, key, noun);
	}

	public static void loadVerb(Verb verb) throws IOException
	{
		loadVerb(verb.getVerb(), verb.getTense());
	}

	public static void loadVerb(String verb, Tense tense) throws IOException
	{
		String key;

		switch(tense)
		{
			case Tense.PAST: key = "pastVerbs"; break;
			case Tense.PRESENT: key = key = "presentVerbs"; break;
			case Tense.FUTURE: key = "futureVerbs"; break;
			default: throw new IllegalArgumentException();
		}

		jsonHandler.appendItemToJson(verbsPath, key, verb);
	}

	public static void loadAdjective(Adjective adjective) throws IOException
	{
		loadAdjective(adjective.getAdjective());
	}

	public static void loadAdjective(String adjective) throws IOException
	{
		String key = "adjectives";

		jsonHandler.appendItemToJson(adjectivesPath, key, adjective);
	}

	public static void loadTemplate(Template template) throws IOException
	{
		loadTemplate(template.getPattern(), template.getType());
	}

	public static void loadTemplate(String template, TemplateType type) throws IOException
	{
		String key;

		switch(type)
		{
			case TemplateType.SINGULAR: key = "singularTemplates"; break;
			case TemplateType.PLURAL: key = "pluralTemplates"; break;
			default: throw new IllegalArgumentException();
		}

		jsonHandler.appendItemToJson(templatesPath, key, template);
	}
}
