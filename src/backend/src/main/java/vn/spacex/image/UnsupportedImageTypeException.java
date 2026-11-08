package vn.spacex.image;

public class UnsupportedImageTypeException extends RuntimeException {

    public UnsupportedImageTypeException() {
        super("Tep khong phai anh PNG, JPEG hoac WebP hop le");
    }
}
