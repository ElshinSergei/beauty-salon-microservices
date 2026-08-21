package ru.elshin.client;

import org.springframework.stereotype.Component;
import ru.elshin.dto.UserDto;
import ru.elshin.exception.AppointmentConflictException;

@Component
public class UserClientFallback implements UserClient {

    @Override
    public UserDto getUserById(Long id) {
        // Здесь мы пишем поведение, если user-service недоступен.
        // Вместо падения с 500 ошибкой, мы выбрасываем понятное бизнес-исключение
        throw new AppointmentConflictException(
                "Не удалось проверить пользователя с ID " + id + ". Сервис авторизации временно недоступен."
        );
    }
}
