package unipd.nonsense.util;

import java.io.IOException;
import java.io.InputStream;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.MockedStatic;
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
		assertEquals(mockClient, googleClient.getClient(), "The two clients should be the same.");
	}

	@Test
	@DisplayName("Test invalid credentials.")
	void testConstructor_InvalidCredentials() throws IOException
	{
		when(ServiceAccountCredentials.fromStream(any())).thenThrow(new IOException());
		assertThrows(IOException.class, () -> new GoogleApiClient(filePath),  "Should throw IOException due to invalid credentials.");
	}

	@Test
	@DisplayName("Test invalid configuration file.")
	void testConstructor_InvalidFile()
	{
		assertThrows(IOException.class, () ->
			{
				new GoogleApiClient("nonexistent.json");
			}, "Should throw IOException due to non existent configuration file.");
	}

	@Test
	@DisplayName("Test multiple clients, same credentials.")
	void testConstructor_MultipleInstancesSameCredentials() throws IOException
	{
		GoogleApiClient client1 = new GoogleApiClient(filePath);
		GoogleApiClient client2 = new GoogleApiClient(filePath);

		assertSame(client1.getClient(), client2.getClient(), "Should create the same session for the same client.");
	}

	@Test
	@DisplayName("Test successfull client getter.")
	void testGetClient_Success()
	{
		LanguageServiceClient client = googleClient.getClient();
		assertEquals(mockClient, client, "The two clients should be the same object.");
	}

	@Test
	@DisplayName("Test thread-safe access to getClient")
	void testGetClient_ThreadSafety() throws InterruptedException
	{
		ExecutorService executor = Executors.newFixedThreadPool(3);
		LanguageServiceClient[] clients = new LanguageServiceClient[3];

		for(int i = 0; i < 3; ++i)
		{
			final int index = i;
			executor.execute(() -> clients[index] = googleClient.getClient());
		}

		executor.shutdown();
		assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

		for(LanguageServiceClient client : clients)
			assertEquals(mockClient, client, "All threads should get the same client instance");

	}

	@Test
	@DisplayName("Test successfull client closure.")
	void testClose_Success()
	{
		googleClient.close();
		verify(mockClient, times(1)).close();
	}

	@Test
	@DisplayName("Test thread safe closure check.")
	void testClose_ThreadSafety()
	{
		ExecutorService executor = Executors.newFixedThreadPool(3);

		executor.execute(() -> googleClient.close());
		executor.execute(() -> googleClient.close());
		executor.execute(() -> googleClient.close());

		executor.shutdown();
		assertDoesNotThrow(() -> executor.awaitTermination(1, TimeUnit.SECONDS), "Client should have been closed only once.");
	}

	@Test
	@DisplayName("Test closing an already closed client")
	void testClose_AlreadyClosed() throws IOException
	{
		googleClient.close();
		verify(mockClient, times(1)).close();

		googleClient.close();
		verify(mockClient, times(1)).close();
	}

	@Test
	@DisplayName("Test validateFile with null or empty file path")
	void testValidateFile_NullOrEmpty()
	{
		assertThrows(IllegalArgumentException.class, () ->
			{
				GoogleApiClient client = new GoogleApiClient(null);
			}, "Should throw IllegalArgumentException due to null file path");

		assertThrows(IllegalArgumentException.class, () ->
			{
				GoogleApiClient client = new GoogleApiClient("");
			}, "Should throw IllegalArgumentException due to empty file path");
	}

	@Test
	@DisplayName("Test validateFile without .json extension")
	void testValidateFile_SuccessNoJsonExtension() throws IOException
	{
		String noExtensionFilePath = "/testConfig";

		GoogleApiClient client = new GoogleApiClient(noExtensionFilePath);
		assertNotNull(client, "Client should be created with automatically appended .json extension");
	}
}
