package unipd.nonsense.util;

import unipd.nonsense.util.SyntaxTreePrinter;

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

	private static String lastCachedString;

	public CommandProcessor() throws IOException
	{
		treeBuilder = new SyntaxTreePrinter();
		analyzer = new SentenceAnalyzer();
		validator = new ToxicityValidator();
		generator = new SentenceGenerator();
	}

	public static String generate()
	{
		lastCachedString = generator.generateRandomSentence().getPattern();

		return lastCachedString;
	}

	public static String analyze(String str)
	{
		return "";
	}

	public static String generateAndAnalyze()
	{
		return analyze(generate());
	}

	public static String printTree(String str)
	{
		return "";
	}

	public static void setTollerance()
	{}

	public static void switchDevMode()
	{}
}
