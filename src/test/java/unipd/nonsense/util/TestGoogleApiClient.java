package unipd.nonsense.util;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.LanguageServiceSettings;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testing GoogleApiClient.")
class TestGoogleApiClient
{
	private static final String filePath = "/testConfig.json";

	@Mock
	private LanguageServiceClient mockClient;

	@Mock
	private ServiceAccountCredentials mockCredentials;

	@Mock
	private LanguageServiceSettings mockSettings;

	@Mock
	private LanguageServiceSettings.Builder mockSettingsBuilder;

	@Mock
	private FixedCredentialsProvider mockCredentialsProvider;

	private GoogleApiClient googleClient;

	private MockedStatic<LanguageServiceClient> mockedLanguageServiceClient;
	private MockedStatic<ServiceAccountCredentials> mockedServiceAccountCredentials;
	private MockedStatic<FixedCredentialsProvider> mockedFixedCredentialsProvider;
	private MockedStatic<LanguageServiceSettings> mockedLanguageServiceSettings;

	@BeforeEach
	@DisplayName("Setup mock environment.")
	void setup() throws IOException
	{
		mockedLanguageServiceClient = mockStatic(LanguageServiceClient.class);
		mockedServiceAccountCredentials = mockStatic(ServiceAccountCredentials.class);
		mockedFixedCredentialsProvider = mockStatic(FixedCredentialsProvider.class);
		mockedLanguageServiceSettings = mockStatic(LanguageServiceSettings.class);

		LanguageServiceSettings.Builder mockSettingsBuilder = mock(LanguageServiceSettings.Builder.class);

		when(ServiceAccountCredentials.fromStream(any(InputStream.class))).thenReturn(mockCredentials);
		when(FixedCredentialsProvider.create(mockCredentials)).thenReturn(mockCredentialsProvider);
		when(LanguageServiceSettings.newBuilder()).thenReturn(mockSettingsBuilder);
		when(mockSettingsBuilder.setCredentialsProvider(mockCredentialsProvider)).thenReturn(mockSettingsBuilder);
		when(mockSettingsBuilder.build()).thenReturn(mockSettings);
		when(LanguageServiceClient.create(mockSettings)).thenReturn(mockClient);

		googleClient = new GoogleApiClient(filePath);
	}

	@AfterEach
	@DisplayName("Removing mock environment used for testing.")
	void tearDown()
	{
		if(googleClient != null)
			googleClient.close();

		mockedLanguageServiceClient.close();
		mockedServiceAccountCredentials.close();
		mockedFixedCredentialsProvider.close();
		mockedLanguageServiceSettings.close();
	}

	@Test
	@DisplayName("Test successfull client creation.")
	void testConstructor_Success()
	{
		assertNotNull(googleClient);
		assertEquals(mockClient, googleClient.getClient());
	}

	@Test
	@DisplayName("Test invalid configuration file.")
	void testConstructor_InvalidFile()
	{
		assertThrows(IOException.class, () ->
			{
				new GoogleApiClient("nonexistent.json");
			});
	}

	@Test
	@DisplayName("Test successfull client getter.")
	void testGetClient_Success()
	{
		LanguageServiceClient client = googleClient.getClient();
		assertEquals(mockClient, client);
	}

	@Test
	@DisplayName("Test successfull client closure.")
	void testClose_Success()
	{
		googleClient.close();
		verify(mockClient, times(1)).close();
	}
}
