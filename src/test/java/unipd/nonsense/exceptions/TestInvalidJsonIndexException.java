package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TestInvalidJsonIndexException
{
	@Test
	@DisplayName("Test constructor with index")
	void testConstructor_Index()
	{
		int index = 5;
		InvalidJsonIndexException exception = new InvalidJsonIndexException(index);

		assertEquals("Invalid index used to access json value. Value: '5'.", exception.getMessage());
		assertEquals(index, exception.getInvalidIndex());
	}

	@Test
	@DisplayName("Test constructor with index and custom message")
	void testConstructor_IndexAndMessage()
	{
		int index = -1;
		String customMessage = "Array index out of bounds";
		InvalidJsonIndexException exception = new InvalidJsonIndexException(index, customMessage);

		assertEquals("Array index out of bounds Value: '-1'.", exception.getMessage());
		assertEquals(index, exception.getInvalidIndex());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		InvalidJsonIndexException exception = new InvalidJsonIndexException(0);
		assertTrue(exception instanceof IndexOutOfBoundsException, "Should be subclass of IndexOutOfBoundsException");
	}

	@Test
	@DisplayName("Test getInvalidIndex with boundary values")
	void testGetInvalidIndex()
	{
		int minValue = Integer.MIN_VALUE;
		InvalidJsonIndexException exception1 = new InvalidJsonIndexException(minValue);
		assertEquals(minValue, exception1.getInvalidIndex());

		InvalidJsonIndexException exception2 = new InvalidJsonIndexException(0);
		assertEquals(0, exception2.getInvalidIndex());

		int maxValue = Integer.MAX_VALUE;
		InvalidJsonIndexException exception3 = new InvalidJsonIndexException(maxValue);
		assertEquals(maxValue, exception3.getInvalidIndex());
	}

	@Test
	@DisplayName("Test message formatting with extreme values")
	void testMessageFormatting()
	{
		int negativeIndex = -5;
		InvalidJsonIndexException exception1 = new InvalidJsonIndexException(negativeIndex);
		assertTrue(exception1.getMessage().contains("Value: '-5'"));

		int largeIndex = 1000000;
		InvalidJsonIndexException exception2 = new InvalidJsonIndexException(largeIndex);
		assertTrue(exception2.getMessage().contains("Value: '1000000'"));
	}
}
