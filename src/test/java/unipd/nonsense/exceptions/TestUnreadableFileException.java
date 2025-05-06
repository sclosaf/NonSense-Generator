package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TestUnreadableFileException
{

	@Test
	@DisplayName("Test default constructor")
	void testConstructor_Default()
	{
		UnreadableFileException exception = new UnreadableFileException();
		assertEquals("Unable to read file.", exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with custom message")
	void testConstructor_Message()
	{
		String customMessage = "Failed to read config file";
		UnreadableFileException exception = new UnreadableFileException(customMessage);
		assertEquals(customMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with cause")
	void testConstructor_Cause()
	{
		IOException rootCause = new IOException("File not found");
		UnreadableFileException exception = new UnreadableFileException(rootCause);

		assertSame(rootCause, exception.getCause());
		assertTrue(exception.getMessage().contains(rootCause.toString()));
	}

	@Test
	@DisplayName("Test constructor with message and cause")
	void testConstructor_MessageAndCause()
	{
		String customMessage = "Failed to read user data";
		IOException rootCause = new IOException("Permission denied");
		UnreadableFileException exception = new UnreadableFileException(customMessage, rootCause);

		assertEquals(customMessage, exception.getMessage());
		assertSame(rootCause, exception.getCause());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		UnreadableFileException exception = new UnreadableFileException();
		assertTrue(exception instanceof IOException, "Should be subclass of IOException");
	}

	@Test
	@DisplayName("Test with various read failure scenarios")
	void testWithReadFailureScenarios()
	{
		IOException missingFileCause = new IOException("missing.txt");
		UnreadableFileException exception1 = new UnreadableFileException(missingFileCause);
		assertSame(missingFileCause, exception1.getCause());

		String corruptMessage = "File appears to be corrupted";
		IOException corruptCause = new IOException("Invalid file format");
		UnreadableFileException exception2 = new UnreadableFileException(corruptMessage, corruptCause);
		assertEquals(corruptMessage, exception2.getMessage());
		assertSame(corruptCause, exception2.getCause());

		String lockedMessage = "File is locked by another process";
		IOException lockedCause = new IOException("The process cannot access the file");
		UnreadableFileException exception3 = new UnreadableFileException(lockedMessage, lockedCause);
		assertEquals(lockedMessage, exception3.getMessage());
		assertSame(lockedCause, exception3.getCause());
	}
}
