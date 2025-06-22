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

/**
 * Utility class for updating JSON files containing grammatical elements and notifying registered observers.
 * <p>
 * This class provides synchronized methods to append nouns, verbs, adjectives, and templates
 * to their respective JSON files. After each successful update, all registered observers
 * are notified. The class maintains thread safety for observer management and file operations.
 * </p>
 *
 * <p>Key features:
 * <ul>
 *	<li>Synchronized observer management</li>
 *	<li>Thread-safe file operations</li>
 *	<li>Comprehensive error logging</li>
 *	<li>Automatic observer notification</li>
 * </ul>
 * </p>
 */
public class JsonUpdater
{
	/**
	 * Map associating {@code Number} values with their corresponding JSON keys for nouns.
	 * <p>
	 * Contains mappings:
	 * <ul>
	 *	<li>{@code Number.SINGULAR} → "singularNouns"</li>
	 *	<li>{@code Number.PLURAL} → "pluralNouns"</li>
	 * </ul>
	 * </p>
	 */
	private static Map<Number, String> nounKeys = Map.of(
		Number.SINGULAR, "singularNouns",
		Number.PLURAL, "pluralNouns"
	);

	/**
	 * Map associating {@code Pair<Number, Tense>} values with their corresponding JSON keys for verbs.
	 * <p>
	 * Contains mappings for all combinations of:
	 * <ul>
	 *	<li>{@code Number.SINGULAR} and {@code Tense.PAST} → "pastSingularVerbs"</li>
	 *	<li>{@code Number.PLURAL} and {@code Tense.PAST} → "pastPluralVerbs"</li>
	 *	<li>... (all other combinations)</li>
	 * </ul>
	 * </p>
	 */
	private static Map<Pair<Number, Tense>, String> verbKeys = Map.of(
		new Pair(Number.SINGULAR, Tense.PAST), "pastSingularVerbs",
		new Pair(Number.PLURAL, Tense.PAST), "pastPluralVerbs",
		new Pair(Number.SINGULAR, Tense.PRESENT), "presentSingularVerbs",
		new Pair(Number.PLURAL, Tense.PRESENT), "presentPluralVerbs",
		new Pair(Number.SINGULAR, Tense.FUTURE), "futureSingularVerbs",
		new Pair(Number.PLURAL, Tense.FUTURE), "futurePluralVerbs"
	);

	/**
	 * Map associating {@code Number} values with their corresponding JSON keys for templates.
	 * <p>
	 * Contains mappings:
	 * <ul>
	 *	<li>{@code Number.SINGULAR} → "singularTemplates"</li>
	 *	<li>{@code Number.PLURAL} → "pluralTemplates"</li>
	 * </ul>
	 * </p>
	 */
	private static Map<Number, String> templateKeys = Map.of(
		Number.SINGULAR, "singularTemplates",
		Number.PLURAL, "pluralTemplates"
	);

	/**
	 * Singleton instance of {@code JsonFileHandler} for JSON file operations.
	 */
	private static JsonFileHandler jsonHandler =  JsonFileHandler.getInstance();

	/**
	 * List of registered observers to be notified after JSON updates.
	 * <p>
	 * Maintained as a thread-safe collection through synchronized access methods.
	 * </p>
	 */
	private static List<JsonUpdateObserver> observers = new ArrayList<>();

	/**
	 * Logger instance for tracking operations and errors.
	 */
	private static LoggerManager logger = new LoggerManager(JsonUpdater.class);

	/**
	 * Path to the nouns JSON file.
	 */
	private static final String nounsPath = "target" + File.separator + "resources" + File.separator + "nouns.json";

	/**
	 * Path to the verbs JSON file.
	 */
	private static final String verbsPath = "target" + File.separator + "resources" + File.separator + "verbs.json";

	/**
	 * Path to the adjectives JSON file.
	 */
	private static final String adjectivesPath = "target" + File.separator + "resources" + File.separator + "adjectives.json";

	/**
	 * Path to the templates JSON file.
	 */
	private static final String templatesPath = "target" + File.separator + "resources" + File.separator + "templates.json";

	/**
	 * Registers an observer to receive update notifications.
	 * <p>
	 * The new observer will be notified after every successful JSON update operation.
	 * This method is thread-safe.
	 * </p>
	 *
	 * @param observer	the {@code JsonUpdateObserver} to register
	 * @throws IllegalArgumentException	if {@code observer} is {@code null}
	 */
	public static synchronized void addObserver(JsonUpdateObserver observer)
	{
		logger.logDebug("addObserver: Adding observer: " + observer.getClass().getSimpleName());
		observers.add(observer);
		logger.logTrace("addObserver: Observer added successfully");
	}

	/**
	 * Unregisters an observer from receiving update notifications.
	 * <p>
	 * If the observer isn't registered, the method completes silently.
	 * This method is thread-safe.
	 * </p>
	 *
	 * @param observer	the {@code JsonUpdateObserver} to unregister
	 * @throws IllegalArgumentException	if {@code observer} is {@code null}
	 */
	public static synchronized void removeObserver(JsonUpdateObserver observer)
	{
		logger.logDebug("removeObserver: Removing observer: " + observer.getClass().getSimpleName());
		observers.remove(observer);
		logger.logTrace("removeObserver: Observer removed successfully");
	}

	/**
	 * Notifies all registered observers of a JSON update.
	 * <p>
	 * Observers are notified in registration order. If any observer throws an exception,
	 * notification stops and the exception is propagated.
	 * </p>
	 *
	 * @throws IOException	if any observer fails to handle the notification
	 */
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

