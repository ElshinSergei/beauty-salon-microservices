package ru.elshin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.elshin.entity.User;
import ru.elshin.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor // Автоматически внедрит UserRepository через конструктор
public class UserService {

    private final UserRepository userRepository;

    public User createUser(User user) {
        // Простая проверка: если email занят, бросим ошибку
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered!");
        }
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }
}
