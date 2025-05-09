package unipd.nonsense.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import static org.junit.jupiter.api.Assertions.*;

class TestJsonElementIsNotPrimitiveException
{
	@Test
	@DisplayName("Test constructor with JsonElement")
	void testConstructor_JsonElement()
	{
		JsonObject element = new JsonObject();
		JsonElementIsNotPrimitiveException exception = new JsonElementIsNotPrimitiveException(element);

		assertEquals("The element found is not a primitive. Element: '{}'.", exception.getMessage());
		assertNull(exception.getCause());
		assertSame(element, exception.getNonPrimitiveElement());
	}

	@Test
	@DisplayName("Test constructor with JsonElement and custom message")
	void testConstructor_JsonElementAndMessage()
	{
		JsonObject element = new JsonObject();
		element.addProperty("key", "value");

		String customMessage = "Expected primitive but found object";
		JsonElementIsNotPrimitiveException exception = new JsonElementIsNotPrimitiveException(element, customMessage);

		assertEquals("Expected primitive but found object Element: '{\"key\":\"value\"}'.", exception.getMessage());
		assertNull(exception.getCause());
		assertSame(element, exception.getNonPrimitiveElement());
	}

	@Test
	@DisplayName("Test constructor with JsonElement and cause")
	void testConstructor_JsonElementAndCause()
	{
		JsonObject element = new JsonObject();
		IllegalArgumentException cause = new IllegalArgumentException("Invalid type");
		JsonElementIsNotPrimitiveException exception = new JsonElementIsNotPrimitiveException(element, cause);

		assertTrue(exception.getMessage().contains("The element found is not a primitive. Element: '{}'."));
		assertSame(cause, exception.getCause());
		assertSame(element, exception.getNonPrimitiveElement());
	}

	@Test
	@DisplayName("Test constructor with JsonElement, message and cause")
	void testConstructor_JsonElementMessageAndCause()
	{
		JsonObject element = new JsonObject();
		element.add("nested", new JsonObject());
		String customMessage = "Configuration requires primitive value";
		IllegalArgumentException cause = new IllegalArgumentException();
		JsonElementIsNotPrimitiveException exception = new JsonElementIsNotPrimitiveException(element, customMessage, cause);

		assertEquals("Configuration requires primitive value Element: '{\"nested\":{}}'.", exception.getMessage());
		assertSame(cause, exception.getCause());
		assertSame(element, exception.getNonPrimitiveElement());
	}

	@Test
	@DisplayName("Test exception type hierarchy")
	void testExceptionHierarchy()
	{
		JsonElementIsNotPrimitiveException exception = new JsonElementIsNotPrimitiveException(new JsonPrimitive("test"));
		assertTrue(exception instanceof IllegalArgumentException, "Should be subclass of IllegalArgumentException");
	}

	@Test
	@DisplayName("Test getNonPrimitiveElement with different Json types")
	void testGetNonPrimitiveElement()
	{
		JsonObject obj = new JsonObject();
		JsonElementIsNotPrimitiveException exception = new JsonElementIsNotPrimitiveException(obj);
		assertSame(obj, exception.getNonPrimitiveElement());
	}
}
