package unipd.nonsense.util;

public class GoogleApiClient
{
	private final LanguageServiceClient client;

	public GoogleApiClient() throws IOException
	{
		InputStream credentialsStream = getClass().getClassLoader().getResourceAsStream("config.json");

		if(credentialsStream == null)
			throw new IOException("File config.json non trovato");

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
