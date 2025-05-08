package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TestInaccessibleFileException
{
	@Test
	@DisplayName("Test default constructor")
	void testConstructor_Default()
	{
		InaccessibleFileException exception = new InaccessibleFileException();
		assertEquals("Unable to open file.", exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with custom message")
	void testConstructor_Message()
	{
		String customMessage = "Cannot access configuration file";
		InaccessibleFileException exception = new InaccessibleFileException(customMessage);
		assertEquals(customMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with cause")
	void testConstructor_Cause()
	{
		IOException cause = new IOException("Permission denied");
		InaccessibleFileException exception = new InaccessibleFileException(cause);

		assertSame(cause, exception.getCause());
		assertTrue(exception.getMessage().contains(cause.toString()));
	}

	@Test
	@DisplayName("Test constructor with message and cause")
	void testConstructor_MessageAndCause()
	{
		String customMessage = "Failed to open log file";
		IOException cause = new IOException("Access restricted");
		InaccessibleFileException exception = new InaccessibleFileException(customMessage, cause);

		assertEquals(customMessage, exception.getMessage());
		assertSame(cause, exception.getCause());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		InaccessibleFileException exception = new InaccessibleFileException();
		assertTrue(exception instanceof IOException, "Should be subclass of IOException");
	}

	@Test
	@DisplayName("Test with various file access failure scenarios")
	void testWithFileAccessScenarios()
	{
		IOException notFoundCause = new IOException("file.txt");
		InaccessibleFileException exception1 = new InaccessibleFileException(notFoundCause);
		assertSame(notFoundCause, exception1.getCause());

		String accessMessage = "Insufficient permissions to open file";
		InaccessibleFileException exception2 = new InaccessibleFileException(accessMessage);
		assertEquals(accessMessage, exception2.getMessage());

		String lockedMessage = "File is locked by another process";
		IOException lockedCause = new IOException("The process cannot access the file");
		InaccessibleFileException exception3 = new InaccessibleFileException(lockedMessage, lockedCause);
		assertEquals(lockedMessage, exception3.getMessage());
		assertSame(lockedCause, exception3.getCause());
	}
}
