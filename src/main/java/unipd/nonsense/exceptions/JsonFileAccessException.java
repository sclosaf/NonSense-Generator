package unipd.nonsense.exception;

public class JsonFileAccessException extends Exception {
    
    private final String filePath;
    
    public JsonFileAccessException(String filePath, String message) {
        super("Error accessing JSON file '" + filePath + "': " + message);
        this.filePath = filePath;
    }
    
    public JsonFileAccessException(String filePath, String message, Throwable cause) {
        super("Error accessing JSON file '" + filePath + "': " + message, cause);
        this.filePath = filePath;
    }
    
    public String getFilePath() {
        return filePath;
    }
}