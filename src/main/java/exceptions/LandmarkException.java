package exceptions;

/**
 * Created by erd_9 on 13.06.2017.
 */
public class LandmarkException extends Exception {
    public LandmarkException(){
        super();
    }

    public LandmarkException(String message) {
        super(message);
    }

    public LandmarkException(Throwable cause) {
        super(cause);
    }

    public LandmarkException(String message, Throwable cause) {
        super(message, cause);
    }

    protected LandmarkException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
    }
}
