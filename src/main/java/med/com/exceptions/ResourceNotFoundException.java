package med.com.exceptions;

public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String code, String message) {
        super(code, message);
    }
    
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
}
