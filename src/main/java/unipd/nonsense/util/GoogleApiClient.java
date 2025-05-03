package unipd.nonsense.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.LanguageServiceSettings;

public class GoogleApiClient
{
	private final LanguageServiceClient client;

	public GoogleApiClient() throws IOException
	{
		InputStream credentialsStream = getClass().getClassLoader().getResourceAsStream("config.json");

		if(credentialsStream == null)
			throw new IOException();

		ServiceAccountCredentials credentials;
        try(InputStreamReader reader = new InputStreamReader(credentialsStream))
		{
				credentials = ServiceAccountCredentials.fromStream(credentialsStream);
		}

		LanguageServiceSettings settings = LanguageServiceSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();

		this.client = LanguageServiceClient.create(settings);
	}

	public void close()
	{
		if (client != null)
			client.close();
	}
}
