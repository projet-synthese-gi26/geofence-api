package ink.yowyob.geofence.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PasswordMismatchException extends RuntimeException {
    public PasswordMismatchException() {
        super("fields Password and password_confirmation not matched");
    }
}
