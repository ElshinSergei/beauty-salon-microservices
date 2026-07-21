package ru.elshin.dto;

import ru.elshin.entity.Role;

public record RegisterRequest(
        String email,
        String password,
        String name,
        String phone,
        Role role
) {}
