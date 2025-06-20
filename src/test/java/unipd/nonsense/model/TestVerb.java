package unipd.nonsense.model;

import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Tense;

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
		verb = new Verb("run", Number.PLURAL, Tense.PRESENT);
	}

	@Test
	@DisplayName("Test valid construction")
	void testValidConstruction()
	{
		assertNotNull(verb);
		assertEquals("run", verb.getVerb());
		assertEquals(Tense.PRESENT, verb.getTense());
	}

	@Test
	@DisplayName("Test empty verb throws exception")
	void testEmptyVerb()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new Verb("", Number.SINGULAR, Tense.PAST));
	}

	@Test
	@DisplayName("Test all tense values")
	void testAllTenses()
	{
		Verb pastVerb = new Verb("ran", Number.PLURAL, Tense.PAST);
		Verb presentVerb = new Verb("run", Number.PLURAL, Tense.PRESENT);
		Verb futureVerb = new Verb("will run", Number.PLURAL, Tense.FUTURE);

		assertEquals(Tense.PAST, pastVerb.getTense());
		assertEquals(Tense.PRESENT, presentVerb.getTense());
		assertEquals(Tense.FUTURE, futureVerb.getTense());
	}
}
