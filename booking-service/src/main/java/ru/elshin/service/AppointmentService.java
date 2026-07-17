package ru.elshin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.elshin.client.UserClient;
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

    public Appointment createAppointment(Appointment appointment) {
        // 1. Проверяем существование клиента в user-service
        UserDto client;
        try {
            client = userClient.getUserById(appointment.getClientId());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Client with ID " + appointment.getClientId() + " not found!");
        }

        // 2. Проверяем существование мастера в user-service
        UserDto master;
        try {
            master = userClient.getUserById(appointment.getMasterId());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Master with ID " + appointment.getMasterId() + " not found!");
        }

        // 3. Проверяем, действительно ли у мастера роль MASTER
        if (!"MASTER".equalsIgnoreCase(master.getRole())) {
            throw new AppointmentConflictException("User with ID " + appointment.getMasterId() + " is not registered as a Master!");
        }

        // 4. Проверяем занятость мастера на это время
        boolean isTimeBusy = appointmentRepository.existsByMasterIdAndAppointmentTime(
                appointment.getMasterId(),
                appointment.getAppointmentTime()
        );

        if (isTimeBusy) {
            throw new AppointmentConflictException("Master is already booked at this time!");
        }

        appointment.setStatus(AppointmentStatus.PENDING);
        return appointmentRepository.save(appointment);
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

}
