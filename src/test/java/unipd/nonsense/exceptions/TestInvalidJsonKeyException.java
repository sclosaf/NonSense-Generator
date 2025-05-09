package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TestInvalidJsonKeyException
{
	@Test
	@DisplayName("Test constructor with key")
	void testConstructor_Key()
	{
		String key = "nonexistent.field";
		InvalidJsonKeyException exception = new InvalidJsonKeyException(key);

		assertEquals("Used a json key: 'nonexistent.field' that was't part of the json file.", exception.getMessage());
		assertNull(exception.getCause());
		assertEquals(key, exception.getInvalidKey());
	}

	@Test
	@DisplayName("Test constructor with key and custom message")
	void testConstructor_KeyAndMessage()
	{
		String key = "user.missing_property";
		String customMessage = "Required key not found";
		InvalidJsonKeyException exception = new InvalidJsonKeyException(key, customMessage);

		assertEquals("Required key not found Key: 'user.missing_property'.", exception.getMessage());
		assertNull(exception.getCause());
		assertEquals(key, exception.getInvalidKey());
	}

	@Test
	@DisplayName("Test constructor with key and cause")
	void testConstructor_KeyAndCause()
	{
		String key = "invalid.path";
		IllegalArgumentException cause = new IllegalArgumentException("Bad key format");
		InvalidJsonKeyException exception = new InvalidJsonKeyException(key, cause);

		assertTrue(exception.getMessage().contains("Used a json key: 'invalid.path' that wasn't part of the json file."));
		assertSame(cause, exception.getCause());
		assertEquals(key, exception.getInvalidKey());
	}

	@Test
	@DisplayName("Test constructor with key, message and cause")
	void testConstructor_KeyMessageAndCause()
	{
		String key = "config.missing_setting";
		String customMessage = "Configuration error";
		IllegalArgumentException cause = new IllegalArgumentException("Key resolution failed");
		InvalidJsonKeyException exception = new InvalidJsonKeyException(key, customMessage, cause);

		assertEquals("Configuration error Key: 'config.missing_setting'.", exception.getMessage());
		assertSame(cause, exception.getCause());
		assertEquals(key, exception.getInvalidKey());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		InvalidJsonKeyException exception = new InvalidJsonKeyException("test");
		assertTrue(exception instanceof IllegalArgumentException, "Should be subclass of IllegalArgumentException");
	}

	@Test
	@DisplayName("Test getInvalidKey with different key formats")
	void testGetInvalidKey()
	{
		String simpleKey = "username";
		InvalidJsonKeyException exception1 = new InvalidJsonKeyException(simpleKey);
		assertEquals(simpleKey, exception1.getInvalidKey());

		String nestedKey = "user.address.zipcode";
		InvalidJsonKeyException exception2 = new InvalidJsonKeyException(nestedKey);
		assertEquals(nestedKey, exception2.getInvalidKey());

		String specialKey = "data@2023";
		InvalidJsonKeyException exception3 = new InvalidJsonKeyException(specialKey);
		assertEquals(specialKey, exception3.getInvalidKey());
	}

	@Test
	@DisplayName("Test message formatting with empty key")
	void testEmptyKey()
	{
		String emptyKey = "";
		InvalidJsonKeyException exception = new InvalidJsonKeyException(emptyKey);
		assertTrue(exception.getMessage().contains("Used a json key: ''"));
		assertEquals(emptyKey, exception.getInvalidKey());
	}
}
