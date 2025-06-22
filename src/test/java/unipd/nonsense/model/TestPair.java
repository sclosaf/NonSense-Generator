package unipd.nonsense.model;

import unipd.nonsense.model.Number;
import unipd.nonsense.model.Tense;
import java.util.Objects;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testing Pair with Enums")
class TestPair
{
	private Pair<Number, Tense> pairEnum;

	@BeforeEach
	void setup()
	{
		pairEnum = new Pair<>(Number.SINGULAR, Tense.PRESENT);
	}

	@Test
	@DisplayName("Test default construction")
	void testDefaultCostruction()
	{
		Pair<String, Integer> pair = new Pair<>("test", 10);
		assertEquals(pair.getFirst(), "test");
		assertEquals(pair.getSecond(), 10);
	}

	@Test
	@DisplayName("Test construction with null first element")
	void testConstructionWithNullFirst()
	{
		Pair<String, Integer> pair = new Pair<>(null, 42);
		assertNull(pair.getFirst());
		assertEquals(42, pair.getSecond());
	}

	@Test
	@DisplayName("Test construction with null second element")
	void testConstructionWithNullSecond()
	{
		Pair<String, Integer> pair = new Pair<>("test", null);
		assertEquals("test", pair.getFirst());
		assertNull(pair.getSecond());
	}

	@Test
	@DisplayName("Test getFirstString with null first element")
	void testGetFirstStringWithNull()
	{
		Pair<String, Integer> pair = new Pair<>(null, 42);
		assertEquals("null", pair.getFirstString());
	}

	@Test
	@DisplayName("Test getSecondString with null second element")
	void testGetSecondStringWithNull()
	{
		Pair<String, Integer> pair = new Pair<>("test", null);
		assertEquals("null", pair.getSecondString());
	}

	@Test
	@DisplayName("Test equals with both null elements")
	void testEqualsWithBothNulls()
	{
		Pair<String, Integer> pair1 = new Pair<>(null, null);
		Pair<String, Integer> pair2 = new Pair<>(null, null);
		assertTrue(pair1.equals(pair2));
	}

	@Test
	@DisplayName("Test equals with mixed null elements")
	void testEqualsWithMixedNulls()
	{
		Pair<String, Integer> pair1 = new Pair<>("test", null);
		Pair<String, Integer> pair2 = new Pair<>(null, 42);
		assertFalse(pair1.equals(pair2));
	}

	@Test
	@DisplayName("Test hashCode with null elements")
	void testHashCodeWithNulls()
	{
		Pair<String, Integer> pair = new Pair<>(null, null);
		assertEquals(Objects.hash(null, null), pair.hashCode());
	}

	@Test
	@DisplayName("Test equals with different class")
	void testEqualsWithDifferentClass()
	{
		Object other = new Object();
		assertFalse(pairEnum.equals(other));
	}

	@Test
	@DisplayName("Test toString representation")
	void testToString()
	{
		String result = pairEnum.toString();
		assertTrue(result.contains("SINGULAR"));
		assertTrue(result.contains("PRESENT"));
	}

	@Test
	@DisplayName("Test with custom objects")
	void testWithCustomObjects()
	{
		class CustomObj
		{
			final int value;

			CustomObj(int value)
			{
				this.value = value;
			}

			@Override
			public boolean equals(Object o)
			{
				if(this == o)
					return true;

				if(!(o instanceof CustomObj))
						return false;

				CustomObj that = (CustomObj) o;


				return value == that.value;
			}

			@Override
			public int hashCode()
			{
					return value;
			}
		}

		CustomObj obj1 = new CustomObj(1);
		CustomObj obj2 = new CustomObj(2);
		Pair<CustomObj, CustomObj> pair1 = new Pair<>(obj1, obj2);
		Pair<CustomObj, CustomObj> pair2 = new Pair<>(obj1, obj2);

		assertTrue(pair1.equals(pair2));
		assertEquals(pair1.hashCode(), pair2.hashCode());
	}

	@Test
	@DisplayName("Test valid construction with enums")
	void testValidConstructionWithEnums()
	{
		assertNotNull(pairEnum);
		assertEquals(Number.SINGULAR, pairEnum.getFirst());
		assertEquals(Tense.PRESENT, pairEnum.getSecond());
	}

	@Test
	@DisplayName("Test getFirstString with enum")
	void testGetFirstStringWithEnum()
	{
		assertEquals("SINGULAR", pairEnum.getFirstString());
	}

	@Test
	@DisplayName("Test getSecondString with enum")
	void testGetSecondStringWithEnum()
	{
		assertEquals("PRESENT", pairEnum.getSecondString());
	}

	@Test
	@DisplayName("Test equals with same enum values")
	void testEqualsWithSameEnumValues()
	{
		Pair<Number, Tense> otherPair = new Pair<>(Number.SINGULAR, Tense.PRESENT);
		assertTrue(pairEnum.equals(otherPair));
	}

	@Test
	@DisplayName("Test equals with different enum values")
	void testEqualsWithDifferentEnumValues()
	{
		Pair<Number, Tense> otherPair1 = new Pair<>(Number.PLURAL, Tense.PRESENT);
		Pair<Number, Tense> otherPair2 = new Pair<>(Number.SINGULAR, Tense.PAST);
		assertFalse(pairEnum.equals(otherPair1));
		assertFalse(pairEnum.equals(otherPair2));
	}

	@Test
	@DisplayName("Test hashCode consistency")
	void testHashCodeConsistency()
	{
		int initialHashCode = pairEnum.hashCode();
		assertEquals(initialHashCode, pairEnum.hashCode());
	}

	@Test
	@DisplayName("Test hashCode equality for equal pairs")
	void testHashCodeEquality()
	{
		Pair<Number, Tense> otherPair = new Pair<>(Number.SINGULAR, Tense.PRESENT);
		assertEquals(pairEnum.hashCode(), otherPair.hashCode());
	}
}
