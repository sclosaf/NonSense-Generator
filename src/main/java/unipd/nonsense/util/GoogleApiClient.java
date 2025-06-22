package unipd.nonsense.util;

import unipd.nonsense.util.LoggerManager;

import unipd.nonsense.exceptions.NullFilePathException;
import unipd.nonsense.exceptions.InvalidFilePathException;
import unipd.nonsense.exceptions.FailedOpeningInputStreamException;
import unipd.nonsense.exceptions.ClientAlreadyClosedException;
import unipd.nonsense.exceptions.ClientNonExistentException;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.LanguageServiceSettings;

/**
 * A client for managing Google Cloud Language Service connections with credential-based caching.
 * <p>
 * This class provides:
 * <ul>
 *	<li>Thread-safe client instance management</li>
 *	<li>Reference counting for shared client instances</li>
 *	<li>Automatic resource cleanup</li>
 *	<li>Credential file validation</li>
 * </ul>
 * </p>
 *
 * <p>Implements {@code AutoCloseable} for proper resource management.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * try(GoogleApiClient client = new GoogleApiClient("credentials.json"))
 * {
 *     LanguageServiceClient service = client.getClient();
 *     // Use the service
 * }
 * catch(IOException e)
 * {
 *     // Handle exception
 * }
 * }</pre>
 * </p>
 *
 * @see AutoCloseable
 * @see LanguageServiceClient
 * @see LanguageServiceSettings
 */
public class GoogleApiClient implements AutoCloseable
{
	/**
	 * Internal class for tracking client instances and their usage counts.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Contains the actual {@code LanguageServiceClient} instance</li>
	 *	<li>Maintains reference count for shared usage</li>
	 *	<li>Used exclusively by the client cache</li>
	 * </ul>
	 * </p>
	 */
	private static final class ClientEntry
	{
		/**
		 * The actual Google Cloud Language client instance.
		 */
		final LanguageServiceClient client;

		/**
		 * Reference count tracking how many {@code GoogleApiClient} instances are using this client.
		 */
		int count;

		/**
		 * Constructs a new {@code ClientEntry} with initial count of 1.
		 *
		 * @param client	The {@code LanguageServiceClient} to wrap
		 */
		ClientEntry(LanguageServiceClient client)
		{
			this.client = client;
			this.count = 1;
		}
	}

	/**
	 * Cache for storing active client instances keyed by credentials path.
	 * <p>
	 * Cache behavior:
	 * <ul>
	 *	<li>Shared across all {@code GoogleApiClient} instances</li>
	 *	<li>Thread-safe through {@code ConcurrentHashMap}</li>
	 *	<li>Accessed under {@code cacheLock} for atomic operations</li>
	 * </ul>
	 * </p>
	 */
	private static final Map<String, ClientEntry> clientCache = new ConcurrentHashMap<>();

	/**
	 * Lock for synchronizing access to the client cache.
	 * <p>
	 * Usage:
	 * <ul>
	 *	<li>Protects all cache modification operations</li>
	 *	<li>Reentrant to allow nested locking</li>
	 *	<li>Held for minimal duration</li>
	 * </ul>
	 * </p>
	 */
	private static final Lock cacheLock = new ReentrantLock();

	/**
	 * Path to the credentials file used by this client instance.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *	<li>Validated during construction</li>
	 *	<li>Used as cache key</li>
	 *	<li>Immutable after construction</li>
	 * </ul>
	 * </p>
	 */
	private final String credentialsPath;

	/**
	 * Logger instance for tracking operations and errors.
	 * <p>
	 * Configured to log messages from {@code GoogleApiClient} class.
	 * </p>
	 */
	private static LoggerManager logger = new LoggerManager(GoogleApiClient.class);

	/**
	 * Flag indicating whether this client instance has been closed.
	 * <p>
	 * State transitions:
	 * <ul>
	 *	<li>Initialized to {@code false} during construction</li>
	 *	<li>Set to {@code true} during {@code close()}</li>
	 *	<li>Checked in {@code getClient()}</li>
	 * </ul>
	 * </p>
	 */
	private boolean isClosed;

