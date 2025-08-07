package com.test.Exceptions;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends ApiException {
    public InvalidRequestException(String message) {
        super("Invalid request: " + message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST; // 400
    }
}
