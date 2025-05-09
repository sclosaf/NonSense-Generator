package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestNullFilePathException
{
	@Test
	@DisplayName("Test default constructor")
	void testConstructor_Default()
	{
		NullFilePathException exception = new NullFilePathException();
		assertEquals("Invalid null file path argument.", exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with custom message")
	void testConstructor_Message()
	{
		String customMessage = "File path cannot be null for this operation";
		NullFilePathException exception = new NullFilePathException(customMessage);
		assertEquals(customMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with cause")
	void testConstructor_Cause()
	{
		IllegalArgumentException rootCause = new IllegalArgumentException("Invalid path");
		NullFilePathException exception = new NullFilePathException(rootCause);

		assertSame(rootCause, exception.getCause());
		assertTrue(exception.getMessage().contains(rootCause.toString()));
	}

	@Test
	@DisplayName("Test constructor with message and cause")
	void testConstructor_MessageAndCause()
	{
		String customMessage = "Null path detected during file operation";
		IllegalArgumentException rootCause = new IllegalArgumentException("Path is null");
		NullFilePathException exception = new NullFilePathException(customMessage, rootCause);

		assertEquals(customMessage, exception.getMessage());
		assertSame(rootCause, exception.getCause());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		NullFilePathException exception = new NullFilePathException();
		assertTrue(exception instanceof IllegalArgumentException, "Should be subclass of IllegalArgumentException");
	}
}
