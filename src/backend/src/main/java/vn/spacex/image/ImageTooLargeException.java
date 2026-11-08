package vn.spacex.image;

public class ImageTooLargeException extends RuntimeException {

    public ImageTooLargeException() {
        super("Anh vuot qua 1 MB");
    }
}
