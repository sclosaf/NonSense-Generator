package unipd.nonsense.model;

import java.util.Objects;

/**
 * A generic container class that holds a pair of two related values.
 * <p>
 * This immutable class can store any two objects of different types while maintaining
 * type safety through Java generics. It provides methods to access both elements
 * and supports standard object operations like equality comparison, hashing, and
 * string representation.
 * </p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Pair<String, Integer> nameAge = new Pair<>("Alice", 30);
 * String name = nameAge.getFirst();
 * int age = nameAge.getSecond();
 * }</pre>
 * </p>
 *
 * @param <T>	the type of the first element in the pair
 * @param <U>	the type of the second element in the pair
 */
public class Pair<T, U>
{
	/**
	 * The first element of the pair.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Stored as immutable reference</li>
	 *	<li>Can be of any type {@code T}</li>
	 *	<li>Can be {@code null}</li>
	 * </ul>
	 */
	private final T first;

	/**
	 * The second element of the pair.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Stored as immutable reference</li>
	 *	<li>Can be of any type {@code U}</li>
	 *	<li>Can be {@code null}</li>
	 * </ul>
	 */
	private final U second;

	/**
	 * Constructs a new {@code Pair} with the specified elements.
	 * <p>
	 * Both elements can be {@code null}, and there are no restrictions
	 * on the types of objects that can be paired together.
	 *
	 * @param first		the first element of the pair
	 * @param second	the second element of the pair
	 */
	public Pair(T first, U second)
	{
		this.first = first;
		this.second = second;
	}

	/**
	 * Retrieves the first element of the pair.
	 *
	 * @return	the first element, which may be {@code null}
	 */
	public T getFirst()
	{
		return first;
	}

	/**
	 * Retrieves the first element as a string representation.
	 * <p>
	 * This method is equivalent to calling {@code String.valueOf()} on the
	 * first element. It handles {@code null} values by converting them to
	 * the string "null".
	 *
	 * @return	string representation of the first element
	 * @see String#valueOf(Object)
	 */
	public String getFirstString()
	{
		return String.valueOf(first);
	}

	/**
	 * Retrieves the second element of the pair.
	 *
	 * @return	the second element, which may be {@code null}
	 */
	public U getSecond()
	{
		return second;
	}

	/**
	 * Retrieves the second element as a string representation.
	 * <p>
	 * This method is equivalent to calling {@code String.valueOf()} on the
	 * second element. It handles {@code null} values by converting them to
	 * the string "null".
	 *
	 * @return	string representation of the second element
	 * @see String#valueOf(Object)
	 */
	public String getSecondString()
	{
		return String.valueOf(second);
	}

	/**
	 * Compares this pair with another object for equality.
	 * <p>
	 * Two pairs are considered equal if:
	 * <ul>
	 *	<li>They reference the same object, or</li>
	 *	<li>Their corresponding elements are equal according to
	 *		{@link Objects#equals(Object, Object)}</li>
	 * </ul>
	 *
	 * @param o	the object to compare with
	 * @return	{@code true} if the objects are equal, {@code false} otherwise
	 */
	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;

		if(o == null || getClass() != o.getClass())
			return false;

		Pair<?, ?> pair = (Pair<?, ?>) o;

		return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
	}

	/**
	 * Generates a hash code for this pair.
	 * <p>
	 * The hash code is computed by combining the hash codes of both elements
	 * using {@link Objects#hash(Object...)}. If either element is {@code null},
	 * its hash code is considered to be 0.
	 *
	 * @return	a hash code value for this pair
	 */
	@Override
	public int hashCode()
	{
		return Objects.hash(first, second);
	}

	/**
	 * Returns a string representation of the pair.
	 * <p>
	 * The format is {@code "< first,second >"} where {@code first} and
	 * {@code second} are the string representations of the respective
	 * elements as returned by their {@code toString()} methods.
	 *
	 * @return	a string representation of the pair
	 */
	@Override
	public String toString()
	{
		return "< " + first + "," + second + " >";
	}
}
