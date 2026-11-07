package vn.spacex.auth;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("Email da duoc dang ky");
    }
}
