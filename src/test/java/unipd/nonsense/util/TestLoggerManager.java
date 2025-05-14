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

class TestLoggerManager {

    private LoggerManager loggerManager;
    private TestAppender testAppender;

    static class TestAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<>();

        protected TestAppender(String name) {
            super(name, null, PatternLayout.createDefaultLayout(), false);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }

        public List<LogEvent> getEvents() {
            return events;
        }

        public void clear() {
            events.clear();
        }
    }

    @BeforeEach
    void setup() {
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
    void tearDown() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        var config = context.getConfiguration();

        config.getLoggerConfig("ConsoleLogger").removeAppender("TestAppender");
        config.getLoggerConfig("FileLogger").removeAppender("TestAppender");
        config.getAppenders().remove("TestAppender");

        context.updateLoggers();
        testAppender.stop();
    }

    @Test
    void testVerboseModeSwitch() {
        assertFalse(loggerManager.getVerbose());
        loggerManager.switchVerboseMode();
        assertTrue(loggerManager.getVerbose());
        loggerManager.switchVerboseMode();
        assertFalse(loggerManager.getVerbose());
    }

    @Test
    void testLogLevels() {
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
	void testLogWithExceptions() {
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

}
