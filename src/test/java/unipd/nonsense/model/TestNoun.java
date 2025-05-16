package unipd.nonsense.model;

import unipd.nonsense.exceptions.InvalidGrammaticalElementException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Noun.Number;

@DisplayName("Testing Noun")
public class TestNoun{

	Noun singularNoun = new Noun("singularNounExample", Number.SINGULAR);
	Noun pluralNoun = new Noun("pluralNounExample", Number.PLURAL);
	
	@Test
	@DisplayName("Create noun with invalid arguements")
	public void testInvalidAdjectiveCreation(){
		assertThrows(InvalidGrammaticalElementException.class, () -> 
			new Noun("", Number.SINGULAR), "Should throw InvalidGrammaticalElementException");
	}

	@Test
	@DisplayName("Getting nouns")
	public void testGetNoun(){
		assertEquals(singularNoun.getNoun(), "singularNounExample");
	}

	@Test
	@DisplayName("Getting numbers")
	public void testGetNumber(){
		assertEquals(singularNoun.getNumber(), Number.SINGULAR);
		assertEquals(pluralNoun.getNumber(), Number.PLURAL);
	}

}
