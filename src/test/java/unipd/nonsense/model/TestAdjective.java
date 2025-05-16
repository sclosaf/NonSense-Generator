package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testing Adjective")
public class TestAdjective{

	Adjective adj = new Adjective("adjectiveExample");
	
	@Test
	@DisplayName("Create adjectives with invalid arguements")
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
