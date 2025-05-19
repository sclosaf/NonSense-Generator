package unipd.nonsense.model;

import org.junit.jupiter.api.*;
import unipd.nonsense.exceptions.InvalidGrammaticalElementException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testing Verb")
class TestVerb
{
	private Verb verb;

	@BeforeEach
	void setup()
	{
		verb = new Verb("run", Verb.Tense.PRESENT);
	}

	@Test
	@DisplayName("Test valid construction")
	void testValidConstruction()
	{
		assertNotNull(verb);
		assertEquals("run", verb.getVerb());
		assertEquals(Verb.Tense.PRESENT, verb.getTense());
	}

	@Test
	@DisplayName("Test empty verb throws exception")
	void testEmptyVerb()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new Verb("", Verb.Tense.PAST));
	}

	@Test
	@DisplayName("Test all tense values")
	void testAllTenses()
	{
		Verb pastVerb = new Verb("ran", Verb.Tense.PAST);
		Verb presentVerb = new Verb("run", Verb.Tense.PRESENT);
		Verb futureVerb = new Verb("will run", Verb.Tense.FUTURE);

		assertEquals(Verb.Tense.PAST, pastVerb.getTense());
		assertEquals(Verb.Tense.PRESENT, presentVerb.getTense());
		assertEquals(Verb.Tense.FUTURE, futureVerb.getTense());
	}
}
