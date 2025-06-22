package unipd.nonsense.model;

import org.junit.jupiter.api.*;
import com.google.cloud.language.v1.PartOfSpeech;
import com.google.cloud.language.v1.DependencyEdge;
import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testing SyntaxToken")
class TestSyntaxToken
{
	private SyntaxToken token;
	private static PartOfSpeech pos;
	private static DependencyEdge.Label label;

	@BeforeAll
	static void setupAll()
	{
		pos = PartOfSpeech.newBuilder().setTag(PartOfSpeech.Tag.NOUN).build();
		label = DependencyEdge.Label.NSUBJ;
	}

	@BeforeEach
	void setup()
	{
		token = new SyntaxToken("test", 0, "lemma", pos, 1, label);
	}

	@Test
	@DisplayName("Test valid construction")
	void testValidConstruction()
	{
		assertNotNull(token);
		assertEquals("test", token.getText());
		assertEquals(0, token.getBeginOffset());
		assertEquals("lemma", token.getLemma());
		assertEquals(pos, token.getPartOfSpeech());
		assertEquals(1, token.getHeadTokenIndex());
		assertEquals(label, token.getDependencyLabel());
	}

	@Test
	@DisplayName("Test null text throws exception")
	void testNullText()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new SyntaxToken(null, 0, "lemma", pos, 1, label));
	}

	@Test
	@DisplayName("Test getPosTag returns correct tag")
	void testGetPosTag()
	{
		assertEquals("NOUN", token.getPosTag());
	}

	@Test
	@DisplayName("Test null lemma throws exception")
	void testNullLemma()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new SyntaxToken("test", 0, null, pos, 1, label));
	}

	@Test
	@DisplayName("Test null partOfSpeech throws exception")
	void testNullPartOfSpeech()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new SyntaxToken("test", 0, "lemma", null, 1, label));
	}

	@Test
	@DisplayName("Test null dependencyLabel throws exception")
	void testNullDependencyLabel()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new SyntaxToken("test", 0, "lemma", pos, 1, null));
	}

	@Test
	@DisplayName("Test negative beginOffset")
	void testNegativeBeginOffset()
	{
		SyntaxToken token = new SyntaxToken("test", -1, "lemma", pos, 1, label);
		assertEquals(-1, token.getBeginOffset());
	}

	@Test
	@DisplayName("Test negative headTokenIndex")
	void testNegativeHeadTokenIndex()
	{
		SyntaxToken token = new SyntaxToken("test", 0, "lemma", pos, -1, label);
		assertEquals(-1, token.getHeadTokenIndex());
	}

	@Test
	@DisplayName("Test empty text")
	void testEmptyText()
	{
		SyntaxToken token = new SyntaxToken("", 0, "lemma", pos, 1, label);
		assertEquals("", token.getText());
	}

	@Test
	@DisplayName("Test empty lemma")
	void testEmptyLemma()
	{
		SyntaxToken token = new SyntaxToken("test", 0, "", pos, 1, label);
		assertEquals("", token.getLemma());
	}

	@Test
	@DisplayName("Test different PartOfSpeech tags")
	void testDifferentPosTags()
	{
		PartOfSpeech verbPos = PartOfSpeech.newBuilder().setTag(PartOfSpeech.Tag.VERB).build();
		SyntaxToken verbToken = new SyntaxToken("run", 0, "run", verbPos, 1, label);
		assertEquals("VERB", verbToken.getPosTag());
	}

	@Test
	@DisplayName("Test different dependency labels")
	void testDifferentDependencyLabels()
	{
		DependencyEdge.Label dobjLabel = DependencyEdge.Label.DOBJ;
		SyntaxToken token = new SyntaxToken("test", 0, "lemma", pos, 1, dobjLabel);
		assertEquals(dobjLabel, token.getDependencyLabel());
	}

	@Test
	@DisplayName("Test very long text")
	void testVeryLongText()
	{
		String longText = "antidisestablishmentarianism".repeat(10);
		SyntaxToken token = new SyntaxToken(longText, 0, "lemma", pos, 1, label);
		assertEquals(longText, token.getText());
	}

	@Test
	@DisplayName("Test special characters in text and lemma")
	void testSpecialCharacters()
	{
		String text = "café";
		String lemma = "naïve";
		SyntaxToken token = new SyntaxToken(text, 0, lemma, pos, 1, label);
		assertEquals(text, token.getText());
		assertEquals(lemma, token.getLemma());
	}

	@Test
	@DisplayName("Test whitespace in text and lemma")
	void testWhitespace()
	{
		String text = "  test  ";
		String lemma = "  lemma  ";
		SyntaxToken token = new SyntaxToken(text, 0, lemma, pos, 1, label);
		assertEquals(text, token.getText());
		assertEquals(lemma, token.getLemma());
	}

	@Test
	@DisplayName("Test maximum integer values for offsets")
	void testMaxIntegerOffsets()
	{
		SyntaxToken token = new SyntaxToken("test", Integer.MAX_VALUE, "lemma", pos, Integer.MIN_VALUE, label);
		assertEquals(Integer.MAX_VALUE, token.getBeginOffset());
		assertEquals(Integer.MIN_VALUE, token.getHeadTokenIndex());
	}
}