	/**
	 * Constructs a new {@code GoogleApiClient} using the specified credentials file.
	 * <p>
	 * Construction process:
	 * <ul>
	 *	<li>Validates credentials file path</li>
	 *	<li>Checks for existing client instances</li>
	 *	<li>Creates new client if needed</li>
	 *	<li>Updates reference counts</li>
	 * </ul>
	 * </p>
	 *
	 * @param filePath	Path to the JSON credentials file
	 * @throws IOException					if client creation fails
	 * @throws NullFilePathException		if {@code filePath} is {@code null}
	 * @throws InvalidFilePathException		if {@code filePath} is empty or invalid
	 */
	public GoogleApiClient(String filePath) throws IOException
	{
		logger.logDebug("Initializing GoogleApiClient with credentials file: " + filePath);

		try
		{
			this.credentialsPath = validateFile(filePath);
			logger.logDebug("Credentials file validated: " + this.credentialsPath);

			cacheLock.lock();
			logger.logTrace("Acquired cache lock for client initialization");

			try
			{
				ClientEntry entry = clientCache.get(this.credentialsPath);

				if(entry != null)
				{
					entry.count++;
					logger.logDebug("Reusing existing client instance. Reference count incremented to: " + entry.count);
				}
				else
				{
					LanguageServiceClient newClient = createClient(this.credentialsPath);
					clientCache.put(this.credentialsPath, new ClientEntry(newClient));
					logger.logTrace("Created new client instance and added to cache");
				}

				isClosed = false;
				logger.logTrace("GoogleApiClient initialized successfully");
			}
			finally
			{
				cacheLock.unlock();
				logger.logTrace("Released cache lock after client initialization");
			}
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize GoogleApiClient", e);
			throw e;
		}
	}

	/**
	 * Retrieves the underlying {@code LanguageServiceClient} instance.
	 * <p>
	 * The returned client:
	 * <ul>
	 *	<li>Is shared among instances using same credentials</li>
	 *	<li>Should not be closed directly</li>
	 *	<li>Becomes invalid after {@code close()} is called</li>
	 * </ul>
	 * </p>
	 *
	 * @return						The active {@code LanguageServiceClient} instance
	 * @throws ClientAlreadyClosedException	if this client has been closed
	 * @throws ClientNonExistentException	if the client was removed from cache
	 */
	public LanguageServiceClient getClient()
	{
		logger.logTrace("getClient: Attempting to get client instance");

		if(isClosed)
		{
			logger.logError("getClient: Client already closed");
			throw new ClientAlreadyClosedException();
		}

		ClientEntry entry = clientCache.get(this.credentialsPath);

		if(entry == null)
		{
			logger.logError("getClient: Client does not exist in cache");
			throw new ClientNonExistentException();
		}

		logger.logTrace("getClient: Returning client instance");
		return entry.client;
	}

	/**
	 * Gets the number of active client instances using the specified credentials file.
	 * <p>
	 * The count represents:
	 * <ul>
	 *	<li>Number of unclosed {@code GoogleApiClient} instances</li>
	 *	<li>Shared client references</li>
	 *	<li>Returns 0 if no clients exist for the path</li>
	 * </ul>
	 * </p>
	 *
	 * @param filePath	The credentials file path to check
	 * @return			Number of active client instances using these credentials
	 */
	public static int getClientCount(String filePath)
	{
		logger.logDebug("getClientCount: Getting client count for path: " + filePath);

		cacheLock.lock();
		logger.logTrace("getClientCount: Acquired cache lock for client count");

		try
		{
			ClientEntry entry = clientCache.get(filePath);
			int count = entry != null ? entry.count : 0;

			logger.logDebug("getClientCount: Found " + count + " instances for path: " + filePath);

			return count;
		}
		finally
		{
			cacheLock.unlock();
			logger.logTrace("getClientCount: Released cache lock after client count");
		}
	}

	/**
	 * Releases resources associated with this client instance.
	 * <p>
	 * Closing process:
	 * <ul>
	 *	<li>Decrements reference count</li>
	 *	<li>Closes underlying client if last reference</li>
	 *	<li>Removes entry from cache if last reference</li>
	 *	<li>Idempotent after first call</li>
	 * </ul>
	 * </p>
	 */
	@Override
	public void close()
	{
		logger.logTrace("close: Closing client");

		if(isClosed)
		{
			logger.logDebug("close: Client already closed, skipping");
			return;
		}

		cacheLock.lock();
		logger.logTrace("close: Acquired cache lock for client closure");

		try
		{
			ClientEntry entry = clientCache.get(this.credentialsPath);

			if(entry != null)
			{
				entry.count--;
				logger.logDebug("close: Decremented client reference count to: " + entry.count);

				if(entry.count == 0)
				{
					entry.client.close();
					clientCache.remove(this.credentialsPath);
					logger.logTrace("close: Client fully closed and removed from cache");
				}
			}

			isClosed = true;
			logger.logTrace("close: GoogleApiClient closed successfully");
		}
		finally
		{
			cacheLock.unlock();
			logger.logTrace("close: Released cache lock after client closure");
		}
	}

