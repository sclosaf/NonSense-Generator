package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TestClientAlreadyClosedException
{
	@Test
	@DisplayName("Test default constructor")
	void testConstructor_Default()
	{
		ClientAlreadyClosedException exception = new ClientAlreadyClosedException();
		assertEquals("Client is already closed.", exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with custom message")
	void testConstructor_Message()
	{
		String customMessage = "Client connection 54321 already closed";
		ClientAlreadyClosedException exception = new ClientAlreadyClosedException(customMessage);
		assertEquals(customMessage, exception.getMessage());
		assertNull(exception.getCause());
	}

	@Test
	@DisplayName("Test constructor with cause")
	void testConstructor_Cause()
	{
		IllegalStateException rootCause = new IllegalStateException("Connection terminated");
		ClientAlreadyClosedException exception = new ClientAlreadyClosedException(rootCause);
		assertSame(rootCause, exception.getCause());
		assertTrue(exception.getMessage().contains(rootCause.toString()));
	}

	@Test
	@DisplayName("Test constructor with message and cause")
	void testConstructor_MessageAndCause()
	{
		String customMessage = "Cannot perform operation on closed client";
		IllegalStateException rootCause = new IllegalStateException("Socket closed");
		ClientAlreadyClosedException exception = new ClientAlreadyClosedException(customMessage, rootCause);
		assertEquals(customMessage, exception.getMessage());
		assertSame(rootCause, exception.getCause());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		ClientAlreadyClosedException exception = new ClientAlreadyClosedException();
		assertTrue(exception instanceof IllegalStateException, "Should be subclass of IllegalStateException");
	}
}
