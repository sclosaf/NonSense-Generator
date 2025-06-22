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

	@Test
	@DisplayName("Test null verb throws exception")
	void testNullVerb()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new Verb(null, Number.SINGULAR, Tense.PRESENT));
	}

	@Test
	@DisplayName("Test null number throws exception")
	void testNullNumber()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new Verb("run", null, Tense.PRESENT));
	}

	@Test
	@DisplayName("Test null tense throws exception")
	void testNullTense()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new Verb("run", Number.SINGULAR, null));
	}

	@Test
	@DisplayName("Test whitespace verb trimming")
	void testWhitespaceVerb()
	{
		Verb testVerb = new Verb("  jump  ", Number.SINGULAR, Tense.PRESENT);
		assertEquals("jump", testVerb.getVerb());
	}

	@Test
	@DisplayName("Test verb with special characters")
	void testVerbWithSpecialChars()
	{
		Verb testVerb = new Verb("célébrer", Number.SINGULAR, Tense.PRESENT);
		assertEquals("célébrer", testVerb.getVerb());
	}

	@Test
	@DisplayName("Test verb with numbers")
	void testVerbWithNumbers()
	{
		Verb testVerb = new Verb("win42", Number.PLURAL, Tense.FUTURE);
		assertEquals("win42", testVerb.getVerb());
	}

	@Test
	@DisplayName("Test very long verb")
	void testVeryLongVerb()
	{
		String longVerb = "supercalifragilisticexpialidocious".repeat(5);
		Verb testVerb = new Verb(longVerb, Number.SINGULAR, Tense.PAST);
		assertEquals(longVerb, testVerb.getVerb());
	}

	@Test
	@DisplayName("Test all number values")
	void testAllNumbers()
	{
		Verb singularVerb = new Verb("runs", Number.SINGULAR, Tense.PRESENT);
		Verb pluralVerb = new Verb("run", Number.PLURAL, Tense.PRESENT);

		assertEquals(Number.SINGULAR, singularVerb.getNumber());
		assertEquals(Number.PLURAL, pluralVerb.getNumber());
	}

	@Test
	@DisplayName("Test verb with mixed case")
	void testVerbWithMixedCase()
	{
		Verb testVerb = new Verb("ReWaRd", Number.SINGULAR, Tense.PAST);
		assertEquals("ReWaRd", testVerb.getVerb());
	}

	@Test
	@DisplayName("Test hyphenated verb")
	void testHyphenatedVerb()
	{
		Verb testVerb = new Verb("double-click", Number.PLURAL, Tense.PRESENT);
		assertEquals("double-click", testVerb.getVerb());
	}

	@Test
	@DisplayName("Test empty string after trimming throws exception")
	void testEmptyAfterTrimming()
	{
		assertThrows(InvalidGrammaticalElementException.class,
			() -> new Verb("   ", Number.SINGULAR, Tense.PAST));
	}

	@Test
	@DisplayName("Test verb with punctuation")
	void testVerbWithPunctuation()
	{
		Verb testVerb = new Verb("'scuse", Number.SINGULAR, Tense.PRESENT);
		assertEquals("'scuse", testVerb.getVerb());
	}

	@Test
	@DisplayName("Test verb with leading/trailing special chars")
	void testVerbWithEdgeSpecialChars()
	{
		Verb testVerb = new Verb("!!!alert!!!", Number.SINGULAR, Tense.PRESENT);
		assertEquals("!!!alert!!!", testVerb.getVerb());
	}
}
