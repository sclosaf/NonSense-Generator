package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testing Adjective")
public class TestAdjective{

	Adjective adj = new Adjective("adjectiveExample");
	
	@Test
	@DisplayName("Test correct adjective building success")
	public void testAdjective_CorrectCreation(){
		Adjective testAdjectiveBuilding = new Adjective("adjectiveBuildingExample");
		assertNotNull(testAdjectiveBuilding, "Should have correctly built and adjective");
		assertNotNull(testAdjectiveBuilding.getAdjective(), "Should return the adjective's string");
	}

	@Test
	@DisplayName("Test building adjectives with invalid arguments")
	public void testAdjective_InvalidElement(){
		assertThrows(InvalidGrammaticalElementException.class, () -> 
			new Adjective(""), "Should throw InvalidGrammaticalElementException");
	}

	@Test
	@DisplayName("Getting adjectives")
	public void testGetAdjective_AdjectiveString(){
		assertEquals(adj.getAdjective(), "adjectiveExample");
	}

}
