package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TestClientNonExistentException
{
	@Test
	@DisplayName("Test default constructor")
	void testConstructor_Default()
	{
		ClientNonExistentException exception = new ClientNonExistentException();
		assertEquals("Client not found.", exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with custom message")
	void testConstructor_Message()
	{
		String customMessage = "Client does not exist";
		ClientNonExistentException exception = new ClientNonExistentException(customMessage);
		assertEquals(customMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with cause")
	void testConstructor_Cause()
	{
		IllegalStateException rootCause = new IllegalStateException("Client registry empty");
		ClientNonExistentException exception = new ClientNonExistentException(rootCause);
		assertSame(rootCause, exception.getCause());
		assertTrue(exception.getMessage().contains(rootCause.toString()));
	}

	@Test
	@DisplayName("Test constructor with message and cause")
	void testConstructor_MessageAndCause()
	{
		String customMessage = "Failed to locate client";
		IllegalStateException rootCause = new IllegalStateException("Client record missing");
		ClientNonExistentException exception = new ClientNonExistentException(customMessage, rootCause);

		assertEquals(customMessage, exception.getMessage());
		assertSame(rootCause, exception.getCause());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		ClientNonExistentException exception = new ClientNonExistentException();
		assertTrue(exception instanceof IllegalStateException, "Should be subclass of IllegalStateException");
	}
}
