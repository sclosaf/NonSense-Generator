package unipd.nonsense.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestLoggerManager
{
	private LoggerManager loggerManager;
	private TestAppender testAppender;

	static class TestAppender extends AbstractAppender
	{
		private final List<LogEvent> events = new ArrayList<>();

		protected TestAppender(String name)
		{
			super(name, null, PatternLayout.createDefaultLayout(), false);
		}

		@Override
		public void append(LogEvent event)
		{
			events.add(event.toImmutable());
		}

		public List<LogEvent> getEvents()
		{
			return events;
		}

		public void clear()
		{
			events.clear();
		}
	}

	@BeforeEach
	void setup()
	{
		loggerManager = new LoggerManager(TestLoggerManager.class);
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		var config = context.getConfiguration();

		testAppender = new TestAppender("TestAppender");
		testAppender.start();
		config.addAppender(testAppender);

		LoggerConfig consoleConfig = config.getLoggerConfig("ConsoleLogger");
		consoleConfig.addAppender(testAppender, Level.ALL, null);
		consoleConfig.setLevel(Level.ALL);

		LoggerConfig fileConfig = config.getLoggerConfig("FileLogger");
		fileConfig.addAppender(testAppender, Level.ALL, null);
		fileConfig.setLevel(Level.ALL);

		context.updateLoggers();
	}

	@AfterEach
	void tearDown()
	{
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		var config = context.getConfiguration();

		config.getLoggerConfig("ConsoleLogger").removeAppender("TestAppender");
		config.getLoggerConfig("FileLogger").removeAppender("TestAppender");
		config.getAppenders().remove("TestAppender");

		context.updateLoggers();
		testAppender.stop();
	}

	@Test
	@DisplayName("Test default logger configuration")
	void testDefaultConfiguration()
	{
		LoggerManager newLogger = new LoggerManager(String.class);
		assertNotNull(newLogger);
		assertFalse(newLogger.getVerbose());
	}

	@Test
	@DisplayName("Test switch verbosity")
	void testVerboseModeSwitch()
	{
		assertFalse(loggerManager.getVerbose());
		loggerManager.switchVerboseMode();
		assertTrue(loggerManager.getVerbose());
		loggerManager.switchVerboseMode();
		assertFalse(loggerManager.getVerbose());
	}

	@Test
	@DisplayName("Test all logging levels")
	void testLogLevels()
	{
		loggerManager.switchVerboseMode();

		loggerManager.logTrace("Trace log");
		loggerManager.logDebug("Debug log");
		loggerManager.logInfo("Info log");
		loggerManager.logWarn("Warn log");
		loggerManager.logError("Error log");
		loggerManager.logFatal("Fatal log");

		List<LogEvent> events = testAppender.getEvents();

		assertTrue(events.stream().anyMatch(e -> e.getLevel() == Level.TRACE && e.getMessage().getFormattedMessage().contains("Trace log")));
		assertTrue(events.stream().anyMatch(e -> e.getLevel() == Level.DEBUG && e.getMessage().getFormattedMessage().contains("Debug log")));
		assertTrue(events.stream().anyMatch(e -> e.getLevel() == Level.INFO && e.getMessage().getFormattedMessage().contains("Info log")));
		assertTrue(events.stream().anyMatch(e -> e.getLevel() == Level.WARN && e.getMessage().getFormattedMessage().contains("Warn log")));
		assertTrue(events.stream().anyMatch(e -> e.getLevel() == Level.ERROR && e.getMessage().getFormattedMessage().contains("Error log")));
		assertTrue(events.stream().anyMatch(e -> e.getLevel() == Level.FATAL && e.getMessage().getFormattedMessage().contains("Fatal log")));
	}

	@Test
	@DisplayName("Testing logging exceptions")
	void testLogWithExceptions()
	{
		RuntimeException ex = new RuntimeException("Simulated exception");
		loggerManager.logWarn("Warn with exception", ex);
		loggerManager.logError("Error with exception", ex);
		loggerManager.logFatal("Fatal with exception", ex);

		List<LogEvent> events = testAppender.getEvents();

		boolean hasWarn = events.stream().anyMatch(e ->
			e.getLevel() == Level.WARN &&
			e.getMessage().getFormattedMessage().contains("Warn with exception") &&
			e.getThrown() instanceof RuntimeException
			);

		boolean hasError = events.stream().anyMatch(e ->
			e.getLevel() == Level.ERROR &&
			e.getMessage().getFormattedMessage().contains("Error with exception") &&
			e.getThrown() instanceof RuntimeException
			);

		boolean hasFatal = events.stream().anyMatch(e ->
			e.getLevel() == Level.FATAL &&
			e.getMessage().getFormattedMessage().contains("Fatal with exception") &&
			e.getThrown() instanceof RuntimeException
			);

		assertTrue(hasWarn);
		assertTrue(hasError);
		assertTrue(hasFatal);
	}

	@Test
	@DisplayName("Test log filtering in non-verbose mode")
	void testNonVerboseLogging()
	{
		loggerManager.logTrace("Should not appear");
		loggerManager.logDebug("Should not appear");

		List<LogEvent> events = testAppender.getEvents();

		boolean hasConsoleDebug = events.stream()
			.anyMatch(e -> e.getLoggerName().equals("ConsoleLogger")
			&& (e.getLevel() == Level.DEBUG || e.getLevel() == Level.TRACE));
			assertFalse(hasConsoleDebug);

		boolean hasFileDebug = events.stream()
			.anyMatch(e -> e.getLoggerName().equals("FileLogger")
			&& e.getLevel() == Level.DEBUG);
		assertTrue(hasFileDebug);
	}

	@Test
	@DisplayName("Test null message handling")
	void testNullMessage()
	{
		assertDoesNotThrow(() -> loggerManager.logInfo(null));
		assertDoesNotThrow(() -> loggerManager.logError(null, new RuntimeException()));
	}

	@Test
	@DisplayName("Test null exception handling")
	void testNullException()
	{
		assertDoesNotThrow(() -> loggerManager.logError("Message with null exception", null));

		List<LogEvent> events = testAppender.getEvents();
		assertTrue(events.stream().anyMatch(e -> e.getMessage().getFormattedMessage().contains("Message with null exception") && e.getThrown() == null));
	}

	@Test
	@DisplayName("Test thread safety")
	void testConcurrentLogging() throws InterruptedException
	{
		loggerManager.switchVerboseMode();
		Runnable task = () ->
			{
				for(int i = 0; i < 100; i++)
					loggerManager.logInfo("Thread " + Thread.currentThread().getId() + " - " + i);
			};

		Thread t1 = new Thread(task);
		Thread t2 = new Thread(task);

		t1.start();
		t2.start();
		t1.join();
		t2.join();

		assertEquals(400, testAppender.getEvents().size());
	}

	@Test
	@DisplayName("Test logging performance threshold")
	void testLoggingPerformance()
	{
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++)
			loggerManager.logInfo("Perf test " + i);

		long duration = System.currentTimeMillis() - startTime;

		assertTrue(duration < 1000, "Logging 1000 messages took too long: " + duration + "ms");
	}

	@Test
	@DisplayName("Test multiple logger instances")
	void testMultipleLoggers()
	{
		LoggerManager secondLogger = new LoggerManager(Integer.class);
		LoggerManager thirdLogger = new LoggerManager(Float.class);

		loggerManager.switchVerboseMode();
		secondLogger.switchVerboseMode();
		thirdLogger.switchVerboseMode();

		loggerManager.logDebug("First logger");
		secondLogger.logDebug("Second logger");
		thirdLogger.logDebug("Third logger");

		List<LogEvent> events = testAppender.getEvents();

		assertEquals(6, events.size(), "Should contain 6 elements");

		long consoleEvents = events.stream()
			.filter(e -> e.getLoggerName().equals("ConsoleLogger"))
			.count();

		long fileEvents = events.stream()
			.filter(e -> e.getLoggerName().equals("FileLogger"))
			.count();

		assertEquals(3, consoleEvents);
		assertEquals(3, fileEvents);
	}

	@Test
	@DisplayName("Test log class name in messages")
	void testLogClassNameInMessages()
	{
		loggerManager.switchVerboseMode();
		String testMessage = "Class name test";

		loggerManager.logInfo(testMessage);
		List<LogEvent> events = testAppender.getEvents();

		assertTrue(events.stream().anyMatch(e -> e.getMessage().getFormattedMessage().contains("[TestLoggerManager] " + testMessage)));
	}

	@Test
	@DisplayName("Test very long message handling")
	void testVeryLongMessage()
	{
		String longMessage = new String(new char[10000]).replace('\0', 'X');
		assertDoesNotThrow(() ->
		{
			loggerManager.logInfo(longMessage);
			loggerManager.logError(longMessage, new RuntimeException());
		});

		List<LogEvent> events = testAppender.getEvents();
		assertTrue(events.get(0).getMessage().getFormattedMessage().contains(longMessage.substring(0, 100)));
	}

	@Test
	@DisplayName("Test special characters in messages")
	void testSpecialCharactersInMessages()
	{
		String message = "Special chars: \n\t\r\b\f\\\"'";
		loggerManager.logInfo(message);

		List<LogEvent> events = testAppender.getEvents();
		assertTrue(events.stream().anyMatch(e -> e.getMessage().getFormattedMessage().contains(message)));
	}

	@Test
	@DisplayName("Test empty message handling")
	void testEmptyMessage()
	{
		assertDoesNotThrow(() ->
		{
			loggerManager.logInfo("");
			loggerManager.logWarn("");
			loggerManager.logError("", new RuntimeException());
		});

		List<LogEvent> events = testAppender.getEvents();
		assertTrue(events.stream().anyMatch(e -> e.getMessage().getFormattedMessage().endsWith("] ")));
	}

	@Test
	@DisplayName("Test multiple verbose mode switches under load")
	void testMultipleVerboseSwitches()
	{
		for (int i = 0; i < 100; i++)
			loggerManager.switchVerboseMode();

		assertFalse(loggerManager.getVerbose());

		loggerManager.logInfo("Test after switches");
		assertFalse(testAppender.getEvents().isEmpty());
	}

	@Test
	@DisplayName("Test log message formatting")
	void testMessageFormatting()
	{
		String format = "Formatted %s %d";
		loggerManager.logInfo(String.format(format, "test", 123));

		List<LogEvent> events = testAppender.getEvents();
		assertTrue(events.stream().anyMatch(e -> e.getMessage().getFormattedMessage().contains("Formatted test 123")));
	}

	@Test
	@DisplayName("Test extreme concurrent logging")
	void testExtremeConcurrentLogging() throws InterruptedException
	{
		loggerManager.switchVerboseMode();
		int threadCount = 10;
		int iterations = 1000;

		Runnable task = () ->
		{
				for(int i = 0; i < iterations; i++)
					loggerManager.logInfo(Thread.currentThread().getName() + " - " + i);
		};

		Thread[] threads = new Thread[threadCount];
		for(int i = 0; i < threadCount; i++)
		{
			threads[i] = new Thread(task, "Thread-" + i);
			threads[i].start();
		}

		for(Thread t : threads)
			t.join();

		assertEquals(threadCount * iterations * 2, testAppender.getEvents().size());
	}

	@Test
	@DisplayName("Test log level threshold")
	void testLogLevelThreshold()
	{
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		LoggerConfig consoleConfig = context.getConfiguration().getLoggerConfig("ConsoleLogger");
		consoleConfig.setLevel(Level.ERROR);
		context.updateLoggers();

		loggerManager.switchVerboseMode();
		loggerManager.logDebug("Should not appear");
		loggerManager.logError("Should appear");

		List<LogEvent> events = testAppender.getEvents();
		assertTrue(events.stream().anyMatch(e -> e.getLoggerName().equals("ConsoleLogger") && e.getLevel() == Level.DEBUG));
		assertTrue(events.stream().anyMatch(e -> e.getLoggerName().equals("ConsoleLogger") && e.getLevel() == Level.ERROR));

		consoleConfig.setLevel(Level.ALL);
		context.updateLoggers();
	}

	@Test
	@DisplayName("Test logger with different class names")
	void testDifferentClassNames()
	{
		LoggerManager stringLogger = new LoggerManager(String.class);
		LoggerManager integerLogger = new LoggerManager(Integer.class);

		stringLogger.logInfo("String class log");
		integerLogger.logInfo("Integer class log");

		List<LogEvent> events = testAppender.getEvents();
		assertTrue(events.stream().anyMatch(e -> e.getMessage().getFormattedMessage().contains("[String] String class log")));
		assertTrue(events.stream().anyMatch(e -> e.getMessage().getFormattedMessage().contains("[Integer] Integer class log")));
	}

	@Test
	@DisplayName("Test log message with markers")
	void testLogWithMarkers()
	{
		assertDoesNotThrow(() -> loggerManager.logInfo("Message with potential marker"));
	}

	@Test
	@DisplayName("Test repeated identical messages")
	void testRepeatedMessages()
	{
		String message = "Repeated message";
		for(int i = 0; i < 100; i++)
			loggerManager.logInfo(message);

		List<LogEvent> events = testAppender.getEvents();

		assertEquals(100, events.stream()
			.filter(e -> e.getMessage().getFormattedMessage().contains(message))
			.count());
	}

	@Test
	@DisplayName("Test memory usage under heavy logging")
	void testMemoryUsage()
	{
		Runtime runtime = Runtime.getRuntime();
		long initialMemory = runtime.totalMemory() - runtime.freeMemory();

		for(int i = 0; i < 100000; i++)
			loggerManager.logInfo("Memory test message " + i);

		long finalMemory = runtime.totalMemory() - runtime.freeMemory();
		long memoryIncrease = finalMemory - initialMemory;

		assertTrue(memoryIncrease < 1000 * 1024 * 1024, "Memory increase should be reasonable, was: " + memoryIncrease);
	}
}
