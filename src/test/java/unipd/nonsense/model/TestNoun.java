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
	public void testNoun_InvalidElement(){
		assertThrows(InvalidGrammaticalElementException.class, () -> 
			new Noun("", Number.SINGULAR), "Should throw InvalidGrammaticalElementException");
	}

	@Test
	@DisplayName("Getting nouns")
	public void testGetNoun_NounString(){
		assertEquals(singularNoun.getNoun(), "singularNounExample");
	}

	@Test
	@DisplayName("Getting singular number")
	public void testGetNumber_SINGULAR(){
		assertEquals(singularNoun.getNumber(), Number.SINGULAR);
	}

	@Test
	@DisplayName("Getting plural number")
	public void testGetNumber_PLURAL(){
		assertEquals(pluralNoun.getNumber(), Number.PLURAL);
	}

}
