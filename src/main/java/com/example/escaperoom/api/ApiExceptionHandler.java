package com.example.escaperoom.api;

import com.example.escaperoom.api.dto.ErrorResponse;
import com.example.escaperoom.service.ApiExceptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiExceptions.NotFound.class)
    public ResponseEntity<ErrorResponse> notFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ApiExceptions.Conflict.class)
    public ResponseEntity<ErrorResponse> conflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(ApiExceptions.Gone.class)
    public ResponseEntity<ErrorResponse> gone(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(new ErrorResponse("GONE", ex.getMessage()));
    }
}
