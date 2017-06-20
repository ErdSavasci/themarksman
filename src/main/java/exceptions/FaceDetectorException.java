package exceptions;

/**
 * Created by erd_9 on 13.06.2017.
 */
public class FaceDetectorException extends Exception {
    public FaceDetectorException(){
        super();
    }

    public FaceDetectorException(String message) {
        super(message);
    }

    public FaceDetectorException(Throwable cause) {
        super(cause);
    }

    public FaceDetectorException(String message, Throwable cause) {
        super(message, cause);
    }

    protected FaceDetectorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
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
