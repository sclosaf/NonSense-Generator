package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestNullJsonKeyException
{
	@Test
	@DisplayName("Test default constructor")
	void testConstructor_Default()
	{
		NullJsonKeyException exception = new NullJsonKeyException();
		assertEquals("Invalid null key argument.", exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with custom message")
	void testConstructor_Message()
	{
		String customMessage = "JSON key cannot be null in this context";
		NullJsonKeyException exception = new NullJsonKeyException(customMessage);
		assertEquals(customMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with cause")
	void testConstructor_Cause()
	{
		IllegalArgumentException rootCause = new IllegalArgumentException("Invalid argument");
		NullJsonKeyException exception = new NullJsonKeyException(rootCause);

		assertSame(rootCause, exception.getCause());
		assertTrue(exception.getMessage().contains(rootCause.toString()));
	}

	@Test
	@DisplayName("Test constructor with message and cause")
	void testConstructor_MessageAndCause()
	{
		String customMessage = "Null key detected during JSON processing";
		IllegalArgumentException rootCause = new IllegalArgumentException("Unexpected null");
		NullJsonKeyException exception = new NullJsonKeyException(customMessage, rootCause);

		assertEquals(customMessage, exception.getMessage());
		assertSame(rootCause, exception.getCause());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		NullJsonKeyException exception = new NullJsonKeyException();
		assertTrue(exception instanceof IllegalArgumentException, "Should be subclass of IllegalArgumentException");
	}
}
