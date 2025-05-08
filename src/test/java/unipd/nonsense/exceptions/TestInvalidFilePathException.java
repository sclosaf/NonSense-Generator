package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TestInvalidFilePathException
{
	@Test
	@DisplayName("Test constructor with path")
	void testConstructor_Path()
	{
		String path = "/invalid/path/to/file";
		InvalidFilePathException exception = new InvalidFilePathException(path);

		assertEquals("Invalid path used. Path: '/invalid/path/to/file'.", exception.getMessage());
		assertNull(exception.getCause());
		assertEquals(path, exception.getInvalidPath());
	}

	@Test
	@DisplayName("Test constructor with path and custom message")
	void testConstructor_PathAndMessage()
	{
		String path = "C:\\nonexistent\\file.txt";
		String customMessage = "File not found at specified location";
		InvalidFilePathException exception = new InvalidFilePathException(path, customMessage);

		assertEquals("File not found at specified location Path: 'C:\\nonexistent\\file.txt'.", exception.getMessage());
		assertNull(exception.getCause());
		assertEquals(path, exception.getInvalidPath());
	}

	@Test
	@DisplayName("Test constructor with path and cause")
	void testConstructor_PathAndCause()
	{
		String path = "~/missing/directory/";
		IllegalArgumentException cause = new IllegalArgumentException("Invalid characters in path");
		InvalidFilePathException exception = new InvalidFilePathException(path, cause);

		assertTrue(exception.getMessage().contains("Invalid path used. Path: '~/missing/directory/'."));
		assertSame(cause, exception.getCause());
		assertEquals(path, exception.getInvalidPath());
	}


	@Test
	@DisplayName("Test constructor with path, message and cause")
	void testConstructor_PathMessageAndCause()
	{
		String path = "relative/path/without/root";
		String customMessage = "Absolute path required";
		IllegalArgumentException cause = new IllegalArgumentException("Access denied");
		InvalidFilePathException exception = new InvalidFilePathException(path, customMessage, cause);

		assertEquals("Absolute path required Path: 'relative/path/without/root'.", exception.getMessage());
		assertSame(cause, exception.getCause());
		assertEquals(path, exception.getInvalidPath());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		InvalidFilePathException exception = new InvalidFilePathException("test");
		assertTrue(exception instanceof IllegalArgumentException, "Should be subclass of IllegalArgumentException");
	}

	@Test
	@DisplayName("Test getInvalidPath with different path formats")
	void testGetInvalidPath()
	{
		String unixPath = "/home/user/invalid.file";
		InvalidFilePathException exception1 = new InvalidFilePathException(unixPath);
		assertEquals(unixPath, exception1.getInvalidPath());

		String windowsPath = "C:\\Program Files\\invalid\\path.exe";
		InvalidFilePathException exception2 = new InvalidFilePathException(windowsPath);
		assertEquals(windowsPath, exception2.getInvalidPath());


		String relativePath = "../parent/../invalid.file";
		InvalidFilePathException exception3 = new InvalidFilePathException(relativePath);
		assertEquals(relativePath, exception3.getInvalidPath());
	}

	@Test
	@DisplayName("Test empty path handling")
	void testEmptyPath()
	{
		String emptyPath = "";
		InvalidFilePathException exception = new InvalidFilePathException(emptyPath);
		assertTrue(exception.getMessage().contains("Path: ''"));
		assertEquals(emptyPath, exception.getInvalidPath());
	}
}
