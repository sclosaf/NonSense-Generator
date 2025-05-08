package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TestJsonElementIsNotArrayException
{
	@Test
	@DisplayName("Test constructor with key")
	void testConstructor_Key()
	{
		String key = "config.items";
		JsonElementIsNotArrayException exception = new JsonElementIsNotArrayException(key);

		assertEquals("Used a json key: 'config.items' that didn't contained an array.", exception.getMessage());
		assertNull(exception.getCause());
		assertEquals(key, exception.getNotArrayKey());
	}

	@Test
	@DisplayName("Test constructor with key and custom message")
	void testConstructor_KeyAndMessage()
	{
		String key = "user.permissions";
		String customMessage = "Expected array but found object";
		JsonElementIsNotArrayException exception = new JsonElementIsNotArrayException(key, customMessage);

		assertEquals("Expected array but found object Key: 'user.permissions'.", exception.getMessage());
		assertNull(exception.getCause());
		assertEquals(key, exception.getNotArrayKey());
	}

	@Test
	@DisplayName("Test constructor with key and cause")
	void testConstructor_KeyAndCause()
	{
		String key = "data.entries";
		IllegalArgumentException cause = new IllegalArgumentException("Invalid JSON type");
		JsonElementIsNotArrayException exception = new JsonElementIsNotArrayException(key, cause);

		assertTrue(exception.getMessage().contains("Used a json key: 'data.entries' that didn't contained an array."));
		assertSame(cause, exception.getCause());
		assertEquals(key, exception.getNotArrayKey());
	}

	@Test
	@DisplayName("Test constructor with key, message and cause")
	void testConstructor_KeyMessageAndCause()
	{
		String key = "response.items";
		String customMessage = "API response format error";
		IllegalArgumentException cause = new IllegalArgumentException("Missing array");
		JsonElementIsNotArrayException exception = new JsonElementIsNotArrayException(key, customMessage, cause);

		assertEquals("API response format error Key: 'response.items'.", exception.getMessage());
		assertSame(cause, exception.getCause());
		assertEquals(key, exception.getNotArrayKey());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		JsonElementIsNotArrayException exception = new JsonElementIsNotArrayException("test");
		assertTrue(exception instanceof IllegalArgumentException, "Should be subclass of IllegalArgumentException");
	}

	@Test
	@DisplayName("Test getNotArrayKey with different key formats")
	void testGetNotArrayKey()
	{
		String simpleKey = "items";
		JsonElementIsNotArrayException exception = new JsonElementIsNotArrayException(simpleKey);
		assertEquals(simpleKey, exception.getNotArrayKey());
	}
}
