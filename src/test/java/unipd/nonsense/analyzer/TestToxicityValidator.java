package unipd.nonsense.analyzer;

import unipd.nonsense.exceptions.InvalidThresholdException;
import unipd.nonsense.exceptions.InvalidTextException;
import unipd.nonsense.util.GoogleApiClient;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.ModerateTextResponse;
import com.google.cloud.language.v1.ClassificationCategory;
import com.google.cloud.language.v1.ModerateTextRequest;
import com.google.cloud.language.v1.Document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Map;

import static org.mockito.Mockito.*;

