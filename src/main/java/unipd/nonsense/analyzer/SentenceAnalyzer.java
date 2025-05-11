package unipd.nonsense.analyzer;

import com.google.cloud.language.v1.AnalyzeSyntaxRequest;
import com.google.cloud.language.v1.AnalyzeSyntaxResponse;
import com.google.cloud.language.v1.Token;
import unipd.nonsense.util.GoogleApiClient;
import java.util.List;

public class SentenceAnalyzer
{
    /** from the string {@code text}. */
    public static List<Token> analyzeSyntaxInput(String text) throws Exception
    {
        GoogleApiClient manager = new GoogleApiClient("/credentials.json");

        // [START language_syntax_text]
        // Instantiate the Language client com.google.cloud.language.v1.LanguageServiceClient
        try (com.google.cloud.language.v1.LanguageServiceClient language = manager.getClient())
        {
            com.google.cloud.language.v1.Document doc = com.google.cloud.language.v1.Document.newBuilder()
                .setContent(text)
                .setType(com.google.cloud.language.v1.Document.Type.PLAIN_TEXT)
                .build();
            AnalyzeSyntaxRequest request = AnalyzeSyntaxRequest.newBuilder()
                .setDocument(doc)
                .setEncodingType(com.google.cloud.language.v1.EncodingType.UTF16)
                .build();
            // Analyze the syntax in the given text
            AnalyzeSyntaxResponse response = language.analyzeSyntax(request);
            // Print the response
            for (Token token : response.getTokensList())
            {
                System.out.printf("\tText: %s\n", token.getText().getContent());
                System.out.printf("\tBeginOffset: %d\n", token.getText().getBeginOffset());
                System.out.printf("Lemma: %s\n", token.getLemma());
                System.out.printf("PartOfSpeechTag: %s\n", token.getPartOfSpeech().getTag());
                System.out.printf("\tAspect: %s\n", token.getPartOfSpeech().getAspect());
                System.out.printf("\tCase: %s\n", token.getPartOfSpeech().getCase());
                System.out.printf("\tForm: %s\n", token.getPartOfSpeech().getForm());
                System.out.printf("\tGender: %s\n", token.getPartOfSpeech().getGender());
                System.out.printf("\tMood: %s\n", token.getPartOfSpeech().getMood());
                System.out.printf("\tNumber: %s\n", token.getPartOfSpeech().getNumber());
                System.out.printf("\tPerson: %s\n", token.getPartOfSpeech().getPerson());
                System.out.printf("\tProper: %s\n", token.getPartOfSpeech().getProper());
                System.out.printf("\tReciprocity: %s\n", token.getPartOfSpeech().getReciprocity());
                System.out.printf("\tTense: %s\n", token.getPartOfSpeech().getTense());
                System.out.printf("\tVoice: %s\n", token.getPartOfSpeech().getVoice());
                System.out.println("DependencyEdge");
                System.out.printf("\tHeadTokenIndex: %d\n", token.getDependencyEdge().getHeadTokenIndex());
                System.out.printf("\tLabel: %s\n\n", token.getDependencyEdge().getLabel());
            }
            return response.getTokensList();
        }
    }
}
