package it.nonsense.util;

class GoogleApiClient
{
	private LanguageServiceClient client;

	public GoogleApiClient() throws IOException
	{
		GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
	}
}
