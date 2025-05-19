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
}
