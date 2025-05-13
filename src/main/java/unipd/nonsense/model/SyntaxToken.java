package unipd.nonsense.model;

import com.google.cloud.language.v1.PartOfSpeech;
import com.google.cloud.language.v1.DependencyEdge;

public class SyntaxToken
{
	private final String text;
	private final int beginOffset;
	private final String lemma;
	private final PartOfSpeech partOfSpeech;
	private final int headTokenIndex;
	private final DependencyEdge.Label dependencyLabel;

	public SyntaxToken(String text, int beginOffset, String lemma, PartOfSpeech partOfSpeech, int headTokenIndex, DependencyEdge.Label dependencyLabel)
	{
		this.text = text;
		this.beginOffset = beginOffset;
		this.lemma = lemma;
		this.partOfSpeech = partOfSpeech;
		this.headTokenIndex = headTokenIndex;
		this.dependencyLabel = dependencyLabel;
	}

	public String getText()
	{
		return text;
	}

	public int getBeginOffset()
	{
		return beginOffset;
	}

	public String getLemma()
	{
		return lemma;
	}

	public PartOfSpeech getPartOfSpeech()
	{
		return partOfSpeech;
	}

	public int getHeadTokenIndex()
	{
		return headTokenIndex;
	}

	public DependencyEdge.Label getDependencyLabel()
	{
		return dependencyLabel;
	}

	public String getPosTag()
	{
		return partOfSpeech.getTag().toString();
	}
}

