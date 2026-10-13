package com.goaltracker.controller;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice public class GlobalExceptionHandler {public record Error(String code,String message){}@ExceptionHandler(NoSuchElementException.class)ResponseEntity<Error> notFound(NoSuchElementException e){return ResponseEntity.status(404).body(new Error("NOT_FOUND",e.getMessage()));}@ExceptionHandler(MethodArgumentNotValidException.class)ResponseEntity<Error> invalid(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(new Error("VALIDATION_ERROR",e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getField()+" "+x.getDefaultMessage()).orElse("Invalid request")));}@ExceptionHandler(AiRejectedException.class)ResponseEntity<Error> rejected(AiRejectedException e){return ResponseEntity.status(422).body(new Error("AI_REJECTED",e.getMessage()));}@ExceptionHandler(IllegalArgumentException.class)ResponseEntity<Error> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(new Error("INVALID_REQUEST",e.getMessage()));}@ExceptionHandler(IllegalStateException.class)ResponseEntity<Error> state(IllegalStateException e){return ResponseEntity.status(429).body(new Error("SERVICE_UNAVAILABLE",e.getMessage()));}}
