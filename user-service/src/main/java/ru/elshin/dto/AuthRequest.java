package ru.elshin.dto;

public record AuthRequest(
        String email,
        String password
) {}
