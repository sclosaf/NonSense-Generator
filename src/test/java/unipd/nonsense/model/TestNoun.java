package unipd.nonsense.model;

import org.junit.jupiter.api.*;
import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Number;
import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testing Noun")
class TestNoun
{
	private Noun noun;

	@BeforeEach
	void setup()
	{
		noun = new Noun("dog", Number.SINGULAR);
	}

	@Test
	@DisplayName("Test valid construction")
	void testValidConstruction()
	{
		assertNotNull(noun);
		assertEquals("dog", noun.getNoun());
		assertEquals(Number.SINGULAR, noun.getNumber());
	}

	@Test
	@DisplayName("Test building noun with invalid arguments")
	public void testNoun_InvalidElement(){
		assertThrows(InvalidGrammaticalElementException.class, () ->
			new Noun("", Number.SINGULAR), "Should throw InvalidGrammaticalElementException");
	}

	@Test
	@DisplayName("Test null noun throws exception")
	void testNullNoun()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new Noun(null, Number.PLURAL));
	}


	@Test
	@DisplayName("Getting singular number")
	public void testGetNumber_SINGULAR()
	{
		assertEquals(noun.getNumber(), Number.SINGULAR);
	}

	@Test
	@DisplayName("Getting plural number")
	public void testGetNumber_PLURAL()
	{
		noun = new Noun("dogs", Number.PLURAL);
		assertEquals(noun.getNumber(), Number.PLURAL);
	}

	@Test
	@DisplayName("Test noun with whitespace only throws exception")
	void testWhitespaceNoun()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new Noun("   ", Number.SINGULAR));
	}

	@Test
	@DisplayName("Test noun with leading/trailing whitespace is trimmed")
	void testTrimmedNoun()
	{
		Noun testNoun = new Noun("  cat  ", Number.SINGULAR);
		assertEquals("cat", testNoun.getNoun());
	}

	@Test
	@DisplayName("Test noun with special characters")
	void testNounWithSpecialChars()
	{
		Noun testNoun = new Noun("dög", Number.SINGULAR);
		assertEquals("dög", testNoun.getNoun());
	}

	@Test
	@DisplayName("Test noun with numbers")
	void testNounWithNumbers()
	{
		Noun testNoun = new Noun("robot123", Number.PLURAL);
		assertEquals("robot123", testNoun.getNoun());
	}

	@Test
	@DisplayName("Test very long noun")
	void testVeryLongNoun()
	{
		String longNoun = "verylooooooooooooooooooooooooooooooooooooooooooooooooooooooongnoun";
		Noun testNoun = new Noun(longNoun, Number.SINGULAR);
		assertEquals(longNoun, testNoun.getNoun());
	}

	@Test
	@DisplayName("Test noun with mixed case")
	void testNounWithMixedCase()
	{
		Noun testNoun = new Noun("MixedCase", Number.SINGULAR);
		assertEquals("MixedCase", testNoun.getNoun());
	}

	@Test
	@DisplayName("Test null number throws NullPointerException")
	void testNullNumber()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new Noun("book", null));
	}

	@Test
	@DisplayName("Test getNoun returns same reference")
	void testGetNounReturnsSameReference()
	{
		String nounString = "tree";
		Noun testNoun = new Noun(nounString, Number.SINGULAR);
		assertSame(nounString, testNoun.getNoun());
	}
}
