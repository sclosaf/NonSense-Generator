package unipd.nonsense.analyzer;

import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.ModerateTextRequest;
import com.google.cloud.language.v1.ModerateTextResponse;
import com.google.cloud.language.v1.TextSpan;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.ModerateTextRequest;
import com.google.cloud.language.v1.ModerateTextResponse;
import com.google.cloud.language.v1.TextSpan;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ToxicityValidator {
    
    public ValidationResult validateToxicity(String sentence) {
        try (LanguageServiceClient languageService = LanguageServiceClient.create()) {
            Document doc = Document.newBuilder()
                    .setContent(sentence)
                    .setType(Type.PLAIN_TEXT)
                    .build();
            
            ModerateTextRequest request = ModerateTextRequest.newBuilder()
                    .setDocument(doc)
                    .build();
            
            ModerateTextResponse response = languageService.moderateText(request);
            return processResponse(response, sentence);
        } catch (IOException e) {
            return new ValidationResult(false, "API error: " + e.getMessage(), new ArrayList<>());
        }
    }
    
    /*private ValidationResult processResponse(ModerateTextResponse response, String originalText) {
        List<String> toxicPhrases = new ArrayList<>();
        boolean isToxic = false;
        
        for (TextSpan span : response.getDetectedProfanityList()) {
            String toxicText = originalText.substring(span.getBeginOffset(), span.getEndOffset());
            toxicPhrases.add(toxicText);
            isToxic = true;
        }
        
        String message = isToxic ? "Inappropriate content detected" : "Content appears appropriate";
        return new ValidationResult(!isToxic, message, toxicPhrases);
    }
    
    public static class ValidationResult {
        private final boolean isAppropriate;
        private final String message;
        private final List<String> toxicPhrases;
        
        public ValidationResult(boolean isAppropriate, String message, List<String> toxicPhrases) {
            this.isAppropriate = isAppropriate;
            this.message = message;
            this.toxicPhrases = toxicPhrases;
        }
        
        public boolean isAppropriate() {
            return isAppropriate;
        }
        
        public String getMessage() {
            return message;
        }
        
        public List<String> getToxicPhrases() {
            return toxicPhrases;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Validation: ").append(message).append("\n");
            
            if (!toxicPhrases.isEmpty()) {
                sb.append("Inappropriate phrases found: ");
                sb.append(String.join(", ", toxicPhrases));
                sb.append("\n");
            }
            
            return sb.toString();
        }
    }
}*/

    