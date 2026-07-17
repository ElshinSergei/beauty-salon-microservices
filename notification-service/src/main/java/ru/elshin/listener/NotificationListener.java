package ru.elshin.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.elshin.dto.AppointmentEvent;

@Component
public class NotificationListener {

    @RabbitListener(queues = "appointment.notifications.queue")
    public void handleAppointmentCreated(AppointmentEvent event) {
        System.out.println("=================================================");
        System.out.println("ПОЛУЧЕНО СОБЫТИЕ ИЗ RABBITMQ!");
        System.out.println("Создана новая запись №: " + event.getAppointmentId());
        System.out.println("Отправляем уведомление клиенту ID: " + event.getClientId());
        System.out.println("Услуга: " + event.getServiceName());
        System.out.println("Время визита: " + event.getAppointmentTime());
        System.out.println("=================================================");
    }
}
