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

	@Test
	@DisplayName("Test null adjective throws exception")
	void testNullAdjective()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new Adjective(null));
	}

	@Test
	@DisplayName("Test adjective with whitespace only throws exception")
	void testWhitespaceAdjective()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new Adjective("   "));
	}

	@Test
	@DisplayName("Test adjective with leading/trailing whitespace is trimmed")
	void testTrimmedAdjective()
	{
		Adjective adj = new Adjective("  joyful  ");
		assertEquals("joyful", adj.getAdjective());
	}

	@Test
	@DisplayName("Test adjective with special characters")
	void testAdjectiveWithSpecialChars()
	{
		Adjective adj = new Adjective("fantastic!");
		assertEquals("fantastic!", adj.getAdjective());
	}

	@Test
	@DisplayName("Test adjective with numbers")
	void testAdjectiveWithNumbers()
	{
		Adjective adj = new Adjective("cool123");
		assertEquals("cool123", adj.getAdjective());
	}

	@Test
	@DisplayName("Test adjective with mixed case")
	void testAdjectiveWithMixedCase()
	{
		Adjective adj = new Adjective("AmAzInG");
		assertEquals("AmAzInG", adj.getAdjective());
	}

	@Test
	@DisplayName("Test very long adjective")
	void testVeryLongAdjective()
	{
		String longAdj = "supercalifragilisticexpialidocious".repeat(10);
		Adjective adj = new Adjective(longAdj);
		assertEquals(longAdj, adj.getAdjective());
	}

	@Test
	@DisplayName("Test adjective with non-ASCII characters")
	void testAdjectiveWithNonAsciiChars()
	{
		Adjective adj = new Adjective("crème brûlée");
		assertEquals("crème brûlée", adj.getAdjective());
	}

	@Test
	@DisplayName("Test getAdjective returns same reference")
	void testGetAdjectiveReturnsSameReference()
	{
		String adjString = "happy";
		Adjective adj = new Adjective(adjString);
		assertSame(adjString, adj.getAdjective());
	}
}
