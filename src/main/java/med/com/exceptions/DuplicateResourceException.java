package med.com.exceptions;

public class DuplicateResourceException extends BaseException {
    public DuplicateResourceException(String code, String message) {
        super(code, message);
    }
    
    public DuplicateResourceException(String message) {
        super("DUPLICATE_RESOURCE", message);
    }
}
