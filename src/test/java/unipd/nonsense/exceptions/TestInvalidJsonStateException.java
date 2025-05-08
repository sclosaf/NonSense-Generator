package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TestInvalidJsonStateException
{
	@Test
	@DisplayName("Test default constructor")
	void testConstructor_Default()
	{
		InvalidJsonStateException exception = new InvalidJsonStateException();
		assertEquals("Json file is in a invalid state.", exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with custom message")
	void testConstructor_Message()
	{
		String customMessage = "JSON structure is malformed";
		InvalidJsonStateException exception = new InvalidJsonStateException(customMessage);
		assertEquals(customMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with cause")
	void testConstructor_Cause()
	{
		IllegalStateException cause = new IllegalStateException("Unexpected JSON format");
		InvalidJsonStateException exception = new InvalidJsonStateException(cause);

		assertSame(cause, exception.getCause());
		assertTrue(exception.getMessage().contains(cause.toString()));
	}

	@Test
	@DisplayName("Test constructor with message and cause")
	void testConstructor_MessageAndCause()
	{
		String customMessage = "Invalid JSON configuration";
		IllegalStateException cause = new IllegalStateException("Missing required field");
		InvalidJsonStateException exception = new InvalidJsonStateException(customMessage, cause);

		assertEquals(customMessage, exception.getMessage());
		assertSame(cause, exception.getCause());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		InvalidJsonStateException exception = new InvalidJsonStateException();
		assertTrue(exception instanceof IllegalStateException, "Should be subclass of IllegalStateException");
	}

	@Test
	@DisplayName("Test with various JSON-related causes")
	void testWithJsonRelatedCauses()
	{
		IllegalStateException jsonParseException = new IllegalStateException("Unexpected token '}'");
		InvalidJsonStateException exception1 = new InvalidJsonStateException(jsonParseException);
		assertSame(jsonParseException, exception1.getCause());

		String validationMessage = "Missing required fields: name, id";
		InvalidJsonStateException exception2 = new InvalidJsonStateException(validationMessage);
		assertEquals(validationMessage, exception2.getMessage());
	}
}
