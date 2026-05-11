package com.vicente.storage.exception.handler;

import com.vicente.storage.exception.ApiException;
import com.vicente.storage.exception.model.StandardError;
import com.vicente.storage.exception.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<StandardError> handleApiException(ApiException e, HttpServletRequest request) {
        StandardError standardError = new StandardError(
                e.getError(),
                e.getMessage(),
                e.getStatus().value(),
                getRequestId(request),
                Instant.now()
        );

        return ResponseEntity.status(e.getStatus()).body(standardError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.METHOD_ARGUMENT_NOT_VALID_ERROR;

        StandardError standardError = new StandardError(
                errorCode.value(),
                buildValidationMessage(e),
                errorCode.status().value(),
                getRequestId(request),
                Instant.now()
        );

        return ResponseEntity.status(errorCode.status()).body(standardError);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StandardError> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.CONSTRAINT_VIOLATION_ERROR;

        StandardError standardError = new StandardError(
                errorCode.value(),
                buildConstraintViolationMessage(e),
                errorCode.status().value(),
                getRequestId(request),
                Instant.now()
        );

        return ResponseEntity.status(errorCode.status()).body(standardError);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleGeneric(HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        String message = "Unexpected error occurred. Please contact support.";

        StandardError standardError = new StandardError(
                errorCode.value(),
                message,
                errorCode.status().value(),
                getRequestId(request),
                Instant.now()
        );

        return ResponseEntity.status(errorCode.status()).body(standardError);
    }

    private static String getRequestId(HttpServletRequest request) {
        return (String) request.getAttribute("requestId");
    }

    private String buildValidationMessage(MethodArgumentNotValidException e) {
        /*
            Converte a lista de FieldError em uma mensagem única:
            Percorre todos os erros de validação dos campos (@Valid),
            transforma cada erro no formato "campo: mensagem",
            remove mensagens duplicadas (caso o mesmo erro apareça mais de uma vez)
            e junta tudo em uma única String separada por "; ".
            Exemplo de saída:
                "dueDate: must be present or future; title: must not be blank"

            - stream(): percorre os erros de validação
            - map(): formata cada erro como "campo: mensagem"
            - distinct(): evita mensagens duplicadas
            - joining(): concatena tudo em uma única String separada por "; "
        */
        return e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
    }

    private String buildConstraintViolationMessage(ConstraintViolationException e) {
        return e.getConstraintViolations()
                .stream()
                .map(v -> extractField(v.getPropertyPath().toString())
                        + ": " + v.getMessage())
                .collect(Collectors.joining("; "));

    }

    private String extractField(String field) {
        if (field == null) return "unknown";

        int lastDot = field.lastIndexOf('.');
        return (lastDot != -1) ? field.substring(lastDot + 1) : field;
    }
}
