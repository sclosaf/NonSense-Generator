package unipd.nonsense.exception;

public class TemplateLoadException extends Exception {
    
    public TemplateLoadException(String message) {
        super(message);
    }
    
    public TemplateLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}