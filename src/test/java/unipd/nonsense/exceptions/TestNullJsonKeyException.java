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

	@Test
	@DisplayName("Test with various JSON processing scenarios")
	void testWithJsonProcessingScenarios()
	{
		String parseMessage = "Null key in JSON parser";
		IllegalArgumentException parseCause = new IllegalArgumentException("Parser state invalid");
		NullJsonKeyException exception1 = new NullJsonKeyException(parseMessage, parseCause);
		assertEquals(parseMessage, exception1.getMessage());
		assertSame(parseCause, exception1.getCause());

		String configMessage = "Configuration key cannot be null";
		NullJsonKeyException exception2 = new NullJsonKeyException(configMessage);
		assertEquals(configMessage, exception2.getMessage());
		assertNull(exception2.getCause());

		String apiMessage = "API response contained null key";
		IllegalArgumentException apiCause = new IllegalArgumentException("response.key");
		NullJsonKeyException exception3 = new NullJsonKeyException(apiMessage, apiCause);
		assertEquals(apiMessage, exception3.getMessage());
		assertSame(apiCause, exception3.getCause());
	}
}
