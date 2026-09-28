package com.sleapy.project.exceptions;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;

import com.sleapy.project.records.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice 
public class GlobalExceptionHandler {

    /**
     * Handles validation errors when API request parameters fail Jakarta validation constraints.
     * Returns 400 Bad Request with detailed field error information.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors().stream()
        .map(field -> new FieldError(field.getObjectName(), field.getField(), field.getDefaultMessage()))
        .toList();
    
        return ResponseEntity.badRequest().body(
            ErrorResponse.withFieldErrors(
                400, "Bad Request", "Field validation failed", request.getRequestURI(), errors
            )
        );
    }
    
    /**
     * Handles resource not found errors when a requested entity cannot be located in the database.
     * Returns 404 Not Found.
     */
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e, HttpServletRequest request) {
        return ResponseEntity.status(404).body(
            ErrorResponse.of(404, "Not Found", e.getMessage(), request.getRequestURI())
        );     
    }

    /**
     * Handles insufficient cash balance errors when a client attempts to withdraw or spend
     * more money than their current available balance.
     * Returns 422 Unprocessable Entity (business logic error).
     */
    @ExceptionHandler(InsufficientCashException.class)
    ResponseEntity<ErrorResponse> handleInsufficentCash(InsufficientCashException e, HttpServletRequest request) {
        return ResponseEntity.status(422).body(
            ErrorResponse.of(422, "Insufficient Cash", e.getMessage(), request.getRequestURI())
        );   
    }

    /**
     * Handles insufficient shares errors when a client attempts to sell more shares
     * than they currently own in their portfolio.
     * Returns 422 Unprocessable Entity (business logic error).
     */
    @ExceptionHandler(InsufficientSharesException.class)
    ResponseEntity<ErrorResponse> handleInsufficentShares(InsufficientSharesException e, HttpServletRequest request) {
        return ResponseEntity.status(422).body(
            ErrorResponse.of(422, "Insufficient Shares", e.getMessage(), request.getRequestURI())
        );   
    }

    /**
     * Handles invalid email format errors when email validation fails against the strict RFC 5322 standard.
     * The ClientValidator uses comprehensive RFC 5322 regex to enforce strict email formatting requirements,
     * including support for quoted strings, special characters, and IP address literals.
     * Returns 422 Unprocessable Entity (business logic error).
     */
    @ExceptionHandler(InvalidEmailFormatException.class)
    ResponseEntity<ErrorResponse> handleInvalidEmail(InvalidEmailFormatException e, HttpServletRequest request) {
        return ResponseEntity.status(422).body(
            ErrorResponse.of(422, "Invalid Email", e.getMessage(), request.getRequestURI())
        );   
    }

    /**
     * Catches all unexpected exceptions not handled by other handlers.
     * Returns 500 Internal Server Error for unrecognized runtime errors.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        return ResponseEntity.status(500).body(
            ErrorResponse.of(500, "Unexpected Error", "Something unexpected occured", request.getRequestURI())
        );   
    }
}
