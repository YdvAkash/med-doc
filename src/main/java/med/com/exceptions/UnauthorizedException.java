package med.com.exceptions;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String code, String message) {
        super(code, message);
    }
    
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
