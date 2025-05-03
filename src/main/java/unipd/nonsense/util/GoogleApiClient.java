package unipd.nonsense.util;

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
	private final LanguageServiceClient client;
	private final Lock lock = new ReentrantLock();
	private static final String filePath = "config.json";

	public GoogleApiClient() throws IOException
	{
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
				client.close();
			}
			finally
			{
				lock.unlock();
			}
		}
	}
}

