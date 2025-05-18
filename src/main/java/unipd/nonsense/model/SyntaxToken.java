package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import com.google.cloud.language.v1.PartOfSpeech;
import com.google.cloud.language.v1.DependencyEdge;

import unipd.nonsense.util.LoggerManager;

public class SyntaxToken
{
	private final String text;
	private final int beginOffset;
	private final String lemma;
	private final PartOfSpeech partOfSpeech;
	private final int headTokenIndex;
	private final DependencyEdge.Label dependencyLabel;

	private static final LoggerManager logger = new LoggerManager(SyntaxToken.class);

	public SyntaxToken(String text, int beginOffset, String lemma, PartOfSpeech partOfSpeech, int headTokenIndex, DependencyEdge.Label dependencyLabel)
	{
		logger.logTrace("Creating SyntaxToken instance");

		if(text == null || lemma == null || partOfSpeech == null || dependencyLabel == null)
		{
			logger.logError("Invalid parameters provided");
			throw new InvalidGrammaticalElementException();
		}

		this.text = text;
		this.beginOffset = beginOffset;
		this.lemma = lemma;
		this.partOfSpeech = partOfSpeech;
		this.headTokenIndex = headTokenIndex;
		this.dependencyLabel = dependencyLabel;

		logger.logDebug("Successfully created SyntaxToken with text: " + text);
	}

	public String getText()
	{
		logger.logTrace("getText: Returning text");
		return text;
	}

	public int getBeginOffset()
	{
		logger.logTrace("getBeginOffset: Returning beginOffset");
		return beginOffset;
	}

	public String getLemma()
	{
		logger.logTrace("getLemma: Returning lemma");
		return lemma;
	}

	public PartOfSpeech getPartOfSpeech()
	{
		logger.logTrace("getPartOfSpeech: Returning partOfSpeech");
		return partOfSpeech;
	}

	public int getHeadTokenIndex()
	{
		logger.logTrace("getHeadTokenIndex: Returning headTokenIndex");
		return headTokenIndex;
	}

	public DependencyEdge.Label getDependencyLabel()
	{
		logger.logTrace("getDependencyLabel: Returning dependencyLabel");
		return dependencyLabel;
	}

	public String getPosTag()
	{
		logger.logTrace("getPosTag: Returning POS tag");
		return partOfSpeech.getTag().toString();
	}
}

