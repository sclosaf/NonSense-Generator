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

@DisplayName("Testing JsonUpdater")
class TestJsonUpdater
{
	private static final String TEST_NOUN = "testNoun";
	private static final String TEST_VERB = "testVerb";
	private static final String TEST_ADJECTIVE = "testAdjective";
	private static final String TEST_TEMPLATE = "The [adjective] [noun] [verb]";

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
		JsonUpdater.loadNoun(TEST_NOUN, Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified after noun loading");

		observerNotified = false;
		JsonUpdater.loadNoun(TEST_NOUN, Number.PLURAL);
		assertTrue(observerNotified, "Observer should be notified after noun loading");
	}

	@Test
	@DisplayName("Test noun loading with invalid number")
	void testLoadNounWithInvalidNumber()
	{
		assertThrows(InvalidNumberException.class, () -> {
			JsonUpdater.loadNoun(TEST_NOUN, null);
		});
	}

	@Test
	@DisplayName("Test verb loading with valid tense")
	void testLoadVerbWithValidTense() throws IOException
	{
		JsonUpdater.loadVerb(TEST_VERB, Tense.PAST, Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified after verb loading");

		observerNotified = false;
		JsonUpdater.loadVerb(TEST_VERB, Tense.PRESENT, Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified after verb loading");

		observerNotified = false;
		JsonUpdater.loadVerb(TEST_VERB, Tense.FUTURE, Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified after verb loading");
	}

	@Test
	@DisplayName("Test verb loading with invalid tense")
	void testLoadVerbWithInvalidTense()
	{
		assertThrows(InvalidTenseException.class, () -> {
			JsonUpdater.loadVerb(TEST_VERB, null, null);
		});
	}

	@Test
	@DisplayName("Test adjective loading")
	void testLoadAdjective() throws IOException
	{
		JsonUpdater.loadAdjective(TEST_ADJECTIVE);
		assertTrue(observerNotified, "Observer should be notified after adjective loading");
	}

	@Test
	@DisplayName("Test template loading with valid number")
	void testLoadTemplateWithValidNumber() throws IOException
	{
		JsonUpdater.loadTemplate(TEST_TEMPLATE, Number.SINGULAR);
		assertTrue(observerNotified, "Observer should be notified after template loading");

		observerNotified = false;
		JsonUpdater.loadTemplate(TEST_TEMPLATE, Number.PLURAL);
		assertTrue(observerNotified, "Observer should be notified after template loading");
	}

	@Test
	@DisplayName("Test template loading with invalid number")
	void testLoadTemplateWithInvalidNumber()
	{
		assertThrows(InvalidNumberException.class, () -> {
			JsonUpdater.loadTemplate(TEST_TEMPLATE, null);
		});
	}

	@Test
	@DisplayName("Test object-based loading methods")
	void testObjectBasedLoading() throws IOException
	{
		Noun noun = new Noun(TEST_NOUN, Number.SINGULAR);
		Verb verb = new Verb(TEST_VERB, Number.SINGULAR, Tense.PRESENT);
		Adjective adjective = new Adjective(TEST_ADJECTIVE);
		Template template = new Template(TEST_TEMPLATE, Number.SINGULAR);

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
	@DisplayName("Test observer management")
	void testObserverManagement()
	{
		JsonUpdateObserver tempObserver = new JsonUpdateObserver() {
			@Override
			public void onJsonUpdate() throws IOException {}
		};

		JsonUpdater.addObserver(tempObserver);
		JsonUpdater.removeObserver(tempObserver);

		assertDoesNotThrow(() -> {
			JsonUpdater.loadAdjective(TEST_ADJECTIVE);
		}, "Should not throw after removing observer");
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

		JsonUpdater.loadAdjective(TEST_ADJECTIVE);

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
			JsonUpdater.loadAdjective(TEST_ADJECTIVE);
		}, "Should propagate IOException from observer");

		JsonUpdater.removeObserver(failingObserver);
	}
}
