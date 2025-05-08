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

	@Test
	@DisplayName("Test with various write failure scenarios")
	void testWithWriteFailureScenarios()
	{
		IOException diskFullCause = new IOException("No space left on device");
		UnwritableFileException exception1 = new UnwritableFileException(diskFullCause);
		assertSame(diskFullCause, exception1.getCause());

		String accessMessage = "Cannot write to protected file";
		IOException accessCause = new IOException("protected.txt");
		UnwritableFileException exception2 = new UnwritableFileException(accessMessage, accessCause);
		assertEquals(accessMessage, exception2.getMessage());
		assertSame(accessCause, exception2.getCause());

		String fsMessage = "Filesystem error during write";
		IOException fsCause = new IOException("Read-only filesystem");
		UnwritableFileException exception3 = new UnwritableFileException(fsMessage, fsCause);
		assertEquals(fsMessage, exception3.getMessage());
		assertSame(fsCause, exception3.getCause());
	}
}