	/**
	 * Closes all active client instances and clears the cache.
	 * <p>
	 * This operation:
	 * <ul>
	 *	<li>Is global across all credentials paths</li>
	 *	<li>Should be used for shutdown cleanup</li>
	 *	<li>Renders all existing {@code GoogleApiClient} instances unusable</li>
	 * </ul>
	 * </p>
	 */
	public static void closeAllClients()
	{
		logger.logTrace("closeAllClients: Initiating shutdown of all clients");
		cacheLock.lock();
		logger.logTrace("closeAllClients: Acquired cache lock");

		try
		{
			int clientCount = clientCache.size();
			logger.logDebug("closeAllClients: Closing " + clientCount + " client instances");

			for(ClientEntry entry : clientCache.values())
			{
				entry.client.close();
				logger.logTrace("closeAllClients: Closed client instance");
			}

			clientCache.clear();
			logger.logDebug("closeAllClients: Successfully closed all " + clientCount + " clients");
		}
		finally
		{
			cacheLock.unlock();
			logger.logTrace("closeAllClients: Released cache lock");
		}
	}

	/**
	 * Creates a new {@code LanguageServiceClient} instance using specified credentials.
	 * <p>
	 * Creation process:
	 * <ul>
	 *	<li>Loads credentials from JSON file</li>
	 *	<li>Configures service settings</li>
	 *	<li>Establishes authenticated connection</li>
	 * </ul>
	 * </p>
	 *
	 * @param filePath	Path to the credentials JSON file
	 * @return			New authenticated {@code LanguageServiceClient} instance
	 * @throws IOException						if credentials are invalid
	 * @throws FailedOpeningInputStreamException	if file cannot be read
	 */
	private LanguageServiceClient createClient(String filePath) throws IOException
	{
		logger.logTrace("createClient: Creating new LanguageServiceClient");

		try(InputStream stream = getClass().getResourceAsStream(filePath))
		{
			if(stream == null)
			{
				logger.logError("createClient: Failed to open input stream for credentials file");
				throw new FailedOpeningInputStreamException();
			}

			ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(stream);

			logger.logTrace("createClient: Successfully loaded service account credentials");

			LanguageServiceSettings settings = LanguageServiceSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();

			logger.logTrace("createClient: LanguageServiceSettings configured");

			return LanguageServiceClient.create(settings);

		}
		catch(IOException e)
		{
			logger.logError("CreateClient: Failed to create LanguageServiceClient", e);
			throw e;
		}
	}

	/**
	 * Validates and normalizes a credentials file path.
	 * <p>
	 * Validation includes:
	 * <ul>
	 *	<li>Null and empty checks</li>
	 *	<li>File existence verification</li>
	 *	<li>.json extension handling</li>
	 *	<li>Stream accessibility test</li>
	 * </ul>
	 * </p>
	 *
	 * @param filePath	The raw file path to validate
	 * @return			Normalized and validated file path
	 * @throws NullFilePathException			if {@code filePath} is {@code null}
	 * @throws InvalidFilePathException		if {@code filePath} is empty
	 * @throws FailedOpeningInputStreamException	if file cannot be accessed
	 */
	private String validateFile(String filePath) throws IOException
	{
		logger.logDebug("validateFile: Validating credentials file path: " + filePath);

		if(filePath == null)
		{
			logger.logError("validateFile: Null file path provided");
			throw new NullFilePathException();
		}

		if(filePath.isEmpty())
		{
			logger.logError("validateFile: Empty file path provided");
			throw new InvalidFilePathException(filePath);
		}

		filePath = filePath.toLowerCase().endsWith(".json") ? filePath : filePath + ".json";

		logger.logDebug("Formatted file path: " + filePath);

		File file = new File(filePath);

		try(InputStream stream = getClass().getResourceAsStream(filePath))
		{
			if(stream == null)
			{
				logger.logError("validateFile: Failed to open input stream for file validation");
				throw new FailedOpeningInputStreamException();
			}

			logger.logTrace("validateFile: File validation successful");
		}

		return filePath;
	}
}

