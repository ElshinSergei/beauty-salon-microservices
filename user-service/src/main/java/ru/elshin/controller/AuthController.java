package ru.elshin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.elshin.dto.AuthRequest;
import ru.elshin.dto.AuthResponse;
import ru.elshin.dto.RegisterRequest;
import ru.elshin.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Controller", description = "API для аутентификации и регистрации")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация", description = "Регистрация нового пользователя в системе")
    @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Вход", description = "Аутентификация пользователя и получение JWT-токена")
    @ApiResponse(responseCode = "200", description = "Вход успешен, токен получен")
    @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
