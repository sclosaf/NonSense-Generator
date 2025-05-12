package unipd.nonsense.util;

import unipd.nonsense.util.SyntaxTreePrinter;
import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.analyzer.SentenceAnalyzer;
import unipd.nonsense.analyzer.ToxicityValidator;

import unipd.nonsense.generator.SentenceGenerator;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Template;

import java.io.IOException;

public class CommandProcessor
{
	private static SyntaxTreePrinter treeBuilder;
	private static SentenceAnalyzer analyzer;
	private static ToxicityValidator validator;
	private static SentenceGenerator generator;
	private LoggerManager logger = new LoggerManager(CommandProcessor.class);

	private static String cachedString;

	private float sentimentTollerance;
	private float toxicityTollerance;

	public CommandProcessor() throws IOException
	{
		this.treeBuilder = new SyntaxTreePrinter();
		this.analyzer = new SentenceAnalyzer();
		this.validator = new ToxicityValidator();
		this.generator = new SentenceGenerator();

		this.cachedString = "";
	}

	public String generate()
	{
		cachedString = generator.generateRandomSentence().getPattern();

		return cachedString;
	}

	public String analyze(String str)
	{
		//
		return "";
	}

	public String generateAndAnalyze()
	{
		return analyze(generate());
	}

	public String printTree(String str)
	{
		return "";
	}

	public void setTollerance()
	{}

	public void switchVerbosity()
	{
		logger.switchVerboseMode();
	}
}
