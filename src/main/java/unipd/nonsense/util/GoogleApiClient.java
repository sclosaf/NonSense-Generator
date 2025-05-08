package unipd.nonsense.util;

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
	private boolean isClosed;

	public GoogleApiClient(String filePath) throws IOException
	{
		this.credentialsPath = validateFile(filePath);

		cacheLock.lock();

		try
		{
			ClientEntry entry = clientCache.get(this.credentialsPath);

			if(entry != null)
				entry.count++;
			else
			{
				LanguageServiceClient newClient = createClient(this.credentialsPath);
				clientCache.put(this.credentialsPath, new ClientEntry(newClient));
			}

			isClosed = false;
		}
		finally
		{
			cacheLock.unlock();
		}
	}

	public LanguageServiceClient getClient()
	{
		if(isClosed)
			throw new ClientAlreadyClosedException();

		ClientEntry entry = clientCache.get(this.credentialsPath);

		if(entry == null)
			throw new ClientNonExistentException();

		return entry.client;
	}

	public static int getClientCount(String filePath)
	{
		cacheLock.lock();

		try
		{
			ClientEntry entry = clientCache.get(filePath);
			return entry != null ? entry.count : 0;
		}
		finally
		{
			cacheLock.unlock();
		}
	}

	public void close()
	{
		if(isClosed)
			return;

		cacheLock.lock();

		try
		{
			ClientEntry entry = clientCache.get(this.credentialsPath);

			if(entry != null)
			{
				entry.count--;

				if(entry.count == 0)
				{
					entry.client.close();
					clientCache.remove(this.credentialsPath);

				}
			}

			isClosed = true;
		}
		finally
		{
			cacheLock.unlock();
		}
	}

	public static void closeAllClients()
	{
		cacheLock.lock();

		try
		{
			for(ClientEntry entry : clientCache.values())
				entry.client.close();

			clientCache.clear();
		}
		finally
		{
			cacheLock.unlock();
		}
	}

	private LanguageServiceClient createClient(String filePath) throws IOException
	{
		try(InputStream stream = getClass().getResourceAsStream(filePath))
		{
			if(stream == null)
				throw new FailedOpeningInputStreamException();

			ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(stream);

			LanguageServiceSettings settings = LanguageServiceSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();

			return LanguageServiceClient.create(settings);

		}
	}

	private String validateFile(String filePath) throws IOException
	{
		if(filePath == null)
			throw new NullFilePathException();

		if(filePath.isEmpty())
			throw new InvalidFilePathException(filePath);

		filePath = filePath.toLowerCase().endsWith(".json") ? filePath : filePath + ".json";

		File file = new File(filePath);

		try(InputStream stream = getClass().getResourceAsStream(filePath))
		{
			if(stream == null)
				throw new FailedOpeningInputStreamException();
		}

		return filePath;
	}
}

