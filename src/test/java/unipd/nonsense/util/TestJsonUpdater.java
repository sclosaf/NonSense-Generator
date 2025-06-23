package unipd.nonsense.util;

import unipd.nonsense.model.Noun;
import unipd.nonsense.model.Adjective;
import unipd.nonsense.model.Verb;
import unipd.nonsense.model.Template;
import unipd.nonsense.model.Number;
import unipd.nonsense.model.Tense;
import unipd.nonsense.exceptions.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

@DisplayName("Testing JsonUpdater")
class TestJsonUpdater
{
	private JsonUpdateObserver testObserver;
	private boolean observerNotified;

	@BeforeEach
	void setup()
	{
		observerNotified = false;
		testObserver = new JsonUpdateObserver() {
			@Override
			public void onJsonUpdate() throws IOException {
				observerNotified = true;
			}
		};
		JsonUpdater.addObserver(testObserver);
	}

	@AfterEach
	void cleanup()
	{
		JsonUpdater.removeObserver(testObserver);
	}

	@Test
	@DisplayName("Test noun loading with valid number")
	void testLoadNounWithValidNumber() throws IOException
	{
		JsonUpdater.loadNoun("car", Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified after noun loading");

		observerNotified = false;
		JsonUpdater.loadNoun("tree", Number.PLURAL);
		assertTrue(observerNotified, "Observer should be notified after noun loading");
	}

	@Test
	@DisplayName("Test noun loading with invalid number")
	void testLoadNounWithInvalidNumber()
	{
		assertThrows(InvalidNumberException.class, () -> {
			JsonUpdater.loadNoun("street", null);
		});
	}

	@Test
	@DisplayName("Test verb loading with valid tense")
	void testLoadVerbWithValidTense() throws IOException
	{
		JsonUpdater.loadVerb("verb", Tense.PAST, Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified after verb loading");

		observerNotified = false;
		JsonUpdater.loadVerb("verb", Tense.PRESENT, Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified after verb loading");

		observerNotified = false;
		JsonUpdater.loadVerb("verb", Tense.FUTURE, Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified after verb loading");
	}

	@Test
	@DisplayName("Test verb loading with invalid tense")
	void testLoadVerbWithInvalidTense()
	{
		assertThrows(InvalidTenseException.class, () -> {
			JsonUpdater.loadVerb("verb", null, null);
		});
	}

	@Test
	@DisplayName("Test adjective loading")
	void testLoadAdjective() throws IOException
	{
		JsonUpdater.loadAdjective("old");
		assertTrue(observerNotified, "Observer should be notified after adjective loading");
	}

	@Test
	@DisplayName("Test template loading with valid number")
	void testLoadTemplateWithValidNumber() throws IOException
	{
		JsonUpdater.loadTemplate("[adjective] [noun] and [noun] [verb].", Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified after template loading");

		observerNotified = false;
		JsonUpdater.loadTemplate("[adjective] [noun] [verb].", Number.PLURAL);
		assertTrue(observerNotified, "Observer should be notified after template loading");
	}

	@Test
	@DisplayName("Test template loading with invalid number")
	void testLoadTemplateWithInvalidNumber()
	{
		assertThrows(InvalidNumberException.class, () -> {
			JsonUpdater.loadTemplate("this is a [adjective] [noun], [verb]!", null);
		});
	}

	@Test
	@DisplayName("Test object-based loading methods")
	void testObjectBasedLoading() throws IOException
	{
		Noun noun = new Noun("Car", Number.SINGULAR);
		Verb verb = new Verb("eats", Number.SINGULAR, Tense.PRESENT);
		Adjective adjective = new Adjective("quick");
		Template template = new Template("[noun] [adjective] [verb]?", Number.SINGULAR);

		JsonUpdater.loadNoun(noun);
		assertTrue(observerNotified, "Observer should be notified after noun loading");

		observerNotified = false;
		JsonUpdater.loadVerb(verb);
		assertTrue(observerNotified, "Observer should be notified after verb loading");

		observerNotified = false;
		JsonUpdater.loadAdjective(adjective);
		assertTrue(observerNotified, "Observer should be notified after adjective loading");

		observerNotified = false;
		JsonUpdater.loadTemplate(template);
		assertTrue(observerNotified, "Observer should be notified after template loading");
	}

	@Test
	@DisplayName("Test multiple observers notification")
	void testMultipleObservers() throws IOException
	{
		final boolean[] observer1Notified = {false};
		final boolean[] observer2Notified = {false};

		JsonUpdateObserver observer1 = new JsonUpdateObserver() {
			@Override
			public void onJsonUpdate() throws IOException {
				observer1Notified[0] = true;
			}
		};

		JsonUpdateObserver observer2 = new JsonUpdateObserver() {
			@Override
			public void onJsonUpdate() throws IOException {
				observer2Notified[0] = true;
			}
		};

		JsonUpdater.addObserver(observer1);
		JsonUpdater.addObserver(observer2);

		JsonUpdater.loadAdjective("slow");

		assertTrue(observer1Notified[0], "First observer should be notified");
		assertTrue(observer2Notified[0], "Second observer should be notified");

		JsonUpdater.removeObserver(observer1);
		JsonUpdater.removeObserver(observer2);
	}

	@Test
	@DisplayName("Test error in observer notification")
	void testObserverNotificationError()
	{
		JsonUpdateObserver failingObserver = new JsonUpdateObserver() {
			@Override
			public void onJsonUpdate() throws IOException {
				throw new IOException("Simulated observer failure");
			}
		};

		JsonUpdater.addObserver(failingObserver);

		assertThrows(IOException.class, () -> {
			JsonUpdater.loadAdjective("difficult");
		}, "Should propagate IOException from observer");

		JsonUpdater.removeObserver(failingObserver);
	}

	@Test
	@DisplayName("Test loading multiple nouns consecutively")
	void testLoadMultipleNouns() throws IOException
	{
		JsonUpdater.loadNoun("observer", Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified after first noun loading");

		observerNotified = false;
		JsonUpdater.loadNoun("tower", Number.PLURAL);
		assertTrue(observerNotified, "Observer should be notified after second noun loading");

		observerNotified = false;
		JsonUpdater.loadNoun("home", Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified after third noun loading");
	}

	@Test
	@DisplayName("Test loading null as noun")
	void testLoadNullNoun()
	{
		assertThrows(IllegalArgumentException.class, () ->
		{
			JsonUpdater.loadNoun(null, Number.SINGULAR);
		}, "Should throw when loading null noun");
	}

	@Test
	@DisplayName("Test loading verb with all possible number-tense combinations")
	void testLoadVerbAllCombinations() throws IOException
	{
		for(Number number : Number.values())
		{
			for(Tense tense : Tense.values())
			{
				observerNotified = false;
				JsonUpdater.loadVerb("comboVerb", tense, number);
				assertTrue(observerNotified, "Observer should be notified for " + number + "-" + tense + " combination");
			}
		}
	}

	@Test
	@DisplayName("Test loading very long adjective")
	void testLoadLongAdjective() throws IOException
	{
		String longAdjective = "looooooooooooooooooooooooooooooooooooooooooooooong".repeat(10);
		JsonUpdater.loadAdjective(longAdjective);
		assertTrue(observerNotified, "Observer should be notified after loading long adjective");
	}


	@Test
	@DisplayName("Test concurrent observer addition and removal")
	void testConcurrentObserverManagement()
	{
		JsonUpdateObserver tempObserver1 = new JsonUpdateObserver()
		{
			@Override public void onJsonUpdate() throws IOException {}
		};

		JsonUpdateObserver tempObserver2 = new JsonUpdateObserver()
		{
			@Override public void onJsonUpdate() throws IOException {}
		};

		JsonUpdater.addObserver(tempObserver1);
		JsonUpdater.removeObserver(tempObserver1);
		JsonUpdater.addObserver(tempObserver2);


		assertDoesNotThrow(() ->
		{
			JsonUpdater.loadAdjective("concurrentTest");
		}, "Should handle concurrent observer changes gracefully");
	}

	@Test
	@DisplayName("Test loading items with whitespace")
	void testLoadItemsWithWhitespace() throws IOException
	{
		JsonUpdater.loadNoun("  room  ", Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified for noun with whitespace");

		observerNotified = false;
		JsonUpdater.loadAdjective("\tspaced\t");
		assertTrue(observerNotified, "Observer should be notified for adjective with tabs");
	}

	@Test
	@DisplayName("Test loading null object parameters")
	void testLoadNullObjects()
	{
		assertThrows(IllegalArgumentException.class, () ->
		{
			JsonUpdater.loadNoun((Noun) null);
		}, "Should throw when loading null Noun object");

		assertThrows(IllegalArgumentException.class, () ->
		{
			JsonUpdater.loadVerb((Verb) null);
		}, "Should throw when loading null Verb object");

		assertThrows(IllegalArgumentException.class, () ->
		{
			JsonUpdater.loadAdjective((Adjective) null);
		}, "Should throw when loading null Adjective object");

		assertThrows(IllegalArgumentException.class, () ->
		{
			JsonUpdater.loadTemplate((Template) null);
		}, "Should throw when loading null Template object");
	}

	@Test
	@DisplayName("Test loading items after observer removal")
	void testLoadingAfterObserverRemoval() throws IOException
	{
		JsonUpdateObserver tempObserver = new JsonUpdateObserver()
		{
			@Override public void onJsonUpdate() throws IOException
			{
				fail("Removed observer should not be notified");
			}
		};

		JsonUpdater.addObserver(tempObserver);
		JsonUpdater.removeObserver(tempObserver);

		assertDoesNotThrow(() ->
		{
			JsonUpdater.loadAdjective("postRemovalTest");
		}, "Should not notify removed observer");
	}

	@Test
	@DisplayName("Test notification order with multiple observers")
	void testObserverNotificationOrder() throws IOException
	{
		final List<String> notificationOrder = new ArrayList<>();

		JsonUpdateObserver firstObserver = new JsonUpdateObserver()
		{
			@Override public void onJsonUpdate() throws IOException
			{
				notificationOrder.add("first");
			}
		};

		JsonUpdateObserver secondObserver = new JsonUpdateObserver()
		{
			@Override public void onJsonUpdate() throws IOException
			{
				notificationOrder.add("second");
			}
		};

		JsonUpdater.addObserver(firstObserver);
		JsonUpdater.addObserver(secondObserver);

		JsonUpdater.loadAdjective("tested");

		assertEquals(List.of("first", "second"), notificationOrder,
			"Observers should be notified in addition order");

		JsonUpdater.removeObserver(firstObserver);
		JsonUpdater.removeObserver(secondObserver);
	}
}
