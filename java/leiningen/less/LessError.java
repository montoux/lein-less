package leiningen.less;

public class LessError extends RuntimeException {
    public LessError(String message, Throwable cause) {
        super(message, cause);
    }
}
