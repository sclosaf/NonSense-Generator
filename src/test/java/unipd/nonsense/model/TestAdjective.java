package unipd.nonsense.model;

import org.junit.jupiter.api.*;
import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testing Adjective")
class TestAdjective
{
	private Adjective adjective;

	@BeforeEach
	void setup()
	{
		adjective = new Adjective("happy");
	}

	@Test
	@DisplayName("Test valid construction")
	void testValidConstruction()
	{
		assertNotNull(adjective);
		assertEquals("happy", adjective.getAdjective());
	}

	@Test
	@DisplayName("Test empty adjective throws exception")
	void testEmptyAdjective()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new Adjective(""));
	}
}
