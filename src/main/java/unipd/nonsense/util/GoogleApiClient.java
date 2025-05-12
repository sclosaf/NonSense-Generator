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

public class GoogleApiClient implements AutoCloseable
{
	private static final class ClientEntry
	{
		final LanguageServiceClient client;
		int count;

		ClientEntry(LanguageServiceClient client)
		{
			this.client = client;
			this.count = 1;
		}
	}

	private static final Map<String, ClientEntry> clientCache = new ConcurrentHashMap<>();
	private static final Lock cacheLock = new ReentrantLock();

	private final String credentialsPath;
	private static LoggerManager logger = new LoggerManager(GoogleApiClient.class);
	private boolean isClosed;

	public GoogleApiClient(String filePath) throws IOException
	{
		logger.logDebug("Initializing GoogleApiClient with credentials file: " + filePath);

		try
		{
			this.credentialsPath = validateFile(filePath);
			logger.logDebug("Credentials file validated: " + this.credentialsPath);

			cacheLock.lock();
			logger.logInfo("Acquired cache lock for client initialization");

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
					logger.logInfo("Created new client instance and added to cache");
				}

				isClosed = false;
				logger.logInfo("GoogleApiClient initialized successfully");
			}
			finally
			{
				cacheLock.unlock();
				logger.logInfo("Released cache lock after client initialization");
			}
		}
		catch(IOException e)
		{
			logger.logError("Failed to initialize GoogleApiClient", e);
			throw e;
		}
	}

	public LanguageServiceClient getClient()
	{
		logger.logInfo("getClient: Attempting to get client instance");

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

		logger.logInfo("getClient: Returning client instance");
		return entry.client;
	}

	public static int getClientCount(String filePath)
	{
		logger.logDebug("getClientCount: Getting client count for path: " + filePath);

		cacheLock.lock();
		logger.logInfo("getClientCount: Acquired cache lock for client count");

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
			logger.logInfo("getClientCount: Released cache lock after client count");
		}
	}

	@Override
	public void close()
	{
		logger.logInfo("close: Closing client");

		if(isClosed)
		{
			logger.logDebug("close: Client already closed, skipping");
			return;
		}

		cacheLock.lock();
		logger.logInfo("close: Acquired cache lock for client closure");

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
					logger.logInfo("close: Client fully closed and removed from cache");
				}
			}

			isClosed = true;
			logger.logInfo("close: GoogleApiClient closed successfully");
		}
		finally
		{
			cacheLock.unlock();
			logger.logInfo("close: Released cache lock after client closure");
		}
	}

	public static void closeAllClients()
	{
		logger.logInfo("closeAllClients: Initiating shutdown of all clients");
		cacheLock.lock();
		logger.logInfo("closeAllClients: Acquired cache lock");

		try
		{
			int clientCount = clientCache.size();
			logger.logDebug("closeAllClients: Closing " + clientCount + " client instances");

			for(ClientEntry entry : clientCache.values())
			{
				entry.client.close();
				logger.logInfo("closeAllClients: Closed client instance");
			}

			clientCache.clear();
			logger.logInfo("closeAllClients: Successfully closed all " + clientCount + " clients");
		}
		finally
		{
			cacheLock.unlock();
			logger.logInfo("closeAllClients: Released cache lock");
		}
	}

	private LanguageServiceClient createClient(String filePath) throws IOException
	{
		logger.logInfo("createClient: Creating new LanguageServiceClient");

		try(InputStream stream = getClass().getResourceAsStream(filePath))
		{
			if(stream == null)
			{
				logger.logError("createClient: Failed to open input stream for credentials file");
				throw new FailedOpeningInputStreamException();
			}

			ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(stream);

			logger.logInfo("createClient: Successfully loaded service account credentials");

			LanguageServiceSettings settings = LanguageServiceSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();

			logger.logDebug("createClient: LanguageServiceSettings configured");

			return LanguageServiceClient.create(settings);

		}
		catch(IOException e)
		{
			logger.logError("CreateClient: Failed to create LanguageServiceClient", e);
			throw e;
		}
	}

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

		logger.logInfo("Formatted file path: " + filePath);

		File file = new File(filePath);

		try(InputStream stream = getClass().getResourceAsStream(filePath))
		{
			if(stream == null)
			{
				logger.logError("validateFile: Failed to open input stream for file validation");
				throw new FailedOpeningInputStreamException();
			}

			logger.logInfo("validateFile: File validation successful");
		}

		return filePath;
	}
}

