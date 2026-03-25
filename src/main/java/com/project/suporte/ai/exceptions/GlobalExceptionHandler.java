package com.project.suporte.ai.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            errors.put(fieldName, error.getDefaultMessage());
        });
        return buildResponse(HttpStatus.BAD_REQUEST, "validation_error", "Os dados enviados sao invalidos.", request, errors);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException ex, WebRequest request) {
        return buildResponse(ex.getStatus(), ex.getCode(), ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<?> handleBadRequest(Exception ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "bad_request", "A requisicao esta malformada ou incompleta.", request, Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        String path = extractPath(request);

        if ("/favicon.ico".equals(path)) {
            return ResponseEntity.noContent().build();
        }

        return buildResponse(HttpStatus.NOT_FOUND, "not_found", "O recurso solicitado nao foi encontrado.", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex, WebRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Ocorreu um erro interno inesperado.", request, Map.of());
    }

    private ResponseEntity<?> buildResponse(
            HttpStatus status,
            String code,
            String message,
            WebRequest request,
            Map<String, String> fieldErrors
    ) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                extractPath(request),
                fieldErrors
        );

        if (isSseRequest(request)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(buildSseErrorEvent(errorResponse));
        }

        return new ResponseEntity<>(errorResponse, status);
    }

    private boolean isSseRequest(WebRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private String buildSseErrorEvent(ErrorResponse errorResponse) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "error");
        payload.put("code", errorResponse.code());
        payload.put("message", errorResponse.message());
        payload.put("path", errorResponse.path());
        payload.put("fieldErrors", errorResponse.fieldErrors());
        payload.put("finished", true);
        payload.put("timestamp", errorResponse.timestamp());

        try {
            return "event: error\ndata: " + objectMapper.writeValueAsString(payload) + "\n\n";
        } catch (JsonProcessingException exception) {
            return "event: error\ndata: {\"type\":\"error\",\"message\":\"Falha ao processar o erro.\",\"finished\":true}\n\n";
        }
    }
}
