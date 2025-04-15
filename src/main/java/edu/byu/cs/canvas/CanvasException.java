package edu.byu.cs.canvas;

/**
 * Thrown whenever an error arises communicating with Canvas
 */
public class CanvasException extends Exception {

    public CanvasException() {
        super();
    }


    public CanvasException(String message) {
        super(message);
    }


    public CanvasException(String message, Throwable cause) {
        super(message, cause);
    }


    public CanvasException(Throwable cause) {
        super(cause);
    }

}
