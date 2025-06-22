package unipd.nonsense.util;

import java.io.IOException;

/**
 * An observer interface for monitoring updates to JSON dictionaries.
 * <p>
 * Implementations of this interface can be registered to receive notifications.
 * </p>
 *
 * <p>Typical usage:
 * <pre>{@code
 * JsonUpdateObserver observer = new JsonUpdateObserver()
 * {
 *     @Override
 *     public void onJsonUpdate() throws IOException
 *     {
 *         // Handle update logic
 *     }
 * };
 * }</pre>
 * </p>
 */
public interface JsonUpdateObserver
{
	/**
	 * Callback method invoked when a JSON file is updated.
	 * <p>
	 * Contract:
	 * <ul>
	 *	<li>Executes after successful write operations</li>
	 *	<li>Runs in the same thread as the modifying operation</li>
	 *	<li>May throw {@code IOException} to signal handling failures</li>
	 * </ul>
	 * </p>
	 *
	 * <p>Implementation notes:
	 * <ul>
	 *	<li>Should perform minimal processing to avoid blocking write operations</li>
	 *	<li>Must handle its own exceptions or propagate them</li>
	 *	<li>Should be thread-safe if registered with multiple handlers</li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException	if observer fails to process the update
	 */
	void onJsonUpdate() throws IOException;
}
