package unipd.nonsense.util;

import java.io.IOException;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.util.JsonUpdateObserver;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Noun.Number;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Verb.Tense;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Template;
import unipd.nonsense.model.Template.TemplateType;

import unipd.nonsense.util.LoggerManager;

import java.util.ArrayList;
import java.util.List;

import java.io.File;

public class JsonUpdater
{
	private static JsonFileHandler jsonHandler =  JsonFileHandler.getInstance();
	private static List<JsonUpdateObserver> observers = new ArrayList<>();
	private static LoggerManager logger = new LoggerManager(JsonUpdater.class);

	private static String nounsPath = "target" + File.separator + "resources" + File.separator + "nouns.json";
	private static String verbsPath = "target" + File.separator + "resources" + File.separator + "verbs.json";
	private static String adjectivesPath = "target" + File.separator + "resources" + File.separator + "adjectives.json";
	private static String templatesPath = "target" + File.separator + "resources" + File.separator + "templates.json";

	public static void addObserver(JsonUpdateObserver observer)
	{
		logger.logDebug("addObserver: Adding observer: " + observer.getClass().getSimpleName());
		observers.add(observer);
		logger.logTrace("addObserver: Observer added successfully");
	}

	public static void removeObserver(JsonUpdateObserver observer)
	{
		logger.logDebug("removeObserver: Removing observer: " + observer.getClass().getSimpleName());
		observers.remove(observer);
		logger.logTrace("removeObserver: Observer removed successfully");
	}

	private static void notifyAllObservers() throws IOException
	{
		logger.logDebug("notifyAllObservers: Notifying " + observers.size() + " observers");

		for(JsonUpdateObserver observer : observers)
		{
			try
			{
				logger.logDebug("notifyAllObservers: Notifying observer: " + observer.getClass().getSimpleName());
				observer.onJsonUpdate();
				logger.logTrace("notifyAllObservers: Observer notified successfully");
			}
			catch(IOException e)
			{
				logger.logError("notifyAllObservers: Failed to notify observer: " + observer.getClass().getSimpleName(), e);
				throw e;
			}
		}
	}

	public static void loadNoun(Noun noun) throws IOException
	{
		logger.logDebug("loadNoun: Loading noun: " + noun.getNoun() + " with number: " + noun.getNumber());
		loadNoun(noun.getNoun(), noun.getNumber());
		logger.logTrace("loadNoun: Noun loaded successfully");
	}

	public static void loadNoun(String noun, Number num) throws IOException
	{
		logger.logDebug("loadNoun: Loading noun: " + noun + " with number: " + num);
		String key;

		switch(num)
		{
			case Number.SINGULAR: key = "singularNouns"; break;
			case Number.PLURAL: key = "pluralNouns"; break;
			default:
				logger.logError("loadNoun: Invalid number type provided: " + num);
				throw new IllegalArgumentException();
		}

		logger.logDebug("loadNoun: Appending noun to JSON with key: " + key);
		jsonHandler.appendItemToJson(nounsPath, key, noun);
		logger.logTrace("loadNoun: Noun appended to JSON successfully");
		notifyAllObservers();
	}

	public static void loadVerb(Verb verb) throws IOException
	{
		logger.logDebug("loadVerb: Loading verb: " + verb.getVerb() + " with tense: " + verb.getTense());
		loadVerb(verb.getVerb(), verb.getTense());
		logger.logTrace("loadVerb: Verb loaded successfully");
	}


	public static void loadVerb(String verb, Tense tense) throws IOException
	{
		logger.logDebug("loadVerb: Loading verb: " + verb + " with tense: " + tense);
		String key;

		switch(tense)
		{
			case Tense.PAST: key = "pastVerbs"; break;
			case Tense.PRESENT: key = key = "presentVerbs"; break;
			case Tense.FUTURE: key = "futureVerbs"; break;
			default:
				logger.logError("loadVerb: Invalid tense provided: " + tense);
				throw new IllegalArgumentException();
		}


		logger.logDebug("loadVerb: Appending verb to JSON with key: " + key);
		jsonHandler.appendItemToJson(verbsPath, key, verb);
		logger.logTrace("loadVerb: Verb appended to JSON successfully");
		notifyAllObservers();
	}

	public static void loadAdjective(Adjective adjective) throws IOException
	{
		logger.logDebug("loadAdjective: Loading adjective: " + adjective.getAdjective());
		loadAdjective(adjective.getAdjective());
		logger.logTrace("loadAdjective: Adjective loaded successfully");
	}

	public static void loadAdjective(String adjective) throws IOException
	{
		logger.logDebug("loadAdjective: Loading adjective: " + adjective);
		String key = "adjectives";

		logger.logDebug("loadAdjective: Appending adjective to JSON with key: " + key);
		jsonHandler.appendItemToJson(adjectivesPath, key, adjective);
		logger.logTrace("loadAdjective: Adjective appended to JSON successfully");
		notifyAllObservers();
	}

	public static void loadTemplate(Template template) throws IOException
	{
		logger.logDebug("loadTemplate: Loading template: " + template.getPattern() + " with type: " + template.getType());
		loadTemplate(template.getPattern(), template.getType());
		logger.logTrace("loadTemplate: Template loaded successfully");
	}

	public static void loadTemplate(String template, TemplateType type) throws IOException
	{
		logger.logDebug("loadTemplate: Loading template: " + template + " with type: " + type);
		String key;

		switch(type)
		{
			case TemplateType.SINGULAR: key = "singularTemplates"; break;
			case TemplateType.PLURAL: key = "pluralTemplates"; break;
			default:
				logger.logError("loadTemplate: Invalid template type provided: " + type);
				throw new IllegalArgumentException();
		}

		logger.logDebug("loadTemplate: Appending template to JSON with key: " + key);
		jsonHandler.appendItemToJson(templatesPath, key, template);
		logger.logTrace("loadTemplate: Template appended to JSON successfully");
		notifyAllObservers();
	}
}
