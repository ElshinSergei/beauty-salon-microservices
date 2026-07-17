package ru.elshin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.elshin.entity.Appointment;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Поиск всех записей конкретного мастера
    List<Appointment> findByMasterId(Long masterId);

    // Проверка: занят ли мастер в это время
    boolean existsByMasterIdAndAppointmentTime(Long masterId, LocalDateTime appointmentTime);

    // Метод для поиска записей мастера в заданном временном интервале (от старта дня до конца)
    List<Appointment> findByMasterIdAndAppointmentTimeBetween(
            Long masterId,
            LocalDateTime start,
            LocalDateTime end
    );
}
