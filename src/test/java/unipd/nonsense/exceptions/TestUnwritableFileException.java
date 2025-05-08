package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TestUnwritableFileException
{
	@Test
	@DisplayName("Test default constructor")
	void tetsConstructor_Default()
	{
		UnwritableFileException exception = new UnwritableFileException();
		assertEquals("Unable to write file.", exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with custom message")
	void testConstructor_Message()
	{
		String customMessage = "Failed to write to config file";
		UnwritableFileException exception = new UnwritableFileException(customMessage);
		assertEquals(customMessage, exception.getMessage());
		assertNull(exception.getCause());

	}

	@Test
	@DisplayName("Test constructor with cause")
	void testConstructor_Cause()
	{
		IOException rootCause = new IOException("Disk full");
		UnwritableFileException exception = new UnwritableFileException(rootCause);

		assertSame(rootCause, exception.getCause());
		assertTrue(exception.getMessage().contains(rootCause.toString()));
    }


	@Test
	@DisplayName("Test constructor with message and cause")
	void testConstructor_MessageAndCause()
	{
		String customMessage = "Failed to write user data";
		IOException rootCause = new IOException("Permission denied");
		UnwritableFileException exception = new UnwritableFileException(customMessage, rootCause);

		assertEquals(customMessage, exception.getMessage());
		assertSame(rootCause, exception.getCause());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		UnwritableFileException exception = new UnwritableFileException();
		assertTrue(exception instanceof IOException, "Should be subclass of IOException");
	}
}
