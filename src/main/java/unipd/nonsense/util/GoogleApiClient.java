package unipd.nonsense.util;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.LanguageServiceSettings;

public class GoogleApiClient implements AutoCloseable
{
	private LanguageServiceClient client;
	private final Lock lock = new ReentrantLock();

	public GoogleApiClient(String filePath) throws IOException
	{
		filePath = validateFile(filePath);

		try(InputStream stream = getClass().getResourceAsStream(filePath))
		{
			ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(stream);

			client = LanguageServiceClient.create(LanguageServiceSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build());
		}
	}

	public LanguageServiceClient getClient()
	{
		return client;
	}

	public void close()
	{
		if(client != null)
		{
			lock.lock();
			try
			{
				if(client != null)
				{
					client.close();
					client = null;
				}
			}
			finally
			{
				lock.unlock();
			}
		}
	}

	private String validateFile(String filePath) throws IOException
	{
		if(filePath == null || filePath.isEmpty())
			throw new IllegalArgumentException();

		filePath = filePath.toLowerCase().endsWith(".json") ? filePath : filePath + ".json";

		File file = new File(filePath);

		try(InputStream stream = getClass().getResourceAsStream(filePath))
		{
			if(stream == null)
				throw new IOException();
		}

		return filePath;

	}
}

