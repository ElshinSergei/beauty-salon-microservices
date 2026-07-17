package ru.elshin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.elshin.dto.UserDto;

// url указывает прямо на запущенный user-service (порт 8081)
@FeignClient(name = "user-service", url = "http://localhost:8081/api/v1/users")
public interface UserClient {

    // Запрос GET http://localhost:8081/api/v1/users/{id}
    @GetMapping("/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
}
