package unipd.nonsense.generator;

import com.google.cloud.language.v1.DependencyEdge;
import com.google.cloud.language.v1.DependencyEdge.Label;
import com.google.cloud.language.v1.PartOfSpeech;
import org.junit.jupiter.api.*;
import unipd.nonsense.model.SyntaxToken;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testing SyntaxTreeBuilder")
class TestSyntaxTreeBuilder
{
	private static class TestToken extends SyntaxToken
	{
		private final String posTag;

		TestToken(String text, int headIndex, Label label, String posTag)
		{
			super(text, 0, text, PartOfSpeech.newBuilder().setTag(PartOfSpeech.Tag.UNKNOWN).build(), headIndex, label);
			this.posTag = posTag;
		}

		@Override
		public String getPosTag()
		{
			return posTag;
		}
	}

	@Test
	@DisplayName("Empty token list should return appropriate message")
	void testEmptyTokenList()
	{
		String result = SyntaxTreeBuilder.getSyntaxTree(new ArrayList<>());
		assertEquals("No tokens available for analysis", result);
	}

	@Test
	@DisplayName("Null token list should return appropriate message")
	void testNullTokenList()
	{
		String result = SyntaxTreeBuilder.getSyntaxTree(null);
		assertEquals("No tokens available for analysis", result);
	}

