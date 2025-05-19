package unipd.nonsense.model;

import org.junit.jupiter.api.*;
import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Noun.Number;
import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testing Noun")
class TestNoun
{
	private Noun noun;

	@BeforeEach
	void setup()
	{
		noun = new Noun("dog", Noun.Number.SINGULAR);
	}

	@Test
	@DisplayName("Test valid construction")
	void testValidConstruction()
	{
		assertNotNull(noun);
		assertEquals("dog", noun.getNoun());
		assertEquals(Noun.Number.SINGULAR, noun.getNumber());
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
			() -> new Noun(null, Noun.Number.PLURAL));
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
}
