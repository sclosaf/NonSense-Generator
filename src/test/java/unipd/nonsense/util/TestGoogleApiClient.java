package unipd.nonsense.util;

import unipd.nonsense.exceptions.NullFilePathException;
import unipd.nonsense.exceptions.InvalidFilePathException;
import unipd.nonsense.exceptions.FailedOpeningInputStreamException;
import unipd.nonsense.exceptions.ClientAlreadyClosedException;
import unipd.nonsense.exceptions.ClientNonExistentException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
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
	private static final String alternativeFilePath = "/testConfigAlternative.json";

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

		when(LanguageServiceClient.create(Mockito.<LanguageServiceSettings>any())).thenReturn(mockClient);

		googleClient = new GoogleApiClient(filePath);
	}

	@AfterEach
	@DisplayName("Removing mock environment used for testing.")
	void tearDown()
	{
		if(googleClient != null)
			googleClient.close();

		GoogleApiClient.closeAllClients();

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

	void testConstructor_InvalidCredentials() throws IOException
	{
		when(ServiceAccountCredentials.fromStream(any())).thenThrow(new IOException());
		assertThrows(IOException.class, () -> new GoogleApiClient(filePath),  "Should throw IOException due to invalid credentials.");
	}

	@Test
	@DisplayName("Test invalid configuration file.")
	void testConstructor_InvalidFile()
	{
		assertThrows(FailedOpeningInputStreamException.class, () ->
			{
				new GoogleApiClient("nonexistent.json");
			}, "Should throw FailedOpeningInputStreamException due to non existent configuration file.");
	}

	@Test
	@DisplayName("Test multiple clients, same credentials.")
	void testConstructor_MultipleInstancesSameCredentials() throws IOException
	{
		GoogleApiClient client1 = new GoogleApiClient(filePath);
		GoogleApiClient client2 = new GoogleApiClient(filePath);

		assertSame(client1.getClient(), client2.getClient(), "Should create the same session for the same client.");

		assertEquals(3, GoogleApiClient.getClientCount(filePath), "Should have 2 plus the first created client with the same credentials.");

		client1.close();
		client2.close();
	}

	@Test
	@DisplayName("Test extreme long file path")
	void testConstructor_ExtremeLongFilePath()
	{
		StringBuilder longPathBuilder = new StringBuilder();
		for(int i = 0; i < 1000; ++i)
			longPathBuilder.append("directory").append(i).append(File.separator);

		longPathBuilder.append("config.json");

		String longPath = longPathBuilder.toString();

		assertThrows(FailedOpeningInputStreamException.class, () -> new GoogleApiClient(longPath), "Should throw FailedOpeningInputStreamException due to extreme long file paths.");
	}

	@Test
	@DisplayName("Test handling many simultaneous clients.")
	void testConstructor_ManySimultaneousClients() throws InterruptedException, IOException
	{
		int threadCount = 100;
		CountDownLatch latch = new CountDownLatch(threadCount);
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		int initialCount = GoogleApiClient.getClientCount(filePath);

		GoogleApiClient sharedClient = new GoogleApiClient(filePath);

		for(int i = 0; i < threadCount; ++i)
		{
			executor.submit(() ->
				{
					try
					{
						LanguageServiceClient client = sharedClient.getClient();
						assertNotNull(client);
					}
					finally
					{
						latch.countDown();
					}
				});
		}

		boolean completed = latch.await(10, TimeUnit.SECONDS);
		executor.shutdown();

		assertEquals(initialCount + 1, GoogleApiClient.getClientCount(filePath));

		sharedClient.close();

		assertEquals(initialCount, GoogleApiClient.getClientCount(filePath));

		if(initialCount == 0)
			verify(mockClient, times(1)).close();

		assertTrue(completed, "Not all threads were able to be completed on time.");
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
	@DisplayName("Test getting client after closure throws exception.")
	void testGetClient_AfterClosure()
	{
		googleClient.close();
		assertThrows(ClientAlreadyClosedException.class, () -> googleClient.getClient(), "Should throw ClientAlreadyClosedException when getting a client after closure.");
	}

	@Test
	@DisplayName("Test getClientCount returns correct count")
	void testGetClientCount_Success() throws IOException
	{
		assertEquals(1, GoogleApiClient.getClientCount(filePath), "Should return 1 for the client created in setup.");

		GoogleApiClient client2 = new GoogleApiClient(filePath);
		assertEquals(2, GoogleApiClient.getClientCount(filePath), "Should return 2 after creating another client with the same credentials.");

		client2.close();
		assertEquals(1, GoogleApiClient.getClientCount(filePath), "Should return 1 after closing one client.");
	}

	@Test
	@DisplayName("Test getClientCount returns 0 for non existent clients")
	void testGetClientCount_NonExistent()
	{
		assertEquals(0, GoogleApiClient.getClientCount("nonexistent.json"), "Should return 0 for non-existent client path.");
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
	@DisplayName("Test closeAllClients closes all clients")
	void testCloseAllClients() throws IOException
	{
		LanguageServiceClient mockClientAlternative = mock(LanguageServiceClient.class);
		when(LanguageServiceClient.create(any(LanguageServiceSettings.class))).thenReturn(mockClientAlternative);
		GoogleApiClient alternativeClient = new GoogleApiClient(alternativeFilePath);

		assertEquals(1, GoogleApiClient.getClientCount(filePath), "Should have one client with the first credentials.");
		assertEquals(1, GoogleApiClient.getClientCount(alternativeFilePath), "Should have one client with the alternative credentials.");

		GoogleApiClient.closeAllClients();

		verify(mockClient, times(1)).close();
		verify(mockClientAlternative, times(1)).close();

		assertEquals(0, GoogleApiClient.getClientCount(filePath), "Should have no clients with the first credentials after closing all.");
		assertEquals(0, GoogleApiClient.getClientCount(alternativeFilePath), "Should have no clients with the alternative credentials after closing all.");
	}

	@Test
	@DisplayName("Test validateFile success.")
	void testValidateFile_Success() throws IOException
	{
		GoogleApiClient client = new GoogleApiClient(filePath);
		assertNotNull(client, "Client should be created");
	}

	@Test
	@DisplayName("Test validateFile without .json extension")
	void testValidateFile_SuccessNoJsonExtension() throws IOException
	{
		String noExtensionFilePath = "/testConfig";

		GoogleApiClient client = new GoogleApiClient(noExtensionFilePath);
		assertNotNull(client, "Client should be created with automatically appended .json extension");
	}

	@Test
	@DisplayName("Test validateFile with null file path")
	void testValidateFile_Null()
	{
		assertThrows(NullFilePathException.class, () -> new GoogleApiClient(null), "Should throw NullFilePathException due to null file path");
	}

	@Test
	@DisplayName("Test validateFile with empty file path")
	void testValidateFile_Empty()
	{
		assertThrows(InvalidFilePathException.class, () -> new GoogleApiClient(""), "Should throw InvalidFilePathException due to empty file path");
	}

	@Test
	@DisplayName("Test stream opening failure")
	void testValidateFile_FailedOpeningInputStream() throws IOException
	{
		try(MockedStatic<GoogleApiClient> mockedClient = mockStatic(GoogleApiClient.class, invocation ->
			{
				if(invocation.getMethod().getName().equals("getResourceAsStream"))
					return null;

				return invocation.callRealMethod();
			}))
		{
			assertThrows(FailedOpeningInputStreamException.class, () -> new GoogleApiClient("wrongConfig.json"), "Should throw FailedOpeningInputStreamException when stream can't be opened.");
		}
	}

	@Test
	@DisplayName("Test constructor with whitespace-only file path")
	void testConstructor_WhitespaceFilePath()
	{
		assertThrows(FailedOpeningInputStreamException.class, () -> new GoogleApiClient("   "),
			"Should throw InvalidFilePathException for whitespace-only file path");
	}

	@Test
	@DisplayName("Test constructor with relative parent path traversal")
	void testConstructor_ParentPathTraversal()
	{
		assertThrows(FailedOpeningInputStreamException.class, () -> new GoogleApiClient("../../file.json"),
			"Should prevent parent directory traversal in file path");
	}

	@Test
	@DisplayName("Test constructor with special characters in path")
	void testConstructor_SpecialCharactersPath()
	{
		String specialPath = "/test!@#$%^&()_+-={}[]|;',.`~.json";
		assertThrows(FailedOpeningInputStreamException.class, () -> new GoogleApiClient(specialPath),
			"Should handle special characters in file path");
	}

	@Test
	@DisplayName("Test getClient from multiple threads after closure")
	void testGetClient_ConcurrentAfterClosure() throws InterruptedException
	{
		googleClient.close();
		int threadCount = 5;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		AtomicInteger exceptionCount = new AtomicInteger(0);

		for(int i = 0; i < threadCount; i++)
		{
			executor.execute(() ->
			{
				try
				{
					googleClient.getClient();
				}
				catch(ClientAlreadyClosedException e)
				{
					exceptionCount.incrementAndGet();
				}
			});
		}

		executor.shutdown();
		assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
		assertEquals(threadCount, exceptionCount.get());
	}

	@Test
	@DisplayName("Test extremely rapid sequential open/close operations")
	void testRapidOpenCloseOperations() throws IOException
	{
		int operations = 1000;
		for(int i = 0; i < operations; i++)
		{
			GoogleApiClient client = new GoogleApiClient(filePath);
			client.close();
		}

		assertEquals(1, GoogleApiClient.getClientCount(filePath));
	}

	@Test
	@DisplayName("Test mixed operations under high concurrency")
	void testMixedOperationsHighConcurrency() throws InterruptedException
	{
		int threadCount = 50;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicReference<Exception> error = new AtomicReference<>();

		for(int i = 0; i < threadCount; i++)
		{
			executor.execute(() ->
			{
				try
				{
					for(int j = 0; j < 10; j++)
					{
						GoogleApiClient client = new GoogleApiClient(filePath);

						try
						{
							assertNotNull(client.getClient());
							Thread.sleep(1);
						}
						finally
						{
							client.close();
						}
					}
				}
				catch(Exception e)
				{
					error.set(e);
				}
				finally
				{
					latch.countDown();
				}
			});
		}

		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertNull(error.get());
		executor.shutdown();
	}

	@Test
	@DisplayName("Test closeAllClients during active operations")
	void testCloseAllClients_DuringActiveOperations() throws InterruptedException
	{
		int threadCount = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);

		for(int i = 0; i < threadCount; i++)
		{
			executor.execute(() ->
			{
				try
				{
					startLatch.await();
					GoogleApiClient client = new GoogleApiClient(filePath);

					try
					{
						assertNotNull(client.getClient());
						successCount.incrementAndGet();
					}
					finally
					{
						client.close();
					}
				}
				catch(Exception e)
				{
					fail("Operation should complete successfully");
				}
				finally
				{
					endLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		Thread.sleep(10);
		GoogleApiClient.closeAllClients();
		assertTrue(endLatch.await(1, TimeUnit.SECONDS));
		assertEquals(threadCount, successCount.get());
		executor.shutdown();
	}

	@Test
	@DisplayName("Test getClientCount during concurrent modifications")
	void testGetClientCount_ConcurrentModifications() throws InterruptedException
	{
		int threadCount = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);

		for(int i = 0; i < threadCount; i++)
		{
			executor.execute(() ->
			{
				try
				{
					for(int j = 0; j < 10; j++)
					{
						int count = GoogleApiClient.getClientCount(filePath);
						assertTrue(count >= 0 && count <= threadCount);
						successCount.incrementAndGet();
						Thread.sleep(1);
					}
				}
				catch(Exception e)
				{
					fail("Should not throw exceptions");
				}
				finally
				{
					latch.countDown();
				}
			});
		}

		assertTrue(latch.await(5, TimeUnit.SECONDS));
		assertEquals(threadCount * 10, successCount.get());
		executor.shutdown();
	}
}
