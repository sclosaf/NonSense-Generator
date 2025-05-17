package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Verb.Tense;

@DisplayName("Testing Verb")
public class TestVerb{

	Verb pastVerb = new Verb("pastVerbExample", Tense.PAST);
	Verb presentVerb = new Verb("presentVerbExample", Tense.PRESENT);
	Verb futureVerb = new Verb("futureVerbExample", Tense.FUTURE);
	
	@Test
	@DisplayName("Test correct verb building success")
	public void testVerb_CorrectCreation(){
		Verb testVerbBuilding = new Verb("verbBuildingExample", Tense.PAST);
		assertNotNull(testVerbBuilding, "Should have correctly built a verb");
		assertNotNull(testVerbBuilding.getVerb(), "Should return the verb's string");
		assertNotNull(testVerbBuilding.getTense(), "Should return the verb's tense");
	}

	@Test
	@DisplayName("Test building verb with invalid arguments")
	public void testVerb_InvalidElement(){
		assertThrows(InvalidGrammaticalElementException.class, () -> 
			new Verb("", Tense.PAST), "Should throw InvalidGrammaticalElementException");
	}

	@Test
	@DisplayName("Getting verbs")
	public void testGetVerb_VerbString(){
		assertEquals(pastVerb.getVerb(), "pastVerbExample");
	}

	@Test
	@DisplayName("Getting past tense")
	public void testGetTense_PAST(){
		assertEquals(pastVerb.getTense(), Tense.PAST);
	}

	@Test
	@DisplayName("Getting present tense")
	public void testGetTense_PRESENT(){
		assertEquals(presentVerb.getTense(), Tense.PRESENT);
	}

	@Test
	@DisplayName("Getting future tense")
	public void testGetTense_FUTURE(){
		assertEquals(futureVerb.getTense(), Tense.FUTURE);
	}

}
