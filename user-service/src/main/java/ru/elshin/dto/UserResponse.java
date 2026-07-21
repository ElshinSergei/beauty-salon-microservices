package ru.elshin.dto;

import ru.elshin.entity.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role
) {}
