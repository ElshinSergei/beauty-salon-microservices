package ru.elshin.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Обрабатываем только наши конфликты бронирования
    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<Map<String, Object>> handleAppointmentConflict(AppointmentConflictException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value()); // 409
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value()); // 404 Not Found
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Обрабатываем ошибки взаимодействия между микросервисами через Feign
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(FeignException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());

        // Проверяем, какой статус пришел от внешнего сервиса
        int status = ex.status();

        if (status == 404) {
            body.put("status", HttpStatus.NOT_FOUND.value());
            body.put("error", "Not Found");
            body.put("message", "Смежный сервис вернул ошибку: запрашиваемый ресурс не найден");
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }

        if (status == -1) {
            // Статус -1 означает, что сервис вообще недоступен или сработал таймаут соединения
            body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value()); // 503
            body.put("error", "Service Unavailable");
            body.put("message", "Сервис пользователей временно недоступен или превышено время ожидания ответа");
            return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
        }

        // Для всех остальных сетевых ошибок (например, 500 от user-service)
        body.put("status", HttpStatus.BAD_GATEWAY.value()); // 502
        body.put("error", "Bad Gateway");
        body.put("message", "Ошибка при обмене данными со смежным сервисом");

        return new ResponseEntity<>(body, HttpStatus.BAD_GATEWAY);
    }

    // Общий перехватчик для всех остальных непредвиденных ошибок на сервере (например, NPE или SQL-ошибки)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value()); // 500
        body.put("error", "Internal Server Error");
        // Не отдаем клиенту технический Stack Trace или системное сообщение в целях безопасности
        body.put("message", "An unexpected error occurred on the server.");

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