	/**
	 * Loads a {@code Noun} object into the nouns JSON file.
	 * <p>
	 * The noun is added to the appropriate section based on its grammatical number.
	 * After successful addition, all observers are notified.
	 * </p>
	 *
	 * @param noun	the {@code Noun} to load
	 * @throws InvalidGrammaticalElementException	if {@code noun} is {@code null}
	 * @throws IOException						if file operation fails or observer notification fails
	 */
	public static void loadNoun(Noun noun) throws IOException
	{
		if(noun == null)
			throw new InvalidGrammaticalElementException();

		logger.logDebug("loadNoun: Loading noun: " + noun.getNoun() + " with number: " + noun.getNumber());
		loadNoun(noun.getNoun(), noun.getNumber());
		logger.logTrace("loadNoun: Noun loaded successfully");
	}

	/**
	 * Loads a noun string into the nouns JSON file with specified grammatical number.
	 * <p>
	 * The noun is added to the appropriate section based on the provided number.
	 * After successful addition, all observers are notified.
	 * </p>
	 *
	 * @param noun		the noun string to load
	 * @param number	the grammatical number of the noun
	 * @throws InvalidGrammaticalElementException	if {@code noun} is {@code null}
	 * @throws InvalidNumberException				if {@code number} is {@code null}
	 * @throws IOException						if file operation fails or observer notification fails
	 */
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

	/**
	 * Loads a {@code Verb} object into the verbs JSON file.
	 * <p>
	 * The verb is added to the appropriate section based on its tense and number.
	 * After successful addition, all observers are notified.
	 * </p>
	 *
	 * @param verb	the {@code Verb} to load
	 * @throws InvalidGrammaticalElementException	if {@code verb} is {@code null}
	 * @throws IOException						if file operation fails or observer notification fails
	 */
	public static void loadVerb(Verb verb) throws IOException
	{
		if(verb == null)
			throw new InvalidGrammaticalElementException();

		logger.logDebug("loadVerb: Loading verb: " + verb.getVerb() + " with tense: " + verb.getTense() + " and number: " + verb.getNumber());

		loadVerb(verb.getVerb(), verb.getTense(), verb.getNumber());

		logger.logTrace("loadVerb: Verb loaded successfully");
	}

	/**
	 * Loads a verb string into the verbs JSON file with specified tense and number.
	 * <p>
	 * The verb is added to the appropriate section based on the provided tense and number.
	 * After successful addition, all observers are notified.
	 * </p>
	 *
	 * @param verb		the verb string to load
	 * @param tense		the grammatical tense of the verb
	 * @param number	the grammatical number of the verb
	 * @throws InvalidGrammaticalElementException	if {@code verb} is {@code null}
	 * @throws InvalidTenseException				if {@code tense} is {@code null}
	 * @throws InvalidNumberException				if {@code number} is {@code null}
	 * @throws IOException						if file operation fails or observer notification fails
	 */
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

	/**
	 * Loads an {@code Adjective} object into the adjectives JSON file.
	 * <p>
	 * After successful addition, all observers are notified.
	 * </p>
	 *
	 * @param adjective	the {@code Adjective} to load
	 * @throws InvalidGrammaticalElementException	if {@code adjective} is {@code null}
	 * @throws IOException						if file operation fails or observer notification fails
	 */
	public static void loadAdjective(Adjective adjective) throws IOException
	{
		if(adjective == null)
			throw new InvalidGrammaticalElementException();

		logger.logDebug("loadAdjective: Loading adjective: " + adjective.getAdjective());
		loadAdjective(adjective.getAdjective());
		logger.logTrace("loadAdjective: Adjective loaded successfully");
	}

	/**
	 * Loads an adjective string into the adjectives JSON file.
	 * <p>
	 * After successful addition, all observers are notified.
	 * </p>
	 *
	 * @param adjective	the adjective string to load
	 * @throws InvalidGrammaticalElementException	if {@code adjective} is {@code null}
	 * @throws IOException						if file operation fails or observer notification fails
	 */
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

	/**
	 * Loads a {@code Template} object into the templates JSON file.
	 * <p>
	 * The template is added to the appropriate section based on its grammatical number.
	 * After successful addition, all observers are notified.
	 * </p>
	 *
	 * @param template	the {@code Template} to load
	 * @throws InvalidGrammaticalElementException	if {@code template} is {@code null}
	 * @throws IOException						if file operation fails or observer notification fails
	 */
	public static void loadTemplate(Template template) throws IOException
	{
		if(template == null)
			throw new InvalidGrammaticalElementException();

		logger.logDebug("loadTemplate: Loading template: " + template.getPattern() + " with number: " + template.getNumber());
		loadTemplate(template.getPattern(), template.getNumber());
		logger.logTrace("loadTemplate: Template loaded successfully");
	}

	/**
	 * Loads a template string into the templates JSON file with specified grammatical number.
	 * <p>
	 * The template is added to the appropriate section based on the provided number.
	 * After successful addition, all observers are notified.
	 * </p>
	 *
	 * @param template	the template string to load
	 * @param number	the grammatical number of the template
	 * @throws InvalidGrammaticalElementException	if {@code template} is {@code null}
	 * @throws InvalidNumberException				if {@code number} is {@code null}
	 * @throws IOException						if file operation fails or observer notification fails
	 */
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
