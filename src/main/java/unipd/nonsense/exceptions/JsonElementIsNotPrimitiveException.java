package unipd.nonsense.exceptions;

import com.google.gson.JsonElement;

public final class JsonElementIsNotPrimitiveException extends IllegalArgumentException
{
	private JsonElement nonPrimitiveElement;

	public JsonElementIsNotPrimitiveException(JsonElement element)
	{
		super("The element found is not a primitive. Element: '" + element.toString() + "'");
		nonPrimitiveElement = element;
	}

	public JsonElementIsNotPrimitiveException(JsonElement element, String msg)
	{
		super(msg + " Element: '" + element.toString() + "'");
		nonPrimitiveElement = element;
	}

	public JsonElementIsNotPrimitiveException(JsonElement element, Throwable cause)
	{
		super("The element found is not a primitive. Element: '" + element.toString() + "'", cause);
		nonPrimitiveElement = element;
	}

	public JsonElementIsNotPrimitiveException(JsonElement element, String msg, Throwable cause)
	{
		super(msg + " Element: '" + element.toString() + "'", cause);
		nonPrimitiveElement = element;
	}

	public JsonElement getNonPrimitiveElement()
	{
		return nonPrimitiveElement;
	}
}
