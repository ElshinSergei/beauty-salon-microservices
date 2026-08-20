package ru.elshin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.elshin.dto.UserResponse;
import ru.elshin.entity.User;
import ru.elshin.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "API для управления пользователями")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Создать пользователя", description = "Регистрация нового пользователя")
    @ApiResponse(responseCode = "200", description = "Пользователь создан")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @GetMapping
    @Operation(summary = "Получить список пользователей", description = "Возвращает всех зарегистрированных пользователей")
    @ApiResponse(responseCode = "200", description = "Список пользователей")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID", description = "Возвращает информацию о пользователе по его идентификатору")
    @ApiResponse(responseCode = "200", description = "Данные пользователя")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
