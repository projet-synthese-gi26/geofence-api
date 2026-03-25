package ink.yowyob.geofence.controller.advice;

import ink.yowyob.geofence.dto.response.ErrorEntity;
import ink.yowyob.geofence.exception.BadCredentialsException;
import ink.yowyob.geofence.exception.PasswordMismatchException;
import ink.yowyob.geofence.exception.UserAlreadyExistsException;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApplicationControllerAdvice {

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler({UserAlreadyExistsException.class})
    public @ResponseBody ErrorEntity handleException(UserAlreadyExistsException exception) {
        return new ErrorEntity(HttpStatus.CONFLICT.value(), exception.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({PasswordMismatchException.class})
    public @ResponseBody ErrorEntity handleException(PasswordMismatchException exception) {
        return new ErrorEntity(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({BadCredentialsException.class})
    public @ResponseBody ErrorEntity handleException(BadCredentialsException exception) {
        return new ErrorEntity(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({UsernameNotFoundException.class})
    public @ResponseBody ErrorEntity handleException(UsernameNotFoundException exception) {
        return new ErrorEntity(HttpStatus.NOT_FOUND.value(), exception.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler({ExpiredJwtException.class})
    public @ResponseBody ErrorEntity handleException(ExpiredJwtException exception) {
        return new ErrorEntity(HttpStatus.FORBIDDEN.value(), exception.getMessage());
    }

}
