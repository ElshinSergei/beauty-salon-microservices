package ru.elshin.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.elshin.dto.AppointmentStatusChangedEvent;

@Component
@Slf4j
public class NotificationListener {

    @RabbitListener(queues = "appointment.notifications.queue")
    public void handleStatusChangeEvent(AppointmentStatusChangedEvent event) {
        log.info("=================================================");
        log.info("ОБРАБОТКА СОБЫТИЯ ИЗ RABBITMQ");
        log.info("Запись №: {}", event.getAppointmentId());
        log.info("Изменение статуса: {} -> {}", event.getPreviousStatus(), event.getNewStatus());
        log.info("Услуга: {} | Время: {}", event.getServiceName(), event.getAppointmentTime());
        log.info("Уведомление отправлено клиенту (ID: {}) и мастеру (ID: {})",
                event.getClientId(), event.getMasterId());
        log.info("=================================================");
    }
}
