package ec.edu.ups.icc.academiceventsapi.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleViolationException extends ApiException {

    public BusinessRuleViolationException(String errorCode, String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, errorCode, message);
    }
}
