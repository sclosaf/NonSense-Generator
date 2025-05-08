package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

class TestFailedOpeningInputStreamException
{

	@Test
	@DisplayName("Test default constructor")
	void testConstructor_Default()
	{
		FailedOpeningInputStreamException exception = new FailedOpeningInputStreamException();
		assertEquals("Failed to open input stream.", exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with custom message")
	void testConstructor_Message()
	{
		String customMessage = "Cannot open input stream for configuration";
		FailedOpeningInputStreamException exception = new FailedOpeningInputStreamException(customMessage);
		assertEquals(customMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with cause")
	void testConstructor_Cause()
	{
		IOException cause = new IOException("Resource unavailable");
		FailedOpeningInputStreamException exception = new FailedOpeningInputStreamException(cause);

		assertSame(cause, exception.getCause());
		assertTrue(exception.getMessage().contains(cause.toString()));
	}

	@Test
	@DisplayName("Test constructor with message and cause")
	void testConstructor_MessageAndCause()
	{
		String customMessage = "Failed to create input stream for user data";
		IOException cause = new IOException("data.bin");
		FailedOpeningInputStreamException exception = new FailedOpeningInputStreamException(customMessage, cause);

		assertEquals(customMessage, exception.getMessage());
		assertSame(cause, exception.getCause());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		FailedOpeningInputStreamException exception = new FailedOpeningInputStreamException();
		assertTrue(exception instanceof IOException, "Should be subclass of IOException");
	}
}
