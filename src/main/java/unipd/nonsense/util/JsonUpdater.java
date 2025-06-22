package unipd.nonsense.util;

import java.io.IOException;

import unipd.nonsense.util.JsonFileHandler;
import unipd.nonsense.util.JsonUpdateObserver;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Tense;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Template;
import unipd.nonsense.model.Pair;

import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;
import unipd.nonsense.exceptions.InvalidNumberException;
import unipd.nonsense.exceptions.InvalidTenseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.io.File;

public class JsonUpdater
{
	private static Map<Number, String> nounKeys = Map.of(
		Number.SINGULAR, "singularNouns",
		Number.PLURAL, "pluralNouns"
	);

	private static Map<Pair<Number, Tense>, String> verbKeys = Map.of(
		new Pair(Number.SINGULAR, Tense.PAST), "pastSingularVerbs",
		new Pair(Number.PLURAL, Tense.PAST), "pastPluralVerbs",
		new Pair(Number.SINGULAR, Tense.PRESENT), "presentSingularVerbs",
		new Pair(Number.PLURAL, Tense.PRESENT), "presentPluralVerbs",
		new Pair(Number.SINGULAR, Tense.FUTURE), "futureSingularVerbs",
		new Pair(Number.PLURAL, Tense.FUTURE), "futurePluralVerbs"
	);

	private static Map<Number, String> templateKeys = Map.of(
		Number.SINGULAR, "singularTemplates",
		Number.PLURAL, "pluralTemplates"
	);

	private static JsonFileHandler jsonHandler =  JsonFileHandler.getInstance();
	private static List<JsonUpdateObserver> observers = new ArrayList<>();
	private static LoggerManager logger = new LoggerManager(JsonUpdater.class);

	private static final String nounsPath = "target" + File.separator + "resources" + File.separator + "nouns.json";
	private static final String verbsPath = "target" + File.separator + "resources" + File.separator + "verbs.json";
	private static final String adjectivesPath = "target" + File.separator + "resources" + File.separator + "adjectives.json";
	private static final String templatesPath = "target" + File.separator + "resources" + File.separator + "templates.json";

	public static synchronized void addObserver(JsonUpdateObserver observer)
	{
		logger.logDebug("addObserver: Adding observer: " + observer.getClass().getSimpleName());
		observers.add(observer);
		logger.logTrace("addObserver: Observer added successfully");
	}

	public static synchronized void removeObserver(JsonUpdateObserver observer)
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
		if(noun == null)
			throw new InvalidGrammaticalElementException();

		logger.logDebug("loadNoun: Loading noun: " + noun.getNoun() + " with number: " + noun.getNumber());
		loadNoun(noun.getNoun(), noun.getNumber());
		logger.logTrace("loadNoun: Noun loaded successfully");
	}

	public static void loadNoun(String noun, Number number) throws IOException
	{
		if(noun == null)
			throw new InvalidGrammaticalElementException();

		if(number == null)
			throw new InvalidNumberException();

		logger.logDebug("loadNoun: Loading noun: " + noun + " with number: " + number);

		jsonHandler.appendItemToJson(nounsPath, nounKeys.get(number), noun);

		logger.logTrace("loadNoun: Noun appended to JSON successfully");

		notifyAllObservers();
	}

	public static void loadVerb(Verb verb) throws IOException
	{
		if(verb == null)
			throw new InvalidGrammaticalElementException();

		logger.logDebug("loadVerb: Loading verb: " + verb.getVerb() + " with tense: " + verb.getTense() + " and number: " + verb.getNumber());

		loadVerb(verb.getVerb(), verb.getTense(), verb.getNumber());

		logger.logTrace("loadVerb: Verb loaded successfully");
	}

	public static void loadVerb(String verb, Tense tense, Number number) throws IOException
	{
		if(verb == null)
			throw new InvalidGrammaticalElementException();

		if(tense == null)
			throw new InvalidTenseException();

		if(number == null)
			throw new InvalidNumberException();

		logger.logDebug("loadVerb: Loading verb: " + verb + " with tense: " + tense + " and number: " + number);

		Pair pair = new Pair<>(number, tense);

		jsonHandler.appendItemToJson(verbsPath, verbKeys.get(pair), verb);
		logger.logTrace("loadVerb: Verb appended to JSON successfully");
		notifyAllObservers();
	}

	public static void loadAdjective(Adjective adjective) throws IOException
	{
		if(adjective == null)
			throw new InvalidGrammaticalElementException();

		logger.logDebug("loadAdjective: Loading adjective: " + adjective.getAdjective());
		loadAdjective(adjective.getAdjective());
		logger.logTrace("loadAdjective: Adjective loaded successfully");
	}

	public static void loadAdjective(String adjective) throws IOException
	{
		if(adjective == null)
			throw new InvalidGrammaticalElementException();

		logger.logDebug("loadAdjective: Loading adjective: " + adjective);
		String key = "adjectives";

		logger.logDebug("loadAdjective: Appending adjective to JSON with key: " + key);
		jsonHandler.appendItemToJson(adjectivesPath, key, adjective);
		logger.logTrace("loadAdjective: Adjective appended to JSON successfully");
		notifyAllObservers();
	}

	public static void loadTemplate(Template template) throws IOException
	{
		if(template == null)
			throw new InvalidGrammaticalElementException();

		logger.logDebug("loadTemplate: Loading template: " + template.getPattern() + " with number: " + template.getNumber());
		loadTemplate(template.getPattern(), template.getNumber());
		logger.logTrace("loadTemplate: Template loaded successfully");
	}

	public static void loadTemplate(String template, Number number) throws IOException
	{
		if(template == null)
			throw new InvalidGrammaticalElementException();

		if(number == null)
			throw new InvalidNumberException();

		logger.logDebug("loadTemplate: Loading template: " + template + " with number: " + number);

		jsonHandler.appendItemToJson(templatesPath, templateKeys.get(number), template);
		logger.logTrace("loadTemplate: Template appended to JSON successfully");
		notifyAllObservers();
	}
}
