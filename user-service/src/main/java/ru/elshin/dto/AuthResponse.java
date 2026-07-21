package ru.elshin.dto;

import ru.elshin.entity.Role;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        Role role
) {}