	@Test
	@DisplayName("Single root token should build correct tree")
	void testSingleRootToken()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Root", -1, Label.ROOT, "NOUN"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Root (NOUN)"));
	}

	@Test
	@DisplayName("Multiple root tokens should be handled as separate sentences")
	void testMultipleRootTokens()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Root1", -1, Label.ROOT, "NOUN"));
		tokens.add(new TestToken("Root2", -1, Label.ROOT, "NOUN"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Sentence 1:"));
		assertTrue(result.contains("Sentence 2:"));
		assertTrue(result.contains("Root1 (NOUN)"));
		assertTrue(result.contains("Root2 (NOUN)"));
	}

	@Test
	@DisplayName("Complex sentence should build correct tree structure")
	void testComplexSentenceStructure()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("The", 1, Label.DET, "DET"));
		tokens.add(new TestToken("cat", 3, Label.NSUBJ, "NOUN"));
		tokens.add(new TestToken("sat", 3, Label.PREP, "VERB"));
		tokens.add(new TestToken("on", -1, Label.ROOT, "ADP"));
		tokens.add(new TestToken("mat", 3, Label.POBJ, "NOUN"));
		tokens.add(new TestToken(".", 4, Label.P, "PUNCT"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("on (ADP)"));
		assertTrue(result.contains("├─cat (NOUN)"));
		assertTrue(result.contains("│ └─The (DET)"));
		assertTrue(result.contains("├─sat (VERB)"));
		assertTrue(result.contains("└─mat (NOUN)"));
		assertTrue(result.contains("  └─. (PUNCT)"));
	}

	@Test
	@DisplayName("Punctuation tokens should be properly attached")
	void testPunctuationAttachment()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Hello", -1, Label.ROOT, "INTJ"));
		tokens.add(new TestToken(",", 0, Label.P, "PUNCT"));
		tokens.add(new TestToken("world", 0, Label.NSUBJ, "NOUN"));
		tokens.add(new TestToken("!", 2, Label.P, "PUNCT"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Hello (INTJ)"));
		assertTrue(result.contains("├─, (PUNCT)"));
		assertTrue(result.contains("└─world (NOUN)"));
		assertTrue(result.contains("  └─! (PUNCT)"));
	}

	@Test
	@DisplayName("Self-referencing tokens should be handled")
	void testSelfReferencingTokens()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Root", 0, Label.ROOT, "NOUN"));
		tokens.add(new TestToken("Child", 0, Label.NSUBJ, "NOUN"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Root (NOUN)"));
		assertTrue(result.contains("└─Child (NOUN)"));
	}

	@Test
	@DisplayName("Tokens with invalid head indices should be handled")
	void testInvalidHeadIndices()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Invalid", 100, Label.ROOT, "NOUN"));
		tokens.add(new TestToken("Child", 0, Label.NSUBJ, "NOUN"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Invalid (NOUN)"));
		assertTrue(result.contains("└─Child (NOUN)"));
	}

	@Test
	@DisplayName("Cycle detection should prevent infinite loops")
	void testCycleDetection()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("A", 1, Label.NSUBJ, "NOUN"));
		tokens.add(new TestToken("B", 0, Label.NSUBJ, "NOUN"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
	}

	@Test
	@DisplayName("Exception during tree building should be caught and handled")
	void testExceptionHandling()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(null);

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);
		assertNotNull(result);
		assertTrue(result.startsWith("Error generating syntax tree"));
	}

	@Test
	@DisplayName("Test punctuation token reattachment")
	void testPunctuationReattachment()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Root", -1, Label.ROOT, "NOUN"));
		tokens.add(new TestToken(".", -1, Label.P, "PUNCT"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Root (NOUN)"));
		assertTrue(result.contains("└─. (PUNCT)"));
	}

	@Test
	@DisplayName("Test punctuation at start of sentence")
	void testPunctuationAtStart()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("\"", -1, Label.P, "PUNCT"));
		tokens.add(new TestToken("Hello", -1, Label.ROOT, "INTJ"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Hello (INTJ)"));
		assertTrue(result.contains("└─\" (PUNCT)"));
	}

	@Test
	@DisplayName("Test finding root tokens by dependency label")
	void testFindRootByDependencyLabel()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Word1", 2, Label.NSUBJ, "NOUN"));
		tokens.add(new TestToken("Word2", 2, Label.DOBJ, "NOUN"));
		tokens.add(new TestToken("Root", -1, Label.ROOT, "VERB"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Root (VERB)"));
		assertTrue(result.contains("├─Word1 (NOUN)"));
		assertTrue(result.contains("└─Word2 (NOUN)"));
	}

	@Test
	@DisplayName("Test sentence with no valid roots")
	void testNoValidRoots()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Word1", 1, Label.NSUBJ, "NOUN"));
		tokens.add(new TestToken("Word2", 0, Label.DOBJ, "NOUN"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertEquals("No root tokens found - invalid syntactic structure", result);
	}

	@Test
	@DisplayName("Test deep tree hierarchy rendering")
	void testDeepTreeHierarchy()
	{
		List<TestToken> tokens = new ArrayList<>();

		tokens.add(new TestToken("Root", -1, Label.ROOT, "VERB"));
		tokens.add(new TestToken("Level1", 0, Label.NSUBJ, "NOUN"));
		tokens.add(new TestToken("Level2", 1, Label.DOBJ, "NOUN"));
		tokens.add(new TestToken("Level3", 2, Label.NSUBJ, "NOUN"));
		tokens.add(new TestToken("Level4", 3, Label.DOBJ, "NOUN"));
		tokens.add(new TestToken("Level5", 4, Label.NSUBJ, "NOUN"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);

		assertTrue(result.contains("Root (VERB)"));
		assertTrue(result.contains("└─Level1 (NOUN)"));
		assertTrue(result.contains("  └─Level2 (NOUN)"));
		assertTrue(result.contains("    └─Level3 (NOUN)"));
		assertTrue(result.contains("      └─Level4 (NOUN)"));
		assertTrue(result.contains("        └─Level5 (NOUN)"));
	}

	@Test
	@DisplayName("Test multiple children with proper tree formatting")
	void testMultipleChildren()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Root", -1, Label.ROOT, "VERB"));
		tokens.add(new TestToken("Child1", 0, Label.NSUBJ, "NOUN"));
		tokens.add(new TestToken("Child2", 0, Label.DOBJ, "NOUN"));
		tokens.add(new TestToken("Child3", 0, Label.IOBJ, "NOUN"));
		tokens.add(new TestToken("GrandChild1", 1, Label.NSUBJ, "ADJ"));
		tokens.add(new TestToken("GrandChild2", 2, Label.NSUBJ, "ADJ"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Root (VERB)"));
		assertTrue(result.contains("├─Child1 (NOUN)"));
		assertTrue(result.contains("│ └─GrandChild1 (ADJ)"));
		assertTrue(result.contains("├─Child2 (NOUN)"));
		assertTrue(result.contains("│ └─GrandChild2 (ADJ)"));
		assertTrue(result.contains("└─Child3 (NOUN)"));
	}

	@Test
	@DisplayName("Test tokens with unconventional POS tags")
	void testUnconventionalPosTags()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Root", -1, Label.ROOT, "X"));
		tokens.add(new TestToken("Symbol", 0, Label.DEP, "SYM"));
		tokens.add(new TestToken("Emoji", 0, Label.DEP, "EMOJI"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Root (X)"));
		assertTrue(result.contains("├─Symbol (SYM)"));
		assertTrue(result.contains("└─Emoji (EMOJI)"));
	}

	@Test
	@DisplayName("Test very long token texts")
	void testLongTokenTexts()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("ThisIsAnExtremelyLongTokenTextThatExceedsNormalLengths", -1, Label.ROOT, "NOUN"));
		tokens.add(new TestToken("AnotherVeryLongTokenWithEvenMoreCharactersThanTheFirstOne", 0, Label.NSUBJ, "NOUN"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);

		assertTrue(result.contains("ThisIsAnExtremelyLongTokenTextThatExceedsNormalLengths (NOUN)"));
		assertTrue(result.contains("└─AnotherVeryLongTokenWithEvenMoreCharactersThanTheFirstOne (NOUN)"));
	}

	@Test
	@DisplayName("Test complex sentence with multiple punctuation tokens")
	void testMultiplePunctuationTokens()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Hello", -1, Label.ROOT, "INTJ"));
		tokens.add(new TestToken(",", 0, Label.P, "PUNCT"));
		tokens.add(new TestToken("how", 0, Label.ADVMOD, "ADV"));
		tokens.add(new TestToken("are", 0, Label.COP, "VERB"));
		tokens.add(new TestToken("you", 3, Label.NSUBJ, "PRON"));
		tokens.add(new TestToken("?", 0, Label.P, "PUNCT"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Hello (INTJ)"));
		assertTrue(result.contains("├─, (PUNCT)"));
		assertTrue(result.contains("├─how (ADV)"));
		assertTrue(result.contains("├─are (VERB)"));
		assertTrue(result.contains("│ └─you (PRON)"));
		assertTrue(result.contains("└─? (PUNCT)"));
	}

	@Test
	@DisplayName("Test special characters in tokens")
	void testSpecialCharacterTokens()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Root@", -1, Label.ROOT, "NOUN"));
		tokens.add(new TestToken("Child%", 0, Label.NSUBJ, "NOUN"));
		tokens.add(new TestToken("!", 1, Label.P, "PUNCT"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Root@ (NOUN)"));
		assertTrue(result.contains("Child% (NOUN)"));
		assertTrue(result.contains("! (PUNCT)"));
	}

	@Test
	@DisplayName("Test tree with tokens having same text but different properties")
	void testDuplicateTextTokens()
	{
		List<TestToken> tokens = new ArrayList<>();

		tokens.add(new TestToken("Word", -1, Label.ROOT, "NOUN"));
		tokens.add(new TestToken("Word", 0, Label.NSUBJ, "VERB"));
		tokens.add(new TestToken("Word", 1, Label.DOBJ, "ADJ"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Word (NOUN)"));
		assertTrue(result.contains("Word (VERB)"));
		assertTrue(result.contains("Word (ADJ)"));
	}

	@Test
	@DisplayName("Test multiple disconnected punctuation tokens")
	void testMultipleDisconnectedPunctuation()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Root", -1, Label.ROOT, "NOUN"));
		tokens.add(new TestToken(".", -1, Label.P, "PUNCT"));
		tokens.add(new TestToken(",", -1, Label.P, "PUNCT"));
		tokens.add(new TestToken("!", -1, Label.P, "PUNCT"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Root (NOUN)"));
		assertTrue(result.contains("├─. (PUNCT)"));
		assertTrue(result.contains("├─, (PUNCT)"));
		assertTrue(result.contains("└─! (PUNCT)"));
	}

	@Test
	@DisplayName("Test tree with maximum depth and breadth")
	void testMaxDepthAndBreadth()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Root", -1, Label.ROOT, "VERB"));

		for(int i = 0; i < 10; i++)
		{
			tokens.add(new TestToken("Child" + i, 0, Label.values()[i % Label.values().length], "NOUN"));

			for (int j = 0; j < 5; j++)
				tokens.add(new TestToken("GrandChild" + i + "-" + j, i+1, Label.values()[j % Label.values().length], "ADJ"));
		}

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Root (VERB)"));

		for(int i = 0; i < 10; i++)
		{
			assertTrue(result.contains("Child" + i + " (NOUN)"));
			for(int j = 0; j < 5; j++)
					assertTrue(result.contains("GrandChild" + i + "-" + j + " (ADJ)"));
		}
	}

	@Test
	@DisplayName("Test tokens with empty text")
	void testEmptyTextTokens()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("", -1, Label.ROOT, "NOUN"));
		tokens.add(new TestToken("", 0, Label.NSUBJ, "NOUN"));
		tokens.add(new TestToken("", 1, Label.P, "PUNCT"));

		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains(" (NOUN)"));
		assertTrue(result.contains(" (PUNCT)"));
	}

	@Test
	@DisplayName("Test tree with all possible dependency labels")
	void testAllDependencyLabels()
	{
		List<TestToken> tokens = new ArrayList<>();
		tokens.add(new TestToken("Root", -1, Label.ROOT, "VERB"));

		for(Label label : Label.values())
			if(label != Label.ROOT && label != Label.UNRECOGNIZED)
				tokens.add(new TestToken(label.toString(), 0, label, "NOUN"));


		String result = SyntaxTreeBuilder.getSyntaxTree(tokens);

		assertNotNull(result);
		assertTrue(result.contains("Root (VERB)"));

		for(Label label : Label.values())
			if(label != Label.ROOT && label != Label.UNRECOGNIZED)
					assertTrue(result.contains(label.toString() + " (NOUN)"));
	}
}
