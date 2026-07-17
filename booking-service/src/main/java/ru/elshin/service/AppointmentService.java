package ru.elshin.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.elshin.client.UserClient;
import ru.elshin.config.RabbitMQConfig;
import ru.elshin.dto.AppointmentEvent;
import ru.elshin.dto.UserDto;
import ru.elshin.entity.Appointment;
import ru.elshin.entity.AppointmentStatus;
import ru.elshin.exception.AppointmentConflictException;
import ru.elshin.exception.ResourceNotFoundException;
import ru.elshin.repository.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserClient userClient; // Внедряем наш Feign-клиент
    private final RabbitTemplate rabbitTemplate; // Внедряем шаблон для работы с RabbitMQ

    @Transactional
    public Appointment createAppointment(Appointment appointment) {
        // 1. Проверяем существование клиента в user-service
        UserDto client = userClient.getUserById(appointment.getClientId());
        // 2. Проверяем существование мастера в user-service
        UserDto master = userClient.getUserById(appointment.getMasterId());

        // 3. Проверяем, действительно ли у мастера роль MASTER
        if (!"MASTER".equalsIgnoreCase(master.getRole())) {
            throw new AppointmentConflictException("Пользователь с ID " + appointment.getMasterId() + " не является мастером!");
        }

        // 4. Проверяем занятость мастера на это время
        boolean isTimeBusy = appointmentRepository.existsByMasterIdAndAppointmentTime(
                appointment.getMasterId(),
                appointment.getAppointmentTime()
        );

        if (isTimeBusy) {
            throw new AppointmentConflictException("Мастер уже занят на это время!");
        }

        appointment.setStatus(AppointmentStatus.PENDING);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Формируем событие для брокера
        AppointmentEvent event = new AppointmentEvent(
                savedAppointment.getId(),
                savedAppointment.getClientId(),
                savedAppointment.getServiceName(),
                savedAppointment.getAppointmentTime().toString()
        );

        // Отправляем асинхронно в обменник с ключом маршрутизации
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                event
        );

        return savedAppointment;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    // метод для получения ВСЕХ записей мастера
    public List<Appointment> getAppointmentsByMaster(Long masterId) {
        return appointmentRepository.findByMasterId(masterId);
    }

    // метод для фильтрации по конкретному дню
    public List<Appointment> getAppointmentsByMasterAndDate(Long masterId, LocalDate date) {
        // Начало дня: 2026-07-20T00:00
        LocalDateTime startOfDay = date.atStartOfDay();

        // Конец дня: 2026-07-20T23:59:59.999999999
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return appointmentRepository.findByMasterIdAndAppointmentTimeBetween(masterId, startOfDay, endOfDay);
    }

    /**
     * Подтверждение записи мастером
     */
    @Transactional
    public Appointment confirmAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запись с ID " + id + " не найдена"));

        // Валидация: подтвердить можно только запись в статусе PENDING
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new AppointmentConflictException(
                    "Нельзя подтвердить запись в статусе " + appointment.getStatus()
            );
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointmentRepository.save(appointment);
    }

    /**
     * Отмена записи
     */
    @Transactional
    public Appointment cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запись с ID " + id + " не найдена"));

        // Валидация: нельзя отменить то, что уже выполнено (COMPLETED) или отменено (CANCELLED)
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AppointmentConflictException("Нельзя отменить уже выполненную запись!");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentConflictException("Запись уже была отменена ранее!");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        return appointmentRepository.save(appointment);
    }
}
